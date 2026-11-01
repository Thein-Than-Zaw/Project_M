package com.couple.privatespace.memory;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MemoryMaintenance {
    public record Result(int deleted,int pending) { }
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(MemoryMaintenance.class);
    private final JdbcTemplate jdbc;private final MemoryPurger purger;private final MemoryFiles files;
    public MemoryMaintenance(JdbcTemplate jdbc,MemoryPurger purger,MemoryFiles files){this.jdbc=jdbc;this.purger=purger;this.files=files;}
    public Result purgeIds(List<UUID> ids){int deleted=0,pending=0;for(UUID id:ids)try{if(purger.purge(id))deleted++;else pending++;}catch(RuntimeException error){pending++;log.warn("Memory removal pending; will retry. Memory {}",id);}return new Result(deleted,pending);}
    @Scheduled(fixedDelayString="${app.memories.maintenance-ms:60000}",initialDelayString="${app.memories.maintenance-initial-ms:1000}")
    public void runOnce(){
        try{
            purger.claimExpired();purgeIds(jdbc.queryForList("SELECT id FROM memory WHERE state='PURGING' LIMIT 100",UUID.class));
            for(UUID id:jdbc.queryForList("SELECT id FROM memory_asset WHERE state='DISCARDED' AND memory_id IS NULL LIMIT 100",UUID.class))try{purger.purgeAsset(id);}catch(RuntimeException error){log.warn("Unpublished file removal pending; will retry. Asset {}",id);}
            reconcile(files.stage,true);reconcile(files.objects,false);
        }catch(RuntimeException error){log.warn("Memory maintenance could not complete; durable work will retry.");}
    }
    private void reconcile(Path directory,boolean staging){
        try(var paths=Files.list(directory)){
            var cutoff=Instant.now().minus(Duration.ofHours(24));int checked=0;
            for(Path path:paths.toList()){
                String name=path.getFileName().toString();if(!name.matches("[a-f0-9-]{36}\\.(data|jpg|log)")||Files.isSymbolicLink(path)||!Files.getLastModifiedTime(path).toInstant().isBefore(cutoff))continue;
                UUID id;try{id=UUID.fromString(name.substring(0,36));}catch(IllegalArgumentException ignored){continue;}
                if(staging||jdbc.queryForObject("SELECT count(*) FROM memory_asset WHERE id=?",Integer.class,id)==0)Files.deleteIfExists(path);
                if(++checked>=100)break;
            }
        }catch(Exception error){log.warn("Private file reconciliation will retry.");}
    }
}
