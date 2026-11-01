package com.couple.privatespace.chat;

import com.couple.privatespace.identity.ApiFailure;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** CHAT-01: per-account, bounded single-instance transport protection; no message text retained. */
@Component
public class ChatLimiter {
    private record Window(long minute,int count) { }
    private final Map<String,Window> windows=new HashMap<>();
    private final int sends,acks;
    public ChatLimiter(@Value("${app.chat.sends-per-minute:60}") int sends,@Value("${app.chat.acks-per-minute:120}") int acks) {this.sends=sends;this.acks=acks;}
    public synchronized void check(UUID actor,boolean send) {
        long minute=Instant.now().getEpochSecond()/60;
        windows.entrySet().removeIf(e->e.getValue().minute()!=minute);
        String key=actor+":"+send;var old=windows.get(key);int count=old==null?1:old.count()+1;
        if(windows.size()>100)throw limited();
        windows.put(key,new Window(minute,Math.min(count,(send?sends:acks)+1)));
        if(count>(send?sends:acks))throw limited();
    }
    private ApiFailure limited(){return new ApiFailure(429,"CHAT_RATE_LIMITED","Slow down a little. Try again in a minute.");}
}
