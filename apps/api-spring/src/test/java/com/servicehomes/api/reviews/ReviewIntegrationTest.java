package com.servicehomes.api.reviews;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.analytics.domain.OutboxEventRepository;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reviews.application.dto.AddHostResponseRequest;
import com.servicehomes.api.reviews.application.dto.CreateReviewRequest;
import com.servicehomes.api.reviews.domain.Review;
import com.servicehomes.api.reviews.domain.ReviewRepository;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private OutboxEventRepository outboxEventRepository;

    private UUID listingId;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        reviewRepository.deleteAll();
        reservationRepository.deleteAll();
        listingRepository.deleteAll();
        listingId = createListing().getId();
    }

    @Test
    void guestCanReviewAfterCheckoutWhenReservationIsConfirmed() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.CONFIRMED,
            LocalDate.now().minusDays(5),
            LocalDate.now().minusDays(2)
        );

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(5, "Fantastic stay"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reservationId").value(reservation.getId().toString()))
            .andExpect(jsonPath("$.listingId").value(listingId.toString()))
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.comment").value("Fantastic stay"))
            .andExpect(jsonPath("$.guestDisplayName").value("Bob"))
            .andExpect(jsonPath("$.hostResponse").doesNotExist());

        assertThat(reviewRepository.existsByReservation_Id(reservation.getId())).isTrue();
        assertThat(outboxEventRepository.findAll())
            .extracting(event -> event.getEventType())
            .contains("review_created");
    }

    @Test
    void onlyGuestCanReviewReservation() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.CONFIRMED,
            LocalDate.now().minusDays(4),
            LocalDate.now().minusDays(1)
        );

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(4, "Nice stay"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Not authorized"));
    }

    @Test
    void guestCannotReviewBeforeCheckout() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.CONFIRMED,
            LocalDate.now().minusDays(1),
            LocalDate.now().plusDays(2)
        );

        mockMvc.perform(post("/reservations/{id}/review", reservation.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateReviewRequest(4, "Too early"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Reviews are available only after checkout"));
    }

    @Test
    void duplicateReviewIsRejected() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.CONFIRMED,
            LocalDate.now().minusDays(6),
            LocalDate.now().minusDays(3)
        );

        CreateReviewRequest request = new CreateReviewRequest(5, "Great host");

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
            .andExpect(jsonPath("$.message").value("Reservation has already been reviewed"));
    }

    @Test
    void hostCanRespondToOwnListingReview() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.CONFIRMED,
            LocalDate.now().minusDays(7),
            LocalDate.now().minusDays(4)
        );
        Review review = createReview(reservation, 5, "Wonderful place");

        mockMvc.perform(post("/reviews/{id}/response", review.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddHostResponseRequest("Thank you for staying with us"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(review.getId().toString()))
            .andExpect(jsonPath("$.hostResponse").value("Thank you for staying with us"));

        assertThat(outboxEventRepository.findAll())
            .extracting(event -> event.getEventType())
            .contains("host_response_added");
    }

    @Test
    void hostCannotRespondToAnotherHostsReview() throws Exception {
        Reservation reservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.CONFIRMED,
            LocalDate.now().minusDays(7),
            LocalDate.now().minusDays(4)
        );
        Review review = createReview(reservation, 3, "Solid stay");

        mockMvc.perform(post("/reviews/{id}/response", review.getId())
                .header("X-Test-User-Id", LOCAL_USER_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AddHostResponseRequest("Unauthorized reply"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Not authorized"));
    }

    @Test
    void listingReviewsEndpointReturnsAverageRatingAndPagination() throws Exception {
        Reservation firstReservation = createReservation(
            SEED_GUEST_ID,
            Reservation.Status.CONFIRMED,
            LocalDate.now().minusDays(10),
            LocalDate.now().minusDays(7)
        );
        Reservation secondReservation = createReservation(
            LOCAL_USER_ID,
            Reservation.Status.COMPLETED,
            LocalDate.now().minusDays(14),
            LocalDate.now().minusDays(11)
        );

        createReview(firstReservation, 5, "Amazing");
        createReview(secondReservation, 3, "Pretty good");

        mockMvc.perform(get("/listings/{id}/reviews", listingId)
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.averageRating").value(4.0))
            .andExpect(jsonPath("$.reviewCount").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.content.length()").value(1));
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

    private Review createReview(Reservation reservation, int rating, String comment) {
        return reviewRepository.save(Review.builder()
            .reservation(reservation)
            .listing(reservation.getListing())
            .guestId(reservation.getGuestId())
            .rating(rating)
            .comment(comment)
            .build());
    }
}
