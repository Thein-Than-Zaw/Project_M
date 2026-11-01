package com.couple.privatespace.memory;

import com.couple.privatespace.home.HomeService;
import java.io.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Durable PURGING rows survive storage failure/restart. No Restore or file access after claim. */
@Service
public class MemoryPurger {
    private final JdbcTemplate jdbc;private final MemoryFiles files;private final ApplicationEventPublisher events;
    public MemoryPurger(JdbcTemplate jdbc,MemoryFiles files,ApplicationEventPublisher events){this.jdbc=jdbc;this.files=files;this.events=events;}
    @Transactional public void claimExpired(){jdbc.update("UPDATE memory SET state='PURGING',version=version+1 WHERE state='TRASHED' AND expires_at<=clock_timestamp()");jdbc.update("UPDATE memory_asset SET state='DISCARDED' WHERE memory_id IS NULL AND state='READY' AND created_at<=clock_timestamp()-INTERVAL '24 hours'");}
    @Transactional public boolean purge(UUID id){
        var rows=jdbc.queryForList("SELECT space_id FROM memory WHERE id=? AND state='PURGING' FOR UPDATE",UUID.class,id);if(rows.isEmpty())return jdbc.queryForObject("SELECT count(*) FROM memory WHERE id=?",Integer.class,id)==0;
        for(UUID asset:jdbc.queryForList("SELECT id FROM memory_asset WHERE memory_id=?",UUID.class,id))remove(asset);
        jdbc.update("DELETE FROM memory WHERE id=? AND state='PURGING'",id);events.publishEvent(new HomeService.Changed(jdbc.queryForList("SELECT user_id FROM couple_membership WHERE space_id=?",UUID.class,rows.getFirst()),"memory.updated"));return true;
    }
    @Transactional public void purgeAsset(UUID id){var ids=jdbc.queryForList("SELECT id FROM memory_asset WHERE id=? AND memory_id IS NULL AND state='DISCARDED' FOR UPDATE",UUID.class,id);if(!ids.isEmpty()){remove(id);jdbc.update("DELETE FROM memory_asset WHERE id=?",id);}}
    private void remove(UUID id){try{files.delete(id);}catch(IOException error){throw new UncheckedIOException("Private file removal must be retried",error);}}
}
