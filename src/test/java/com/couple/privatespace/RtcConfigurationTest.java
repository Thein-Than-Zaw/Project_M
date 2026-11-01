package com.couple.privatespace;

import com.couple.privatespace.call.RtcConfiguration;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class RtcConfigurationTest {
 @Test void authenticatedIdentityGetsExpiringCoturnCredentialsWithoutSharedSecret()throws Exception{
  String secret="Synthetic-turn-secret-for-native-tests-only";UUID actor=UUID.randomUUID();var config=new RtcConfiguration("turn:127.0.0.1:3478?transport=udp","stun:127.0.0.1:3478",secret).configuration(actor);
  assertTrue(config.relayConfigured());assertTrue(config.expiresAt().isAfter(Instant.now().plusSeconds(3500)));var server=config.iceServers().getLast();assertEquals(config.expiresAt().getEpochSecond()+":"+actor,server.username());
  var mac=Mac.getInstance("HmacSHA1");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA1"));assertEquals(Base64.getEncoder().encodeToString(mac.doFinal(server.username().getBytes(StandardCharsets.UTF_8))),server.credential());assertFalse(new JsonMapper().writeValueAsString(config).contains(secret));
 }
 @Test void configuredUrlsAndSecretRejectUnsafeOrIncompleteProviderSetup(){
  assertThrows(IllegalArgumentException.class,()->new RtcConfiguration("turn:localhost:3478","","short"));assertThrows(IllegalArgumentException.class,()->new RtcConfiguration("https://untrusted.invalid","","x".repeat(32)));assertThrows(IllegalArgumentException.class,()->new RtcConfiguration("turn:user:password@localhost:3478","","x".repeat(32)));
  var local=new RtcConfiguration("","","").configuration(UUID.randomUUID());assertFalse(local.relayConfigured());assertTrue(local.iceServers().isEmpty());
 }
 @Test void managedProviderCredentialsArePassedUnmodifiedToAuthenticatedRtcConfig(){
  var config=new RtcConfiguration("turn:relay.example.invalid:3478?transport=udp,turn:relay.example.invalid:3478?transport=tcp","stun:stun.example.invalid:3478","","test-user","test-turn-credential").configuration(UUID.randomUUID());
  assertTrue(config.relayConfigured());assertEquals(2,config.iceServers().size());
  var relay=config.iceServers().getLast();assertEquals("test-user",relay.username());assertEquals("test-turn-credential",relay.credential());assertEquals(2,relay.urls().size());
 }
 @Test void incompleteOrAmbiguousManagedCredentialsFailClosed(){
  for(var pair:List.of(List.of("user",""),List.of("","password"),List.of("user\n","password")))assertThrows(IllegalArgumentException.class,()->new RtcConfiguration("turn:relay.example.invalid:3478","","",pair.get(0),pair.get(1)));
  assertThrows(IllegalArgumentException.class,()->new RtcConfiguration("turn:relay.example.invalid:3478","","x".repeat(32),"user","password"));
  assertThrows(IllegalArgumentException.class,()->new RtcConfiguration("","","","user","password"));
 }
}
