package com.couple.privatespace.realtime;

import com.couple.privatespace.identity.AccountStore;
import com.couple.privatespace.pairing.PairingService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class PairingNotifications {
    public record Update(UUID eventId,String type,Instant serverTimestamp,String state) { }
    private final SimpMessagingTemplate messaging;
    private final AccountStore accounts;
    public PairingNotifications(SimpMessagingTemplate messaging,AccountStore accounts){this.messaging=messaging;this.accounts=accounts;}
    /** PAIR-02/AC-F02-04: signal only committed state; REST catch-up is authoritative. */
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void paired(PairingService.Changed event){
        var update=new Update(UUID.randomUUID(),"pairing.updated",Instant.now(),"ACTIVE");
        for(var id:event.members())accounts.byId(id).filter(AccountStore.Account::enabled).ifPresent(account->{
            try{messaging.convertAndSendToUser(account.email(),"/queue/pairing",update);}catch(org.springframework.messaging.MessagingException ignored){ /* poll/reconnect reconciles committed DB state */ }
        });
    }
    /** HOME-02/DATE-03: state-only invalidation; same authenticated queue, no new broker permission. */
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void homeChanged(com.couple.privatespace.home.HomeService.Changed event){
        var update=new Update(UUID.randomUUID(),event.type(),Instant.now(),"ACTIVE");
        for(var id:event.members())accounts.byId(id).filter(AccountStore.Account::enabled).ifPresent(account->{
            try{messaging.convertAndSendToUser(account.email(),"/queue/pairing",update);}catch(org.springframework.messaging.MessagingException ignored){ /* REST catch-up retains committed state */ }
        });
    }
}
