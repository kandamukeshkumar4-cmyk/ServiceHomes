package com.servicehomes.api.identity.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.identity.application.ProfileService;
import com.servicehomes.api.identity.application.dto.AvatarUploadRequest;
import com.servicehomes.api.identity.application.dto.AvatarUploadResponse;
import com.servicehomes.api.identity.application.dto.MeDto;
import com.servicehomes.api.identity.application.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final CurrentUserService currentUserService;
    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<MeDto> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(profileService.getMe(userId));
    }

    @PatchMapping("/profile")
    public ResponseEntity<MeDto> updateProfile(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    @PostMapping("/profile/avatar-upload-url")
    public ResponseEntity<AvatarUploadResponse> avatarUploadUrl(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody AvatarUploadRequest request
    ) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(profileService.createAvatarUploadTarget(userId, request));
    }

    @PostMapping("/become-host")
    public ResponseEntity<MeDto> becomeHost(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(profileService.becomeHost(userId));
    }
}
