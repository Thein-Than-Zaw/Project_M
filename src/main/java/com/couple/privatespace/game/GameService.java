package com.couple.privatespace.game;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.pairing.PairingService;
import java.util.*;
import java.security.SecureRandom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class GameService {
 public record Game(UUID id,UUID inviterId,UUID redId,UUID yellowId,String status,String board,int moves,UUID nextPlayerId,UUID winnerId,List<Integer> winningCells,long version){}
 public record Page(UUID selfId,List<PairingService.Member> members,Game game){}
 public record Command(UUID actionId,UUID gameId,Long version,Integer column){}
 public record Changed(List<UUID> members){}
 private record Row(Game game,UUID lastFirst){}
 private final JdbcTemplate jdbc;private final PairingService pairing;private final ApplicationEventPublisher events;
 private final SecureRandom random=new SecureRandom();
 public GameService(JdbcTemplate jdbc,PairingService pairing,ApplicationEventPublisher events){this.jdbc=jdbc;this.pairing=pairing;this.events=events;}
 private PairingService.Space active(UUID actor){var s=pairing.current(actor);if(!s.state().equals("ACTIVE"))throw new ApiFailure(403,"PAIRING_REQUIRED","Connect with your partner first.");return s;}
 private Row row(UUID space){return jdbc.query("SELECT * FROM couple_connect_four WHERE space_id=?",(r,n)->{
  String status=r.getString("status"),board=r.getString("board");int moves=r.getInt("moves");UUID red=r.getObject("red_id",UUID.class),yellow=r.getObject("yellow_id",UUID.class);
  return new Row(new Game(r.getObject("id",UUID.class),r.getObject("inviter_id",UUID.class),red,yellow,status,board,moves,status.equals("ACTIVE")?(moves%2==0?red:yellow):null,r.getObject("winner_id",UUID.class),status.equals("WON")?ConnectFour.winningLine(board):List.of(),r.getLong("version")),r.getObject("last_first_id",UUID.class));
 },space).stream().findFirst().orElse(null);}
 private Page page(UUID actor,PairingService.Space s,Row row){return new Page(actor,s.members(),row==null?null:row.game());}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Page current(UUID actor){var s=active(actor);return page(actor,s,row(s.id()));}
 /** Membership derives from session; lock covers all tabs/players and the retry receipt. */
 @Transactional
 public Page command(UUID actor,String operation,Command c){
  if(c==null||c.actionId()==null||!Set.of("invite","accept","decline","cancel","move","resign").contains(operation))throw new ApiFailure(400,"INVALID_GAME_ACTION","Choose a valid game action.");
  var s=active(actor);jdbc.queryForObject("SELECT version FROM couple_space WHERE id=? FOR UPDATE",Long.class,s.id());s=active(actor);
  String hash=RegistrationService.digest(operation+"|"+c.gameId()+"|"+c.version()+"|"+c.column());
  var receipts=jdbc.queryForList("SELECT request_hash FROM connect_four_command WHERE space_id=? AND actor_id=? AND action_id=?",String.class,s.id(),actor,c.actionId());
  if(!receipts.isEmpty()){if(!receipts.getFirst().equals(hash))throw new ApiFailure(409,"GAME_RETRY_CONFLICT","This action key was used for a different move.");return page(actor,s,row(s.id()));}
  var old=row(s.id());var g=old==null?null:old.game();
  // Bounded per-account durable rate protection, independent of chat/call controls.
  if(jdbc.queryForObject("SELECT count(*) FROM connect_four_command WHERE actor_id=? AND created_at>clock_timestamp()-INTERVAL '10 seconds'",Integer.class,actor)>=20)throw new ApiFailure(429,"GAME_RATE_LIMITED","A little slower. Try again in a moment.");
  if(operation.equals("invite")){
   if(c.column()!=null)throw new ApiFailure(400,"INVALID_GAME_ACTION","Choose New game to invite your partner.");
   if(g!=null&&Set.of("WAITING","ACTIVE").contains(g.status()))throw new ApiFailure(409,"GAME_IN_PROGRESS","Finish or cancel the current game first.");
   expected(g,c);
   var ids=s.members().stream().map(PairingService.Member::id).toList();UUID first=old==null||old.lastFirst()==null?ids.get(random.nextInt(2)):ids.stream().filter(id->!id.equals(old.lastFirst())).findFirst().orElseThrow();UUID second=ids.stream().filter(id->!id.equals(first)).findFirst().orElseThrow();
   jdbc.update("INSERT INTO couple_connect_four(space_id,id,inviter_id,red_id,yellow_id,status,board,last_first_id) VALUES (?,?,?,?,?,'WAITING',?,?) ON CONFLICT(space_id) DO UPDATE SET id=EXCLUDED.id,inviter_id=EXCLUDED.inviter_id,red_id=EXCLUDED.red_id,yellow_id=EXCLUDED.yellow_id,status='WAITING',board=EXCLUDED.board,moves=0,winner_id=NULL,version=couple_connect_four.version+1,updated_at=clock_timestamp()",s.id(),UUID.randomUUID(),actor,first,second,ConnectFour.EMPTY,old==null?null:old.lastFirst());
  }else{
   if(g==null)throw new ApiFailure(404,"GAME_UNAVAILABLE","Invite your partner to start a game.");expected(g,c);
   if(!operation.equals("move")&&c.column()!=null)throw new ApiFailure(400,"INVALID_GAME_ACTION","Unexpected column for this action.");
   switch(operation){
    case "accept","decline","cancel" -> {
     if(!g.status().equals("WAITING"))throw conflict();
     boolean inviter=g.inviterId().equals(actor);if(operation.equals("cancel")?!inviter:inviter)throw new ApiFailure(403,"GAME_INVITATION_FORBIDDEN","Only the invited partner can accept/decline; the sender can cancel.");
     String status=operation.equals("accept")?"ACTIVE":operation.equals("decline")?"DECLINED":"CANCELLED";
     jdbc.update("UPDATE couple_connect_four SET status=?,last_first_id=CASE WHEN ?='ACTIVE' THEN red_id ELSE last_first_id END,version=version+1,updated_at=clock_timestamp() WHERE space_id=?",status,status,s.id());
    }
    case "move" -> {
     if(!g.status().equals("ACTIVE"))throw conflict();if(!actor.equals(g.nextPlayerId()))throw new ApiFailure(409,"NOT_YOUR_TURN","It is your partner’s turn.");if(c.column()==null)throw new ApiFailure(400,"INVALID_COLUMN","Choose a column.");
     var drop=ConnectFour.drop(g.board(),c.column(),actor.equals(g.redId())?'R':'Y');int moves=g.moves()+1;String status=!drop.line().isEmpty()?"WON":moves==42?"DRAW":"ACTIVE";
     jdbc.update("UPDATE couple_connect_four SET board=?,moves=?,status=?,winner_id=?,version=version+1,updated_at=clock_timestamp() WHERE space_id=?",drop.board(),moves,status,status.equals("WON")?actor:null,s.id());
    }
    case "resign" -> {if(!g.status().equals("ACTIVE"))throw conflict();UUID winner=actor.equals(g.redId())?g.yellowId():g.redId();jdbc.update("UPDATE couple_connect_four SET status='RESIGNED',winner_id=?,version=version+1,updated_at=clock_timestamp() WHERE space_id=?",winner,s.id());}
    default -> throw new IllegalStateException();
   }
  }
  jdbc.update("INSERT INTO connect_four_command(space_id,actor_id,action_id,request_hash) VALUES (?,?,?,?)",s.id(),actor,c.actionId(),hash);
  events.publishEvent(new Changed(s.members().stream().map(PairingService.Member::id).toList()));return page(actor,s,row(s.id()));
 }
 private void expected(Game game,Command c){if(game==null){if(c.gameId()!=null||c.version()!=null)throw conflict();}else if(!game.id().equals(c.gameId())||c.version()==null||game.version()!=c.version())throw conflict();}
 private ApiFailure conflict(){return new ApiFailure(409,"GAME_CHANGED","The game changed. Refresh the board before trying again.");}
}
