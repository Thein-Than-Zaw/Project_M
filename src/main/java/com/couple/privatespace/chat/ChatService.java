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
    public record Message(UUID id,long sequence,UUID senderId,UUID clientMessageId,String body,String type,String giftCode,String giftName,String giftAssetPath,
            Instant createdAt,Instant deliveredAt,Instant readAt) { }
    public record Receipt(UUID id,long sequence,Instant deliveredAt,Instant readAt) { }
    public record State(long latestSequence,long unreadCount) { }
    public record Page(UUID selfId,String selfName,PairingService.Member partner,String timezone,List<Message> items,
            boolean hasMore,long latestSequence,long unreadCount) { }
    public record Send(UUID clientMessageId,String body,String type,String giftCode,UUID recipientId) { }
    public record Gift(String code,String name,String pack,String assetPath) { }
    public record Catalogue(List<Gift> items,List<String> packs,long total,boolean hasMore) { }
    public record GiftHistory(List<Message> items,boolean hasMore) { }
    public record Ack(List<UUID> messageIds) { }
    public record Changed(List<UUID> members,String type,Message message,List<Receipt> receipts) { }

    /** DEV-06 GIFT-01/03: only bundled catalogue assets, actor's active space, bounded queries. */
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Catalogue gifts(UUID actor,String pack,String q,int offset) {
        active(actor);q=q==null?"":q.strip().toLowerCase(Locale.ROOT);pack=pack==null?"":pack;
        if(offset<0||offset>100000||q.codePointCount(0,q.length())>80||pack.length()>40)throw new ApiFailure(400,"INVALID_GIFT_SEARCH","Choose a valid sticker search.");
        var args=new Object[]{pack,pack,q,q};
        String where=" WHERE active AND (?='' OR pack=?) AND (?='' OR strpos(lower(name),?)>0)";
        long total=jdbc.queryForObject("SELECT count(*) FROM gift_catalog"+where,Long.class,args);
        var items=jdbc.query("SELECT code,name,pack,asset_path FROM gift_catalog"+where+" ORDER BY code LIMIT 60 OFFSET ?",
            (r,n)->new Gift(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),pack,pack,q,q,offset);
        return new Catalogue(items,jdbc.queryForList("SELECT DISTINCT pack FROM gift_catalog WHERE active ORDER BY pack",String.class),total,offset+items.size()<total);
    }
    @Transactional(readOnly=true)
    public GiftHistory giftHistory(UUID actor,String direction,Long before) {
        var space=active(actor);
        if(!Set.of("all","sent","received").contains(direction)||before!=null&&before<=0)throw new ApiFailure(400,"INVALID_GIFT_HISTORY","Choose sent or received stickers.");
        String filter=direction.equals("all")?"":direction.equals("sent")?" AND sender_id=?":" AND sender_id<>?";
        var args=new ArrayList<Object>();args.add(space.id());args.add(before==null?Long.MAX_VALUE:before);if(!direction.equals("all"))args.add(actor);
        var rows=jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND sequence<? AND type='GIFT'"+filter+" ORDER BY sequence DESC LIMIT 51",(r,n)->map(r),args.toArray());
        return new GiftHistory(rows.stream().limit(50).toList(),rows.size()>50);
    }

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
        String type=command.type()==null?"TEXT":command.type();boolean gift=type.equals("GIFT");
        if(!Set.of("TEXT","GIFT").contains(type)||(!gift&&command.giftCode()!=null))throw new ApiFailure(400,"INVALID_MESSAGE_TYPE","Choose a supported message type.");
        if(command.recipientId()!=null)throw new ApiFailure(400,"INVALID_RECIPIENT","Your partner is selected by the server.");
        String body=gift&&command.body()==null?"":command.body();
        // Unicode space separators (including NBSP) are blank even when String.isBlank excludes them.
        if(!gift&&(body==null||body.codePoints().allMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c)||c==0xFEFF)))throw new ApiFailure(400,"EMPTY_MESSAGE","Write a message first.");
        if(body.codePointCount(0,body.length())>(gift?500:4000)||body.codePoints().anyMatch(c->c==0||Character.isSurrogate((char)c)&&c<=65535))throw new ApiFailure(400,gift?"GIFT_NOTE_TOO_LONG":"MESSAGE_TOO_LONG",gift?"Keep your note within 500 characters.":"Use up to 4,000 valid Unicode characters.");
        var space=active(actor);lock(space.id());
        var existing=jdbc.query("SELECT * FROM chat_message WHERE sender_id=? AND client_message_id=?",(r,n)->map(r),actor,command.clientMessageId());
        if(!existing.isEmpty()) {
            var old=existing.getFirst();
            if(!old.body().equals(body)||!old.type().equals(type)||!Objects.equals(old.giftCode(),command.giftCode()))throw new ApiFailure(409,"MESSAGE_CONFLICT","This message identifier was already used for different content.");
            return existing.getFirst();
        }
        Gift selected=null;
        if(gift){
            var choices=jdbc.query("SELECT code,name,pack,asset_path FROM gift_catalog WHERE code=? AND active FOR SHARE",(r,n)->new Gift(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),command.giftCode());
            if(choices.isEmpty())throw new ApiFailure(400,"INVALID_GIFT","This gift is not available.");selected=choices.getFirst();
        }
        limiter.check(actor,true);
        long sequence=jdbc.queryForObject("UPDATE chat_conversation SET last_sequence=last_sequence+1,updated_at=clock_timestamp() WHERE space_id=? RETURNING last_sequence",Long.class,space.id());
        var message=jdbc.queryForObject("INSERT INTO chat_message(id,space_id,sequence,sender_id,client_message_id,body,type,gift_code,gift_name,gift_asset_path) VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING *",
            (r,n)->map(r),UUID.randomUUID(),space.id(),sequence,actor,command.clientMessageId(),body,type,selected==null?null:selected.code(),selected==null?null:selected.name(),selected==null?null:selected.assetPath());
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
        return new Message(r.getObject("id",UUID.class),r.getLong("sequence"),r.getObject("sender_id",UUID.class),r.getObject("client_message_id",UUID.class),r.getString("body"),r.getString("type"),r.getString("gift_code"),r.getString("gift_name"),r.getString("gift_asset_path"),r.getTimestamp("created_at").toInstant(),instant(r,"delivered_at"),instant(r,"read_at"));
    }
    private static Instant instant(java.sql.ResultSet r,String field)throws java.sql.SQLException {var t=r.getTimestamp(field);return t==null?null:t.toInstant();}
    private static Instant instant(java.sql.ResultSet r,int field)throws java.sql.SQLException {var t=r.getTimestamp(field);return t==null?null:t.toInstant();}
}
