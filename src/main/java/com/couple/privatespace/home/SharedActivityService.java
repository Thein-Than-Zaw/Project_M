package com.couple.privatespace.home;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
/** HOME-05 / CHAT-07: same private, shared totals on both surfaces. */
@Service
public class SharedActivityService {
 private final JdbcTemplate jdbc;
 public SharedActivityService(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public HomeService.Activity forSpace(UUID space){
  long messages=jdbc.queryForObject("SELECT count(*) FROM chat_message WHERE space_id=? AND deleted_at IS NULL AND type IN ('TEXT','GIFT','FILE')",Long.class,space);
  return jdbc.queryForObject("SELECT count(*) calls,COALESCE(floor(sum(GREATEST(0,extract(epoch FROM(COALESCE(ended_at,CURRENT_TIMESTAMP)-connected_at))))),0)::bigint seconds FROM call_session WHERE space_id=? AND mode='VIDEO' AND connected_at IS NOT NULL",(r,n)->new HomeService.Activity(messages,r.getLong("seconds"),r.getLong("calls")),space);
 }
}
