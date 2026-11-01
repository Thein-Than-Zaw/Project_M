package com.couple.privatespace.call;

import com.couple.privatespace.identity.ApiFailure;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** CALL-03: coturn REST shared secret stays on server; browser receives expiring credentials only. */
@Component
public class RtcConfiguration {
    public record IceServer(List<String> urls,String username,String credential) { }
    public record Config(List<IceServer> iceServers,boolean relayConfigured,Instant expiresAt) { }
    private final List<String> turn,stun;private final String secret;
    public RtcConfiguration(@Value("${app.calls.turn-urls:}") String turn,@Value("${app.calls.stun-urls:}") String stun,@Value("${app.calls.turn-secret:}") String secret){
        this.turn=urls(turn,true);this.stun=urls(stun,false);this.secret=secret;
        if(!this.turn.isEmpty()&&(secret.length()<32||secret.length()>256))throw new IllegalArgumentException("Configured TURN requires a server-only secret of 32 to 256 characters");
    }
    private List<String> urls(String input,boolean relay){
        if(input.isBlank())return List.of();var urls=Arrays.stream(input.split(",")).map(String::strip).toList();
        if(urls.size()>4||urls.stream().anyMatch(s->s.length()>200||!s.matches((relay?"turns?":"stuns?")+":[A-Za-z0-9.\\[\\]:_-]+(\\?transport=(udp|tcp))?")))throw new IllegalArgumentException("Invalid configured ICE server URL");return urls;
    }
    public Config configuration(UUID actor){
        Instant expires=Instant.now().plusSeconds(3600);var servers=new ArrayList<IceServer>();if(!stun.isEmpty())servers.add(new IceServer(stun,null,null));
        if(!turn.isEmpty())try{String user=expires.getEpochSecond()+":"+actor;var mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA1"));servers.add(new IceServer(turn,user,Base64.getEncoder().encodeToString(mac.doFinal(user.getBytes(StandardCharsets.UTF_8)))));}catch(java.security.GeneralSecurityException error){throw new ApiFailure(503,"RTC_CONFIG_UNAVAILABLE","Call connection settings are temporarily unavailable.");}
        return new Config(servers,!turn.isEmpty(),expires);
    }
}
