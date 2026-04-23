package com.servicehomes.api.dashboards;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.listings.domain.*;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
class HostDashboardIntegrationTest {

    private static final UUID HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID GUEST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");
    private static final String HOST_AUTH0_ID = "auth0|seed-host-1";
    private static final String GUEST_AUTH0_ID = "auth0|seed-guest-1";

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
    private ListingRepository listingRepository;

    @Autowired
    private ListingCategoryRepository categoryRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ListingCategory category;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        listingRepository.deleteAll();
        category = categoryRepository.findAll().get(0);
    }

    @Test
    void hostDashboardReturnsPipelineAndMetrics() throws Exception {
        Listing listing = createListing("Beach Villa", 250);
        LocalDate today = LocalDate.now();

        // Pending request
        createReservation(listing, GUEST_ID, today.plusDays(5), today.plusDays(10), Reservation.Status.PENDING);
        // Confirmed upcoming
        createReservation(listing, GUEST_ID, today.plusDays(2), today.plusDays(4), Reservation.Status.CONFIRMED);
        // Confirmed but past
        createReservation(listing, GUEST_ID, today.minusDays(10), today.minusDays(5), Reservation.Status.CONFIRMED);

        mockMvc.perform(get("/host/dashboard")
                .header("X-Test-User-Id", HOST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upcomingReservations.length()").value(1))
            .andExpect(jsonPath("$.pendingRequests.length()").value(1))
            .andExpect(jsonPath("$.mockEarnings").value(org.hamcrest.Matchers.greaterThan(0)))
            .andExpect(jsonPath("$.listingPerformance.length()").value(1))
            .andExpect(jsonPath("$.listingPerformance[0].bookingCount").value(3))
            .andExpect(result -> {
                JsonNode occupancyRate = objectMapper.readTree(result.getResponse().getContentAsString())
                    .path("occupancyRate");
                org.junit.jupiter.api.Assertions.assertTrue(occupancyRate.isNumber(), "occupancyRate must be numeric");
                org.junit.jupiter.api.Assertions.assertTrue(occupancyRate.asDouble() > 0.0, "occupancyRate must be positive");
            });
    }

    @Test
    void hostDashboardRequiresAuth() throws Exception {
        mockMvc.perform(get("/host/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    private Listing createListing(String title, int nightlyPrice) {
        Listing listing = Listing.builder()
            .hostId(HOST_ID)
            .title(title)
            .description("A great place")
            .category(category)
            .propertyType(Listing.PropertyType.HOUSE)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal(nightlyPrice))
            .status(Listing.Status.PUBLISHED)
            .build();
        listing.setLocation(ListingLocation.builder()
            .listing(listing)
            .addressLine1("123 Main St")
            .city("Miami")
            .country("USA")
            .latitude(25.76)
            .longitude(-80.19)
            .build());
        return listingRepository.save(listing);
    }

    private void createReservation(Listing listing, UUID guestId, LocalDate checkIn, LocalDate checkOut, Reservation.Status status) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal total = listing.getNightlyPrice().multiply(BigDecimal.valueOf(nights));
        reservationRepository.save(Reservation.builder()
            .listing(listing)
            .guestId(guestId)
            .checkIn(checkIn)
            .checkOut(checkOut)
            .guests(2)
            .totalNights(nights)
            .nightlyPrice(listing.getNightlyPrice())
            .cleaningFee(BigDecimal.ZERO)
            .serviceFee(BigDecimal.ZERO)
            .totalAmount(total)
            .status(status)
            .build());
    }
}
