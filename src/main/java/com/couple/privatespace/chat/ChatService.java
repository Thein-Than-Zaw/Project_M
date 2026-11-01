package com.couple.privatespace.chat;

import com.couple.privatespace.identity.ApiFailure;
import com.couple.privatespace.pairing.PairingService;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class ChatService {
    private final JdbcTemplate jdbc;
    private final PairingService pairing;
    private final ApplicationEventPublisher events;
    private final ChatLimiter limiter;
    public ChatService(JdbcTemplate jdbc,PairingService pairing,ApplicationEventPublisher events,ChatLimiter limiter) {
        this.jdbc=jdbc;this.pairing=pairing;this.events=events;this.limiter=limiter;
    }
    public record Message(UUID id,long sequence,UUID senderId,UUID clientMessageId,String body,
            Instant createdAt,Instant deliveredAt,Instant readAt) { }
    public record Receipt(UUID id,long sequence,Instant deliveredAt,Instant readAt) { }
    public record State(long latestSequence,long unreadCount) { }
    public record Page(UUID selfId,String selfName,PairingService.Member partner,String timezone,List<Message> items,
            boolean hasMore,long latestSequence,long unreadCount) { }
    public record Send(UUID clientMessageId,String body) { }
    public record Ack(List<UUID> messageIds) { }
    public record Changed(List<UUID> members,String type,Message message,List<Receipt> receipts) { }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public State state(UUID actor) {var space=active(actor);return state(space.id(),actor);}
    private State state(UUID space,UUID actor) {
        long sequence=jdbc.queryForObject("SELECT COALESCE((SELECT last_sequence FROM chat_conversation WHERE space_id=?),0)",Long.class,space);
        long unread=jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=? AND sender_id<>? AND read_at IS NULL",Long.class,space,actor);
        return new State(sequence,unread);
    }
    /** CHAT-02: bounded keyset pages, stable server order, no other user's cursor/space inputs. */
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page history(UUID actor,Long before,Long after) {
        if((before!=null&&after!=null)||(before!=null&&before<=0)||(after!=null&&after<0))throw new ApiFailure(400,"INVALID_CURSOR","Choose one valid history cursor.");
        var space=active(actor);var state=state(space.id(),actor);List<Message> rows;
        if(after!=null)rows=jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND sequence>? ORDER BY sequence LIMIT 51",(r,n)->map(r),space.id(),after);
        else rows=jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND sequence<? ORDER BY sequence DESC LIMIT 51",(r,n)->map(r),space.id(),before==null?Long.MAX_VALUE:before);
        boolean more=rows.size()>50;var page=new ArrayList<>(rows.stream().limit(50).toList());
        if(after==null)Collections.reverse(page);
        return new Page(actor,space.members().stream().filter(m->m.id().equals(actor)).findFirst().orElseThrow().displayName(),space.members().stream().filter(m->!m.id().equals(actor)).findFirst().orElseThrow(),space.timezone(),page,more,state.latestSequence(),state.unreadCount());
    }
    @Transactional(readOnly=true)
    public List<Receipt> receipts(UUID actor,List<UUID> ids) {
        if(ids==null||ids.isEmpty()||ids.size()>50||ids.stream().anyMatch(Objects::isNull))throw new ApiFailure(400,"INVALID_ACK","Choose between 1 and 50 message identifiers.");
        var space=active(actor);var unique=ids.stream().distinct().toList();var args=new ArrayList<Object>();args.add(space.id());args.addAll(unique);
        var rows=jdbc.query("SELECT id,sequence,delivered_at,read_at FROM chat_message WHERE space_id=? AND id IN ("+String.join(",",Collections.nCopies(unique.size(),"?"))+")",
            (r,n)->new Receipt(r.getObject(1,UUID.class),r.getLong(2),instant(r,3),instant(r,4)),args.toArray());
        if(rows.size()!=unique.size())throw new ApiFailure(403,"INVALID_MESSAGE_SCOPE","These message receipts are not available.");return rows;
    }
    /** CHAT-02 / AC-F04-02/03: dedup, allocation and insert are one transaction; rollback has no SENT. */
    @Transactional
    public Message send(UUID actor,Send command) {
        if(command.clientMessageId()==null)throw new ApiFailure(400,"INVALID_MESSAGE_ID","Retry with a valid message identifier.");
        String body=command.body();
        // Unicode space separators (including NBSP) are blank even when String.isBlank excludes them.
        if(body==null||body.codePoints().allMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c)||c==0xFEFF))throw new ApiFailure(400,"EMPTY_MESSAGE","Write a message first.");
        if(body.codePointCount(0,body.length())>4000||body.codePoints().anyMatch(c->c==0||Character.isSurrogate((char)c)&&c<=65535))throw new ApiFailure(400,"MESSAGE_TOO_LONG","Use up to 4,000 valid Unicode characters.");
        var space=active(actor);lock(space.id());
        var existing=jdbc.query("SELECT * FROM chat_message WHERE sender_id=? AND client_message_id=?",(r,n)->map(r),actor,command.clientMessageId());
        if(!existing.isEmpty()) {
            if(!existing.getFirst().body().equals(body))throw new ApiFailure(409,"MESSAGE_CONFLICT","This message identifier was already used for different text.");
            return existing.getFirst();
        }
        limiter.check(actor,true);
        long sequence=jdbc.queryForObject("UPDATE chat_conversation SET last_sequence=last_sequence+1,updated_at=clock_timestamp() WHERE space_id=? RETURNING last_sequence",Long.class,space.id());
        var message=jdbc.queryForObject("INSERT INTO chat_message(id,space_id,sequence,sender_id,client_message_id,body) VALUES (?,?,?,?,?,?) RETURNING *",
            (r,n)->map(r),UUID.randomUUID(),space.id(),sequence,actor,command.clientMessageId(),body);
        events.publishEvent(new Changed(memberIds(space),"message.created",message,List.of()));return message;
    }
    /** CHAT-03 / AC-F04-06: recipient-only explicit per-message receipts never mark unseen older pages. */
    @Transactional
    public List<Receipt> acknowledge(UUID actor,Ack command,boolean read) {
        if(command.messageIds()==null||command.messageIds().isEmpty()||command.messageIds().size()>50||command.messageIds().stream().anyMatch(Objects::isNull))throw new ApiFailure(400,"INVALID_ACK","Acknowledge between 1 and 50 messages.");
        var ids=command.messageIds().stream().distinct().toList();var space=active(actor);limiter.check(actor,false);lock(space.id());
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        var args=new ArrayList<Object>();args.add(space.id());args.add(actor);args.addAll(ids);
        int count=jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=? AND sender_id<>? AND id IN ("+placeholders+")",Integer.class,args.toArray());
        if(count!=ids.size())throw new ApiFailure(403,"INVALID_RECIPIENT","You can acknowledge only messages sent to you.");
        String condition=read?"read_at IS NULL":"delivered_at IS NULL";
        var receipts=jdbc.query("UPDATE chat_message SET delivered_at=COALESCE(delivered_at,clock_timestamp())"+(read?",read_at=clock_timestamp()":"")+" WHERE space_id=? AND sender_id<>? AND id IN ("+placeholders+") AND "+condition+" RETURNING id,sequence,delivered_at,read_at",
            (r,n)->new Receipt(r.getObject(1,UUID.class),r.getLong(2),instant(r,3),instant(r,4)),args.toArray());
        if(!receipts.isEmpty())events.publishEvent(new Changed(memberIds(space),"message.receipt",null,receipts));return receipts(actor,ids);
    }
    private PairingService.Space active(UUID actor) {
        var space=pairing.current(actor);if(!space.state().equals("ACTIVE"))throw new ApiFailure(403,"PAIRING_REQUIRED","Connect with your partner first.");return space;
    }
    private void lock(UUID space) {
        jdbc.update("INSERT INTO chat_conversation(space_id) VALUES (?) ON CONFLICT DO NOTHING",space);
        jdbc.queryForObject("SELECT last_sequence FROM chat_conversation WHERE space_id=? FOR UPDATE",Long.class,space);
    }
    private List<UUID> memberIds(PairingService.Space space){return space.members().stream().map(PairingService.Member::id).toList();}
    private Message map(java.sql.ResultSet r)throws java.sql.SQLException {
        return new Message(r.getObject("id",UUID.class),r.getLong("sequence"),r.getObject("sender_id",UUID.class),r.getObject("client_message_id",UUID.class),r.getString("body"),r.getTimestamp("created_at").toInstant(),instant(r,"delivered_at"),instant(r,"read_at"));
    }
    private static Instant instant(java.sql.ResultSet r,String field)throws java.sql.SQLException {var t=r.getTimestamp(field);return t==null?null:t.toInstant();}
    private static Instant instant(java.sql.ResultSet r,int field)throws java.sql.SQLException {var t=r.getTimestamp(field);return t==null?null:t.toInstant();}
}
