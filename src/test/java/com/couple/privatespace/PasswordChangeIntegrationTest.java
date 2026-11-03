package com.couple.privatespace;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class PasswordChangeIntegrationTest {
 @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired SimpUserRegistry registry;
 final ChatIntegrationTest f=new ChatIntegrationTest();final String next="  new private password 🌸  ";
 @BeforeEach void setup()throws Exception{f.jdbc=jdbc;f.encoder=encoder;f.registry=registry;f.fixtures();}
 @AfterEach void cleanup(){f.cleanup();}
 Map<String,Object> form(String old,String password,String confirm){return Map.of("currentPassword",old,"newPassword",password,"confirmPassword",confirm);}
 HttpResponse<String> change(PairingIntegrationTest.Browser browser,Object body,boolean csrf)throws Exception{var r=HttpRequest.newBuilder(f.transport.uri("/api/auth/change-password")).header("Content-Type","application/json");if(csrf)r.header("X-CSRF-TOKEN",browser.csrf());return browser.client.send(r.POST(HttpRequest.BodyPublishers.ofString(f.transport.json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
 HttpResponse<String> login(PairingIntegrationTest.Browser browser,String password)throws Exception{return browser.client.send(HttpRequest.newBuilder(f.transport.uri("/api/auth/login")).header("X-CSRF-TOKEN",browser.csrf()).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("email="+URLEncoder.encode(f.prefix+"a@example.invalid",StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());}
 long revision(){return jdbc.queryForObject("SELECT security_revision FROM app_user WHERE id=?",Long.class,f.aId);}
 String hash(){return jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=?",String.class,f.aId);}
 @Test void successInvalidatesAllOwnSessionsSocketsAndOldPasswordButPreservesPartner()throws Exception{
  var another=f.transport.new Browser();another.login(f.prefix+"a@example.invalid");
  try(var a=f.socket(f.a,f.prefix+"a@example.invalid");var other=f.socket(another,f.prefix+"a@example.invalid");var b=f.socket(f.b,f.prefix+"b@example.invalid")){
   long rev=revision();var response=change(f.a,form(f.transport.password,next,next),true);assertEquals(204,response.statusCode(),response.body());assertTrue(response.body().isEmpty());assertEquals(rev+1,revision());assertTrue(encoder.matches(next,hash()));assertFalse(encoder.matches(next.strip(),hash()));
   a.closed.get(5,TimeUnit.SECONDS);other.closed.get(5,TimeUnit.SECONDS);assertFalse(b.closed.isDone());assertEquals(401,f.a.get("/api/me").statusCode());assertEquals(401,another.get("/api/me").statusCode());assertEquals(200,f.b.get("/api/home").statusCode());assertEquals(0,jdbc.queryForObject("SELECT security_revision FROM app_user WHERE id=?",Long.class,f.bId));
   var fresh=f.transport.new Browser();assertEquals(401,login(fresh,f.transport.password).statusCode());assertEquals(200,login(fresh,next).statusCode());assertEquals(200,fresh.get("/api/home").statusCode());
  }
 }
 @Test void wrongCurrentPasswordNeverChangesHashRevisionOrSession()throws Exception{
  String before=hash();try(var a=f.socket(f.a,f.prefix+"a@example.invalid")){var response=change(f.a,form("incorrect password",next,next),true);assertEquals(400,response.statusCode());assertEquals(before,hash());assertEquals(0,revision());assertFalse(response.body().contains(next));assertFalse(a.closed.isDone());assertEquals(200,f.a.get("/api/me").statusCode());}
 }
 @Test void unicodeBoundariesConfirmationAndDifferentPasswordAreEnforced()throws Exception{
  for(var body:List.of(form(f.transport.password,"short","short"),form(f.transport.password,next,"mismatch"),form(f.transport.password,f.transport.password,f.transport.password),form(f.transport.password,"🌸".repeat(129),"🌸".repeat(129))))assertEquals(400,change(f.a,body,true).statusCode());
  assertEquals(0,revision());String maximum="🌸".repeat(128);assertEquals(204,change(f.a,form(f.transport.password,maximum,maximum),true).statusCode());assertTrue(encoder.matches(maximum,hash()));
 }
 @Test void cookieCsrfAndSessionActorAreRequired()throws Exception{
  assertEquals(401,change(f.transport.new Browser(),form(f.transport.password,next,next),true).statusCode());assertEquals(403,change(f.a,form(f.transport.password,next,next),false).statusCode());
  var spoof=new HashMap<String,Object>(form(f.transport.password,next,next));spoof.put("accountId",f.bId);assertEquals(204,change(f.a,spoof,true).statusCode());assertTrue(encoder.matches(f.transport.password,jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=?",String.class,f.bId)));assertEquals(200,f.b.get("/api/home").statusCode());
 }
 @Test void concurrentOwnSessionsCannotOverwriteACommittedPassword()throws Exception{
  var another=f.transport.new Browser();another.login(f.prefix+"a@example.invalid");var pool=Executors.newFixedThreadPool(2);var latch=new CountDownLatch(1);String alternative="another strong secret 🌷";
  try{var one=pool.submit(()->{latch.await();return change(f.a,form(f.transport.password,next,next),true).statusCode();});var two=pool.submit(()->{latch.await();return change(another,form(f.transport.password,alternative,alternative),true).statusCode();});latch.countDown();int a=one.get(15,TimeUnit.SECONDS),b=two.get(15,TimeUnit.SECONDS);assertTrue((a==204&&Set.of(401,409).contains(b))||(b==204&&Set.of(401,409).contains(a)),a+" / "+b);assertEquals(1,revision());assertEquals(a==204,encoder.matches(next,hash()));}finally{pool.shutdownNow();}
 }
 @Test void failedTransactionDoesNotLogUserOutOrRevokeSockets()throws Exception{
  String before=hash();jdbc.execute("ALTER TABLE app_user ADD CONSTRAINT account01_password_failure CHECK(security_revision=0)");try(var a=f.socket(f.a,f.prefix+"a@example.invalid")){assertEquals(409,change(f.a,form(f.transport.password,next,next),true).statusCode());assertEquals(before,hash());assertEquals(0,revision());assertFalse(a.closed.isDone());assertEquals(200,f.a.get("/api/me").statusCode());}finally{jdbc.execute("ALTER TABLE app_user DROP CONSTRAINT account01_password_failure");}
 }
 @Test void failedCurrentPasswordAttemptsAreRateLimitedBeforeMoreHashing()throws Exception{
  for(int i=0;i<10;i++)assertEquals(400,change(f.a,form("wrong current",next,next),true).statusCode());var limited=change(f.a,form("wrong current",next,next),true);assertEquals(429,limited.statusCode());assertTrue(Long.parseLong(limited.headers().firstValue("Retry-After").orElseThrow())>0);assertEquals(0,revision());
 }
}
