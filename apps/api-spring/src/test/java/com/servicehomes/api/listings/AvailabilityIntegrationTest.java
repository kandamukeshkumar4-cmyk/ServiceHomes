package com.servicehomes.api.listings;

import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingAvailabilityRule;
import com.servicehomes.api.listings.domain.ListingAvailabilityRuleRepository;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingPolicy;
import com.servicehomes.api.listings.domain.ListingRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
class AvailabilityIntegrationTest {

    private static final UUID SEED_HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final String SEED_HOST_AUTH0_ID = "auth0|seed-host-1";

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
    private ListingCategoryRepository categoryRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingAvailabilityRuleRepository availabilityRuleRepository;

    @Test
    void hostCanReplaceAndReadAvailabilityRules() throws Exception {
        Listing listing = createListing();

        String body = """
            {
              "rules": [
                { "ruleType": "BLOCKED_DATE", "startDate": "2026-09-10", "endDate": "2026-09-12" },
                { "ruleType": "MIN_NIGHTS_OVERRIDE", "startDate": "2026-09-20", "endDate": "2026-09-22", "value": 4 },
                { "ruleType": "PRICE_OVERRIDE", "startDate": "2026-09-25", "endDate": "2026-09-26", "value": 175.00 }
              ]
            }
            """;

        mockMvc.perform(put("/listings/{id}/availability", listing.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.listingId").value(listing.getId().toString()))
            .andExpect(jsonPath("$.baseNightlyPrice").value(120.00))
            .andExpect(jsonPath("$.defaultMinNights").value(2))
            .andExpect(jsonPath("$.rules.length()").value(3))
            .andExpect(jsonPath("$.rules[0].ruleType").value("BLOCKED_DATE"))
            .andExpect(jsonPath("$.rules[1].value").value(4))
            .andExpect(jsonPath("$.rules[2].value").value(175.0));

        mockMvc.perform(get("/listings/{id}/availability", listing.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rules.length()").value(3))
            .andExpect(jsonPath("$.rules[0].startDate").value("2026-09-10"))
            .andExpect(jsonPath("$.rules[1].ruleType").value("MIN_NIGHTS_OVERRIDE"))
            .andExpect(jsonPath("$.rules[2].ruleType").value("PRICE_OVERRIDE"));
    }

    @Test
    void calendarReflectsBlockedDatesAndOverrides() throws Exception {
        Listing listing = createListing();
        addRule(listing, ListingAvailabilityRule.RuleType.BLOCKED_DATE, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11), null);
        addRule(listing, ListingAvailabilityRule.RuleType.MIN_NIGHTS_OVERRIDE, LocalDate.of(2026, 9, 12), LocalDate.of(2026, 9, 13), new BigDecimal("5"));
        addRule(listing, ListingAvailabilityRule.RuleType.PRICE_OVERRIDE, LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 15), new BigDecimal("155.00"));

        mockMvc.perform(get("/listings/{id}/calendar", listing.getId())
                .header("X-Test-User-Id", SEED_HOST_AUTH0_ID)
                .param("startDate", "2026-09-10")
                .param("endDate", "2026-09-15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.days.length()").value(6))
            .andExpect(jsonPath("$.days[0].date").value("2026-09-10"))
            .andExpect(jsonPath("$.days[0].blocked").value(true))
            .andExpect(jsonPath("$.days[2].minNights").value(5))
            .andExpect(jsonPath("$.days[2].minNightsOverride").value(true))
            .andExpect(jsonPath("$.days[4].nightlyPrice").value(155.0))
            .andExpect(jsonPath("$.days[4].priceOverride").value(true));
    }

    private Listing createListing() {
        ListingCategory category = categoryRepository.findAll().isEmpty()
            ? categoryRepository.save(ListingCategory.builder().name("Availability").icon("pi pi-calendar").description("Availability tests").build())
            : categoryRepository.findAll().get(0);

        Listing listing = Listing.builder()
            .hostId(SEED_HOST_ID)
            .title("Availability Test Listing")
            .description("Calendar enabled listing")
            .category(category)
            .propertyType(Listing.PropertyType.HOUSE)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("120.00"))
            .cleaningFee(new BigDecimal("25.00"))
            .serviceFee(new BigDecimal("12.00"))
            .status(Listing.Status.PUBLISHED)
            .build();

        ListingPolicy policy = ListingPolicy.builder()
            .listing(listing)
            .minNights(2)
            .instantBook(false)
            .build();
        listing.setPolicy(policy);

        return listingRepository.save(listing);
    }

    private void addRule(
        Listing listing,
        ListingAvailabilityRule.RuleType ruleType,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal value
    ) {
        availabilityRuleRepository.save(ListingAvailabilityRule.builder()
            .listing(listing)
            .ruleType(ruleType)
            .startDate(startDate)
            .endDate(endDate)
            .value(value)
            .build());
    }
}
