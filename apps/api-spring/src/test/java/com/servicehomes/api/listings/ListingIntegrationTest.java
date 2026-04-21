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

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:15-3.4")
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

        mockMvc.perform(post("/api/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Test Listing"))
            .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getListingById() throws Exception {
        mockMvc.perform(get("/api/listings/search"))
            .andExpect(status().isOk());
    }
}
