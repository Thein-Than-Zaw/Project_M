package com.couple.privatespace.call;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.pairing.PairingService;
import com.couple.privatespace.realtime.SocketRegistry;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

/** DEV-08 CALL-01–03: space-serialized metadata; never persist SDP, candidates or media. */
@Service
public class CallService {
    public record Command(UUID eventId,UUID callId,UUID clientId,String mode,String reason,UUID negotiationId,
                          String sdp,String candidate,String sdpMid,Integer sdpMLineIndex,Boolean micEnabled,Boolean cameraEnabled,String emoji) { }
    public record View(UUID id,UUID callerId,UUID calleeId,String callerClient,String calleeClient,String mode,String state,
                       Instant createdAt,Instant ringDeadline,Instant acceptedAt,Instant connectedAt,Instant recoveryDeadline,
                       Instant endedAt,String endReason,long version,Instant serverTime) { }
    public record Current(View call) { }
    public record Leave(UUID clientId,UUID eventId) { }
    record Row(UUID space,View view,String callerSocket,String calleeSocket,Instant callerSeen,Instant calleeSeen,Instant connectDeadline,UUID negotiation,UUID callerClient,UUID calleeClient) { }
    public record State(UUID eventId,View call) { }
    public record Relay(UUID eventId,String type,UUID recipient,String socketId,UUID callId,UUID negotiationId,Map<String,Object> payload) { }
    private record Seen(String hash,long at) { }
    private record Window(long start,int count) { }
    private static final Set<String> LIVE=Set.of("RINGING","CONNECTING","ACTIVE","RECONNECTING");
    private static final Set<String> DURABLE=Set.of("invite","accept","decline","cancel","end");
    private static final Set<String> REACTIONS=Set.of("❤️","😂","👍","👏","🥰","🎉");
    private final JdbcTemplate jdbc;
    private final PairingService pairing;
    private final AccountStore accounts;
    private final SocketRegistry sockets;
    private final SimpUserRegistry users;
    private final ApplicationEventPublisher events;
    private final Map<String,Seen> signals=new ConcurrentHashMap<>();
    private final Map<String,Window> rates=new ConcurrentHashMap<>();

