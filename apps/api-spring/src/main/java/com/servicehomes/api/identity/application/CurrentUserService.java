package com.servicehomes.api.identity.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserBootstrapService userBootstrapService;

    public UUID requireUserId(Jwt jwt) {
        String explicitUserId = jwt.getClaimAsString(com.servicehomes.api.config.LocalJwtFilter.SERVICEHOMES_USER_ID_CLAIM);
        if (explicitUserId != null) {
            try {
                return UUID.fromString(explicitUserId);
            } catch (IllegalArgumentException ignored) {
                // Fall back to auth0/bootstrap resolution for non-UUID test subjects.
            }
        }
        return userBootstrapService.bootstrapFromJwt(jwt).getId();
    }
}
