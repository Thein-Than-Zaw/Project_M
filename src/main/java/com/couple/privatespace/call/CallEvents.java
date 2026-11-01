package com.couple.privatespace.call;

import com.couple.privatespace.identity.AccountStore;
import java.util.*;
import org.springframework.messaging.simp.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
public class CallEvents {
    public record Update(UUID eventId,String type,CallService.View call) { }
    public record Signal(UUID eventId,String type,UUID callId,UUID negotiationId,Map<String,Object> payload) { }
    private final AccountStore accounts;private final SimpMessagingTemplate messaging;
    public CallEvents(AccountStore accounts,SimpMessagingTemplate messaging){this.accounts=accounts;this.messaging=messaging;}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void state(CallService.State state){for(UUID id:List.of(state.call().callerId(),state.call().calleeId()))send(id,null,new Update(state.eventId(),"call.updated",state.call()));}
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void relay(CallService.Relay event){send(event.recipient(),event.socketId(),new Signal(event.eventId(),event.type(),event.callId(),event.negotiationId(),event.payload()));}
    private void send(UUID user,String socket,Object payload){
        accounts.byId(user).filter(AccountStore.Account::enabled).ifPresent(account->{try{
            var headers=SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);if(socket!=null)headers.setSessionId(socket);headers.setLeaveMutable(true);
            // A user+missing-session route can fall back to all user sessions. Address the session
            // itself so a disconnect race drops SDP/ICE instead of delivering it to another tab.
            messaging.convertAndSendToUser(socket==null?account.email():socket,"/queue/calls",payload,headers.getMessageHeaders());
        }catch(org.springframework.messaging.MessagingException ignored){/* metadata reconciles through REST; media negotiates again */}});
    }
}
