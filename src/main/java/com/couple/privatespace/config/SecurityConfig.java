package com.couple.privatespace.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import com.couple.privatespace.identity.*;
import java.util.Map;
import com.couple.privatespace.pairing.PairingService;
import org.springframework.security.authorization.AuthorizationDecision;

@Configuration
public class SecurityConfig {
    /** AUTH-03 / CHAT-03: explicit identity routes and ACTIVE-only feature reads; default deny. */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountStore accounts, AuthRateLimiter limiter,PairingService pairing) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/auth.css", "/auth.js", "/space.js", "/home.js", "/pairing.css", "/pairing-socket.js", "/api/csrf",
                    "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").anonymous()
                .requestMatchers(HttpMethod.GET, "/api/me", "/space.html").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/partner-registration-code").authenticated()
                .requestMatchers(HttpMethod.GET,"/api/space").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/spaces","/api/invitations","/api/invitations/preview","/api/invitations/join").authenticated()
                .requestMatchers(HttpMethod.GET,"/home.css","/chat.css","/chat.js","/chat-socket.js","/memories.css","/memories.js","/games.js","/games.css").permitAll()
                .requestMatchers("/api/games/connect4","/api/games/connect4/*").access((identity,context)->new AuthorizationDecision(
                    identity.get().getPrincipal() instanceof AccountPrincipal actor && pairing.active(actor.accountId())))
                .requestMatchers(HttpMethod.GET,"/chat-media.js","/profile-ui.js","/browser-notifications.js","/notification-inbox.js","/notifications.css","/notification-sounds.js","/call-widget.js","/call-socket.js","/call-media.js","/call-reactions.js","/calls.css").permitAll()
                .requestMatchers("/api/chat/uploads","/api/chat/uploads/**","/api/chat/assets/*","/api/presence","/api/profile/photo","/api/profiles/*/photo","/api/chat/messages","/api/chat/messages/**","/api/notifications","/api/notifications/**","/api/calls/**","/api/rtc/config").access((identity,context)->new AuthorizationDecision(
                    identity.get().getPrincipal() instanceof AccountPrincipal actor && pairing.active(actor.accountId())))
                .requestMatchers("/memories.html","/api/memories","/api/memories/**","/api/memory-uploads","/api/memory-uploads/**","/api/memory-assets/*").access((identity,context)->new AuthorizationDecision(
                    identity.get().getPrincipal() instanceof AccountPrincipal actor && pairing.active(actor.accountId())))
                .requestMatchers(HttpMethod.GET,"/home.html","/api/home","/api/dates","/chat.html","/api/chat","/api/chat/state","/api/chat/receipts","/api/gifts","/api/gifts/history","/stickers/**").access((identity,context)->new AuthorizationDecision(
                    identity.get().getPrincipal() instanceof AccountPrincipal actor && pairing.active(actor.accountId())))
                .requestMatchers(HttpMethod.PATCH,"/api/profile","/api/relationship-date","/api/dates/*").authenticated()
                .requestMatchers(HttpMethod.POST,"/api/dates").authenticated()
                .requestMatchers(HttpMethod.DELETE,"/api/dates/*").authenticated()
                .requestMatchers(request->request.getServletPath().equals("/ws")&&"websocket".equalsIgnoreCase(request.getHeader("Upgrade"))).authenticated()
                .anyRequest().denyAll())
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> {
                    if (request.getRequestURI().equals("/space.html")||request.getRequestURI().equals("/home.html")||request.getRequestURI().equals("/chat.html")||request.getRequestURI().equals("/memories.html")) response.sendRedirect("/?expired=1");
                    else ApiFailure.write(response, 401, "UNAUTHENTICATED", "Please sign in to continue.");
                })
                .accessDeniedHandler((request, response, error) -> ApiFailure.write(response, 403, "FORBIDDEN", "This request is not allowed. Refresh and try again.")))
            .formLogin(login -> login.loginPage("/").loginProcessingUrl("/api/auth/login").usernameParameter("email")
                .successHandler((request, response, authentication) -> {
                    var actor=(AccountPrincipal)authentication.getPrincipal();
                    String next=pairing.active(actor.accountId())?"/home.html":"/space.html";
                    response.setContentType("application/json"); response.getWriter().write("{\"next\":\""+next+"\"}");
                })
                .failureHandler((request, response, error) -> ApiFailure.write(response, 401, "INVALID_CREDENTIALS", "Email or password is incorrect.")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout").invalidateHttpSession(true).clearAuthentication(true)
                .deleteCookies("JSESSIONID").logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
            .addFilterBefore(new AuthFilters.RequestId(), SecurityContextHolderFilter.class)
            .addFilterBefore(new com.couple.privatespace.memory.MemoryUploadGate(), org.springframework.security.web.csrf.CsrfFilter.class)
            .addFilterBefore(new AuthFilters.Traffic(limiter), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new AuthFilters.CurrentAccount(accounts), AuthorizationFilter.class)
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self'")))
            .build(); // Native form filter rotates/saves session; CSRF and no-store remain enabled.
    }

    @Bean
    UserDetailsService accountDetails(AccountStore accounts) {
        return email -> accounts.byEmail(email).map(a -> new AccountPrincipal(a.id(), a.email(), a.hash(), a.enabled(), a.revision()))
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
    @Bean
    PasswordEncoder passwordEncoder() {
        String id = "pbkdf2@SpringSecurity_v5_8";
        return new DelegatingPasswordEncoder(id, Map.of(id, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }
}
