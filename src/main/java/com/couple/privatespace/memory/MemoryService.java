package com.couple.privatespace.memory;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.pairing.PairingService;
import com.couple.privatespace.home.HomeService;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.*;

/** DEV-07 MEM-01–03: shared authorisation, immutable upload bytes, atomic publication and 72h Trash. */
@Service
public class MemoryService {
    public record Asset(UUID id,String name,String kind,String mime,long size,String sha256,Double duration,Integer width,Integer height,String url,String thumbnail) { }
    public record Entry(UUID id,String title,String caption,LocalDate memoryDate,String state,UUID createdBy,UUID updatedBy,UUID deletedBy,Instant createdAt,Instant updatedAt,Instant deletedAt,Instant expiresAt,long version,boolean favorite,List<Asset> assets) { }
    public record Edit(String title,String caption,LocalDate memoryDate,List<UUID> assetIds,Long version) { }
    public record Page(List<Entry> items,UUID nextCursor,String today) { }
    public record Version(Long version) { }
    public record Favorite(boolean favorite) { }
    public record TrashPreview(String snapshot,int count,int pending) { }
    public record EmptyTrash(String snapshot,boolean confirmed) { }
    public record Download(org.springframework.core.io.Resource resource,String mime,String name,long size,boolean attachment) { }
    public record Published(UUID space,UUID actor,UUID memoryId) { }
    private final JdbcTemplate jdbc;private final PairingService pairing;private final MemoryFiles files;private final ApplicationEventPublisher events;
    public MemoryService(JdbcTemplate jdbc,PairingService pairing,MemoryFiles files,ApplicationEventPublisher events){this.jdbc=jdbc;this.pairing=pairing;this.files=files;this.events=events;}
    private PairingService.Space active(UUID actor){var s=pairing.current(actor);if(!s.state().equals("ACTIVE"))throw new ApiFailure(403,"PAIRING_REQUIRED","Connect with your partner first.");return s;}
    private PairingService.Space lock(UUID actor){var s=active(actor);jdbc.queryForObject("SELECT version FROM couple_space WHERE id=? FOR UPDATE",Long.class,s.id());return active(actor);}
    private ApiFailure missing(){return new ApiFailure(404,"MEMORY_NOT_FOUND","This memory is no longer available. Reload your memories.");}
    private ApiFailure stale(){return new ApiFailure(409,"VERSION_CONFLICT","This memory changed. Reload it before saving again.");}
    private void changed(UUID space){events.publishEvent(new HomeService.Changed(jdbc.queryForList("SELECT user_id FROM couple_membership WHERE space_id=?",UUID.class,space),"memory.updated"));}
    private void version(Long version){if(version==null||version<0)throw new ApiFailure(400,"VERSION_REQUIRED","Reload to get the current memory version.");}
    @Transactional
    public Asset registerUpload(UUID actor,UUID key,MemoryFiles.Validated file){
        var space=lock(actor);String digest=RegistrationService.digest(file.sha()+"|"+file.size()+"|"+file.mime()+"|"+file.name());
        UUID old=receipt(actor,key,"UPLOAD",digest);
        if(old!=null){files.discardStaged(file.id());var rows=jdbc.query("SELECT * FROM memory_asset WHERE id=? AND state='READY'",(r,n)->asset(r),old);if(rows.isEmpty())throw new ApiFailure(410,"UPLOAD_EXPIRED","This upload was removed. Select the file again.");return rows.getFirst();}
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){files.discardStaged(file.id());if(status==STATUS_ROLLED_BACK)try{files.delete(file.id());}catch(IOException ignored){/* orphan reconciliation after24h */}}});
        try{files.promote(file);}catch(IOException error){throw new ApiFailure(507,"STORAGE_UNAVAILABLE","The file could not be stored. Retry this upload.");}
        jdbc.update("INSERT INTO memory_asset(id,space_id,uploaded_by,original_name,kind,mime,byte_size,sha256,duration,width,height) VALUES (?,?,?,?,?,?,?,?,?,?,?)",file.id(),space.id(),actor,file.name(),file.kind(),file.mime(),file.size(),file.sha(),file.duration(),file.width(),file.height());
        saveReceipt(actor,key,"UPLOAD",digest,file.id());return assetById(file.id());
    }
    private UUID receipt(UUID actor,UUID key,String operation,String digest){
        if(key==null)throw new ApiFailure(400,"SAVE_KEY_REQUIRED","Use a valid retry identifier.");
        var rows=jdbc.queryForList("SELECT target_id,request_hash FROM memory_receipt WHERE actor_id=? AND request_key=? AND operation=?",actor,key,operation);
        if(rows.isEmpty())return null;if(!digest.equals(rows.getFirst().get("request_hash")))throw new ApiFailure(409,"IDEMPOTENCY_CONFLICT","This retry identifier was used for different content.");return (UUID)rows.getFirst().get("target_id");
    }
    @Transactional
    public void discardUpload(UUID actor,UUID id){var space=lock(actor);if(jdbc.update("UPDATE memory_asset SET state='DISCARDED' WHERE id=? AND space_id=? AND uploaded_by=? AND memory_id IS NULL",id,space.id(),actor)!=1)throw new ApiFailure(404,"UPLOAD_NOT_FOUND","This unpublished upload is not available.");}
    private void saveReceipt(UUID actor,UUID key,String operation,String hash,UUID id){jdbc.update("INSERT INTO memory_receipt(actor_id,request_key,operation,request_hash,target_id) VALUES (?,?,?,?,?)",actor,key,operation,hash,id);}
    private Edit validate(Edit body,PairingService.Space space){
        String title=body.title()==null?"":body.title().strip(),caption=body.caption()==null?"":body.caption();
        if(title.codePoints().allMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c))||title.codePointCount(0,title.length())>120)throw new ApiFailure(400,"INVALID_TITLE","Use a title of 1 to 120 characters.");
        if(caption.codePointCount(0,caption.length())>2000||title.indexOf(0)>=0||caption.indexOf(0)>=0)throw new ApiFailure(400,"INVALID_CAPTION","Keep the caption within 2,000 valid characters.");
        if(body.memoryDate()==null||body.memoryDate().getYear()<1||body.memoryDate().getYear()>9999||body.memoryDate().isAfter(LocalDate.now(ZoneId.of(space.timezone()))))throw new ApiFailure(400,"INVALID_MEMORY_DATE","Choose today or an earlier date in Myanmar time.");
        var ids=body.assetIds();if(ids==null||ids.isEmpty()||ids.size()>10||ids.stream().anyMatch(Objects::isNull)||new HashSet<>(ids).size()!=ids.size())throw new ApiFailure(400,"INVALID_ASSETS","Add between 1 and 10 different files.");
        return new Edit(title,caption,body.memoryDate(),ids,body.version());
    }
    @Transactional
    public Entry create(UUID actor,UUID key,Edit raw){
        var space=lock(actor);var body=validate(raw,space);
        String hash=RegistrationService.digest(body.title().length()+":"+body.title()+body.memoryDate()+body.caption().length()+":"+body.caption()+body.assetIds());
        UUID old=receipt(actor,key,"CREATE",hash);if(old!=null)return entry(space.id(),actor,old,false);
        UUID id=UUID.randomUUID();checkAssets(space.id(),actor,id,body.assetIds());
        jdbc.update("INSERT INTO memory(id,space_id,title,caption,memory_date,created_by,updated_by) VALUES (?,?,?,?,?,?,?)",id,space.id(),body.title(),body.caption(),body.memoryDate(),actor,actor);
        attach(id,body.assetIds());saveReceipt(actor,key,"CREATE",hash,id);changed(space.id());events.publishEvent(new Published(space.id(),actor,id));return entry(space.id(),actor,id,false);
    }
    private void checkAssets(UUID space,UUID actor,UUID memory,List<UUID> ids){
        for(UUID id:ids){Integer count=jdbc.queryForObject("SELECT count(*) FROM memory_asset WHERE id=? AND space_id=? AND state='READY' AND (memory_id=? OR (memory_id IS NULL AND uploaded_by=? AND created_at>clock_timestamp()-INTERVAL '24 hours'))",Integer.class,id,space,memory,actor);if(count!=1)throw new ApiFailure(400,"ASSET_UNAVAILABLE","A selected file is unavailable or belongs to another draft. Re-upload it.");try{if(!files.available(id))throw new IOException("Missing object");}catch(IOException error){throw new ApiFailure(503,"ASSET_UNAVAILABLE","A stored file is unavailable. Your memory was not changed.");}}
    }
    private void attach(UUID memory,List<UUID> ids){for(int i=0;i<ids.size();i++)jdbc.update("UPDATE memory_asset SET memory_id=?,position=? WHERE id=?",memory,i,ids.get(i));}
    @Transactional
    public Entry edit(UUID actor,UUID id,Edit raw){
        var space=lock(actor);var body=validate(raw,space);version(body.version());var current=entry(space.id(),actor,id,false);if(current.version()!=body.version())throw stale();checkAssets(space.id(),actor,id,body.assetIds());
        for(var old:current.assets())if(!body.assetIds().contains(old.id()))jdbc.update("UPDATE memory_asset SET memory_id=NULL,state='DISCARDED' WHERE id=?",old.id());
        attach(id,body.assetIds());jdbc.update("UPDATE memory SET title=?,caption=?,memory_date=?,updated_by=?,updated_at=clock_timestamp(),version=version+1 WHERE id=?",body.title(),body.caption(),body.memoryDate(),actor,id);changed(space.id());return entry(space.id(),actor,id,false);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page list(UUID actor,String filter,UUID before){
        var space=active(actor);if(!Set.of("ALL","IMAGE","VIDEO","DOCUMENT","FAVORITES","TRASH").contains(filter))throw new ApiFailure(400,"INVALID_FILTER","Choose a valid memory filter.");
        var args=new ArrayList<Object>();args.add(actor);args.add(space.id());String where=" AND m.state='"+(filter.equals("TRASH")?"TRASHED":"ACTIVE")+"'";
        if(filter.equals("TRASH"))where+=" AND m.expires_at>clock_timestamp()";
        if(Set.of("IMAGE","VIDEO","DOCUMENT").contains(filter)){where+=" AND EXISTS(SELECT 1 FROM memory_asset a WHERE a.memory_id=m.id AND a.state='READY' AND a.kind=?)";args.add(filter);}
        if(filter.equals("FAVORITES")){where+=" AND EXISTS(SELECT 1 FROM memory_favorite f WHERE f.memory_id=m.id AND f.user_id=?)";args.add(actor);}
        if(before!=null){var cursor=jdbc.queryForList("SELECT memory_date FROM memory WHERE id=? AND space_id=?",before,space.id());if(cursor.isEmpty())throw new ApiFailure(400,"CURSOR_EXPIRED","The list changed. Reload your memories.");where+=" AND (m.memory_date,m.id)<(CAST(? AS date),CAST(? AS uuid))";args.add(cursor.getFirst().get("memory_date"));args.add(before);}
        var rows=jdbc.query(select()+" WHERE m.space_id=?"+where+" ORDER BY m.memory_date DESC,m.id DESC LIMIT 25",(r,n)->map(r),args.toArray());
        var page=rows.stream().limit(24).toList();return new Page(page,rows.size()>24?page.getLast().id():null,LocalDate.now(ZoneId.of(space.timezone())).toString());
    }
    @Transactional(readOnly=true) public Entry get(UUID actor,UUID id,boolean trash){return entry(active(actor).id(),actor,id,trash);}
    private String select(){return "SELECT m.*,EXISTS(SELECT 1 FROM memory_favorite f WHERE f.memory_id=m.id AND f.user_id=?) AS favorite FROM memory m";}
    private Entry entry(UUID space,UUID actor,UUID id,boolean trash){
        String state=trash?"m.state='TRASHED' AND m.expires_at>clock_timestamp()":"m.state='ACTIVE'";
        var rows=jdbc.query(select()+" WHERE m.id=? AND m.space_id=? AND "+state,(r,n)->map(r),actor,id,space);if(rows.isEmpty())throw missing();return rows.getFirst();
    }
    @Transactional
    public void favorite(UUID actor,UUID id,boolean value){var space=lock(actor);entry(space.id(),actor,id,false);if(value)jdbc.update("INSERT INTO memory_favorite(memory_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING",id,actor);else jdbc.update("DELETE FROM memory_favorite WHERE memory_id=? AND user_id=?",id,actor);events.publishEvent(new HomeService.Changed(List.of(actor),"memory.favorite"));}
    @Transactional
    public Entry trash(UUID actor,UUID id,Long expected){
        version(expected);var space=lock(actor);
        var rows=jdbc.queryForList("SELECT state,version,deleted_by FROM memory WHERE id=? AND space_id=?",id,space.id());if(rows.isEmpty())throw missing();var row=rows.getFirst();
        if(row.get("state").equals("TRASHED")&&((Number)row.get("version")).longValue()==expected+1&&actor.equals(row.get("deleted_by")))return entry(space.id(),actor,id,true);
        var current=entry(space.id(),actor,id,false);if(current.version()!=expected)throw stale();
        jdbc.update("UPDATE memory SET state='TRASHED',deleted_by=?,updated_by=?,deleted_at=statement_timestamp(),expires_at=statement_timestamp()+INTERVAL '72 hours',updated_at=statement_timestamp(),version=version+1 WHERE id=?",actor,actor,id);changed(space.id());return entry(space.id(),actor,id,true);
    }
    @Transactional
    public Entry restore(UUID actor,UUID id,Long expected){version(expected);var space=lock(actor);var current=entry(space.id(),actor,id,true);if(current.version()!=expected)throw stale();if(jdbc.update("UPDATE memory SET state='ACTIVE',deleted_at=NULL,expires_at=NULL,updated_by=?,updated_at=clock_timestamp(),version=version+1 WHERE id=? AND state='TRASHED' AND expires_at>clock_timestamp()",actor,id)!=1)throw missing();changed(space.id());return entry(space.id(),actor,id,false);}
    @Transactional(readOnly=true) public TrashPreview previewTrash(UUID actor){return preview(active(actor).id());}
    private TrashPreview preview(UUID space){var rows=jdbc.queryForList("SELECT id,version FROM memory WHERE space_id=? AND state='TRASHED' ORDER BY id",space);return new TrashPreview(RegistrationService.digest(space+"|"+rows),rows.size(),jdbc.queryForObject("SELECT count(*) FROM memory WHERE space_id=? AND state='PURGING'",Integer.class,space));}
    @Transactional
    public List<UUID> emptyTrash(UUID actor,EmptyTrash command){
        var space=lock(actor);if(!command.confirmed())throw new ApiFailure(400,"CONFIRMATION_REQUIRED","Confirm that Empty Trash permanently removes these memories and files.");
        if(!Objects.equals(preview(space.id()).snapshot(),command.snapshot()))throw new ApiFailure(409,"TRASH_CHANGED","Trash changed. Review its current contents and confirm again.");
        var ids=jdbc.queryForList("UPDATE memory SET state='PURGING',version=version+1,updated_by=?,updated_at=clock_timestamp() WHERE space_id=? AND state='TRASHED' RETURNING id",UUID.class,actor,space.id());changed(space.id());return ids;
    }
    /** Request-time checks deny expired/deleted objects even before the maintenance worker runs. */
    @Transactional(readOnly=true)
    public Download download(UUID actor,UUID id,boolean thumbnail,boolean attachment){
        var space=active(actor);var rows=jdbc.queryForList("SELECT a.* FROM memory_asset a LEFT JOIN memory m ON m.id=a.memory_id WHERE a.id=? AND a.space_id=? AND a.state='READY' AND ((a.memory_id IS NULL AND a.uploaded_by=? AND a.created_at>clock_timestamp()-INTERVAL '24 hours') OR m.state='ACTIVE')",id,space.id(),actor);
        if(rows.isEmpty())throw new ApiFailure(404,"ASSET_NOT_FOUND","This file is not available.");var row=rows.getFirst();boolean doc=row.get("kind").equals("DOCUMENT");if(thumbnail&&doc)throw new ApiFailure(404,"ASSET_NOT_FOUND","Documents do not have a thumbnail.");
        try{var resource=files.resource(id,thumbnail);return new Download(resource,thumbnail?"image/jpeg":(String)row.get("mime"),thumbnail?"thumbnail.jpg":(String)row.get("original_name"),resource.contentLength(),doc||attachment);}catch(IOException error){throw new ApiFailure(503,"ASSET_UNAVAILABLE","This file is temporarily unavailable.");}
    }
    private Asset assetById(UUID id){return jdbc.queryForObject("SELECT * FROM memory_asset WHERE id=?",(r,n)->asset(r),id);}
    private Asset asset(java.sql.ResultSet r)throws java.sql.SQLException{UUID id=r.getObject("id",UUID.class);String kind=r.getString("kind");return new Asset(id,r.getString("original_name"),kind,r.getString("mime"),r.getLong("byte_size"),r.getString("sha256"),(Double)r.getObject("duration"),(Integer)r.getObject("width"),(Integer)r.getObject("height"),"/api/memory-assets/"+id,kind.equals("DOCUMENT")?null:"/api/memory-assets/"+id+"?thumbnail=true");}
    private Entry map(java.sql.ResultSet r)throws java.sql.SQLException{UUID id=r.getObject("id",UUID.class);return new Entry(id,r.getString("title"),r.getString("caption"),r.getObject("memory_date",LocalDate.class),r.getString("state"),r.getObject("created_by",UUID.class),r.getObject("updated_by",UUID.class),r.getObject("deleted_by",UUID.class),instant(r,"created_at"),instant(r,"updated_at"),instant(r,"deleted_at"),instant(r,"expires_at"),r.getLong("version"),r.getBoolean("favorite"),jdbc.query("SELECT * FROM memory_asset WHERE memory_id=? AND state='READY' ORDER BY position",(a,n)->asset(a),id));}
    private Instant instant(java.sql.ResultSet r,String column)throws java.sql.SQLException{var t=r.getTimestamp(column);return t==null?null:t.toInstant();}
}
