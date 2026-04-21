package com.servicehomes.api.media;

import com.servicehomes.api.listings.application.dto.CreateListingRequest;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
class MediaIntegrationTest {

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

    private UUID listingId;

    private static final UUID SEED_HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @BeforeEach
    void setUp() {
        ListingCategory category = categoryRepository.findAll().isEmpty()
            ? categoryRepository.save(ListingCategory.builder().name("Test").icon("test").description("Test").build())
            : categoryRepository.findAll().get(0);

        Listing listing = Listing.builder()
            .hostId(SEED_HOST_ID)
            .title("Media Test Listing")
            .description("A place with photos")
            .category(category)
            .propertyType(Listing.PropertyType.HOUSE)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("150.00"))
            .cleaningFee(new BigDecimal("30.00"))
            .serviceFee(new BigDecimal("15.00"))
            .status(Listing.Status.PUBLISHED)
            .build();
        listing = listingRepository.save(listing);
        listingId = listing.getId();
    }

    @Test
    @WithMockUser
    void addPhotoByUrl() throws Exception {
        var body = """
            {"url": "https://example.com/photo.jpg"}
            """;

        mockMvc.perform(post("/listings/{id}/photos/upload-by-url", listingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value("https://example.com/photo.jpg"));
    }

    @Test
    @WithMockUser
    void listPhotosReturnsEmptyForNewListing() throws Exception {
        mockMvc.perform(get("/listings/{id}/photos", listingId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void getPresignedUrl() throws Exception {
        var body = """
            {"contentType": "image/jpeg", "fileName": "test.jpg"}
            """;

        mockMvc.perform(post("/listings/{id}/photos/presigned-url", listingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uploadUrl").exists())
            .andExpect(jsonPath("$.s3Key").exists())
            .andExpect(jsonPath("$.publicUrl").exists());
    }
}
