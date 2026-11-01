package com.couple.privatespace.memory;

import com.couple.privatespace.identity.AccountStore;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class MemoryNotifications {
    public record Notice(UUID eventId,String type,UUID memoryId,String notification) { }
    private final JdbcTemplate jdbc;private final AccountStore accounts;private final SimpMessagingTemplate messaging;
    public MemoryNotifications(JdbcTemplate jdbc,AccountStore accounts,SimpMessagingTemplate messaging){this.jdbc=jdbc;this.accounts=accounts;this.messaging=messaging;}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void published(MemoryService.Published event){for(UUID id:jdbc.queryForList("SELECT user_id FROM couple_membership WHERE space_id=? AND user_id<>?",UUID.class,event.space(),event.actor()))accounts.byId(id).filter(AccountStore.Account::enabled).ifPresent(account->{try{messaging.convertAndSendToUser(account.email(),"/queue/pairing",new Notice(event.memoryId(),"memory.published",event.memoryId(),"A new memory was added."));}catch(org.springframework.messaging.MessagingException ignored){/* saved memories reload through private REST */}});}
}
