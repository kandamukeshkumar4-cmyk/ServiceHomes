package com.servicehomes.api.saved;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.analytics.domain.OutboxEventRepository;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingLocation;
import com.servicehomes.api.listings.domain.ListingRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
class SavedListingIntegrationTest {

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

    private static final String SEED_GUEST_AUTH0_ID = "auth0|seed-guest-1";
    private static final UUID SEED_HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingCategoryRepository categoryRepository;

    @Autowired
    private SavedListingRepository savedListingRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        savedListingRepository.deleteAll();
        listingRepository.deleteAll();
    }

    @Test
    void guestCanSaveListAndUnsaveListings() throws Exception {
        Listing first = createListing("Saved loft", "Porto");
        Listing second = createListing("Saved villa", "Lisbon");

        mockMvc.perform(put("/saved-listings/{listingId}", first.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/saved-listings/{listingId}", second.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/saved-listings")
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].isSaved").value(true))
            .andExpect(jsonPath("$[1].isSaved").value(true));

        mockMvc.perform(delete("/saved-listings/{listingId}", first.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/saved-listings")
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(second.getId().toString()));

        assertThat(outboxEventRepository.findAll())
            .extracting(event -> event.getEventType())
            .contains("listing_saved", "listing_unsaved");
    }

    @Test
    void savingSameListingTwiceIsIdempotent() throws Exception {
        Listing listing = createListing("Repeat save", "Madrid");

        mockMvc.perform(put("/saved-listings/{listingId}", listing.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isNoContent());

        mockMvc.perform(put("/saved-listings/{listingId}", listing.getId())
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/saved-listings")
                .header("X-Test-User-Id", SEED_GUEST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    private Listing createListing(String title, String city) {
        ListingCategory category = categoryRepository.findAll().isEmpty()
            ? categoryRepository.save(ListingCategory.builder().name("Saved").icon("heart").description("Saved").build())
            : categoryRepository.findAll().get(0);

        Listing listing = Listing.builder()
            .hostId(SEED_HOST_ID)
            .title(title)
            .description("Listing used for saved listing tests")
            .category(category)
            .propertyType(Listing.PropertyType.APARTMENT)
            .maxGuests(3)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("180.00"))
            .status(Listing.Status.PUBLISHED)
            .build();
        listing.setLocation(ListingLocation.builder()
            .listing(listing)
            .addressLine1("123 Saved St")
            .city(city)
            .country("Portugal")
            .latitude(40.0)
            .longitude(-8.0)
            .build());
        return listingRepository.save(listing);
    }
}
