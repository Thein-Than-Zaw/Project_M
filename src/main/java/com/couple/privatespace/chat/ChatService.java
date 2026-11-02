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
    public record Attachment(UUID id,String name,String kind,String mime,long size,Double duration,Integer width,Integer height,String url,String thumbnail) {}
    public record Reaction(UUID userId,String emoji) { }
    public record Quote(UUID id,UUID senderId,String body,String type,String giftName,boolean unavailable) { }
    public record Message(UUID id,long sequence,UUID senderId,UUID clientMessageId,String body,String type,String giftCode,String giftName,String giftAssetPath,
            Instant createdAt,Instant deliveredAt,Instant readAt,UUID replyToId,Quote reply,List<Reaction> reactions,Instant deletedAt,long version,boolean hidden,UUID attachmentId,Attachment attachment) {
        public Message(UUID id,long sequence,UUID senderId,UUID clientMessageId,String body,String type,String giftCode,String giftName,String giftAssetPath,Instant createdAt,Instant deliveredAt,Instant readAt,UUID replyToId,Quote reply,List<Reaction> reactions,Instant deletedAt,long version,boolean hidden){this(id,sequence,senderId,clientMessageId,body,type,giftCode,giftName,giftAssetPath,createdAt,deliveredAt,readAt,replyToId,reply,reactions,deletedAt,version,hidden,null,null);}
        public Message(UUID id,long sequence,UUID senderId,UUID clientMessageId,String body,String type,String giftCode,String giftName,String giftAssetPath,Instant createdAt,Instant deliveredAt,Instant readAt){this(id,sequence,senderId,clientMessageId,body,type,giftCode,giftName,giftAssetPath,createdAt,deliveredAt,readAt,null,null,List.of(),null,0,false);}
    }
    public record Receipt(UUID id,long sequence,Instant deliveredAt,Instant readAt) { }
    public record State(long latestSequence,long unreadCount,long changeVersion) { }
    public record Page(UUID selfId,String selfName,PairingService.Member partner,String timezone,List<Message> items,
            boolean hasMore,long latestSequence,long unreadCount,long changeVersion,String selfAvatarUrl) { }
    public record Send(UUID clientMessageId,String body,String type,String giftCode,UUID recipientId,UUID replyToId,UUID attachmentId) {
        public Send(UUID key,String body,String type,String giftCode,UUID recipientId,UUID replyToId){this(key,body,type,giftCode,recipientId,replyToId,null);}
        public Send(UUID key,String body,String type,String giftCode,UUID recipientId){this(key,body,type,giftCode,recipientId,null);}
    }
    public record Snapshot(List<Message> items,List<UUID> removedIds) { }
    public record React(String emoji) { }
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
        var args=new ArrayList<Object>();args.add(space.id());args.add(before==null?Long.MAX_VALUE:before);args.add(actor);if(!direction.equals("all"))args.add(actor);
        var rows=jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND sequence<? AND type='GIFT' AND deleted_at IS NULL AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=chat_message.id AND h.user_id=?)"+filter+" ORDER BY sequence DESC LIMIT 51",(r,n)->map(r),args.toArray());
        return new GiftHistory(hydrate(space.id(),actor,rows.stream().limit(50).toList()),rows.size()>50);
    }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public State state(UUID actor) {var space=active(actor);return state(space.id(),actor);}
    private State state(UUID space,UUID actor) {
        long sequence=jdbc.queryForObject("SELECT COALESCE((SELECT last_sequence FROM chat_conversation WHERE space_id=?),0)",Long.class,space);
        long unread=jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=? AND sender_id<>? AND read_at IS NULL AND deleted_at IS NULL AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=chat_message.id AND h.user_id=?)",Long.class,space,actor,actor);
        return new State(sequence,unread,jdbc.queryForObject("SELECT COALESCE((SELECT change_version FROM chat_conversation WHERE space_id=?),0)",Long.class,space));
    }
    /** CHAT-02: bounded keyset pages, stable server order, no other user's cursor/space inputs. */
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page history(UUID actor,Long before,Long after) {
        if((before!=null&&after!=null)||(before!=null&&before<=0)||(after!=null&&after<0))throw new ApiFailure(400,"INVALID_CURSOR","Choose one valid history cursor.");
        var space=active(actor);var state=state(space.id(),actor);List<Message> rows;
        if(after!=null)rows=jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND sequence>? AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=chat_message.id AND h.user_id=?) ORDER BY sequence LIMIT 51",(r,n)->map(r),space.id(),after,actor);
        else rows=jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND sequence<? AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=chat_message.id AND h.user_id=?) ORDER BY sequence DESC LIMIT 51",(r,n)->map(r),space.id(),before==null?Long.MAX_VALUE:before,actor);
        boolean more=rows.size()>50;var page=new ArrayList<>(rows.stream().limit(50).toList());
        if(after==null)Collections.reverse(page);
        return new Page(actor,space.members().stream().filter(m->m.id().equals(actor)).findFirst().orElseThrow().displayName(),space.members().stream().filter(m->!m.id().equals(actor)).findFirst().orElseThrow(),space.timezone(),hydrate(space.id(),actor,page),more,state.latestSequence(),state.unreadCount(),state.changeVersion(),space.members().stream().filter(m->m.id().equals(actor)).findFirst().orElseThrow().avatarUrl());
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
        String type=command.type()==null?"TEXT":command.type();boolean gift=type.equals("GIFT"),file=type.equals("FILE");
        if(!Set.of("TEXT","GIFT","FILE").contains(type)||(!gift&&command.giftCode()!=null))throw new ApiFailure(400,"INVALID_MESSAGE_TYPE","Choose a supported message type.");
        if(file!=(command.attachmentId()!=null))throw new ApiFailure(400,"INVALID_ATTACHMENT","Choose one uploaded file for this message.");
        if(command.recipientId()!=null)throw new ApiFailure(400,"INVALID_RECIPIENT","Your partner is selected by the server.");
        String body=(gift||file)&&command.body()==null?"":command.body();
        // Unicode space separators (including NBSP) are blank even when String.isBlank excludes them.
        if(!gift&&!file&&(body==null||body.codePoints().allMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c)||c==0xFEFF)))throw new ApiFailure(400,"EMPTY_MESSAGE","Write a message first.");
        if(body.codePointCount(0,body.length())>(gift?500:4000)||body.codePoints().anyMatch(c->c==0||Character.isSurrogate((char)c)&&c<=65535))throw new ApiFailure(400,gift?"GIFT_NOTE_TOO_LONG":"MESSAGE_TOO_LONG",gift?"Keep your note within 500 characters.":"Use up to 4,000 valid Unicode characters.");
        var space=active(actor);lockUpload(jdbc,actor,command.clientMessageId());ensureNotCancelled(jdbc,actor,command.clientMessageId());lock(space.id());
        var existing=jdbc.query("SELECT * FROM chat_message WHERE sender_id=? AND client_message_id=?",(r,n)->map(r),actor,command.clientMessageId());
        if(!existing.isEmpty()) {
            var old=existing.getFirst();
            var fingerprint=new ArrayList<Object>(Arrays.asList(body,type,command.giftCode(),command.replyToId()));if(file)fingerprint.add(command.attachmentId());fingerprint.add(old.id());
            boolean same=jdbc.queryForObject("SELECT request_hash=sha256(convert_to(jsonb_build_array(?::text,?::text,?::text,?::uuid"+(file?",?::uuid":"")+")::text,'UTF8')) FROM chat_message WHERE id=?",Boolean.class,fingerprint.toArray());
            if(!same)throw new ApiFailure(409,"MESSAGE_CONFLICT","This message identifier was already used for different content.");
            if(hidden(actor,old.id()))return copy(old,"",null,null,List.of(),true);
            return hydrate(space.id(),actor,List.of(old)).getFirst();
        }
        if(command.replyToId()!=null){Message original;try{original=visible(space.id(),actor,command.replyToId());}catch(ApiFailure unavailable){throw new ApiFailure(409,"REPLY_UNAVAILABLE","The original message is no longer available. Send without reply or cancel and try again.");}if(original.deletedAt()!=null)throw new ApiFailure(409,"REPLY_UNAVAILABLE","The original message is no longer available. Send without reply or cancel and try again.");}
        Gift selected=null;
        if(gift){
            var choices=jdbc.query("SELECT code,name,pack,asset_path FROM gift_catalog WHERE code=? AND active FOR SHARE",(r,n)->new Gift(r.getString(1),r.getString(2),r.getString(3),r.getString(4)),command.giftCode());
            if(choices.isEmpty())throw new ApiFailure(400,"INVALID_GIFT","This gift is not available.");selected=choices.getFirst();
        }
        if(file){var uploads=jdbc.queryForList("SELECT id FROM chat_attachment WHERE id=? AND space_id=? AND uploader_id=? AND client_message_id=? AND state='UPLOADED' FOR UPDATE",command.attachmentId(),space.id(),actor,command.clientMessageId());if(uploads.isEmpty())throw new ApiFailure(409,"ATTACHMENT_UNAVAILABLE","This upload is unavailable or cancelled. Choose it again.");}
        limiter.check(actor,true);
        long sequence=jdbc.queryForObject("UPDATE chat_conversation SET last_sequence=last_sequence+1,change_version=change_version+1,updated_at=clock_timestamp() WHERE space_id=? RETURNING last_sequence",Long.class,space.id());
        var message=jdbc.queryForObject("INSERT INTO chat_message(id,space_id,sequence,sender_id,client_message_id,body,type,gift_code,gift_name,gift_asset_path,reply_to_id,attachment_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) RETURNING *",
            (r,n)->map(r),UUID.randomUUID(),space.id(),sequence,actor,command.clientMessageId(),body,type,selected==null?null:selected.code(),selected==null?null:selected.name(),selected==null?null:selected.assetPath(),command.replyToId(),command.attachmentId());
        if(file)jdbc.update("UPDATE chat_attachment SET state='ATTACHED',message_id=? WHERE id=?",message.id(),command.attachmentId());
        var result=hydrate(space.id(),actor,List.of(message)).getFirst();events.publishEvent(new Changed(memberIds(space),"message.created",file?copy(result,result.body(),result.replyToId(),null,List.of(),false):message,List.of()));return result;
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
        String condition=(read?"read_at IS NULL":"delivered_at IS NULL")+" AND deleted_at IS NULL";
        var receipts=jdbc.query("UPDATE chat_message SET delivered_at=COALESCE(delivered_at,clock_timestamp())"+(read?",read_at=clock_timestamp()":"")+" WHERE space_id=? AND sender_id<>? AND id IN ("+placeholders+") AND "+condition+" RETURNING id,sequence,delivered_at,read_at",
            (r,n)->new Receipt(r.getObject(1,UUID.class),r.getLong(2),instant(r,3),instant(r,4)),args.toArray());
        if(!receipts.isEmpty())events.publishEvent(new Changed(memberIds(space),"message.receipt",null,receipts));return receipts(actor,ids);
    }
    private static final Set<String> EMOJI=Set.of("❤️","😂","😮","😢","👍","🥰");
    private boolean hidden(UUID actor,UUID id){return jdbc.queryForObject("SELECT count(*) FROM chat_message_hidden WHERE message_id=? AND user_id=?",Integer.class,id,actor)>0;}
    private Message visible(UUID space,UUID actor,UUID id){return jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND id=? AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=chat_message.id AND h.user_id=?)",(r,n)->map(r),space,id,actor).stream().findFirst().orElseThrow(()->new ApiFailure(404,"MESSAGE_UNAVAILABLE","This message is not available."));}
    private Message copy(Message m,String body,UUID replyTo,Quote quote,List<Reaction> reactions,boolean hidden){return new Message(m.id(),m.sequence(),m.senderId(),m.clientMessageId(),body,m.type(),hidden?null:m.giftCode(),hidden?null:m.giftName(),hidden?null:m.giftAssetPath(),m.createdAt(),m.deliveredAt(),m.readAt(),replyTo,quote,reactions,m.deletedAt(),m.version(),hidden,hidden||m.deletedAt()!=null?null:m.attachmentId(),hidden||m.deletedAt()!=null?null:m.attachment());}
    private List<Message> hydrate(UUID space,UUID actor,List<Message> rows){
        if(rows.isEmpty())return List.of();var ids=rows.stream().map(Message::id).toList();var replies=rows.stream().map(Message::replyToId).filter(Objects::nonNull).distinct().toList();var quotes=new HashMap<UUID,Quote>();
        if(!replies.isEmpty()){
            var args=new ArrayList<Object>();args.add(space);args.add(actor);args.addAll(replies);
            jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=chat_message.id AND h.user_id=?) AND id IN ("+String.join(",",Collections.nCopies(replies.size(),"?"))+")",(org.springframework.jdbc.core.RowCallbackHandler)r->{var original=map(r);boolean deleted=original.deletedAt()!=null;String body=deleted?null:original.body();if(body!=null&&body.codePointCount(0,body.length())>200)body=body.substring(0,body.offsetByCodePoints(0,200))+"…";quotes.put(original.id(),new Quote(original.id(),original.senderId(),body,original.type(),deleted?null:original.giftName(),deleted));},args.toArray());
        }
        var reactions=new HashMap<UUID,List<Reaction>>();var args=new ArrayList<Object>();args.add(space);args.addAll(ids);
        jdbc.query("SELECT message_id,user_id,emoji FROM chat_message_reaction WHERE space_id=? AND message_id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+") ORDER BY user_id",(org.springframework.jdbc.core.RowCallbackHandler)r->reactions.computeIfAbsent(r.getObject(1,UUID.class),key->new ArrayList<>()).add(new Reaction(r.getObject(2,UUID.class),r.getString(3))),args.toArray());
        var attachments=new HashMap<UUID,Attachment>();
        jdbc.query("SELECT * FROM chat_attachment WHERE space_id=? AND message_id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+") AND state='ATTACHED'",(org.springframework.jdbc.core.RowCallbackHandler)r->attachments.put(r.getObject("message_id",UUID.class),attachment(r)),args.toArray());
        rows=rows.stream().map(m->new Message(m.id(),m.sequence(),m.senderId(),m.clientMessageId(),m.body(),m.type(),m.giftCode(),m.giftName(),m.giftAssetPath(),m.createdAt(),m.deliveredAt(),m.readAt(),m.replyToId(),m.reply(),m.reactions(),m.deletedAt(),m.version(),m.hidden(),m.attachmentId(),attachments.get(m.id()))).toList();
        return rows.stream().map(m->copy(m,m.body(),m.deletedAt()==null?m.replyToId():null,m.replyToId()==null||m.deletedAt()!=null?null:quotes.getOrDefault(m.replyToId(),new Quote(m.replyToId(),null,null,null,null,true)),m.deletedAt()!=null?List.of():List.copyOf(reactions.getOrDefault(m.id(),List.of())),false)).toList();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Snapshot snapshots(UUID actor,List<UUID> requested){
        if(requested==null||requested.isEmpty()||requested.size()>50||requested.stream().anyMatch(Objects::isNull))throw new ApiFailure(400,"INVALID_MESSAGE_IDS","Choose up to 50 messages.");var space=active(actor);var ids=requested.stream().distinct().toList();var args=new ArrayList<Object>();args.add(space.id());args.addAll(ids);
        var rows=jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")",(r,n)->map(r),args.toArray());if(rows.size()!=ids.size())throw new ApiFailure(403,"INVALID_MESSAGE_SCOPE","These messages are not available.");
        var hiddenArgs=new ArrayList<Object>();hiddenArgs.add(space.id());hiddenArgs.add(actor);hiddenArgs.addAll(ids);var hiddenIds=new HashSet<>(jdbc.queryForList("SELECT message_id FROM chat_message_hidden WHERE space_id=? AND user_id=? AND message_id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")",UUID.class,hiddenArgs.toArray()));var visible=new ArrayList<Message>();var removed=new ArrayList<UUID>();for(var m:rows){if(hiddenIds.contains(m.id()))removed.add(m.id());else visible.add(m);}return new Snapshot(hydrate(space.id(),actor,visible),List.copyOf(removed));
    }
    private void changed(PairingService.Space space,List<UUID> recipients,String type){jdbc.update("UPDATE chat_conversation SET change_version=change_version+1,updated_at=clock_timestamp() WHERE space_id=?",space.id());events.publishEvent(new Changed(recipients,type,null,List.of()));}
    @Transactional
    public void hide(UUID actor,UUID id){var space=active(actor);limiter.check(actor,false);lock(space.id());var rows=jdbc.queryForList("SELECT id FROM chat_message WHERE space_id=? AND id=?",UUID.class,space.id(),id);if(rows.isEmpty())throw new ApiFailure(404,"MESSAGE_UNAVAILABLE","This message is not available.");if(jdbc.update("INSERT INTO chat_message_hidden(space_id,message_id,user_id) VALUES (?,?,?) ON CONFLICT DO NOTHING",space.id(),id,actor)>0)changed(space,List.of(actor),"message.hidden");}
    @Transactional
    public Message delete(UUID actor,UUID id){
        var space=active(actor);limiter.check(actor,false);lock(space.id());var message=visible(space.id(),actor,id);if(!message.senderId().equals(actor))throw new ApiFailure(403,"MESSAGE_DELETE_FORBIDDEN","You can delete for everyone only messages you sent.");
        if(message.deletedAt()==null)tombstone(space,id);return hydrate(space.id(),actor,List.of(visible(space.id(),actor,id))).getFirst();
    }
    /** CHAT-06: cancellation covers every own send, including a hidden, already committed message. */
    @Transactional
    public void cancelSend(UUID actor,UUID id){
        var space=active(actor);lock(space.id());var rows=jdbc.query("SELECT * FROM chat_message WHERE space_id=? AND id=? AND sender_id=?",(r,n)->map(r),space.id(),id,actor);
        if(rows.isEmpty())throw new ApiFailure(404,"MESSAGE_UNAVAILABLE","This message is not available.");if(rows.getFirst().deletedAt()==null)tombstone(space,id);
    }
    private void tombstone(PairingService.Space space,UUID id){
        jdbc.update("UPDATE chat_attachment SET state='DELETED' WHERE message_id=? AND space_id=? AND state='ATTACHED'",id,space.id());jdbc.update("UPDATE chat_message SET attachment_id=NULL,body='',gift_code=NULL,gift_name=NULL,gift_asset_path=NULL,reply_to_id=NULL,deleted_at=clock_timestamp(),version=version+1 WHERE id=?",id);jdbc.update("DELETE FROM chat_message_reaction WHERE message_id=?",id);changed(space,memberIds(space),"message.deleted");
    }
    @Transactional
    public Message react(UUID actor,UUID id,React request){
        if(request==null||request.emoji()!=null&&!EMOJI.contains(request.emoji()))throw new ApiFailure(400,"INVALID_REACTION","Choose one of the available reactions.");var space=active(actor);limiter.check(actor,false);lock(space.id());var message=visible(space.id(),actor,id);if(message.deletedAt()!=null)throw new ApiFailure(409,"MESSAGE_DELETED","This message was deleted. Reactions are no longer available.");
        int changed=request.emoji()==null?jdbc.update("DELETE FROM chat_message_reaction WHERE message_id=? AND user_id=?",id,actor):jdbc.update("INSERT INTO chat_message_reaction(space_id,message_id,user_id,emoji) VALUES (?,?,?,?) ON CONFLICT(message_id,user_id) DO UPDATE SET emoji=EXCLUDED.emoji,updated_at=clock_timestamp() WHERE chat_message_reaction.emoji<>EXCLUDED.emoji",space.id(),id,actor,request.emoji());
        if(changed>0){jdbc.update("UPDATE chat_message SET version=version+1 WHERE id=?",id);changed(space,memberIds(space),"message.reacted");}return hydrate(space.id(),actor,List.of(visible(space.id(),actor,id))).getFirst();
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
        return new Message(r.getObject("id",UUID.class),r.getLong("sequence"),r.getObject("sender_id",UUID.class),r.getObject("client_message_id",UUID.class),r.getString("body"),r.getString("type"),r.getString("gift_code"),r.getString("gift_name"),r.getString("gift_asset_path"),r.getTimestamp("created_at").toInstant(),instant(r,"delivered_at"),instant(r,"read_at"),r.getObject("reply_to_id",UUID.class),null,List.of(),instant(r,"deleted_at"),r.getLong("version"),false,r.getObject("attachment_id",UUID.class),null);
    }
    static void lockUpload(JdbcTemplate jdbc,UUID actor,UUID key){jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>)connection->{try(var statement=connection.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,0))")){statement.setString(1,"chat-upload:"+actor+":"+key);statement.execute();}return null;});}
    static void ensureNotCancelled(JdbcTemplate jdbc,UUID actor,UUID key){if(jdbc.queryForObject("SELECT count(*) FROM chat_upload_intent WHERE uploader_id=? AND client_message_id=? AND cancelled_at IS NOT NULL",Integer.class,actor,key)>0)throw new ApiFailure(409,"UPLOAD_CANCELLED","This send was cancelled. Choose a new file or record again.");}
    static Attachment attachment(java.sql.ResultSet r)throws java.sql.SQLException{var id=r.getObject("id",UUID.class);String kind=r.getString("kind"),url="/api/chat/assets/"+id;return new Attachment(id,r.getString("name"),kind,r.getString("mime"),r.getLong("bytes"),(Double)r.getObject("duration"),(Integer)r.getObject("width"),(Integer)r.getObject("height"),url,Set.of("IMAGE","VIDEO").contains(kind)?url+"?thumbnail=true":null);}
    private static Instant instant(java.sql.ResultSet r,String field)throws java.sql.SQLException {var t=r.getTimestamp(field);return t==null?null:t.toInstant();}
    private static Instant instant(java.sql.ResultSet r,int field)throws java.sql.SQLException {var t=r.getTimestamp(field);return t==null?null:t.toInstant();}
}
