package com.servicehomes.api.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.ApiApplication;
import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingCategory;
import com.servicehomes.api.listings.domain.ListingCategoryRepository;
import com.servicehomes.api.listings.domain.ListingPolicy;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.messaging.application.dto.SendMessageRequest;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("ci")
@TestPropertySource(properties = {
    "messaging.email.enabled=true",
    "messaging.email.from-address=test@servicehomes.example"
})
class MessagingIntegrationTest {

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

    private static final UUID HOST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    private static final UUID GUEST_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12");
    private static final String HOST_AUTH0_ID = "auth0|seed-host-1";
    private static final String GUEST_AUTH0_ID = "auth0|seed-guest-1";

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

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private SesClient sesClient;

    @BeforeEach
    void setUp() {
        reset(sesClient);
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
            .thenReturn(SendEmailResponse.builder().messageId(UUID.randomUUID().toString()).build());
    }

    @Test
    void guestCanSendMessageToHost() throws Exception {
        Reservation reservation = createReservation("Messaging Test Home");

        mockMvc.perform(post("/reservations/{id}/messages", reservation.getId())
                .header("X-Test-User-Id", GUEST_AUTH0_ID)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendMessageRequest("Hello host, is parking included?"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reservationId").value(reservation.getId().toString()))
            .andExpect(jsonPath("$.messages.length()").value(1))
            .andExpect(jsonPath("$.messages[0].content").value("Hello host, is parking included?"))
            .andExpect(jsonPath("$.messages[0].mine").value(true));

        ArgumentCaptor<SendEmailRequest> emailCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient, times(1)).sendEmail(emailCaptor.capture());
        SendEmailRequest emailRequest = emailCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(emailRequest.destination().toAddresses()).containsExactly("host@example.com");
        org.assertj.core.api.Assertions.assertThat(emailRequest.message().subject().data()).contains("Messaging Test Home");
    }

    @Test
    void hostCanReplyAndInboxUsesLatestMessageOrdering() throws Exception {
        Reservation firstReservation = createReservation("First Listing");
        Reservation secondReservation = createReservation("Second Listing");

        sendMessage(firstReservation.getId(), GUEST_AUTH0_ID, "Hello from reservation one");
        sendMessage(secondReservation.getId(), GUEST_AUTH0_ID, "Hello from reservation two");

        mockMvc.perform(post("/reservations/{id}/messages", firstReservation.getId())
                .header("X-Test-User-Id", HOST_AUTH0_ID)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendMessageRequest("Reply from host"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages.length()").value(2))
            .andExpect(jsonPath("$.messages[1].content").value("Reply from host"))
            .andExpect(jsonPath("$.messages[1].mine").value(true));

        mockMvc.perform(get("/inbox")
                .header("X-Test-User-Id", GUEST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].reservationId").value(firstReservation.getId().toString()))
            .andExpect(jsonPath("$[0].lastMessagePreview").value("Reply from host"))
            .andExpect(jsonPath("$[1].reservationId").value(secondReservation.getId().toString()));
    }

    @Test
    void thirdUserCannotAccessReservationThread() throws Exception {
        Reservation reservation = createReservation("Protected Thread Listing");
        User outsider = createUser("auth0|outsider", "outsider@example.com", "Mallory");
        sendMessage(reservation.getId(), GUEST_AUTH0_ID, "Private thread message");

        mockMvc.perform(get("/reservations/{id}/messages", reservation.getId())
                .header("X-Test-User-Id", outsider.getAuth0Id()))
            .andExpect(status().isForbidden());
    }

    @Test
    void openingThreadMarksIncomingMessagesRead() throws Exception {
        Reservation reservation = createReservation("Unread Tracking Listing");
        sendMessage(reservation.getId(), GUEST_AUTH0_ID, "Please confirm the check-in details");

        mockMvc.perform(get("/inbox")
                .header("X-Test-User-Id", HOST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].unreadCount").value(1));

        mockMvc.perform(get("/reservations/{id}/messages", reservation.getId())
                .header("X-Test-User-Id", HOST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(0))
            .andExpect(jsonPath("$.messages[0].readAt").isNotEmpty());

        mockMvc.perform(get("/inbox")
                .header("X-Test-User-Id", HOST_AUTH0_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].unreadCount").value(0));
    }

    @Test
    void emailNotificationsRateLimitFreshUnreadThreads() throws Exception {
        Reservation reservation = createReservation("Quiet Period Listing");

        sendMessage(reservation.getId(), GUEST_AUTH0_ID, "First ping");
        sendMessage(reservation.getId(), GUEST_AUTH0_ID, "Second ping");

        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    private void sendMessage(UUID reservationId, String auth0Id, String content) throws Exception {
        mockMvc.perform(post("/reservations/{id}/messages", reservationId)
                .header("X-Test-User-Id", auth0Id)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendMessageRequest(content))))
            .andExpect(status().isOk());
    }

    private Reservation createReservation(String title) {
        Listing listing = createListing(title);
        return reservationRepository.save(Reservation.builder()
            .listing(listing)
            .guestId(GUEST_ID)
            .checkIn(LocalDate.of(2026, 9, 1))
            .checkOut(LocalDate.of(2026, 9, 5))
            .guests(2)
            .totalNights(4)
            .nightlyPrice(new BigDecimal("180.00"))
            .cleaningFee(new BigDecimal("25.00"))
            .serviceFee(new BigDecimal("15.00"))
            .totalAmount(new BigDecimal("760.00"))
            .status(Reservation.Status.CONFIRMED)
            .build());
    }

    private Listing createListing(String title) {
        ListingCategory category = categoryRepository.findAll().isEmpty()
            ? categoryRepository.save(ListingCategory.builder().name("Messaging").icon("msg").description("Messaging").build())
            : categoryRepository.findAll().get(0);

        Listing listing = Listing.builder()
            .hostId(HOST_ID)
            .title(title)
            .description("Listing used for messaging tests")
            .category(category)
            .propertyType(Listing.PropertyType.HOUSE)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("180.00"))
            .cleaningFee(new BigDecimal("25.00"))
            .serviceFee(new BigDecimal("15.00"))
            .status(Listing.Status.PUBLISHED)
            .build();

        ListingPolicy policy = ListingPolicy.builder()
            .listing(listing)
            .minNights(1)
            .instantBook(true)
            .build();
        listing.setPolicy(policy);

        return listingRepository.save(listing);
    }

    private User createUser(String auth0Id, String email, String displayName) {
        User user = User.builder()
            .auth0Id(auth0Id)
            .email(email)
            .emailVerified(true)
            .build();

        Profile profile = Profile.builder()
            .user(user)
            .displayName(displayName)
            .build();
        user.setProfile(profile);

        return userRepository.save(user);
    }
}
