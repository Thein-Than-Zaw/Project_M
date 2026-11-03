package com.couple.privatespace.game;
import com.couple.privatespace.identity.AccountStore;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
@Component
public class GameNotifications {
 public record Invalidation(UUID eventId,String type){}
 private final AccountStore accounts;private final SimpMessagingTemplate messaging;
 public GameNotifications(AccountStore accounts,SimpMessagingTemplate messaging){this.accounts=accounts;this.messaging=messaging;}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
 public void committed(GameService.Changed event){var notice=new Invalidation(UUID.randomUUID(),"game.updated");for(var id:event.members())accounts.byId(id).filter(AccountStore.Account::enabled).ifPresent(a->{try{messaging.convertAndSendToUser(a.email(),"/queue/pairing",notice);}catch(org.springframework.messaging.MessagingException ignored){/* REST polling recovers committed board */}});}
}
