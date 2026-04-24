package com.servicehomes.api.search;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
class SearchIntegrationTest {

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
    void postSearchReturnsResults() throws Exception {
        createListing("Beach house", "Oceanfront property", "Miami", "USA", 25.76, -80.19, "HOUSE", 3, 4, "250.00");

        String body = """
            {
              "query": "beach",
              "page": 0,
              "size": 10
            }
            """;

        mockMvc.perform(post("/api/listings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Beach house"));
    }

    @Test
    void postSearchFiltersByPriceRange() throws Exception {
        createListing("Budget stay", "Affordable option", "Austin", "USA", 30.26, -97.74, "APARTMENT", 1, 2, "80.00");
        createListing("Luxury villa", "Premium experience", "Austin", "USA", 30.27, -97.73, "VILLA", 5, 10, "500.00");

        String body = """
            {
              "minPrice": 100,
              "maxPrice": 400,
              "page": 0,
              "size": 10
            }
            """;

        mockMvc.perform(post("/api/listings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void postSearchFiltersByBedrooms() throws Exception {
        createListing("Studio", "Compact living", "Seattle", "USA", 47.60, -122.33, "APARTMENT", 0, 1, "90.00");
        createListing("Family home", "Spacious rooms", "Seattle", "USA", 47.61, -122.32, "HOUSE", 4, 8, "300.00");

        String body = """
            {
              "bedrooms": 3,
              "page": 0,
              "size": 10
            }
            """;

        mockMvc.perform(post("/api/listings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Family home"));
    }

    @Test
    void postSearchFiltersByPropertyTypes() throws Exception {
        createListing("Apartment life", "City center", "Chicago", "USA", 41.87, -87.62, "APARTMENT", 2, 4, "150.00");
        createListing("Cabin fever", "Woodland escape", "Chicago", "USA", 41.88, -87.61, "CABIN", 2, 4, "180.00");

        String body = """
            {
              "propertyTypes": ["CABIN"],
              "page": 0,
              "size": 10
            }
            """;

        mockMvc.perform(post("/api/listings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].propertyType").value("CABIN"));
    }

    @Test
    void postSearchExcludesBookedListings() throws Exception {
        Listing booked = createListing("Reserved", "Not available", "Denver", "USA", 39.73, -104.99, "HOUSE", 2, 4, "200.00");
        createListing("Available", "Open dates", "Denver", "USA", 39.74, -104.98, "HOUSE", 2, 4, "210.00");

        reservationRepository.save(Reservation.builder()
            .listing(booked)
            .guestId(SEED_GUEST_ID)
            .checkIn(LocalDate.of(2026, 7, 1))
            .checkOut(LocalDate.of(2026, 7, 10))
            .guests(2)
            .totalNights(9)
            .nightlyPrice(new BigDecimal("200.00"))
            .cleaningFee(BigDecimal.ZERO)
            .serviceFee(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("1800.00"))
            .status(Reservation.Status.CONFIRMED)
            .build());

        String body = """
            {
              "checkIn": "2026-07-03",
              "checkOut": "2026-07-07",
              "page": 0,
              "size": 10
            }
            """;

        mockMvc.perform(post("/api/listings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Available"));
    }

    @Test
    void postSearchSortsByPriceAsc() throws Exception {
        createListing("Expensive", "High end", "Boston", "USA", 42.36, -71.05, "VILLA", 4, 8, "400.00");
        createListing("Cheap", "Budget friendly", "Boston", "USA", 42.37, -71.04, "APARTMENT", 1, 2, "100.00");
        createListing("Mid range", "Average price", "Boston", "USA", 42.38, -71.03, "HOUSE", 2, 4, "200.00");

        String body = """
            {
              "sort": "PRICE_ASC",
              "page": 0,
              "size": 10
            }
            """;

        mockMvc.perform(post("/api/listings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].nightlyPrice").value(100))
            .andExpect(jsonPath("$.content[1].nightlyPrice").value(200))
            .andExpect(jsonPath("$.content[2].nightlyPrice").value(400));
    }

    @Test
    void postSearchValidatesRadiusLimit() throws Exception {
        String body = """
            {
              "lat": 30.26,
              "lng": -97.74,
              "radiusKm": 150,
              "page": 0,
              "size": 10
            }
            """;

        mockMvc.perform(post("/api/listings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getSuggestionsReturnsResults() throws Exception {
        createListing("Portland gem", "Pacific Northwest charm", "Portland", "USA", 45.51, -122.67, "HOUSE", 2, 4, "175.00");

        mockMvc.perform(get("/api/listings/search/suggestions")
                .param("q", "port"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void recordClickSavesClick() throws Exception {
        createListing("Click target", "Description here", "Nashville", "USA", 36.16, -86.78, "HOUSE", 2, 4, "160.00");

        String searchBody = """
            {
              "query": "nashville",
              "page": 0,
              "size": 10
            }
            """;

        String searchResponse = mockMvc.perform(post("/api/listings/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchBody))
            .andReturn()
            .getResponse()
            .getContentAsString();

        var listing = listingRepository.findAll().get(0);
        String searchQueryId = objectMapper.readTree(searchResponse).get("searchQueryId").asText();

        String clickBody = """
            {
              "searchQueryId": "%s",
              "listingId": "%s",
              "resultPosition": 1
            }
            """.formatted(searchQueryId, listing.getId());

        mockMvc.perform(post("/api/listings/search/click")
                .contentType(MediaType.APPLICATION_JSON)
                .content(clickBody))
            .andExpect(status().isNoContent());
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
