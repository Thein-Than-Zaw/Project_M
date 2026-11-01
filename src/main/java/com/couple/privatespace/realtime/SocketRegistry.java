package com.couple.privatespace.realtime;

import com.couple.privatespace.identity.*;
import com.couple.privatespace.pairing.PairingService;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.concurrent.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

/** PRIV-02 / PAIR-03: subscription authorization alone does not revoke an existing socket. */
@Component
public class SocketRegistry {
    public static final String HTTP_SESSION="couple.httpSession";
    private record Entry(WebSocketSession socket,HttpSession http,AccountPrincipal actor) { }
    private final ConcurrentMap<String,Entry> entries=new ConcurrentHashMap<>();
    private final AccountStore accounts;
    private final PairingService pairing;
    public SocketRegistry(AccountStore accounts,PairingService pairing){this.accounts=accounts;this.pairing=pairing;}
    public void add(WebSocketSession socket){
        if(socket.getPrincipal() instanceof Authentication auth && auth.getPrincipal() instanceof AccountPrincipal actor
                && socket.getAttributes().get(HTTP_SESSION) instanceof HttpSession http) entries.put(socket.getId(),new Entry(socket,http,actor));
    }
    public void remove(String id){entries.remove(id);}
    public boolean validChat(String id){var entry=entries.get(id);return entry!=null&&valid(id,true)&&pairing.active(entry.actor().accountId());}
    public boolean valid(String id,boolean requireMembership){
        var entry=entries.get(id);
        if(entry==null)return false;
        try {
            int idle=entry.http().getMaxInactiveInterval();
            boolean alive=idle<=0 || Instant.now().toEpochMilli()-entry.http().getLastAccessedTime()<idle*1000L;
            boolean identity=accounts.byId(entry.actor().accountId()).filter(a->a.enabled()&&a.revision()==entry.actor().securityRevision()).isPresent();
            if(alive&&identity&&(!requireMembership||pairing.member(entry.actor().accountId())))return true;
        } catch(IllegalStateException invalidated){ /* invalid/expired HTTP session */ }
        close(id);return false;
    }
    public void closeHttp(String httpId){entries.forEach((id,entry)->{try{if(entry.http().getId().equals(httpId))close(id);}catch(IllegalStateException invalid){close(id);}});}
    private void close(String id){var entry=entries.remove(id);if(entry!=null)try{entry.socket().close(CloseStatus.POLICY_VIOLATION);}catch(java.io.IOException ignored){ /* already closing */ }}
    @Scheduled(fixedDelay=2000)
    public void sweep(){entries.keySet().forEach(id->valid(id,false));}
}
