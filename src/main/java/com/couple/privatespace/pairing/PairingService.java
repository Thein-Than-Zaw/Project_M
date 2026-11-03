package com.couple.privatespace.pairing;

import com.couple.privatespace.identity.*;
import java.time.*;
import java.security.SecureRandom;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PairingService {
    private final JdbcTemplate jdbc;
    private final AccountStore accounts;
    private final ApplicationEventPublisher events;
    private final ZoneId zone;
    private final long inviteHours;
    public PairingService(JdbcTemplate jdbc, AccountStore accounts, ApplicationEventPublisher events,
            @Value("${app.couple.timezone}") String timezone, @Value("${app.pairing.invite-hours:24}") long hours) {
        this.jdbc=jdbc; this.accounts=accounts; this.events=events; this.zone=ZoneId.of(timezone); this.inviteHours=hours;
    }
    public record Member(UUID id, String displayName, int slot, String avatarUrl) { public Member(UUID id,String displayName,int slot){this(id,displayName,slot,null);} }
    public record Space(String state, UUID id, LocalDate relationshipDate, String timezone,
            List<Member> members, LocalDate signupRelationshipDate, boolean canCreate, long version) { }
    public record Create(LocalDate relationshipDate, boolean confirmed) { }
    public record Code(String code) { }
    public record Invitation(String code, Instant expiresAt) { }
    public record Preview(UUID spaceId, String inviterName, LocalDate relationshipDate,
            LocalDate signupRelationshipDate, String timezone, String confirmationToken, Instant expiresAt) { }
    public record Join(String code, String confirmationToken, LocalDate confirmedRelationshipDate, boolean confirmed) { }
    public record Changed(List<UUID> members) { }
    private record Invite(UUID id, UUID spaceId, UUID creator, Instant expiresAt) { }

    public boolean active(UUID user) {
        return jdbc.queryForObject("SELECT count(*) FROM couple_membership own WHERE own.user_id=? AND (SELECT count(*) FROM couple_membership other WHERE other.space_id=own.space_id)=2",Integer.class,user)>0;
    }
    public boolean member(UUID user) { return jdbc.queryForObject("SELECT count(*) FROM couple_membership WHERE user_id=?",Integer.class,user)>0; }
    @Transactional(readOnly=true)
    public Space current(UUID user) {
        var account=account(user);
        var ids=jdbc.query("SELECT space_id FROM couple_membership WHERE user_id=?",(r,n)->r.getObject(1,UUID.class),user);
        if(ids.isEmpty()) return new Space("UNPAIRED",null,null,zone.getId(),List.of(),account.relationshipDate(),
            jdbc.queryForObject("SELECT count(*) FROM couple_space",Integer.class)==0,0);
        return summary(ids.getFirst(),account);
    }
    private Space summary(UUID id, AccountStore.Account actor) {
        return jdbc.queryForObject("SELECT relationship_date,timezone,version FROM couple_space WHERE id=?",(r,n)->{
            var members=jdbc.query("SELECT u.id,COALESCE(n.nickname,u.display_name),m.member_slot,u.avatar_jpeg IS NOT NULL,u.avatar_version FROM couple_membership m JOIN app_user u ON u.id=m.user_id LEFT JOIN partner_nickname n ON n.space_id=m.space_id AND n.partner_id=u.id AND n.owner_id=? WHERE m.space_id=? ORDER BY m.member_slot",
                (row,index)->new Member(row.getObject(1,UUID.class),row.getString(2),row.getInt(3),row.getBoolean(4)?"/api/profiles/"+row.getObject(1,UUID.class)+"/photo?v="+row.getLong(5):null),actor.id(),id);
            return new Space(members.size()==2?"ACTIVE":"WAITING",id,r.getObject(1,LocalDate.class),r.getString(2),members,actor.relationshipDate(),false,r.getLong(3));
        },id);
    }
    /** PAIR-01/D01/D02: create space and its creator membership atomically, never on login. */
    @Transactional
    public Space create(UUID user, Create request) {
        if(!request.confirmed()) throw bad("CONFIRMATION_REQUIRED","Confirm your shared relationship date.");
        if(request.relationshipDate()==null || request.relationshipDate().getYear()<1 || request.relationshipDate().getYear()>9999
                || request.relationshipDate().isAfter(LocalDate.now(zone)))
            throw new ApiFailure(400,"VALIDATION_FAILED","Choose a relationship date that is today or earlier in Myanmar time.",Map.of("relationshipDate","Choose today or an earlier date."));
        lock(); var own=current(user);
        if(!own.state().equals("UNPAIRED")) {
            if(own.state().equals("WAITING") && own.members().getFirst().id().equals(user) && own.relationshipDate().equals(request.relationshipDate())) return own;
            throw conflict("ALREADY_PAIRED","You already belong to a couple space.");
        }
        if(!own.canCreate()) throw conflict("SPACE_EXISTS","Your partner has already started a space. Join with their pairing code.");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO couple_space(id,deployment_slot,relationship_date,timezone) VALUES (?,1,?,?)",id,request.relationshipDate(),zone.getId());
        jdbc.update("INSERT INTO couple_membership(space_id,user_id,member_slot) VALUES (?,?,1)",id,user);
        return current(user);
    }
    @Transactional
    public Invitation invite(UUID user) {
        lock(); var own=current(user);
        if(own.state().equals("UNPAIRED")) throw new ApiFailure(403,"MEMBERSHIP_REQUIRED","Create your space before inviting your partner.");
        if(own.state().equals("ACTIVE")) throw conflict("SPACE_FULL","Your space already has two members.");
        jdbc.update("UPDATE pairing_invitation SET revoked_at=CURRENT_TIMESTAMP WHERE space_id=? AND consumed_at IS NULL AND revoked_at IS NULL",own.id());
        String code=randomCode(); Instant expires=Instant.now().plus(Duration.ofHours(inviteHours));
        jdbc.update("INSERT INTO pairing_invitation(id,space_id,created_by,token_hash,expires_at) VALUES (?,?,?,?,?)",UUID.randomUUID(),own.id(),user,RegistrationService.digest(code),java.sql.Timestamp.from(expires));
        return new Invitation(code,expires);
    }
    @Transactional
    public Preview preview(UUID user, String code) {
        validateCode(code); lock(); var actor=account(user);
        if(member(user)) throw conflict("ALREADY_PAIRED","You already belong to a couple space.");
        var invite=validInvite(code,user); var space=summary(invite.spaceId(),actor);
        if(!space.state().equals("WAITING")) throw conflict("SPACE_FULL","This space already has two members.");
        String confirmation=randomCode(); Instant expires=Instant.now().plusSeconds(600);
        if(expires.isAfter(invite.expiresAt())) expires=invite.expiresAt();
        jdbc.update("INSERT INTO pairing_confirmation(user_id,invitation_id,token_hash,space_version,relationship_date,expires_at) VALUES (?,?,?,?,?,?) ON CONFLICT(user_id) DO UPDATE SET invitation_id=EXCLUDED.invitation_id,token_hash=EXCLUDED.token_hash,space_version=EXCLUDED.space_version,relationship_date=EXCLUDED.relationship_date,expires_at=EXCLUDED.expires_at",
            user,invite.id(),RegistrationService.digest(confirmation),space.version(),space.relationshipDate(),java.sql.Timestamp.from(expires));
        String inviter=accounts.byId(invite.creator()).orElseThrow().displayName();
        return new Preview(space.id(),inviter,space.relationshipDate(),actor.relationshipDate(),space.timezone(),confirmation,expires);
    }
    /** PAIR-02/AC-F02-03/06: effect, consumed code and retry receipt commit together. */
    @Transactional
    public Space join(UUID user, UUID key, Join request) {
        if(key==null || !request.confirmed() || request.confirmedRelationshipDate()==null)
            throw bad("CONFIRMATION_REQUIRED","Review and confirm the shared date before joining.");
        validateCode(request.code()); validateCode(request.confirmationToken());
        String intent=RegistrationService.digest(request.code()+"|"+request.confirmationToken()+"|"+request.confirmedRelationshipDate());
        lock(); account(user);
        var receipts=jdbc.queryForList("SELECT idempotency_key,request_hash FROM pairing_join_receipt WHERE user_id=?",user);
        if(!receipts.isEmpty()) {
            var receipt=receipts.getFirst();
            if(key.equals(receipt.get("idempotency_key")) && intent.equals(receipt.get("request_hash"))) return current(user);
            throw conflict("IDEMPOTENCY_CONFLICT","You have already joined. Refresh to open your space.");
        }
        if(member(user)) throw conflict("ALREADY_PAIRED","You already belong to a couple space.");
        var invite=validInvite(request.code(),user); var own=summary(invite.spaceId(),account(user));
        if(!own.state().equals("WAITING")) throw conflict("SPACE_FULL","This space already has two members.");
        int confirmed=jdbc.queryForObject("SELECT count(*) FROM pairing_confirmation WHERE user_id=? AND invitation_id=? AND token_hash=? AND space_version=? AND relationship_date=? AND expires_at>clock_timestamp()",
            Integer.class,user,invite.id(),RegistrationService.digest(request.confirmationToken()),own.version(),request.confirmedRelationshipDate());
        if(confirmed!=1 || !own.relationshipDate().equals(request.confirmedRelationshipDate()))
            throw conflict("PREVIEW_EXPIRED","The invitation or shared date changed. Review the invitation again.");
        jdbc.update("INSERT INTO couple_membership(space_id,user_id,member_slot) VALUES (?,?,2)",own.id(),user);
        jdbc.update("UPDATE pairing_invitation SET consumed_at=CURRENT_TIMESTAMP,consumed_by=? WHERE id=?",user,invite.id());
        jdbc.update("UPDATE pairing_invitation SET revoked_at=CURRENT_TIMESTAMP WHERE space_id=? AND consumed_at IS NULL AND revoked_at IS NULL",own.id());
        jdbc.update("UPDATE couple_space SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id=?",own.id());
        jdbc.update("DELETE FROM pairing_confirmation WHERE user_id=?",user);
        jdbc.update("INSERT INTO pairing_join_receipt(user_id,idempotency_key,request_hash,space_id) VALUES (?,?,?,?)",user,key,intent,own.id());
        var paired=current(user);
        events.publishEvent(new Changed(paired.members().stream().map(Member::id).toList()));
        return paired;
    }
    private Invite validInvite(String code, UUID user) {
        var rows=jdbc.query("SELECT id,space_id,created_by,expires_at FROM pairing_invitation WHERE token_hash=? AND consumed_at IS NULL AND revoked_at IS NULL AND expires_at>clock_timestamp()",
            (r,n)->new Invite(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getTimestamp(4).toInstant()),RegistrationService.digest(code));
        if(rows.isEmpty()) throw bad("INVALID_INVITATION","This invitation is invalid or expired.");
        if(rows.getFirst().creator().equals(user)) throw bad("SELF_JOIN","You cannot join your own invitation.");
        return rows.getFirst();
    }
    private AccountStore.Account account(UUID user) { return accounts.byId(user).filter(AccountStore.Account::enabled).orElseThrow(()->new ApiFailure(401,"UNAUTHENTICATED","Please sign in again.")); }
    private void lock() { jdbc.queryForObject("SELECT id FROM registration_gate WHERE id=1 FOR UPDATE",Integer.class); }
    private static String randomCode() { byte[] data=new byte[32];new SecureRandom().nextBytes(data);return Base64.getUrlEncoder().withoutPadding().encodeToString(data); }
    private static void validateCode(String code) { if(code==null || !code.matches("[A-Za-z0-9_-]{43}")) throw bad("INVALID_INVITATION","This invitation is invalid or expired."); }
    private static ApiFailure bad(String code,String message) { return new ApiFailure(400,code,message); }
    private static ApiFailure conflict(String code,String message) { return new ApiFailure(409,code,message); }
}
