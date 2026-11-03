package com.couple.privatespace.identity;

import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** AUTH-04: current-password proof, untrimmed password policy and atomic credential revision. */
@Service
public class PasswordService {
 public record Change(String currentPassword,String newPassword,String confirmPassword){}
 public record Changed(UUID accountId){}
 private final JdbcTemplate jdbc;private final AccountStore accounts;private final PasswordEncoder encoder;private final ApplicationEventPublisher events;
 public PasswordService(JdbcTemplate jdbc,AccountStore accounts,PasswordEncoder encoder,ApplicationEventPublisher events){this.jdbc=jdbc;this.accounts=accounts;this.encoder=encoder;this.events=events;}
 @Transactional
 public void change(UUID actor,long expectedRevision,Change form){
  if(form==null||form.currentPassword()==null||form.currentPassword().isEmpty()||length(form.currentPassword())>128)throw invalid("currentPassword","Enter your current password.");
  if(form.newPassword()==null||length(form.newPassword())<12||length(form.newPassword())>128)throw invalid("newPassword","Use between 12 and 128 characters.");
  if(form.confirmPassword()==null||!form.newPassword().equals(form.confirmPassword()))throw invalid("confirmPassword","Passwords must match.");
  var account=accounts.byId(actor).filter(a->a.enabled()&&a.revision()==expectedRevision).orElseThrow(()->new ApiFailure(401,"SESSION_EXPIRED","Please sign in again."));
  if(!encoder.matches(form.currentPassword(),account.hash()))throw invalid("currentPassword","Current password is incorrect.");
  if(encoder.matches(form.newPassword(),account.hash()))throw invalid("newPassword","Choose a different new password.");
  String encoded=encoder.encode(form.newPassword());
  if(jdbc.update("UPDATE app_user SET password_hash=?,security_revision=security_revision+1,updated_at=clock_timestamp() WHERE id=? AND security_revision=? AND enabled",encoded,actor,expectedRevision)!=1)throw new ApiFailure(409,"PASSWORD_CHANGED","Your password changed in another session. Sign in again.");
  events.publishEvent(new Changed(actor));
 }
 private int length(String value){return value.codePointCount(0,value.length());}
 private ApiFailure invalid(String field,String message){return new ApiFailure(400,"VALIDATION_FAILED",message,Map.of(field,message));}
}
