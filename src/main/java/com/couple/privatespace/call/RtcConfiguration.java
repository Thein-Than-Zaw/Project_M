package com.couple.privatespace.call;

import com.couple.privatespace.identity.ApiFailure;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** CALL-03: coturn master secret stays server-side; managed TURN access is paired-user only. */
@Component
public class RtcConfiguration {
    public record IceServer(List<String> urls,String username,String credential) { }
    public record Config(List<IceServer> iceServers,boolean relayConfigured,Instant expiresAt) { }
    private final List<String> turn,stun;private final String secret,username,password;
    @org.springframework.beans.factory.annotation.Autowired
    public RtcConfiguration(@Value("${app.calls.turn-urls:}") String turn,@Value("${app.calls.stun-urls:}") String stun,@Value("${app.calls.turn-secret:}") String secret,
            @Value("${app.calls.turn-username:}") String username,@Value("${app.calls.turn-password:}") String password){
        this.turn=urls(turn,true);this.stun=urls(stun,false);this.secret=secret;
        this.username=username;this.password=password;
        boolean managed=!username.isBlank()||!password.isBlank();
        if(managed&&(this.turn.isEmpty()||username.isBlank()||password.isBlank()||!secret.isBlank()||username.length()>256||password.length()>1024||username.codePoints().anyMatch(Character::isISOControl)||password.codePoints().anyMatch(Character::isISOControl)))
            throw new IllegalArgumentException("Managed TURN requires URLs, username and credential password, without a coturn shared secret");
        if(!this.turn.isEmpty()&&!managed&&(secret.length()<32||secret.length()>256))throw new IllegalArgumentException("Configured TURN requires managed credentials or a server-only secret of 32 to 256 characters");
    }
    public RtcConfiguration(String turn,String stun,String secret){this(turn,stun,secret,"","");}
    private List<String> urls(String input,boolean relay){
        if(input.isBlank())return List.of();var urls=Arrays.stream(input.split(",")).map(String::strip).toList();
        if(urls.size()>4||urls.stream().anyMatch(s->s.length()>200||!s.matches((relay?"turns?":"stuns?")+":[A-Za-z0-9.\\[\\]:_-]+(\\?transport=(udp|tcp))?")))throw new IllegalArgumentException("Invalid configured ICE server URL");return urls;
    }
    public Config configuration(UUID actor){
        Instant expires=Instant.now().plusSeconds(3600);var servers=new ArrayList<IceServer>();if(!stun.isEmpty())servers.add(new IceServer(stun,null,null));
        if(!turn.isEmpty()&&!username.isBlank())servers.add(new IceServer(turn,username,password));
        else if(!turn.isEmpty())try{String user=expires.getEpochSecond()+":"+actor;var mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA1"));servers.add(new IceServer(turn,user,Base64.getEncoder().encodeToString(mac.doFinal(user.getBytes(StandardCharsets.UTF_8)))));}catch(java.security.GeneralSecurityException error){throw new ApiFailure(503,"RTC_CONFIG_UNAVAILABLE","Call connection settings are temporarily unavailable.");}
        // expiresAt is the browser's config refresh deadline. Managed credential
        // lifetime/rotation is controlled by the provider, not this timestamp.
        return new Config(servers,!turn.isEmpty(),expires);
    }
}
