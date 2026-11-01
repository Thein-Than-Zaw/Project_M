package com.couple.privatespace.call;

import com.couple.privatespace.realtime.SocketRegistry;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CallMaintenance {
    private final CallService calls;private final Set<String> revoked=ConcurrentHashMap.newKeySet();
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(CallMaintenance.class);
    public CallMaintenance(CallService calls){this.calls=calls;}
    @EventListener public void revoked(SocketRegistry.Revoked event){revoked.addAll(event.socketIds());}
    @Scheduled(fixedDelayString="${app.calls.maintenance-ms:2000}",initialDelayString="${app.calls.maintenance-initial-ms:2000}")
    public void runOnce(){try{var batch=new HashSet<>(revoked);if(!batch.isEmpty()){calls.revoked(batch);revoked.removeAll(batch);}for(UUID id:calls.pending())calls.tick(id);}catch(RuntimeException error){log.warn("Call lifecycle reconciliation will retry.");}}
}
