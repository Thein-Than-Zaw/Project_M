package com.couple.privatespace.chat;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.realtime.SocketRegistry;
import java.security.Principal;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ChatController {
    public record Reply(String type,UUID clientMessageId,ChatService.Message message,List<ChatService.Receipt> receipts,String code,String detail) { }
    private final ChatService chat;private final SocketRegistry sockets;
    public ChatController(ChatService chat,SocketRegistry sockets){this.chat=chat;this.sockets=sockets;}
    @GetMapping("/api/chat") @ResponseBody
    public ChatService.Page history(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam(required=false) Long before,@RequestParam(required=false) Long after){return chat.history(actor.accountId(),before,after);}
    @GetMapping("/api/chat/state") @ResponseBody
    public ChatService.State state(@AuthenticationPrincipal AccountPrincipal actor){return chat.state(actor.accountId());}
    @GetMapping("/api/gifts") @ResponseBody
    public ChatService.Catalogue gifts(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam(required=false) String pack,@RequestParam(required=false) String q,@RequestParam(defaultValue="0") int offset){return chat.gifts(actor.accountId(),pack,q,offset);}
    @GetMapping("/api/gifts/history") @ResponseBody
    public ChatService.GiftHistory giftsHistory(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam(defaultValue="all") String direction,@RequestParam(required=false) Long before){return chat.giftHistory(actor.accountId(),direction,before);}
    @GetMapping("/api/chat/receipts") @ResponseBody
    public List<ChatService.Receipt> receipts(@AuthenticationPrincipal AccountPrincipal actor,@RequestParam List<UUID> ids){return chat.receipts(actor.accountId(),ids);}
    @MessageMapping("/chat.send") @SendToUser(value="/queue/chat",broadcast=false)
    public Reply send(Principal principal,@Header("simpSessionId") String session,ChatService.Send command){
        try{return new Reply("chat.sent",command.clientMessageId(),chat.send(actor(principal,session),command),List.of(),null,null);}
        catch(ApiFailure error){return error(command.clientMessageId(),error.code(),error.getMessage());}
        catch(DataAccessException error){return error(command.clientMessageId(),"MESSAGE_NOT_SAVED","We could not save this message. Retry with the same message identifier.");}
    }
    @MessageMapping("/chat.delivered") @SendToUser(value="/queue/chat",broadcast=false)
    public Reply delivered(Principal principal,@Header("simpSessionId") String session,ChatService.Ack command){return ack(principal,session,command,false);}
    @MessageMapping("/chat.read") @SendToUser(value="/queue/chat",broadcast=false)
    public Reply read(Principal principal,@Header("simpSessionId") String session,ChatService.Ack command){return ack(principal,session,command,true);}
    private Reply ack(Principal principal,String session,ChatService.Ack command,boolean read){
        try{return new Reply("chat.ack",null,null,chat.acknowledge(actor(principal,session),command,read),null,null);}
        catch(ApiFailure error){return error(null,error.code(),error.getMessage());}
        catch(DataAccessException error){return error(null,"ACK_NOT_SAVED","Receipt could not be saved. Reconnect to try again.");}
    }
    @MessageExceptionHandler @SendToUser(value="/queue/chat",broadcast=false)
    public Reply malformed(Exception ignored){return error(null,"INVALID_CHAT_COMMAND","This chat request could not be processed. Check your connection and retry.");}
    private UUID actor(Principal principal,String session){
        if(!sockets.validChat(session)||!(principal instanceof Authentication auth)||!(auth.getPrincipal() instanceof AccountPrincipal actor))throw new ApiFailure(403,"PAIRING_REQUIRED","Your chat session is no longer available.");
        return actor.accountId();
    }
    private Reply error(UUID id,String code,String detail){return new Reply("chat.error",id,null,List.of(),code,detail);}
}
