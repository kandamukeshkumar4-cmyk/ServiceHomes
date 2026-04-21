package com.servicehomes.api.identity.web;

import com.servicehomes.api.identity.application.UserBootstrapService;
import com.servicehomes.api.identity.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserBootstrapService userBootstrapService;

    @GetMapping
    public ResponseEntity<MeDto> me(@AuthenticationPrincipal Jwt jwt) {
        User user = userBootstrapService.bootstrapFromJwt(jwt);
        return ResponseEntity.ok(toDto(user));
    }

    private MeDto toDto(User user) {
        return new MeDto(
            user.getId().toString(),
            user.getEmail(),
            user.isEmailVerified(),
            user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList()),
            user.getProfile() != null ? new ProfileDto(
                user.getProfile().getFirstName(),
                user.getProfile().getLastName(),
                user.getProfile().getDisplayName(),
                user.getProfile().getBio(),
                user.getProfile().getAvatarUrl(),
                user.getProfile().getPhoneNumber()
            ) : null
        );
    }

    public record MeDto(
        String id,
        String email,
        boolean emailVerified,
        List<String> roles,
        ProfileDto profile
    ) {}

    public record ProfileDto(
        String firstName,
        String lastName,
        String displayName,
        String bio,
        String avatarUrl,
        String phoneNumber
    ) {}
}
