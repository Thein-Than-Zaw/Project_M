package com.couple.privatespace.call;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.realtime.SocketRegistry;
import java.security.Principal;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class CallController {
    public record Reply(UUID eventId,String type,CallService.View call,String code,String detail) { }
    private final CallService calls;private final SocketRegistry sockets;private final RtcConfiguration rtc;
    public CallController(CallService calls,SocketRegistry sockets,RtcConfiguration rtc){this.calls=calls;this.sockets=sockets;this.rtc=rtc;}
    @GetMapping("/api/calls/current") @ResponseBody public CallService.Current current(@AuthenticationPrincipal AccountPrincipal actor){return calls.current(actor.accountId());}
    @GetMapping("/api/calls/recent") @ResponseBody public List<CallService.View> recent(@AuthenticationPrincipal AccountPrincipal actor){return calls.recent(actor.accountId());}
    @GetMapping("/api/calls/{id}") @ResponseBody public CallService.View get(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id){return calls.get(actor.accountId(),id);}
    @PostMapping("/api/calls/{id}/leave") @ResponseBody public CallService.View leave(@AuthenticationPrincipal AccountPrincipal actor,@PathVariable UUID id,@RequestBody CallService.Leave body){return calls.leave(actor.accountId(),id,body);}
    @GetMapping("/api/rtc/config") public ResponseEntity<RtcConfiguration.Config> config(@AuthenticationPrincipal AccountPrincipal actor){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(rtc.configuration(actor.accountId()));}
    @MessageMapping("/call.{operation}") @SendToUser(value="/queue/calls",broadcast=false)
    public Reply command(Principal principal,@Header("simpSessionId") String session,@DestinationVariable String operation,CallService.Command command){
        UUID event=command==null?null:command.eventId();
        try{
            if(!sockets.validChat(session)||!(principal instanceof Authentication auth)||!(auth.getPrincipal() instanceof AccountPrincipal actor))throw new ApiFailure(403,"CALL_SESSION_EXPIRED","Sign in again before calling.");
            return new Reply(event,"call.ack",calls.command(actor.accountId(),session,operation,command),null,null);
        }catch(ApiFailure error){return new Reply(event,"call.error",null,error.code(),error.getMessage());}
        catch(DataAccessException error){return new Reply(event,"call.error",null,"CALL_NOT_CONFIRMED","This call action was not confirmed. Reconnect and retry with the same identifier.");}
    }
    @MessageExceptionHandler @SendToUser(value="/queue/calls",broadcast=false)
    public Reply invalid(Exception ignored){return new Reply(null,"call.error",null,"INVALID_CALL_COMMAND","This call action could not be processed.");}
}
