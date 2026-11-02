package com.couple.privatespace.memory;

import com.couple.privatespace.identity.AccountPrincipal;
import com.couple.privatespace.identity.ApiFailure;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** MEM-01 resource bound before CSRF parameter lookup / servlet multipart disk spooling. */
public final class MemoryUploadGate extends OncePerRequestFilter {
    private final Semaphore requests = new Semaphore(2);

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") || !java.util.Set.of("/api/memory-uploads","/api/chat/uploads").contains(request.getRequestURI());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal)) {
            ApiFailure.write(response, 401, "UNAUTHENTICATED", "Please sign in to continue."); return;
        }
        // The normal Spring CSRF filter still validates this token. Do not fall back to a multipart parameter.
        if (request.getHeader("X-CSRF-TOKEN") == null) {
            ApiFailure.write(response, 403, "FORBIDDEN", "Refresh and retry with your session token."); return;
        }
        if (request.getContentLengthLong() > 101000000L) {
            ApiFailure.write(response, 413, "FILE_TOO_LARGE", "This file exceeds its upload limit."); return;
        }
        if (!requests.tryAcquire()) {
            response.setHeader("Retry-After", "2");
            ApiFailure.write(response, 429, "UPLOAD_BUSY", "Two uploads are in progress. Retry shortly."); return;
        }
        try { chain.doFilter(request, response); }
        finally { requests.release(); }
    }
}
