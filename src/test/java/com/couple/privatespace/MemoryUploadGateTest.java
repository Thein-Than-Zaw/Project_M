package com.couple.privatespace;

import com.couple.privatespace.identity.AccountPrincipal;
import com.couple.privatespace.memory.MemoryUploadGate;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

class MemoryUploadGateTest {
    private void login() {
        var actor = new AccountPrincipal(UUID.randomUUID(), "gate@example.invalid", "synthetic", true, 0);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(actor, "", actor.getAuthorities()));
    }
    private MockHttpServletRequest request(boolean token) {
        var request = new MockHttpServletRequest("POST", "/api/memory-uploads");
        if (token) request.addHeader("X-CSRF-TOKEN", "validated-by-the-next-filter");
        return request;
    }
    @Test void anonymousAndMissingHeaderRejectBeforeTouchingMultipartBody() throws Exception {
        var gate = new MemoryUploadGate();var response = new MockHttpServletResponse();
        gate.doFilter(request(true), response, (q,s)->fail("Must not reach multipart parsing"));assertEquals(401,response.getStatus());
        try {login();response=new MockHttpServletResponse();gate.doFilter(request(false),response,(q,s)->fail("Must not reach multipart parsing"));assertEquals(403,response.getStatus());}
        finally {SecurityContextHolder.clearContext();}
    }
    @Test void twoSlowRequestsBoundSpoolingAndReleasedPermitAcceptsRetry() throws Exception {
        var gate=new MemoryUploadGate();var pool=Executors.newFixedThreadPool(2);var entered=new CountDownLatch(2);var release=new CountDownLatch(1);
        Callable<Void> slow=()->{try{login();gate.doFilter(request(true),new MockHttpServletResponse(),(q,s)->{entered.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new java.io.IOException(e);}});return null;}finally{SecurityContextHolder.clearContext();}};
        var first=pool.submit(slow);var second=pool.submit(slow);
        try {
            assertTrue(entered.await(5,TimeUnit.SECONDS));login();var rejected=new MockHttpServletResponse();
            gate.doFilter(request(true),rejected,(q,s)->fail("Third request must not spool"));assertEquals(429,rejected.getStatus());assertEquals("2",rejected.getHeader("Retry-After"));
            release.countDown();first.get(5,TimeUnit.SECONDS);second.get(5,TimeUnit.SECONDS);
            var accepted=new java.util.concurrent.atomic.AtomicBoolean();gate.doFilter(request(true),new MockHttpServletResponse(),(q,s)->accepted.set(true));assertTrue(accepted.get());
        }finally{release.countDown();pool.shutdownNow();SecurityContextHolder.clearContext();}
    }
}
