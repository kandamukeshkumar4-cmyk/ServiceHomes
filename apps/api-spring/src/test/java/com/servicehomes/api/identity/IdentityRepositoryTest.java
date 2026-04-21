package com.servicehomes.api.identity;

import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.Role;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("ci")
class IdentityRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgis/postgis:15-3.4").asCompatibleSubstituteFor("postgres"))
        .withDatabaseName("servicehomes")
        .withUsername("servicehomes")
        .withPassword("servicehomes");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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
