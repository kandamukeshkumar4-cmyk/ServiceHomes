package com.servicehomes.api.notifications.infrastructure;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.identity.application.UserBootstrapService;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class NotificationWebSocketPrincipalResolver {

    private final CurrentUserService currentUserService;
    private final UserBootstrapService userBootstrapService;
    private final UserRepository userRepository;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;

    Optional<UUID> resolve(Session session) {
        Optional<UUID> tokenUserId = resolveToken(session);
        if (tokenUserId.isPresent()) {
            return tokenUserId;
        }

        Principal principal = session.getUserPrincipal();
        if (principal == null) {
            return Optional.empty();
        }

        if (principal instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return Optional.of(currentUserService.requireUserId(jwtAuthenticationToken.getToken()));
        }

        if (principal instanceof Authentication authentication) {
            Object authenticationPrincipal = authentication.getPrincipal();
            if (authenticationPrincipal instanceof Jwt jwt) {
                return Optional.of(currentUserService.requireUserId(jwt));
            }
            return resolveName(authentication.getName());
        }

        return resolveName(principal.getName());
    }

    private Optional<UUID> resolveToken(Session session) {
        String token = session.getRequestParameterMap().getOrDefault("token", java.util.List.of()).stream()
            .findFirst()
            .orElse(null);
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        JwtDecoder decoder = jwtDecoderProvider.getIfAvailable();
        if (decoder != null) {
            Jwt jwt = decoder.decode(token);
            return Optional.of(userBootstrapService.bootstrapFromJwt(jwt).getId());
        }

        return resolveName(token);
    }

    private Optional<UUID> resolveName(String name) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(name));
        } catch (IllegalArgumentException ignored) {
            return userRepository.findByAuth0Id(name).map(User::getId);
        }
    }
}
