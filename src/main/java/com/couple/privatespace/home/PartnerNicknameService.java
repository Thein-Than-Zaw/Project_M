package com.couple.privatespace.home;
import com.couple.privatespace.identity.*;
import com.couple.privatespace.pairing.PairingService;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class PartnerNicknameService {
 public record Label(UUID partnerId,String profileName,String nickname,long version){}
 public record Edit(String nickname,Long version){}
 private final JdbcTemplate jdbc;private final PairingService pairing;private final AccountStore accounts;private final ApplicationEventPublisher events;
 public PartnerNicknameService(JdbcTemplate jdbc,PairingService pairing,AccountStore accounts,ApplicationEventPublisher events){this.jdbc=jdbc;this.pairing=pairing;this.accounts=accounts;this.events=events;}
 private PairingService.Space active(UUID actor){var space=pairing.current(actor);if(!space.state().equals("ACTIVE"))throw new ApiFailure(403,"PAIRING_REQUIRED","Connect with your partner first.");return space;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Label current(UUID actor){var space=active(actor);UUID partner=space.members().stream().map(PairingService.Member::id).filter(id->!id.equals(actor)).findFirst().orElseThrow();String name=accounts.byId(partner).orElseThrow().displayName();return jdbc.query("SELECT nickname,version FROM partner_nickname WHERE owner_id=? AND space_id=? AND partner_id=?",(r,n)->new Label(partner,name,r.getString(1),r.getLong(2)),actor,space.id(),partner).stream().findFirst().orElse(new Label(partner,name,null,0));}
 @Transactional
 public Label edit(UUID actor,Edit edit){
  if(edit==null||edit.version()==null||edit.version()<0)throw new ApiFailure(400,"INVALID_VERSION","Reload the nickname before saving.");
  String value=edit.nickname()==null?"":edit.nickname().strip();if(value.codePoints().allMatch(c->Character.isWhitespace(c)||Character.isSpaceChar(c)||c==0xFEFF))value=null;
  if(value!=null&&(value.codePointCount(0,value.length())>60||value.codePoints().anyMatch(c->Character.isISOControl(c)||c<=65535&&Character.isSurrogate((char)c))))throw new ApiFailure(400,"VALIDATION_FAILED","Use up to 60 valid characters without line breaks.",Map.of("nickname","Use up to 60 valid characters without line breaks."));
  var space=active(actor);jdbc.queryForObject("SELECT version FROM couple_space WHERE id=? FOR UPDATE",Long.class,space.id());var previous=current(actor);
  if(previous.version()!=edit.version())throw new ApiFailure(409,"VERSION_CONFLICT","Your nickname changed in another tab. Reload before saving again.");
  if(Objects.equals(previous.nickname(),value))return previous;
  jdbc.update("INSERT INTO partner_nickname(space_id,owner_id,partner_id,nickname,version) VALUES (?,?,?,?,1) ON CONFLICT(owner_id) DO UPDATE SET nickname=EXCLUDED.nickname,version=partner_nickname.version+1,updated_at=clock_timestamp()",space.id(),actor,previous.partnerId(),value);
  events.publishEvent(new HomeService.Changed(List.of(actor),"partner.nickname.updated"));return current(actor);
 }
}
