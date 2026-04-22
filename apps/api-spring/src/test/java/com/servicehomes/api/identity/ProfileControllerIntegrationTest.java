package com.servicehomes.api.identity;

import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.Role;
import com.servicehomes.api.identity.domain.RoleRepository;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingLocation;
import com.servicehomes.api.listings.domain.ListingPolicy;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
class ProfileControllerIntegrationTest {

    private static final UUID SEED_HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID SEED_GUEST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");
    private static final String SEED_HOST_AUTH0_ID = "auth0|seed-host-1";
    private static final String SEED_GUEST_AUTH0_ID = "auth0|seed-guest-1";

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
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ListingCategoryRepository categoryRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void patchProfileUpdatesOwnProfile() throws Exception {
        String body = """
            {
              "displayName": "Bobby Traveler",
              "bio": "Remote-first guest who loves long stays.",
              "avatarUrl": "https://cdn.example.com/avatar.jpg",
              "phoneNumber": "+1-555-0100",
              "location": "Boston, MA",
              "languages": ["English", "Spanish"]
            }
            """;

        mockMvc.perform(patch("/me/profile")
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profile.displayName").value("Bobby Traveler"))
            .andExpect(jsonPath("$.profile.bio").value("Remote-first guest who loves long stays."))
            .andExpect(jsonPath("$.profile.phoneNumber").value("+1-555-0100"))
            .andExpect(jsonPath("$.profile.location").value("Boston, MA"))
            .andExpect(jsonPath("$.profile.languages[0]").value("English"))
            .andExpect(jsonPath("$.profile.languages[1]").value("Spanish"));

        User guest = userRepository.findById(SEED_GUEST_ID).orElseThrow();
        assertThat(guest.getProfile().getDisplayName()).isEqualTo("Bobby Traveler");
        assertThat(guest.getProfile().getLocation()).isEqualTo("Boston, MA");
        assertThat(guest.getProfile().getLanguages()).containsExactly("English", "Spanish");
    }

    @Test
    void becomeHostAddsHostRole() throws Exception {
        mockMvc.perform(post("/me/become-host")
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles[?(@ == 'HOST')]").exists())
            .andExpect(jsonPath("$.roles[?(@ == 'TRAVELER')]").exists());

        User guest = userRepository.findById(SEED_GUEST_ID).orElseThrow();
        assertThat(guest.getRoles()).extracting(role -> role.getName().name())
            .contains("HOST", "TRAVELER");
    }

    @Test
    void becomeHostRejectsExistingHost() throws Exception {
        mockMvc.perform(post("/me/become-host")
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("You are already a host"));
    }

    @Test
    void becomeHostRequiresVerifiedEmail() throws Exception {
        User unverifiedTraveler = createTravelerUser(false);

        mockMvc.perform(post("/me/become-host")
                .header("X-Test-User-Id", unverifiedTraveler.getAuth0Id()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Verify your email before becoming a host"));
    }

    @Test
    void publicHostProfileReturnsStatsAndPublishedListings() throws Exception {
        User host = createHostUser();
        Listing publishedOne = createListing(host.getId(), "Downtown Loft", Listing.Status.PUBLISHED, false);
        Listing publishedTwo = createListing(host.getId(), "Harbor Apartment", Listing.Status.PUBLISHED, false);
        createListing(host.getId(), "Draft Test Listing", Listing.Status.DRAFT, false);

        Reservation quickResponse = createReservation(publishedOne, Reservation.Status.CONFIRMED);
        overrideReservationTimes(quickResponse.getId(), Instant.now().minusSeconds(72 * 3600), Instant.now().minusSeconds(69 * 3600));

        Reservation lateResponse = createReservation(publishedOne, Reservation.Status.DECLINED);
        overrideReservationTimes(lateResponse.getId(), Instant.now().minusSeconds(96 * 3600), Instant.now().minusSeconds(69 * 3600));

        Reservation overduePending = createReservation(publishedTwo, Reservation.Status.PENDING);
        overrideReservationTimes(overduePending.getId(), Instant.now().minusSeconds(80 * 3600), Instant.now().minusSeconds(80 * 3600));

        Reservation recentPending = createReservation(publishedTwo, Reservation.Status.PENDING);
        overrideReservationTimes(recentPending.getId(), Instant.now().minusSeconds(2 * 3600), Instant.now().minusSeconds(2 * 3600));

        mockMvc.perform(get("/hosts/{hostId}", host.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hostId").value(host.getId().toString()))
            .andExpect(jsonPath("$.displayName").value("Riley Host"))
            .andExpect(jsonPath("$.bio").value("Super responsive host"))
            .andExpect(jsonPath("$.location").value("Seattle, WA"))
            .andExpect(jsonPath("$.languages[0]").value("English"))
            .andExpect(jsonPath("$.listingsCount").value(2))
            .andExpect(jsonPath("$.responseRate").value(33))
            .andExpect(jsonPath("$.listings.length()").value(2))
            .andExpect(jsonPath("$.listings[0].title").value("Harbor Apartment"))
            .andExpect(jsonPath("$.listings[1].title").value("Downtown Loft"));
    }

    private User createHostUser() {
        Role hostRole = roleRepository.findByName(Role.RoleName.HOST).orElseThrow();
        User user = User.builder()
            .auth0Id("auth0|public-host-" + UUID.randomUUID())
            .email("public-host-" + UUID.randomUUID() + "@example.com")
            .emailVerified(true)
            .build();
        user.getRoles().add(hostRole);
        Profile profile = Profile.builder()
            .user(user)
            .displayName("Riley Host")
            .bio("Super responsive host")
            .avatarUrl("https://cdn.example.com/riley.jpg")
            .location("Seattle, WA")
            .languages(List.of("English"))
            .build();
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private User createTravelerUser(boolean emailVerified) {
        Role travelerRole = roleRepository.findByName(Role.RoleName.TRAVELER).orElseThrow();
        User user = User.builder()
            .auth0Id("auth0|traveler-" + UUID.randomUUID())
            .email("traveler-" + UUID.randomUUID() + "@example.com")
            .emailVerified(emailVerified)
            .build();
        user.getRoles().add(travelerRole);
        Profile profile = Profile.builder()
            .user(user)
            .displayName("Taylor Traveler")
            .build();
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private Listing createListing(UUID hostId, String title, Listing.Status status, boolean instantBook) {
        ListingCategory category = categoryRepository.findAll().isEmpty()
            ? categoryRepository.save(ListingCategory.builder().name("Host Stats").icon("pi pi-home").description("Host stats tests").build())
            : categoryRepository.findAll().get(0);

        Listing listing = Listing.builder()
            .hostId(hostId)
            .title(title)
            .description(title + " description")
            .category(category)
            .propertyType(Listing.PropertyType.APARTMENT)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("180.00"))
            .cleaningFee(new BigDecimal("25.00"))
            .serviceFee(new BigDecimal("12.00"))
            .status(status)
            .publishedAt(status == Listing.Status.PUBLISHED ? Instant.now() : null)
            .build();
        listing.setLocation(ListingLocation.builder()
            .listing(listing)
            .addressLine1("123 Test Street")
            .city("Seattle")
            .country("USA")
            .build());
        listing.setPolicy(ListingPolicy.builder()
            .listing(listing)
            .minNights(1)
            .instantBook(instantBook)
            .build());
        return listingRepository.save(listing);
    }

    private Reservation createReservation(Listing listing, Reservation.Status status) {
        return reservationRepository.save(Reservation.builder()
            .listing(listing)
            .guestId(SEED_GUEST_ID)
            .checkIn(java.time.LocalDate.of(2026, 11, 1))
            .checkOut(java.time.LocalDate.of(2026, 11, 4))
            .guests(2)
            .totalNights(3)
            .nightlyPrice(new BigDecimal("180.00"))
            .cleaningFee(new BigDecimal("25.00"))
            .serviceFee(new BigDecimal("12.00"))
            .totalAmount(new BigDecimal("577.00"))
            .status(status)
            .build());
    }

    private void overrideReservationTimes(UUID reservationId, Instant createdAt, Instant updatedAt) {
        jdbcTemplate.update(
            "UPDATE reservations SET created_at = ?, updated_at = ? WHERE id = ?",
            Timestamp.from(createdAt),
            Timestamp.from(updatedAt),
            reservationId
        );
    }
}
