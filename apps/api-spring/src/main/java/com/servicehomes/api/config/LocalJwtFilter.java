package com.servicehomes.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.Jwt.Builder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@Profile("local | ci")
public class LocalJwtFilter extends OncePerRequestFilter {

    public static final String SERVICEHOMES_USER_ID_CLAIM = "servicehomes_user_id";
    private static final String DEFAULT_LOCAL_SUBJECT = "00000000-0000-0000-0000-000000000001";

    private final Environment environment;

    public LocalJwtFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String testUserId = request.getHeader("X-Test-User-Id");
        if (testUserId != null && !testUserId.isBlank()) {
            authenticate(testUserId, false);
        } else if (isLocalProfileActive() || isMeRequest(request)) {
            authenticate(DEFAULT_LOCAL_SUBJECT, true);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLocalProfileActive() {
        for (String profile : environment.getActiveProfiles()) {
            if ("local".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMeRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/me".equals(path) || path.startsWith("/me/");
    }

    private void authenticate(String subject, boolean includeLocalProfileClaims) {
        Builder jwtBuilder = Jwt.withTokenValue("local")
            .header("alg", "none")
            .claim("sub", subject)
            .claim(SERVICEHOMES_USER_ID_CLAIM, subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600));

        if (includeLocalProfileClaims) {
            jwtBuilder
                .claim("email", "local@example.com")
                .claim("email_verified", true)
                .claim("given_name", "Local")
                .claim("family_name", "User")
                .claim("nickname", "LocalUser");
        }

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwtBuilder.build(), List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
