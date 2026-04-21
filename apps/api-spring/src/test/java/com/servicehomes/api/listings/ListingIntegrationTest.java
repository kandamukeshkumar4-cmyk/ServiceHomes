package com.servicehomes.api.listings;

import com.servicehomes.api.listings.application.dto.CreateListingRequest;
import com.servicehomes.api.listings.application.dto.ListingDto;
import com.servicehomes.api.listings.domain.*;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
class ListingIntegrationTest {

    private static final UUID SEED_HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

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

    @Test
    void createListing() throws Exception {
        ListingCategory category = categoryRepository.findAll().get(0);

        var request = new CreateListingRequest(
            "Test Listing",
            "A nice place to stay",
            category.getId(),
            "APARTMENT",
            4,
            2,
            2,
            1,
            new BigDecimal("100.00"),
            new BigDecimal("20.00"),
            new BigDecimal("10.00"),
            new CreateListingRequest.LocationDto("123 Main St", "", "New York", "NY", "10001", "USA", 40.7, -74.0),
            new CreateListingRequest.PolicyDto(null, null, 1, null, "FLEXIBLE", false),
            List.of()
        );

        mockMvc.perform(post("/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test Listing"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getListingById() throws Exception {
        ListingCategory category = categoryRepository.findAll().get(0);

        Listing listing = Listing.builder()
            .hostId(SEED_HOST_ID)
            .title("Get By Id Test")
            .description("Test description")
            .category(category)
            .propertyType(Listing.PropertyType.APARTMENT)
            .maxGuests(2)
            .bedrooms(1)
            .beds(1)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("50.00"))
            .cleaningFee(new BigDecimal("10.00"))
            .serviceFee(new BigDecimal("5.00"))
            .status(Listing.Status.PUBLISHED)
            .build();
        listing = listingRepository.save(listing);

        mockMvc.perform(get("/listings/{id}", listing.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(listing.getId().toString()))
            .andExpect(jsonPath("$.title").value("Get By Id Test"));
    }
}