    public CallService(JdbcTemplate jdbc,PairingService pairing,AccountStore accounts,SocketRegistry sockets,SimpUserRegistry users,ApplicationEventPublisher events){
        this.jdbc=jdbc;this.pairing=pairing;this.accounts=accounts;this.sockets=sockets;this.users=users;this.events=events;
    }
    private ApiFailure error(int status,String code,String message){return new ApiFailure(status,code,message);}
    private UUID active(UUID actor){var space=pairing.current(actor);if(!space.state().equals("ACTIVE"))throw error(403,"PAIRING_REQUIRED","Connect with your partner first.");return space.id();}
    private void lock(UUID space){jdbc.queryForObject("SELECT version FROM couple_space WHERE id=? FOR UPDATE",Long.class,space);}
    private UUID lockActor(UUID actor){UUID space=active(actor);lock(space);if(!space.equals(active(actor)))throw error(403,"FORBIDDEN","This call is not available.");return space;}
    private Row row(UUID space,UUID id){var rows=jdbc.query("SELECT * FROM call_session WHERE id=? AND space_id=?",(r,n)->map(r),id,space);if(rows.isEmpty())throw error(404,"CALL_NOT_FOUND","This call is no longer available.");return rows.getFirst();}
    private Row live(UUID space){var rows=jdbc.query("SELECT * FROM call_session WHERE space_id=? AND ended_at IS NULL",(r,n)->map(r),space);return rows.isEmpty()?null:rows.getFirst();}
    private void participant(Row row,UUID actor){if(!row.view.callerId.equals(actor)&&!row.view.calleeId.equals(actor))throw error(404,"CALL_NOT_FOUND","This call is not available.");}
    private boolean caller(Row row,UUID actor){return row.view.callerId.equals(actor);}
    private void selected(Row row,UUID actor,String socket,UUID client){
        participant(row,actor);boolean a=caller(row,actor);
        if(!Objects.equals(a?row.callerSocket:row.calleeSocket,socket)||!Objects.equals(a?row.callerClient:row.calleeClient,client))throw error(409,"CALL_ELSEWHERE","This call is being handled in another tab.");
    }
    private boolean online(UUID actor){return accounts.byId(actor).map(account->{var user=users.getUser(account.email());return user!=null&&user.getSessions().stream().anyMatch(s->sockets.validChat(s.getId())&&s.getSubscriptions().stream().anyMatch(sub->sub.getDestination().equals("/user/queue/calls")));}).orElse(false);}
    private void changed(UUID space,UUID id){events.publishEvent(new State(UUID.randomUUID(),row(space,id).view));}
    private View finish(Row row,String state,String reason){
        jdbc.update("UPDATE call_session SET state=?,end_reason=?,ended_at=clock_timestamp(),recovery_deadline=NULL,version=version+1 WHERE id=? AND ended_at IS NULL",state,reason,row.view.id);
        changed(row.space,row.view.id);return row(row.space,row.view.id).view;
    }
    private boolean expired(Row row){
        Instant now=Instant.now();var v=row.view;if(!LIVE.contains(v.state))return true;
        if(v.state.equals("RINGING")&&!v.ringDeadline.isAfter(now)){finish(row,"MISSED","RING_TIMEOUT");return true;}
        if(v.state.equals("CONNECTING")&&row.connectDeadline!=null&&!row.connectDeadline.isAfter(now)){finish(row,"FAILED","CONNECT_TIMEOUT");return true;}
        if(v.recoveryDeadline!=null&&!v.recoveryDeadline.isAfter(now)){finish(row,"FAILED","CONNECTION_LOST");return true;}return false;
    }
    private String hash(String operation,Command c){return RegistrationService.digest(operation+"|"+c.callId+"|"+c.clientId+"|"+c.mode+"|"+c.reason);}
    private UUID receipt(UUID actor,UUID event,String hash){var rows=jdbc.queryForList("SELECT call_id,request_hash FROM call_command_receipt WHERE actor_id=? AND event_id=?",actor,event);if(rows.isEmpty())return null;if(!rows.getFirst().get("request_hash").equals(hash))throw error(409,"CALL_RETRY_CONFLICT","This action identifier was used for a different call action.");return (UUID)rows.getFirst().get("call_id");}
    private void saveReceipt(UUID actor,Command c,String operation,String hash,UUID call){jdbc.update("INSERT INTO call_command_receipt(actor_id,event_id,call_id,operation,request_hash) VALUES (?,?,?,?,?)",actor,c.eventId,call,operation,hash);}

