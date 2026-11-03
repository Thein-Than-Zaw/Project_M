package com.couple.privatespace.identity;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public final class AuthFilters {
    private AuthFilters() { }

    public static class RequestId extends OncePerRequestFilter {
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            response.setHeader("X-Request-ID", UUID.randomUUID().toString());
            response.setHeader("Referrer-Policy", "no-referrer");
            chain.doFilter(request, response);
        }
    }

    public static class Traffic extends OncePerRequestFilter {
        private final AuthRateLimiter limiter;
        public Traffic(AuthRateLimiter limiter) { this.limiter = limiter; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            boolean login = request.getRequestURI().equals("/api/auth/login");
            boolean register = request.getRequestURI().equals("/api/auth/register");
            boolean passwordChange=request.getRequestURI().equals("/api/auth/change-password");
            boolean pairing=request.getRequestURI().startsWith("/api/invitations")||request.getRequestURI().equals("/api/spaces");
            boolean homeWrite=java.util.Set.of("POST","PATCH","DELETE","PUT").contains(request.getMethod()) &&
                (request.getRequestURI().equals("/api/partner-nickname")||request.getRequestURI().equals("/api/profile")||request.getRequestURI().equals("/api/relationship-date")||request.getRequestURI().equals("/api/dates")||request.getRequestURI().startsWith("/api/dates/"));
            if (("POST".equals(request.getMethod()) && (login || register || pairing || passwordChange)) || homeWrite) {
                if (request.getContentLengthLong() > 16384) {
                    ApiFailure.write(response, 413, "REQUEST_TOO_LARGE", "This request is too large."); return;
                }
                var authentication=SecurityContextHolder.getContext().getAuthentication();
                String passwordActor=authentication!=null&&authentication.getPrincipal() instanceof AccountPrincipal principal?principal.accountId().toString():"anonymous";
                long retry = passwordChange?limiter.passwordChange(request.getRemoteAddr(),passwordActor):login ? limiter.login(request.getRemoteAddr(), request.getParameter("email")) : (pairing||homeWrite)?limiter.pairing(request.getRemoteAddr()):limiter.registration(request.getRemoteAddr());
                if (retry > 0) {
                    response.setHeader("Retry-After", Long.toString(retry));
                    ApiFailure.write(response, 429, "TOO_MANY_ATTEMPTS", "Too many attempts. Please try again later."); return;
                }
                if (register || pairing || homeWrite || passwordChange) {
                    // Bound chunked JSON too; no body is logged or retained by the filter.
                    HttpServletRequest limited = new HttpServletRequestWrapper(request) {
                        @Override public ServletInputStream getInputStream() throws IOException {
                            ServletInputStream original = request.getInputStream();
                            return new ServletInputStream() {
                                int read;
                                @Override public int read() throws IOException {
                                    if (++read > 16384) throw new IOException("Request body exceeds limit");
                                    return original.read();
                                }
                                @Override public boolean isFinished() { return original.isFinished(); }
                                @Override public boolean isReady() { return original.isReady(); }
                                @Override public void setReadListener(ReadListener listener) { original.setReadListener(listener); }
                            };
                        }
                    };
                    chain.doFilter(limited, response); return;
                }
            }
            chain.doFilter(request, response);
        }
    }

    /** PRIV-02: database disable/revision changes immediately invalidate existing HTTP sessions. */
    public static class CurrentAccount extends OncePerRequestFilter {
        private final AccountStore accounts;
        public CurrentAccount(AccountStore accounts) { this.accounts = accounts; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
                boolean valid = accounts.byId(principal.accountId()).filter(a -> a.enabled() && a.revision() == principal.securityRevision()).isPresent();
                if (!valid) {
                    var session = request.getSession(false);
                    if (session != null) session.invalidate();
                    SecurityContextHolder.clearContext();
                    ApiFailure.write(response, 401, "SESSION_EXPIRED", "Your session has expired. Please sign in again."); return;
                }
            }
            chain.doFilter(request, response);
        }
    }
}
