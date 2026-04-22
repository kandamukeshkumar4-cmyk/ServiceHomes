package com.servicehomes.api.listings;

import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingLocation;
import com.servicehomes.api.listings.domain.ListingRepository;
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
class ListingSearchIntegrationTest {

    private static final UUID SEED_HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID SEED_GUEST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");

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

    private ListingCategory category;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        listingRepository.deleteAll();
        category = categoryRepository.findAll().get(0);
    }

    @Test
    void searchMatchesFullTextTermsAndCityNames() throws Exception {
        createListing("Sunny beach house", "Wake up next to the ocean", "Porto", "Portugal", 41.15, -8.61, "HOUSE", 3, 4, "220.00");
        createListing("Cozy cabin retreat", "Forest hideaway with fireplace", "Braga", "Portugal", 41.55, -8.42, "CABIN", 2, 2, "140.00");
        createListing("Urban loft", "City stay with skyline views", "Lisbon", "Portugal", 38.72, -9.13, "APARTMENT", 1, 2, "180.00");

        mockMvc.perform(get("/listings/search")
                .param("locationQuery", "beach"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Sunny beach house"));

        mockMvc.perform(get("/listings/search")
                .param("locationQuery", "cozy cabin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Cozy cabin retreat"));

        mockMvc.perform(get("/listings/search")
                .param("locationQuery", "lisbon"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].city").value("Lisbon"));
    }

    @Test
    void searchOrdersByDistanceWhenCoordinatesProvided() throws Exception {
        createListing("Closest stay", "At origin", "Origin", "Nowhere", 0.0, 0.0, "HOUSE", 1, 2, "100.00");
        createListing("Farther stay", "One degree away", "Faraway", "Nowhere", 1.0, 1.0, "HOUSE", 1, 2, "100.00");

        mockMvc.perform(get("/listings/search")
                .param("lat", "0")
                .param("lng", "0")
                .param("radiusKm", "200")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].title").value("Closest stay"))
            .andExpect(jsonPath("$.content[1].title").value("Farther stay"));
    }

    @Test
    void searchFiltersByBedroomsAndPropertyType() throws Exception {
        createListing("One bedroom flat", "Compact and central", "Madrid", "Spain", 40.41, -3.70, "APARTMENT", 1, 2, "110.00");
        createListing("Villa grande", "Family-sized villa", "Madrid", "Spain", 40.42, -3.69, "VILLA", 4, 8, "320.00");

        mockMvc.perform(get("/listings/search")
                .param("bedrooms", "3")
                .param("propertyTypes", "VILLA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Villa grande"));
    }

    @Test
    void searchExcludesUnavailableListingsForRequestedDates() throws Exception {
        Listing blocked = createListing("Booked stay", "Reserved for the week", "Rome", "Italy", 41.90, 12.49, "HOUSE", 2, 4, "210.00");
        createListing("Open stay", "Still available", "Rome", "Italy", 41.91, 12.50, "HOUSE", 2, 4, "215.00");
        reservationRepository.save(Reservation.builder()
            .listing(blocked)
            .guestId(SEED_GUEST_ID)
            .checkIn(LocalDate.of(2026, 6, 10))
            .checkOut(LocalDate.of(2026, 6, 15))
            .guests(2)
            .totalNights(5)
            .nightlyPrice(new BigDecimal("210.00"))
            .cleaningFee(BigDecimal.ZERO)
            .serviceFee(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("1050.00"))
            .status(Reservation.Status.CONFIRMED)
            .build());

        mockMvc.perform(get("/listings/search")
                .param("checkIn", "2026-06-12")
                .param("checkOut", "2026-06-14"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Open stay"));
    }

    @Test
    void searchPaginatesWithStableTotals() throws Exception {
        for (int index = 1; index <= 5; index++) {
            createListing(
                "Paged listing " + index,
                "Pagination case " + index,
                "Dublin",
                "Ireland",
                53.34 + (index * 0.01),
                -6.26 - (index * 0.01),
                "HOUSE",
                2,
                4,
                String.valueOf(100 + (index * 10))
            );
        }

        mockMvc.perform(get("/listings/search")
                .param("sort", "PRICE_ASC")
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].nightlyPrice").value(110))
            .andExpect(jsonPath("$.content[1].nightlyPrice").value(120));

        mockMvc.perform(get("/listings/search")
                .param("sort", "PRICE_ASC")
                .param("page", "1")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].nightlyPrice").value(130))
            .andExpect(jsonPath("$.content[1].nightlyPrice").value(140));
    }

    @Test
    void searchRejectsOversizedRadius() throws Exception {
        createListing("Radius listing", "Any description", "Austin", "USA", 30.26, -97.74, "HOUSE", 2, 4, "150.00");

        mockMvc.perform(get("/listings/search")
                .param("lat", "30.26")
                .param("lng", "-97.74")
                .param("radiusKm", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("radiusKm must be less than or equal to 100"));
    }

    private Listing createListing(
        String title,
        String description,
        String city,
        String country,
        double latitude,
        double longitude,
        String propertyType,
        int bedrooms,
        int maxGuests,
        String nightlyPrice
    ) {
        Listing listing = Listing.builder()
            .hostId(SEED_HOST_ID)
            .title(title)
            .description(description)
            .category(category)
            .propertyType(Listing.PropertyType.valueOf(propertyType))
            .maxGuests(maxGuests)
            .bedrooms(bedrooms)
            .beds(Math.max(1, bedrooms))
            .bathrooms(1)
            .nightlyPrice(new BigDecimal(nightlyPrice))
            .status(Listing.Status.PUBLISHED)
            .build();

        listing.setLocation(ListingLocation.builder()
            .listing(listing)
            .addressLine1("123 Search St")
            .city(city)
            .country(country)
            .latitude(latitude)
            .longitude(longitude)
            .build());

        return listingRepository.save(listing);
    }
}
