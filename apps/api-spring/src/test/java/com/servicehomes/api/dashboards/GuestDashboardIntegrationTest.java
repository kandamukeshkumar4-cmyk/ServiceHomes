package com.servicehomes.api.dashboards;

import com.servicehomes.api.listings.domain.*;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import com.servicehomes.api.saved.domain.SavedListing;
import com.servicehomes.api.saved.domain.SavedListingRepository;
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
class GuestDashboardIntegrationTest {

    private static final UUID HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID GUEST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");

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
    private SavedListingRepository savedListingRepository;

    private ListingCategory category;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        savedListingRepository.deleteAll();
        listingRepository.deleteAll();
        category = categoryRepository.findAll().get(0);
    }

    @Test
    void guestDashboardReturnsTripsAndSavedCount() throws Exception {
        Listing listing = createListing("Cozy Cabin", 150);
        LocalDate today = LocalDate.now();

        // Upcoming trip
        createReservation(listing, GUEST_ID, today.plusDays(3), today.plusDays(6), Reservation.Status.CONFIRMED);
        // Past trip (reviewable)
        createReservation(listing, GUEST_ID, today.minusDays(10), today.minusDays(5), Reservation.Status.CONFIRMED);
        // Past cancelled trip
        createReservation(listing, GUEST_ID, today.minusDays(20), today.minusDays(15), Reservation.Status.CANCELLED_BY_GUEST);

        savedListingRepository.save(SavedListing.builder().guestId(GUEST_ID).listing(listing).build());

        mockMvc.perform(get("/api/guest/dashboard")
                .header("X-Test-User-Id", GUEST_ID.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.upcomingTrips.length()").value(1))
            .andExpect(jsonPath("$.pastTrips.length()").value(2))
            .andExpect(jsonPath("$.pastTrips[?(@.canReview == true)]").exists())
            .andExpect(jsonPath("$.savedListingsCount").value(1));
    }

    @Test
    void guestDashboardRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/guest/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    private Listing createListing(String title, int nightlyPrice) {
        Listing listing = Listing.builder()
            .hostId(HOST_ID)
            .title(title)
            .description("A great place")
            .category(category)
            .propertyType(Listing.PropertyType.CABIN)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal(nightlyPrice))
            .status(Listing.Status.PUBLISHED)
            .build();
        listing.setLocation(ListingLocation.builder()
            .listing(listing)
            .addressLine1("123 Forest Rd")
            .city("Denver")
            .country("USA")
            .latitude(39.74)
            .longitude(-104.99)
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
