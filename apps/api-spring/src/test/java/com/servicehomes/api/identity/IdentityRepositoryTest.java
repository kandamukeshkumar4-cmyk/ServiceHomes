package com.servicehomes.api.identity;

import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("ci")
class IdentityRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByAuth0IdReturnsUser() {
        String auth0Id = "auth0|test123";
        User user = User.builder()
            .auth0Id(auth0Id)
            .email("test@example.com")
            .build();
        Profile profile = Profile.builder()
            .user(user)
            .displayName("Test User")
            .build();
        user.setProfile(profile);
        userRepository.save(user);

        Optional<User> found = userRepository.findByAuth0Id(auth0Id);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getProfile().getDisplayName()).isEqualTo("Test User");
    }

    @Test
    void findByAuth0IdReturnsEmptyForUnknownId() {
        Optional<User> found = userRepository.findByAuth0Id("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByAuth0IdReturnsTrue() {
        String auth0Id = "auth0|exists";
        User user = User.builder()
            .auth0Id(auth0Id)
            .email("exists@example.com")
            .build();
        userRepository.save(user);

        boolean exists = userRepository.existsByAuth0Id(auth0Id);

        assertThat(exists).isTrue();
    }
}
