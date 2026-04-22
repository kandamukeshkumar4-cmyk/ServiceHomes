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
        return userBootstrapService.bootstrapFromJwt(jwt).getId();
    }
}
