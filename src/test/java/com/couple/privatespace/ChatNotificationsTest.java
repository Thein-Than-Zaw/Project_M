package com.couple.privatespace;

import com.couple.privatespace.chat.*;
import com.couple.privatespace.identity.AccountStore;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import static org.junit.jupiter.api.Assertions.*;

class ChatNotificationsTest {
 @Test void onlyPartnerReceivesIncomingMessageMarkerAndReceiptsNeverChime(){
  UUID sender=UUID.randomUUID(),partner=UUID.randomUUID(),id=UUID.randomUUID();
  var delivered=new ArrayList<org.springframework.messaging.Message<?>>();
  var channel=new ExecutorSubscribableChannel();channel.subscribe(delivered::add);
  var accounts=new AccountStore(null){@Override public Optional<Account> byId(UUID user){return Optional.of(new Account(user,1,"Person",user+"@example.invalid","unused",LocalDate.of(2025,1,1),true,0));}};
  var notifications=new ChatNotifications(accounts,new SimpMessagingTemplate(channel));
  var message=new ChatService.Message(id,1,sender,UUID.randomUUID(),"hello","TEXT",null,null,null,Instant.now(),null,null);
  notifications.committed(new ChatService.Changed(List.of(sender,partner),"chat.message",message,List.of()));
  var markers=delivered.stream().map(org.springframework.messaging.Message::getPayload).filter(ChatNotifications.Invalidation.class::isInstance).map(ChatNotifications.Invalidation.class::cast).toList();
  assertEquals(2,markers.size());assertNull(markers.get(0).incomingMessageId());assertEquals(id,markers.get(1).incomingMessageId());
  delivered.clear();notifications.committed(new ChatService.Changed(List.of(sender,partner),"chat.receipts",null,List.of()));
  assertTrue(delivered.stream().map(org.springframework.messaging.Message::getPayload).filter(ChatNotifications.Invalidation.class::isInstance).map(ChatNotifications.Invalidation.class::cast).allMatch(item->item.incomingMessageId()==null));
 }
}
