package com.servicehomes.api.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.RequestPostProcessor;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

public class WithMockJwt {

    public static RequestPostProcessor jwt(String subject) {
        return request -> {
            Jwt token = Jwt.withTokenValue("mock-token")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .header("alg", "RS256")
                .claims(claims -> claims.put("sub", subject))
                .build();

            Authentication auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                token, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    public static RequestPostProcessor jwt(UUID userId) {
        return jwt(userId.toString());
    }
}
