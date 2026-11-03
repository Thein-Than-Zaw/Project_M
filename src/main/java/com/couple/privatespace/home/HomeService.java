package com.couple.privatespace.home;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.pairing.PairingService;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class HomeService {
    private final JdbcTemplate jdbc;
    private final SharedActivityService activity;
    private final PairingService pairing;
    private final ApplicationEventPublisher events;
    public HomeService(JdbcTemplate jdbc,PairingService pairing,ApplicationEventPublisher events,SharedActivityService activity) {
        this.jdbc=jdbc;this.pairing=pairing;this.events=events;this.activity=activity;
    }
    public record Profile(UUID id,String displayName,String email,long version,String avatarUrl) { }
    public record ProfileEdit(String displayName,Long version) { }
    public record RelationshipEdit(LocalDate relationshipDate,Long version,boolean confirmed) { }
    public record EventEdit(String title,LocalDate eventDate,Boolean repeatYearly,Long version) { }
    public record SpecialDate(UUID id,String title,LocalDate eventDate,boolean repeatYearly,
            UUID createdBy,long version,LocalDate nextDate,long daysUntil,boolean upcoming) { }
    public record Dates(DatePolicy.Calendar calendar,List<SpecialDate> items,boolean hasMore,int page) { }
    public record Home(String state,UUID id,LocalDate relationshipDate,String timezone,
            List<PairingService.Member> members,LocalDate signupRelationshipDate,boolean canCreate,long version,
            Profile self,DatePolicy.Calendar calendar,SpecialDate nextEvent,long unreadMessages,Activity activity) { }
    public record Activity(long messageCount,long videoCallSeconds,long videoCallCount) { }
    public record Changed(List<UUID> members,String type) { }

    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Home home(UUID actor) {
        var space=active(actor);var calendar=calendar(space);
        var next=list(space.id(),calendar.today(),0,true);
        return new Home(space.state(),space.id(),space.relationshipDate(),space.timezone(),space.members(),
            space.signupRelationshipDate(),space.canCreate(),space.version(),profile(actor),calendar,next.isEmpty()?null:next.getFirst(),
            jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=? AND sender_id<>? AND read_at IS NULL AND deleted_at IS NULL AND NOT EXISTS(SELECT 1 FROM chat_message_hidden h WHERE h.message_id=chat_message.id AND h.user_id=?)",Long.class,space.id(),actor,actor),activity.forSpace(space.id()));
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Dates dates(UUID actor,int page) {
        if(page<0||page>100000)throw invalid("page","Choose a valid page.");
        var space=active(actor);var calendar=calendar(space);var rows=list(space.id(),calendar.today(),page,false);
        return new Dates(calendar,rows.stream().limit(50).toList(),rows.size()>50,page);
    }
    private DatePolicy.Calendar calendar(PairingService.Space space) {
        return DatePolicy.calendar(space.relationshipDate(),ZoneId.of(space.timezone()),Clock.systemUTC());
    }
    private Profile profile(UUID actor) {
        return jdbc.queryForObject("SELECT id,display_name,email,version,avatar_jpeg IS NOT NULL,avatar_version FROM app_user WHERE id=?",
            (r,n)->new Profile(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getLong(4),r.getBoolean(5)?"/api/profiles/"+actor+"/photo?v="+r.getLong(6):null),actor);
    }
    /** HOME-01/03: actor comes from session, never an arbitrary target profile. */
    @Transactional
    public Profile editProfile(UUID actor,ProfileEdit body) {
        var name=text(body.displayName(),60,"displayName");version(body.version());var space=active(actor);
        if(jdbc.update("UPDATE app_user SET display_name=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=?",name,actor,body.version())!=1)throw stale();
        changed(space,"profile.updated");return profile(actor);
    }
    /** DATE-03: both members may update the shared date, with explicit impact confirmation. */
    @Transactional
    public Home editRelationship(UUID actor,RelationshipEdit body) {
        version(body.version());var space=active(actor);var date=body.relationshipDate();
        if(!body.confirmed())throw new ApiFailure(400,"CONFIRMATION_REQUIRED","Confirm that this changes the shared date for both of you.");
        validDate(date,"relationshipDate");
        if(date.isAfter(LocalDate.now(ZoneId.of(space.timezone()))))throw invalid("relationshipDate","Choose today or an earlier date in Myanmar time.");
        if(jdbc.update("UPDATE couple_space SET relationship_date=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND version=?",date,space.id(),body.version())!=1)throw stale();
        changed(space,"relationship.updated");return home(actor);
    }
    /** DATE-01–03: same creation key retries cannot create a second event. */
    @Transactional
    public SpecialDate create(UUID actor,UUID key,EventEdit body) {
        var title=text(body.title(),100,"title");validDate(body.eventDate(),"eventDate");
        if(body.repeatYearly()==null)throw invalid("repeatYearly","Choose whether this repeats yearly.");
        var space=lockSpace(actor);
        String digest=RegistrationService.digest(title+"|"+body.eventDate()+"|"+body.repeatYearly());
        var existing=jdbc.queryForList("SELECT event_id,request_hash FROM special_date_creation_receipt WHERE created_by=? AND creation_key=?",actor,key);
        if(!existing.isEmpty()) {
            if(!digest.equals(existing.getFirst().get("request_hash")))throw new ApiFailure(409,"IDEMPOTENCY_CONFLICT","This save identifier was already used for a different date.");
            return event(space,(UUID)existing.getFirst().get("event_id"));
        }
        var id=UUID.randomUUID();
        jdbc.update("INSERT INTO special_date(id,space_id,created_by,updated_by,title,event_date,repeat_yearly,creation_key,creation_hash) VALUES (?,?,?,?,?,?,?,?,?)",
            id,space.id(),actor,actor,title,body.eventDate(),body.repeatYearly(),key,digest);
        jdbc.update("INSERT INTO special_date_creation_receipt(created_by,creation_key,request_hash,event_id) VALUES (?,?,?,?)",actor,key,digest,id);
        changed(space,"date.updated");return event(space,id);
    }
    @Transactional
    public SpecialDate edit(UUID actor,UUID id,EventEdit body) {
        var title=text(body.title(),100,"title");validDate(body.eventDate(),"eventDate");version(body.version());
        if(body.repeatYearly()==null)throw invalid("repeatYearly","Choose whether this repeats yearly.");
        var space=lockSpace(actor);event(space,id);
        if(jdbc.update("UPDATE special_date SET title=?,event_date=?,repeat_yearly=?,updated_by=?,version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND space_id=? AND version=?",
            title,body.eventDate(),body.repeatYearly(),actor,id,space.id(),body.version())!=1)throw stale();
        changed(space,"date.updated");return event(space,id);
    }
    /** Owner instruction: either member can delete a special date, preserving stale-write checks. */
    @Transactional
    public void delete(UUID actor,UUID id,Long expectedVersion) {
        version(expectedVersion);var space=lockSpace(actor);event(space,id);
        if(jdbc.update("DELETE FROM special_date WHERE id=? AND space_id=? AND version=?",id,space.id(),expectedVersion)!=1)throw stale();
        changed(space,"date.updated");
    }
    private PairingService.Space active(UUID actor) {
        var space=pairing.current(actor);
        if(!space.state().equals("ACTIVE"))throw new ApiFailure(403,"PAIRING_REQUIRED","Connect with your partner first.");
        return space;
    }
    private PairingService.Space lockSpace(UUID actor) {
        var space=active(actor);jdbc.queryForObject("SELECT version FROM couple_space WHERE id=? FOR UPDATE",Long.class,space.id());return active(actor);
    }
    private void changed(PairingService.Space space,String type) {
        events.publishEvent(new Changed(space.members().stream().map(PairingService.Member::id).toList(),type));
    }
    // PostgreSQL interval arithmetic clamps leap-day exactly as DatePolicy.withYear.
    // Both candidate years derive from ORIGINAL event_date, so leap years restore Feb29.
    private String occurrenceSql(LocalDate today) {
        int year=today.getYear();
        return "WITH first AS (SELECT d.*,CASE WHEN repeat_yearly THEN (event_date+make_interval(years => GREATEST(0,"+year+"-extract(year from event_date)::integer)))::date ELSE event_date END first_date FROM special_date d WHERE space_id=?), occurrences AS (SELECT first.*,CASE WHEN repeat_yearly AND first_date<CAST(? AS date) THEN (event_date+make_interval(years => GREATEST(0,"+(year+1)+"-extract(year from event_date)::integer)))::date ELSE first_date END next_date FROM first) ";
    }
    private List<SpecialDate> list(UUID space,LocalDate today,int page,boolean upcomingOnly) {
        return jdbc.query(occurrenceSql(today)+"SELECT * FROM occurrences "+(upcomingOnly?"WHERE next_date>=CAST(? AS date) ":"")+"ORDER BY (next_date<CAST(? AS date)),next_date,id LIMIT ? OFFSET ?",
            (r,n)->map(r,today),upcomingOnly?new Object[]{space,today,today,today,1,0}:new Object[]{space,today,today,51,page*50});
    }
    private SpecialDate event(PairingService.Space space,UUID id) {
        var today=LocalDate.now(ZoneId.of(space.timezone()));
        var rows=jdbc.query(occurrenceSql(today)+"SELECT * FROM occurrences WHERE id=?",(r,n)->map(r,today),space.id(),today,id);
        if(rows.isEmpty())throw new ApiFailure(404,"DATE_NOT_FOUND","This date is no longer available. Reload your dates.");return rows.getFirst();
    }
    private SpecialDate map(java.sql.ResultSet r,LocalDate today)throws java.sql.SQLException {
        var next=r.getObject("next_date",LocalDate.class);
        return new SpecialDate(r.getObject("id",UUID.class),r.getString("title"),r.getObject("event_date",LocalDate.class),
            r.getBoolean("repeat_yearly"),r.getObject("created_by",UUID.class),r.getLong("version"),next,ChronoUnit.DAYS.between(today,next),!next.isBefore(today));
    }
    private static String text(String raw,int max,String field) {
        String value=raw==null?"":raw.strip();int count=value.codePointCount(0,value.length());
        if(value.isBlank()||count>max||value.codePoints().anyMatch(Character::isISOControl))throw invalid(field,"Enter 1–"+max+" characters without control characters.");return value;
    }
    private static void validDate(LocalDate date,String field) {
        if(date==null||date.getYear()<1||date.getYear()>9999)throw invalid(field,"Choose a valid calendar date.");
    }
    private static void version(Long value) {if(value==null||value<0)throw invalid("version","Reload before saving this change.");}
    private static ApiFailure invalid(String field,String message) {return new ApiFailure(400,"VALIDATION_FAILED",message,Map.of(field,message));}
    private static ApiFailure stale() {return new ApiFailure(409,"VERSION_CONFLICT","This was updated in another tab or by your partner. Reload and review before saving again.");}
}
