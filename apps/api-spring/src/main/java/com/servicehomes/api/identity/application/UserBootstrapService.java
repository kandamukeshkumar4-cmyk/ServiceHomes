package com.servicehomes.api.identity.application;

import com.servicehomes.api.identity.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserBootstrapService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public User bootstrapFromJwt(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        Optional<User> existing = userRepository.findByAuth0Id(auth0Id);
        if (existing.isPresent()) {
            return refreshExistingUser(existing.get(), jwt, email, emailVerified);
        }

        Role travelerRole = roleRepository.findByName(Role.RoleName.TRAVELER)
            .orElseThrow(() -> new IllegalStateException("TRAVELER role not found"));

        User user = User.builder()
            .auth0Id(auth0Id)
            .email(email != null ? email : "")
            .emailVerified(Boolean.TRUE.equals(emailVerified))
            .build();
        user.getRoles().add(travelerRole);

        Profile profile = Profile.builder()
            .user(user)
            .firstName(jwt.getClaimAsString("given_name"))
            .lastName(jwt.getClaimAsString("family_name"))
            .displayName(jwt.getClaimAsString("nickname"))
            .avatarUrl(jwt.getClaimAsString("picture"))
            .build();
        user.setProfile(profile);

        return userRepository.save(user);
    }

    private User refreshExistingUser(User user, Jwt jwt, String email, Boolean emailVerified) {
        boolean changed = false;

        if (email != null && !email.equals(user.getEmail())) {
            user.setEmail(email);
            changed = true;
        }

        if (emailVerified != null && user.isEmailVerified() != emailVerified) {
            user.setEmailVerified(emailVerified);
            changed = true;
        }

        Profile profile = user.getProfile();
        if (profile == null) {
            profile = Profile.builder().user(user).build();
            user.setProfile(profile);
            changed = true;
        }

        changed |= setIfDifferent(profile.getFirstName(), jwt.getClaimAsString("given_name"), profile::setFirstName);
        changed |= setIfDifferent(profile.getLastName(), jwt.getClaimAsString("family_name"), profile::setLastName);
        changed |= setIfDifferent(profile.getDisplayName(), jwt.getClaimAsString("nickname"), profile::setDisplayName);
        changed |= setIfDifferent(profile.getAvatarUrl(), jwt.getClaimAsString("picture"), profile::setAvatarUrl);

        return changed ? userRepository.save(user) : user;
    }

    private boolean setIfDifferent(String currentValue, String nextValue, java.util.function.Consumer<String> setter) {
        if (nextValue == null || nextValue.equals(currentValue)) {
            return false;
        }
        setter.accept(nextValue);
        return true;
    }
}
