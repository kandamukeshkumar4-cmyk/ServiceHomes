package com.servicehomes.api.reservations;

import com.servicehomes.api.config.WithMockJwt;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reservations.application.dto.CreateReservationRequest;
import com.servicehomes.api.reservations.application.dto.QuoteRequest;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
class ReservationIntegrationTest {

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
    private ObjectMapper objectMapper;

    @Autowired
    private ListingCategoryRepository categoryRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private static final UUID SEED_HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID SEED_GUEST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");

    private UUID listingId;

    @BeforeEach
    void setUp() {
        ListingCategory category = categoryRepository.findAll().isEmpty()
            ? categoryRepository.save(ListingCategory.builder().name("Test").icon("test").description("Test").build())
            : categoryRepository.findAll().get(0);

        Listing listing = Listing.builder()
            .hostId(SEED_HOST_ID)
            .title("Test Listing")
            .description("A nice place")
            .category(category)
            .propertyType(Listing.PropertyType.APARTMENT)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("100.00"))
            .cleaningFee(new BigDecimal("20.00"))
            .serviceFee(new BigDecimal("10.00"))
            .status(Listing.Status.PUBLISHED)
            .build();
        listing = listingRepository.save(listing);
        listingId = listing.getId();
    }

    @AfterEach
    void tearDown() {
        WithMockJwt.clear();
    }

    @Test
    void quoteReturnsCorrectPricing() throws Exception {
        QuoteRequest request = new QuoteRequest(listingId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5), 2);

        mockMvc.perform(post("/reservations/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalNights").value(4))
            .andExpect(jsonPath("$.nightlyPrice").value(100.00))
            .andExpect(jsonPath("$.subtotal").value(400.00))
            .andExpect(jsonPath("$.cleaningFee").value(20.00))
            .andExpect(jsonPath("$.serviceFee").value(10.00))
            .andExpect(jsonPath("$.totalAmount").value(430.00));
    }

    @Test
    void quoteRejectsInvalidDates() throws Exception {
        QuoteRequest request = new QuoteRequest(listingId, LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 1), 2);

        mockMvc.perform(post("/reservations/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createReservationSucceedsForPublishedListing() throws Exception {
        WithMockJwt.setup(SEED_GUEST_ID);

        CreateReservationRequest request = new CreateReservationRequest(
            listingId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 2
        );

        mockMvc.perform(post("/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.listingId").value(listingId.toString()))
            .andExpect(jsonPath("$.totalNights").value(4))
            .andExpect(jsonPath("$.totalAmount").value(430.00));
    }

    @Test
    void createReservationRejectsOwnListing() throws Exception {
        WithMockJwt.setup(SEED_HOST_ID);

        CreateReservationRequest request = new CreateReservationRequest(
            listingId, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 2
        );

        mockMvc.perform(post("/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cannot book your own listing"));
    }

    @Test
    void createReservationRejectsOverlappingDates() throws Exception {
        Reservation existing = Reservation.builder()
            .listing(listingRepository.findById(listingId).get())
            .guestId(SEED_GUEST_ID)
            .checkIn(LocalDate.of(2026, 7, 1))
            .checkOut(LocalDate.of(2026, 7, 5))
            .guests(2)
            .totalNights(4)
            .nightlyPrice(new BigDecimal("100.00"))
            .cleaningFee(new BigDecimal("20.00"))
            .serviceFee(new BigDecimal("10.00"))
            .totalAmount(new BigDecimal("430.00"))
            .status(Reservation.Status.CONFIRMED)
            .build();
        reservationRepository.save(existing);

        WithMockJwt.setup(SEED_GUEST_ID);

        CreateReservationRequest request = new CreateReservationRequest(
            listingId, LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 8), 2
        );

        mockMvc.perform(post("/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Dates are not available for this listing"));
    }

    @Test
    void createReservationRejectsExceedingGuestCapacity() throws Exception {
        WithMockJwt.setup(SEED_GUEST_ID);

        CreateReservationRequest request = new CreateReservationRequest(
            listingId, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), 10
        );

        mockMvc.perform(post("/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Exceeds maximum guest capacity"));
    }
}