    @Transactional
    public View command(UUID actor,String socket,String operation,Command c){
        if(c==null||c.clientId==null||c.eventId==null)throw error(400,"INVALID_CALL_COMMAND","A call client and action identifier are required.");
        if(!sockets.validChat(socket))throw error(403,"CALL_SESSION_EXPIRED","Sign in again before calling.");
        UUID space=lockActor(actor);String hash=hash(operation,c);
        if(DURABLE.contains(operation)){UUID old=receipt(actor,c.eventId,hash);if(old!=null){var oldRow=row(space,old);participant(oldRow,actor);return oldRow.view;}}
        if(operation.equals("invite")){
            if(!Set.of("VOICE","VIDEO").contains(c.mode==null?"":c.mode))throw error(400,"INVALID_CALL_MODE","Choose a voice or video call.");
            Row current=live(space);if(current!=null&&!expired(current))throw error(409,"CALL_BUSY","A call is already in progress.");
            UUID peer=pairing.current(actor).members().stream().map(PairingService.Member::id).filter(id->!id.equals(actor)).findFirst().orElseThrow();
            if(!online(peer))throw error(409,"PARTNER_OFFLINE","Your partner is not currently connected for calls.");
            UUID id=UUID.randomUUID();Instant now=Instant.now();
            jdbc.update("INSERT INTO call_session(id,space_id,caller_id,callee_id,mode,state,caller_client,caller_socket,created_at,ring_deadline,caller_seen) VALUES (?,?,?,?,?,'RINGING',?,?,?,?,?)",id,space,actor,peer,c.mode,c.clientId,socket,Timestamp.from(now),Timestamp.from(now.plusSeconds(30)),Timestamp.from(now));
            saveReceipt(actor,c,operation,hash,id);changed(space,id);return row(space,id).view;
        }
        if(c.callId==null)throw error(400,"CALL_ID_REQUIRED","Choose an existing call.");
        Row row=row(space,c.callId);participant(row,actor);
        if(expired(row))return row(space,c.callId).view;
        boolean a=caller(row,actor);String own=a?"caller":"callee";
        switch(operation){
            case "accept" -> {
                if(a)throw error(403,"CALLEE_ONLY","Only your partner can answer this call.");
                if(row.view.state.equals("RINGING"))jdbc.update("UPDATE call_session SET state='CONNECTING',callee_client=?,callee_socket=?,callee_seen=clock_timestamp(),accepted_at=clock_timestamp(),connect_deadline=clock_timestamp()+INTERVAL '30 seconds',version=version+1 WHERE id=?",c.clientId,socket,c.callId);
                else {selected(row,actor,socket,c.clientId);if(!Set.of("CONNECTING","ACTIVE","RECONNECTING").contains(row.view.state))throw error(409,"CALL_STATE","This call cannot be answered now.");}
                saveReceipt(actor,c,operation,hash,c.callId);changed(space,c.callId);
            }
            case "decline" -> {if(a)throw error(403,"CALLEE_ONLY","Only your partner can decline this call.");if(!row.view.state.equals("RINGING"))throw error(409,"CALL_STATE","This call is no longer ringing.");finish(row,"DECLINED","DECLINED");saveReceipt(actor,c,operation,hash,c.callId);}
            case "cancel" -> {selected(row,actor,socket,c.clientId);if(!a||!row.view.state.equals("RINGING"))throw error(409,"CALL_STATE","Only the outgoing ringing call can be cancelled.");finish(row,"CANCELLED","CANCELLED");saveReceipt(actor,c,operation,hash,c.callId);}
            case "end" -> {selected(row,actor,socket,c.clientId);if(row.view.state.equals("RINGING"))finish(row,a?"CANCELLED":"DECLINED",a?"CANCELLED":"DECLINED");else finish(row,"ENDED","ENDED");saveReceipt(actor,c,operation,hash,c.callId);}
            case "resume" -> {
                UUID expected=a?row.callerClient:row.calleeClient;String previous=a?row.callerSocket:row.calleeSocket;
                if(!c.clientId.equals(expected)||!Objects.equals(previous,socket)&&sockets.validChat(previous))throw error(409,"CALL_ELSEWHERE","This call is active in another tab.");
                jdbc.update("UPDATE call_session SET "+own+"_socket=?,"+own+"_seen=clock_timestamp(),"+own+"_connected=FALSE,state=CASE WHEN state='RINGING' THEN state ELSE 'RECONNECTING' END,version=version+1 WHERE id=?",socket,c.callId);
                if(!row.view.state.equals("RINGING"))recover(row(space,c.callId),actor);
                changed(space,c.callId);
            }
            case "heartbeat" -> {selected(row,actor,socket,c.clientId);jdbc.update("UPDATE call_session SET "+own+"_seen=clock_timestamp() WHERE id=?",c.callId);}
            case "disconnected" -> {selected(row,actor,socket,c.clientId);if(!row.view.state.equals("RINGING"))recover(row,actor);}
            case "connected" -> {
                selected(row,actor,socket,c.clientId);if(row.view.state.equals("RINGING")||row.negotiation==null)throw error(409,"CALL_STATE","Complete call negotiation first.");
                if(!row.negotiation.equals(c.negotiationId))throw error(409,"STALE_NEGOTIATION","This connection report belongs to an older negotiation.");
                jdbc.update("UPDATE call_session SET "+own+"_connected=TRUE,"+own+"_seen=clock_timestamp() WHERE id=?",c.callId);
                if(jdbc.queryForObject("SELECT caller_connected AND callee_connected FROM call_session WHERE id=?",Boolean.class,c.callId))jdbc.update("UPDATE call_session SET state='ACTIVE',connected_at=COALESCE(connected_at,clock_timestamp()),recovery_deadline=NULL,version=version+1 WHERE id=?",c.callId);
                changed(space,c.callId);
            }
            case "offer","answer","ice","media","reaction" -> relay(row,actor,socket,operation,c);
            default -> throw error(400,"INVALID_CALL_COMMAND","This call action is not supported.");
        }
        return row(space,c.callId).view;
    }
    private void recover(Row row,UUID actor){String own=caller(row,actor)?"caller":"callee";if(jdbc.update("UPDATE call_session SET state='RECONNECTING',"+own+"_connected=FALSE,recovery_deadline=COALESCE(recovery_deadline,clock_timestamp()+INTERVAL '15 seconds'),version=version+1 WHERE id=? AND (state<>'RECONNECTING' OR "+own+"_connected OR recovery_deadline IS NULL)",row.view.id)>0)changed(row.space,row.view.id);}
    private void relay(Row row,UUID actor,String socket,String op,Command c){
        selected(row,actor,socket,c.clientId);if(row.view.state.equals("RINGING"))throw error(409,"CALL_STATE","Wait until your partner answers.");
        if(c.negotiationId==null)throw error(400,"NEGOTIATION_REQUIRED","A current negotiation identifier is required.");
        boolean a=caller(row,actor);Map<String,Object> payload=new LinkedHashMap<>();
        if(op.equals("offer")||op.equals("answer")){
            if(a!=op.equals("offer"))throw error(403,"CALL_ROLE","Only the caller offers and the answering tab answers.");
            if(c.sdp==null||c.sdp.length()>16000||!c.sdp.startsWith("v=0")||c.sdp.indexOf(0)>=0)throw error(400,"INVALID_SDP","This call description is invalid or too large.");payload.put("sdp",c.sdp);
        }else if(op.equals("ice")){
            if(c.candidate==null||!c.candidate.startsWith("candidate:")||c.candidate.length()>4000||c.candidate.indexOf(0)>=0||c.sdpMid!=null&&c.sdpMid.length()>64||c.sdpMLineIndex!=null&&(c.sdpMLineIndex<0||c.sdpMLineIndex>16))throw error(400,"INVALID_ICE","This connection candidate is invalid.");
            payload.put("candidate",c.candidate);payload.put("sdpMid",c.sdpMid);payload.put("sdpMLineIndex",c.sdpMLineIndex);
        }else if(op.equals("reaction")){
            if(!row.view.state.equals("ACTIVE"))throw error(409,"CALL_STATE","Reactions are available during a connected call.");
            if(c.emoji==null||!REACTIONS.contains(c.emoji))throw error(400,"INVALID_CALL_REACTION","Choose an available call reaction.");payload.put("emoji",c.emoji);
        }else{if(c.micEnabled==null||c.cameraEnabled==null)throw error(400,"INVALID_MEDIA_STATE","Provide your current microphone and camera state.");payload.put("micEnabled",c.micEnabled);payload.put("cameraEnabled",row.view.mode.equals("VIDEO")&&c.cameraEnabled);}
        if(!op.equals("offer")&&!c.negotiationId.equals(row.negotiation))throw error(409,"STALE_NEGOTIATION","This connection attempt has been replaced.");
        long now=System.currentTimeMillis();rates.entrySet().removeIf(e->e.getValue().start<now-120000);
        rates.compute(socket,(id,w)->{if(w==null||now-w.start>=60000)return new Window(now,1);if(w.count>=240)throw error(429,"CALL_RATE_LIMIT","Too many call signals. Try again shortly.");return new Window(w.start,w.count+1);});
        signals.entrySet().removeIf(e->e.getValue().at<now-120000);if(signals.size()>2048)throw error(429,"CALL_RATE_LIMIT","Call signaling is busy. Retry shortly.");
        String key=actor+":"+c.eventId,digest=RegistrationService.digest(op+"|"+c.callId+"|"+c.clientId+"|"+c.negotiationId+"|"+payload);
        Seen prior=signals.get(key);if(prior!=null){if(!prior.hash.equals(digest))throw error(409,"CALL_RETRY_CONFLICT","This signal identifier was already used.");return;}
        // CALL-UI05: transient, selected-session-only effects; bounded independently of ICE traffic.
        if(op.equals("reaction"))rates.compute(socket+":reaction",(id,w)->{if(w==null||now-w.start>=10000)return new Window(now,1);if(w.count>=6)throw error(429,"CALL_REACTION_LIMIT","Wait a moment before sending another reaction.");return new Window(w.start,w.count+1);});
        if(op.equals("offer"))jdbc.update("UPDATE call_session SET negotiation_id=? WHERE id=?",c.negotiationId,c.callId);
        events.publishEvent(new Relay(c.eventId,"call."+op,a?row.view.calleeId:row.view.callerId,a?row.calleeSocket:row.callerSocket,c.callId,c.negotiationId,Collections.unmodifiableMap(payload)));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCommit(){signals.put(key,new Seen(digest,now));}});
    }

    @Transactional public Current current(UUID actor){UUID space=lockActor(actor);Row row=live(space);if(row==null||expired(row))return new Current(null);return new Current(row.view);}
    @Transactional(readOnly=true) public View get(UUID actor,UUID id){Row row=row(active(actor),id);participant(row,actor);return row.view;}
    @Transactional(readOnly=true) public List<View> recent(UUID actor){return jdbc.query("SELECT * FROM call_session WHERE space_id=? ORDER BY created_at DESC,id DESC LIMIT 20",(r,n)->map(r).view,active(actor));}
    @Transactional public View leave(UUID actor,UUID id,Leave body){
        if(body==null||body.clientId==null||body.eventId==null)throw error(400,"INVALID_CALL_COMMAND","Provide the selected call client.");UUID space=lockActor(actor);Row row=row(space,id);participant(row,actor);
        UUID client=caller(row,actor)?row.callerClient:row.calleeClient;if(!body.clientId.equals(client))throw error(409,"CALL_ELSEWHERE","This call belongs to another tab.");
        if(!LIVE.contains(row.view.state))return row.view;return finish(row,row.view.state.equals("RINGING")?"CANCELLED":"ENDED","PAGE_LEFT");
    }
    public List<UUID> pending(){return jdbc.queryForList("SELECT id FROM call_session WHERE ended_at IS NULL",UUID.class);}
    @Transactional public void tick(UUID id){
        var spaces=jdbc.queryForList("SELECT space_id FROM call_session WHERE id=? AND ended_at IS NULL",UUID.class,id);if(spaces.isEmpty())return;UUID space=spaces.getFirst();lock(space);Row row=row(space,id);if(expired(row))return;
        Instant now=Instant.now();boolean a=!sockets.validChat(row.callerSocket)||row.callerSeen.isBefore(now.minusSeconds(15));boolean b=row.calleeSocket!=null&&(!sockets.validChat(row.calleeSocket)||row.calleeSeen==null||row.calleeSeen.isBefore(now.minusSeconds(15)));
        if(row.view.state.equals("RINGING")){if(a)finish(row,"FAILED","CALLER_DISCONNECTED");return;}
        if(a||b){if(a)recover(row,row.view.callerId);if(b)recover(row(space,id),row.view.calleeId);}
    }
    @Transactional public void revoked(Set<String> ids){
        if(ids.isEmpty())return;
        for(UUID id:pending()){
            var spaces=jdbc.queryForList("SELECT space_id FROM call_session WHERE id=?",UUID.class,id);if(spaces.isEmpty())continue;UUID space=spaces.getFirst();lock(space);Row row=row(space,id);
            if(LIVE.contains(row.view.state)&&(ids.contains(row.callerSocket)||row.calleeSocket!=null&&ids.contains(row.calleeSocket)))finish(row,"FAILED","SESSION_ENDED");
        }
    }
    private Row map(ResultSet r)throws SQLException{
        UUID callerClient=r.getObject("caller_client",UUID.class),calleeClient=r.getObject("callee_client",UUID.class);
        var view=new View(r.getObject("id",UUID.class),r.getObject("caller_id",UUID.class),r.getObject("callee_id",UUID.class),RegistrationService.digest(callerClient.toString()),calleeClient==null?null:RegistrationService.digest(calleeClient.toString()),r.getString("mode"),r.getString("state"),time(r,"created_at"),time(r,"ring_deadline"),time(r,"accepted_at"),time(r,"connected_at"),time(r,"recovery_deadline"),time(r,"ended_at"),r.getString("end_reason"),r.getLong("version"),Instant.now());
        return new Row(r.getObject("space_id",UUID.class),view,r.getString("caller_socket"),r.getString("callee_socket"),time(r,"caller_seen"),time(r,"callee_seen"),time(r,"connect_deadline"),r.getObject("negotiation_id",UUID.class),callerClient,calleeClient);
    }
    private Instant time(ResultSet r,String column)throws SQLException{var t=r.getTimestamp(column);return t==null?null:t.toInstant();}
}
