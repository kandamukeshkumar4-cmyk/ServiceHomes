package com.servicehomes.api.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.context.TestSecurityContextHolder;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class WithMockJwt {

    public static void setup(String subject) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .subject(subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .header("alg", "RS256")
            .claims(claims -> claims.put("sub", subject))
            .build();

        TestSecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                jwt, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            )
        );
    }

    public static void setup(UUID userId) {
        setup(userId.toString());
    }

    public static void clear() {
        TestSecurityContextHolder.clearContext();
    }
}
