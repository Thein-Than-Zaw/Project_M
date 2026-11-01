package com.couple.privatespace;

import com.couple.privatespace.identity.RegistrationService;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** AUTH-01–03 / AC-F01-01/02/03/05/07/08: actual cookie clients + native PostgreSQL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class AuthenticationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    private final String prefix = "dev02-" + UUID.randomUUID() + "-";
    private final Set<UUID> seededTokens = new HashSet<>();
    private final JsonMapper json = new JsonMapper();
    private final String password = "  ချစ်သူ 🌸 private space  ";

    @BeforeEach void isolatedDatabase() {
        assertTrue(jdbc.queryForObject("SELECT current_database()", String.class).endsWith("_test"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class), "Use an empty dedicated test DB; never delete unrelated users to run tests");
    }
    @AfterEach void removeOnlyOwnedFixtures() {
        if (!jdbc.queryForObject("SELECT current_database()", String.class).endsWith("_test")) return;
        jdbc.update("DELETE FROM registration_token WHERE issued_by IN (SELECT id FROM app_user WHERE email LIKE ?)", prefix + "%");
        for (UUID id : seededTokens) jdbc.update("DELETE FROM registration_token WHERE id=?", id);
        jdbc.update("DELETE FROM app_user WHERE email LIKE ?", prefix + "%");
    }

    @Test void signupValidationLoginRotationLogoutAndPermissions() throws Exception {
        String code = setup(false), email = prefix + "first@example.invalid";
        Browser guest = new Browser();
        Map<String,Object> form = signup(email, code);
        form.remove("relationshipDate");
        var missing = guest.jsonPost("/api/auth/register", form);
        assertEquals(400, missing.statusCode()); assertTrue(missing.body().contains("relationshipDate"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class));
        form.put("relationshipDate", "+999999999-01-01");
        assertEquals(400, guest.jsonPost("/api/auth/register", form).statusCode(), "Transport dates must fit the documented four-digit calendar year");
        form.put("relationshipDate", "2020-02-29"); form.put("confirmPassword", "wrong");
        assertEquals(400, guest.jsonPost("/api/auth/register", form).statusCode());
        form.put("confirmPassword", password);
        var noCsrf = guest.client.send(HttpRequest.newBuilder(uri("/api/auth/register")).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(form))).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, noCsrf.statusCode());
        var registered = guest.jsonPost("/api/auth/register", form);
        assertEquals(201, registered.statusCode(), registered.body());
        assertFalse(registered.body().contains("hash")); assertFalse(registered.body().contains("password"));
        var row = jdbc.queryForMap("SELECT display_name,email,password_hash,signup_relationship_date FROM app_user WHERE account_slot=1");
        assertEquals("နှစ်ယောက် 🌸", row.get("display_name"));
        assertEquals("2020-02-29", row.get("signup_relationship_date").toString());
        assertNotEquals(password, row.get("password_hash"));
        assertTrue(encoder.matches(password, row.get("password_hash").toString()));
        assertFalse(encoder.matches(password.strip(), row.get("password_hash").toString()), "Passwords must retain outer spaces");
        assertEquals(401, guest.get("/api/me").statusCode(), "Register must not silently authenticate");
        assertEquals(401, guest.login(email, "incorrect password").statusCode());
        assertEquals(401, guest.get("/api/me").statusCode());
        String oldCsrf = guest.csrf(); String oldCookie = guest.cookie();
        var signedIn = guest.login("  " + email.toUpperCase(Locale.ROOT) + "  ", password);
        assertEquals(200, signedIn.statusCode(), signedIn.body());
        assertNotEquals(oldCookie, guest.cookie(), "Native login must rotate the session ID");
        assertTrue(signedIn.headers().allValues("Set-Cookie").stream().anyMatch(value -> value.contains("HttpOnly") && value.contains("SameSite=Lax")));
        var self = guest.get("/api/me"); assertEquals(200, self.statusCode());
        assertTrue(self.body().contains("2020-02-29")); assertFalse(self.body().contains("password_hash"));
        assertEquals(403, guest.get("/api/messages").statusCode());
        assertEquals(403, guest.get("/ws").statusCode());
        var stale = guest.client.send(HttpRequest.newBuilder(uri("/api/auth/partner-registration-code")).header("X-CSRF-TOKEN", oldCsrf).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, stale.statusCode());
        assertEquals(403, guest.jsonPost("/api/auth/register", signup(prefix + "other@example.invalid", code)).statusCode());
        String sessionCookie = guest.cookie();
        assertEquals(204, guest.jsonPost("/api/auth/logout", Map.of()).statusCode());
        assertEquals(401, guest.get("/api/me").statusCode());
        assertEquals(401, HttpClient.newHttpClient().send(HttpRequest.newBuilder(uri("/api/me")).header("Cookie",sessionCookie).build(), HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test void partnerCodeIsOneUseScopedAndAccountCapSurvivesConcurrentSignup() throws Exception {
        String firstEmail = prefix + "first@example.invalid";
        Browser first = new Browser();
        assertEquals(201, first.jsonPost("/api/auth/register", signup(firstEmail, setup(false))).statusCode());
        assertEquals(200, first.login(firstEmail, password).statusCode());
        var issued = first.jsonPost("/api/auth/partner-registration-code", Map.of());
        assertEquals(200, issued.statusCode());
        String oldCode = json.readTree(issued.body()).get("code").asText();
        String code = json.readTree(first.jsonPost("/api/auth/partner-registration-code", Map.of()).body()).get("code").asText();
        assertEquals(400, new Browser().jsonPost("/api/auth/register", signup(prefix+"old@example.invalid", oldCode)).statusCode());
        assertEquals(409, new Browser().jsonPost("/api/auth/register", signup(firstEmail, code)).statusCode(), "Duplicate email must not consume code");
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        Browser a = new Browser(), b = new Browser(); a.csrf(); b.csrf();
        try {
            var resultA = pool.submit(() -> { start.await(); return a.jsonPost("/api/auth/register", signup(prefix+"a@example.invalid", code)); });
            var resultB = pool.submit(() -> { start.await(); return b.jsonPost("/api/auth/register", signup(prefix+"b@example.invalid", code)); });
            start.countDown();
            List<Integer> statuses = new ArrayList<>(List.of(resultA.get(20,TimeUnit.SECONDS).statusCode(), resultB.get(20,TimeUnit.SECONDS).statusCode()));
            Collections.sort(statuses); assertEquals(List.of(201,409), statuses);
        } finally { pool.shutdownNow(); }
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM app_user", Integer.class));
        assertEquals(409, new Browser().jsonPost("/api/auth/register", signup(prefix+"third@example.invalid",code)).statusCode());
        String secondEmail = jdbc.queryForObject("SELECT email FROM app_user WHERE account_slot=2",String.class);
        Browser second = new Browser(); assertEquals(200,second.login(secondEmail,password).statusCode());
        assertEquals(403,second.jsonPost("/api/auth/partner-registration-code",Map.of()).statusCode());
        assertEquals(409,first.jsonPost("/api/auth/partner-registration-code",Map.of()).statusCode());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM couple_membership",Integer.class),"Registration must not silently pair users");
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM registration_token WHERE token_hash=?",Integer.class,code),"Raw code must never be stored");
    }

    @Test void expiredInvitationWrongCodesAndGenericRateLimitedLogin() throws Exception {
        Browser guest = new Browser(); String code = setup(true);
        assertEquals(400,guest.jsonPost("/api/auth/register",signup(prefix+"expired@example.invalid",code)).statusCode());
        assertEquals(400,guest.jsonPost("/api/auth/register",signup(prefix+"invalid@example.invalid","x".repeat(43))).statusCode());
        assertEquals(401,guest.get("/api/me").statusCode());
        String email=prefix+"missing@example.invalid";
        for(int i=0;i<10;i++){
            var failure=guest.login(email,password); assertEquals(401,failure.statusCode());
            assertTrue(failure.body().contains("Email or password is incorrect."));
        }
        var limited=guest.login(email,password); assertEquals(429,limited.statusCode());
        assertTrue(Integer.parseInt(limited.headers().firstValue("Retry-After").orElseThrow())>0);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class));
    }

    @Test void disabledAndRevisedAccountsRevokeExistingSessions() throws Exception {
        Browser browser=new Browser();String email=prefix+"revoked@example.invalid";
        assertEquals(201,browser.jsonPost("/api/auth/register",signup(email,setup(false))).statusCode());
        assertEquals(200,browser.login(email,password).statusCode());
        jdbc.update("UPDATE app_user SET security_revision=security_revision+1 WHERE email=?",email);
        assertEquals(401,browser.get("/api/me").statusCode());
        assertEquals(200,browser.login(email,password).statusCode());
        jdbc.update("UPDATE app_user SET enabled=false WHERE email=?",email);
        assertEquals(401,browser.get("/api/me").statusCode());
        assertEquals(401,browser.login(email,password).statusCode());
    }

    @Test void idleSessionExpiresOnRealHttpRequest() throws Exception {
        Browser browser=new Browser();String email=prefix+"idle@example.invalid";
        assertEquals(201,browser.jsonPost("/api/auth/register",signup(email,setup(false))).statusCode());
        assertEquals(200,browser.login(email,password).statusCode());
        assertEquals(200,browser.get("/api/me").statusCode());
        // Test profile uses a real one-minute container timeout; no requests keep it alive.
        Thread.sleep(62000);
        assertEquals(401,browser.get("/api/me").statusCode());
    }

    private String setup(boolean expired) {
        UUID id=UUID.randomUUID();seededTokens.add(id);String code=Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)).substring(0,43);
        jdbc.update("INSERT INTO registration_token(id,kind,token_hash,created_at,expires_at) VALUES (?,'SETUP',?,CURRENT_TIMESTAMP-INTERVAL '2 days',CURRENT_TIMESTAMP + (? * INTERVAL '1 hour'))",id,RegistrationService.digest(code),expired?-1:24);
        return code;
    }
    private Map<String,Object> signup(String email,String code){return new LinkedHashMap<>(Map.of("displayName","နှစ်ယောက် 🌸","email",email,"password",password,"confirmPassword",password,"relationshipDate","2020-02-29","registrationToken",code));}
    private URI uri(String path){return URI.create("http://127.0.0.1:18080"+path);}
    private class Browser {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).build();
        HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(uri(path)).build(),HttpResponse.BodyHandlers.ofString());}
        String csrf()throws Exception{return json.readTree(get("/api/csrf").body()).get("token").asText();}
        String cookie(){return cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).map(c->c.getName()+"="+c.getValue()).findFirst().orElse("");}
        HttpResponse<String> jsonPost(String path,Object body)throws Exception{return client.send(HttpRequest.newBuilder(uri(path)).header("X-CSRF-TOKEN",csrf()).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
        HttpResponse<String> login(String email,String password)throws Exception{return client.send(HttpRequest.newBuilder(uri("/api/auth/login")).header("X-CSRF-TOKEN",csrf()).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString("email="+URLEncoder.encode(email,StandardCharsets.UTF_8)+"&password="+URLEncoder.encode(password,StandardCharsets.UTF_8))).build(),HttpResponse.BodyHandlers.ofString());}
    }
}
