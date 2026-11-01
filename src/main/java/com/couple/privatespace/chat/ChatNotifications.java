package com.couple.privatespace.chat;

import com.couple.privatespace.identity.AccountStore;
import java.time.Instant;
import java.util.*;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class ChatNotifications {
    public record Event(UUID eventId,String type,Instant serverTimestamp,ChatService.Message message,List<ChatService.Receipt> receipts,String notification) { }
    public record Invalidation(UUID eventId,String type,String state) { }
    private final AccountStore accounts;private final SimpMessagingTemplate messaging;
    public ChatNotifications(AccountStore accounts,SimpMessagingTemplate messaging){this.accounts=accounts;this.messaging=messaging;}
    /** CHAT-02: transaction completion precedes ACK/content delivery; lost dispatch recovers through history. */
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void committed(ChatService.Changed change){
        var event=new Event(UUID.randomUUID(),change.type(),Instant.now(),change.message(),change.receipts(),null);
        for(var user:change.members())accounts.byId(user).filter(AccountStore.Account::enabled).ifPresent(account->{
            boolean gift=change.message()!=null&&"GIFT".equals(change.message().type())&&!change.message().senderId().equals(user);
            var recipientEvent=gift?new Event(event.eventId(),event.type(),event.serverTimestamp(),event.message(),event.receipts(),"You received a little gift."):event;
            try{messaging.convertAndSendToUser(account.email(),"/queue/chat",recipientEvent);}catch(MessagingException ignored){ /* REST reconciles committed state */ }
            try{messaging.convertAndSendToUser(account.email(),"/queue/pairing",new Invalidation(event.eventId(),"chat.updated","ACTIVE"));}catch(MessagingException ignored){ /* Home badge polls */ }
        });
    }
}
