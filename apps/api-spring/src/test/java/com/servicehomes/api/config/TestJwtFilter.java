package com.servicehomes.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

@Component
@Profile("ci")
public class TestJwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String testUserId = request.getHeader("X-Test-User-Id");
        if (testUserId == null || testUserId.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Jwt jwt = Jwt.withTokenValue("mock-token")
            .subject(testUserId)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .header("alg", "RS256")
            .claims(claims -> claims.put("sub", testUserId))
            .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            jwt, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
