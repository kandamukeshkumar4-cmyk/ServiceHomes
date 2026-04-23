package com.servicehomes.api.reviews;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.analytics.domain.OutboxEventRepository;
import com.servicehomes.api.identity.domain.Role;
import com.servicehomes.api.identity.domain.RoleRepository;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import com.servicehomes.api.reviews.application.dto.AddHostResponseRequest;
import com.servicehomes.api.reviews.application.dto.CreateHostReviewRequest;
import com.servicehomes.api.reviews.application.dto.CreateReviewRequest;
import com.servicehomes.api.reviews.application.dto.ModerateReviewRequest;
import com.servicehomes.api.reviews.application.dto.ReportReviewRequest;
import com.servicehomes.api.reviews.domain.Review;
import com.servicehomes.api.reviews.domain.ReviewReportRepository;
import com.servicehomes.api.reviews.domain.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class ReviewIntegrationTest {

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

    private static final UUID SEED_HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID SEED_GUEST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");
    private static final UUID LOCAL_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String SEED_HOST_AUTH0_ID = "auth0|seed-host-1";
    private static final String SEED_GUEST_AUTH0_ID = "auth0|seed-guest-1";
    private static final String LOCAL_USER_AUTH0_ID = "local-user";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ListingCategoryRepository categoryRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewReportRepository reviewReportRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UUID listingId;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        reviewReportRepository.deleteAll();
        reviewRepository.deleteAll();
        reservationRepository.deleteAll();
        listingRepository.deleteAll();
        ensureLocalUserIsAdmin();
        listingId = createListing().getId();
    }

    @Test
    void guestCanReviewCompletedStayAndListingRatingCacheIsUpdated() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(24),
            LocalDate.now().minusDays(21)
        );

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest(5, "Fantastic stay"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reservationId").value(reservation.getId().toString()))
            .andExpect(jsonPath("$.listingId").value(listingId.toString()))
            .andExpect(jsonPath("$.reviewerRole").value("GUEST"))
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.cleanlinessRating").value(5))
            .andExpect(jsonPath("$.comment").value("Fantastic stay"))
            .andExpect(jsonPath("$.guestDisplayName").value("Bob"));

        Listing listing = listingRepository.findById(listingId).orElseThrow();
        assertThat(listing.getAverageRating()).isEqualByComparingTo("5.00");
        assertThat(listing.getReviewCount()).isEqualTo(1);
        assertThat(listing.getTrustScore()).isEqualByComparingTo("81.00");
        assertThat(outboxEventRepository.findAll())
            .extracting(event -> event.getEventType())
            .contains("review_created");
    }

    @Test
    void onlyGuestCanReviewReservation() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(24),
            LocalDate.now().minusDays(21)
        );

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest(4, "Nice stay"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Not authorized"));
    }

    @Test
    void guestCannotReviewReservationThatIsNotCompleted() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.CONFIRMED,
            LocalDate.now().minusDays(4),
            LocalDate.now().minusDays(1)
        );

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest(4, "Too early"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Only completed reservations can be reviewed"));
    }

    @Test
    void guestCannotReviewBeforeCheckout() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(2)
        );

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest(4, "Too early"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Reviews are available only after checkout"));
    }

    @Test
    void duplicateGuestReviewIsRejected() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(24),
            LocalDate.now().minusDays(21)
        );
        CreateReviewRequest request = reviewRequest(5, "Great host");

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Reservation has already been reviewed by the guest"));
    }

    @Test
    void doubleBlindReviewIsHiddenUntilHostSubmitsCounterpart() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(4),
            LocalDate.now().minusDays(2)
        );

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reviewRequest(5, "Hidden for now"))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/listings/{id}/reviews", listingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewCount").value(0))
            .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(post("/reservations/{id}/host-review", reservation.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateHostReviewRequest(5, "Thoughtful guest"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewerRole").value("HOST"));

        mockMvc.perform(get("/listings/{id}/reviews", listingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewCount").value(1))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].comment").value("Hidden for now"));

        assertThat(outboxEventRepository.findAll())
            .extracting(event -> event.getEventType())
            .contains("review_created", "host_review_created");
    }

    @Test
    void hostCanRespondOnceToOwnVisibleGuestReview() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(24),
            LocalDate.now().minusDays(21)
        );
        Review review = createVisibleGuestReview(reservation, 5, "Wonderful place");

        mockMvc.perform(post("/reviews/{id}/response", review.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddHostResponseRequest("Thank you for staying with us"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(review.getId().toString()))
            .andExpect(jsonPath("$.hostResponse").value("Thank you for staying with us"));

        mockMvc.perform(post("/reviews/{id}/response", review.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddHostResponseRequest("Second reply"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Host response already exists"));

        assertThat(outboxEventRepository.findAll())
            .extracting(event -> event.getEventType())
            .contains("host_response_added");
    }

    @Test
    void hostCannotRespondToAnotherHostsReview() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(24),
            LocalDate.now().minusDays(21)
        );
        Review review = createVisibleGuestReview(reservation, 3, "Solid stay");

        mockMvc.perform(post("/reviews/{id}/response", review.getId())
                .header("X-Test-User-Id", LOCAL_USER_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddHostResponseRequest("Unauthorized reply"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Not authorized"));
    }

    @Test
    void reviewReportsFeedAdminModerationAndHiddenReviewsLeavePublicAggregate() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(24),
            LocalDate.now().minusDays(21)
        );
        Review review = createVisibleGuestReview(reservation, 2, "Misleading review");

        mockMvc.perform(post("/reviews/{id}/report", review.getId())
                .header("X-Test-User-Id", LOCAL_USER_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReportReviewRequest("IRRELEVANT", "Does not describe the stay"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId").value(review.getId().toString()))
            .andExpect(jsonPath("$.reason").value("IRRELEVANT"))
            .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(post("/reviews/{id}/report", review.getId())
                .header("X-Test-User-Id", LOCAL_USER_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ReportReviewRequest("OTHER", ""))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Review has already been reported by this user"));

        mockMvc.perform(get("/admin/reviews/moderation")
                .header("X-Test-User-Id", LOCAL_USER_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].review.id").value(review.getId().toString()));

        mockMvc.perform(patch("/admin/reviews/{id}/moderation", review.getId())
                .header("X-Test-User-Id", LOCAL_USER_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ModerateReviewRequest("HIDDEN", "Abuse report accepted"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.moderationStatus").value("HIDDEN"));

        mockMvc.perform(get("/listings/{id}/reviews", listingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewCount").value(0))
            .andExpect(jsonPath("$.content.length()").value(0));

        Listing listing = listingRepository.findById(listingId).orElseThrow();
        assertThat(listing.getAverageRating()).isNull();
        assertThat(listing.getReviewCount()).isZero();
        assertThat(outboxEventRepository.findAll())
            .extracting(event -> event.getEventType())
            .contains("review_reported", "review_moderated");
    }

    @Test
    void listingReviewsEndpointReturnsRatingBreakdownAndPagination() throws Exception {
        Reservation firstReservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(24),
            LocalDate.now().minusDays(21)
        );
        Reservation secondReservation = createReservation(
            LOCAL_USER_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(28),
            LocalDate.now().minusDays(25)
        );

        createVisibleGuestReview(firstReservation, 5, "Amazing");
        createVisibleGuestReview(secondReservation, 3, "Pretty good");

        mockMvc.perform(get("/listings/{id}/reviews", listingId)
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageRating").value(4.0))
            .andExpect(jsonPath("$.reviewCount").value(2))
            .andExpect(jsonPath("$.cleanlinessRating").value(4.0))
            .andExpect(jsonPath("$.trustScore").value(66.0))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    private void ensureLocalUserIsAdmin() {
        var adminRole = roleRepository.findByName(Role.RoleName.ADMIN).orElseThrow();
        var localUser = userRepository.findById(LOCAL_USER_ID).orElseThrow();
        localUser.getRoles().add(adminRole);
        userRepository.save(localUser);
    }

    private CreateReviewRequest reviewRequest(int rating, String comment) {
        return new CreateReviewRequest(rating, rating, rating, rating, rating, rating, comment);
    }

    private Listing createListing() {
        ListingCategory category = categoryRepository.findAll().isEmpty()
            ? categoryRepository.save(ListingCategory.builder().name("Test").icon("test").description("Test").build())
            : categoryRepository.findAll().get(0);

        return listingRepository.save(Listing.builder()
            .hostId(SEED_HOST_ID)
            .title("Review Test Listing")
            .description("Listing used for review integration tests")
            .category(category)
            .propertyType(Listing.PropertyType.HOUSE)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("180.00"))
            .cleaningFee(new BigDecimal("25.00"))
            .serviceFee(new BigDecimal("15.00"))
            .status(Listing.Status.PUBLISHED)
            .build());
    }

    private Reservation createReservation(UUID guestId, Reservation.Status status, LocalDate checkIn, LocalDate checkOut) {
        return reservationRepository.save(Reservation.builder()
            .listing(listingRepository.findById(listingId).orElseThrow())
            .guestId(guestId)
            .checkIn(checkIn)
            .checkOut(checkOut)
            .guests(2)
            .totalNights((int) (checkOut.toEpochDay() - checkIn.toEpochDay()))
            .nightlyPrice(new BigDecimal("180.00"))
            .cleaningFee(new BigDecimal("25.00"))
            .serviceFee(new BigDecimal("15.00"))
            .totalAmount(new BigDecimal("400.00"))
            .status(status)
            .build());
    }

    private Review createVisibleGuestReview(Reservation reservation, int rating, String comment) {
        return reviewRepository.save(Review.builder()
            .reservation(reservation)
            .listing(reservation.getListing())
            .guestId(reservation.getGuestId())
            .hostId(reservation.getListing().getHostId())
            .reviewerId(reservation.getGuestId())
            .reviewerRole(Review.ReviewerRole.GUEST)
            .rating(rating)
            .cleanlinessRating(rating)
            .accuracyRating(rating)
            .communicationRating(rating)
            .locationRating(rating)
            .valueRating(rating)
            .comment(comment)
            .visibleAt(Instant.now().minusSeconds(60))
            .moderationStatus(Review.ModerationStatus.APPROVED)
            .build());
    }
}
