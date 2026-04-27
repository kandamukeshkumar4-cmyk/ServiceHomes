package com.servicehomes.api.listings;

import com.servicehomes.api.listings.application.HostCalendarService;
import com.servicehomes.api.listings.application.dto.*;
import com.servicehomes.api.listings.domain.*;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HostCalendarServiceTest {

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private ListingAvailabilityRuleRepository availabilityRuleRepository;
    @Mock
    private SeasonalPricingTemplateRepository seasonalPricingTemplateRepository;
    @Mock
    private LengthOfStayDiscountRepository lengthOfStayDiscountRepository;
    @Mock
    private WeekendMultiplierRepository weekendMultiplierRepository;
    @Mock
    private TurnoverDayRepository turnoverDayRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private com.servicehomes.api.analytics.application.EventPublisher eventPublisher;

    @InjectMocks
    private HostCalendarService hostCalendarService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    @Test
    void calendarAppliesSeasonalMultiplierOnTopOfBasePrice() {
        Listing listing = createListing();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(availabilityRuleRepository.findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(seasonalPricingTemplateRepository.findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(any(), any(), any()))
            .thenReturn(List.of(
                SeasonalPricingTemplate.builder()
                    .listing(listing)
                    .name("Summer")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 31))
                    .multiplier(new BigDecimal("1.50"))
                    .build()
            ));
        when(weekendMultiplierRepository.findByListingId(listingId)).thenReturn(Optional.empty());
        when(turnoverDayRepository.findByListingId(listingId)).thenReturn(Optional.empty());

        HostCalendarResponse response = hostCalendarService.getCalendar(
            hostId, listingId, LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10)
        );

        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).finalNightlyPrice()).isEqualTo(new BigDecimal("150.00"));
        assertThat(response.days().get(0).hasSeasonalTemplate()).isTrue();
    }

    @Test
    void calendarAppliesWeekendMultiplierOnTopOfSeasonal() {
        Listing listing = createListing();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(availabilityRuleRepository.findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(seasonalPricingTemplateRepository.findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(any(), any(), any()))
            .thenReturn(List.of(
                SeasonalPricingTemplate.builder()
                    .listing(listing)
                    .name("Summer")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 31))
                    .multiplier(new BigDecimal("2.00"))
                    .build()
            ));
        when(weekendMultiplierRepository.findByListingId(listingId)).thenReturn(Optional.of(
            WeekendMultiplier.builder()
                .listing(listing)
                .fridayMultiplier(new BigDecimal("1.00"))
                .saturdayMultiplier(new BigDecimal("1.50"))
                .sundayMultiplier(new BigDecimal("1.00"))
                .build()
        ));
        when(turnoverDayRepository.findByListingId(listingId)).thenReturn(Optional.empty());

        // 2026-07-11 is a Saturday
        HostCalendarResponse response = hostCalendarService.getCalendar(
            hostId, listingId, LocalDate.of(2026, 7, 11), LocalDate.of(2026, 7, 11)
        );

        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).finalNightlyPrice()).isEqualTo(new BigDecimal("300.00"));
        assertThat(response.days().get(0).hasWeekendMultiplier()).isTrue();
    }

    @Test
    void priceOverrideTakesPrecedenceOverSeasonalAndWeekend() {
        Listing listing = createListing();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(availabilityRuleRepository.findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(any(), any(), any()))
            .thenReturn(List.of(
                ListingAvailabilityRule.builder()
                    .listing(listing)
                    .ruleType(ListingAvailabilityRule.RuleType.PRICE_OVERRIDE)
                    .startDate(LocalDate.of(2026, 7, 10))
                    .endDate(LocalDate.of(2026, 7, 10))
                    .value(new BigDecimal("75.00"))
                    .build()
            ));
        when(seasonalPricingTemplateRepository.findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(any(), any(), any()))
            .thenReturn(List.of(
                SeasonalPricingTemplate.builder()
                    .listing(listing)
                    .name("Summer")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 31))
                    .multiplier(new BigDecimal("2.00"))
                    .build()
            ));
        when(weekendMultiplierRepository.findByListingId(listingId)).thenReturn(Optional.empty());
        when(turnoverDayRepository.findByListingId(listingId)).thenReturn(Optional.empty());

        HostCalendarResponse response = hostCalendarService.getCalendar(
            hostId, listingId, LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10)
        );

        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).finalNightlyPrice()).isEqualTo(new BigDecimal("75.00"));
        assertThat(response.days().get(0).hasPriceOverride()).isTrue();
    }

    @Test
    void turnoverDaysAreComputedFromConfirmedReservations() {
        Listing listing = createListing();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(availabilityRuleRepository.findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(seasonalPricingTemplateRepository.findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(weekendMultiplierRepository.findByListingId(listingId)).thenReturn(Optional.empty());
        when(turnoverDayRepository.findByListingId(listingId)).thenReturn(Optional.of(
            TurnoverDay.builder().listing(listing).bufferDays(2).build()
        ));
        when(reservationRepository.findConfirmedByListingIdAndDateRange(any(), any(), any()))
            .thenReturn(List.of(
                Reservation.builder()
                    .listing(listing)
                    .guestId(UUID.randomUUID())
                    .checkIn(LocalDate.of(2026, 7, 5))
                    .checkOut(LocalDate.of(2026, 7, 8))
                    .guests(2)
                    .totalNights(3)
                    .nightlyPrice(new BigDecimal("100"))
                    .totalAmount(new BigDecimal("300"))
                    .status(Reservation.Status.CONFIRMED)
                    .build()
            ));

        HostCalendarResponse response = hostCalendarService.getCalendar(
            hostId, listingId, LocalDate.of(2026, 7, 7), LocalDate.of(2026, 7, 12)
        );

        // July 9 and 10 should be turnover days (2 buffer days after July 8 checkout)
        List<HostCalendarDayDto> days = response.days();
        assertThat(days.stream().filter(d -> d.date().equals(LocalDate.of(2026, 7, 9))).findFirst().orElseThrow().turnover()).isTrue();
        assertThat(days.stream().filter(d -> d.date().equals(LocalDate.of(2026, 7, 10))).findFirst().orElseThrow().turnover()).isTrue();
        assertThat(days.stream().filter(d -> d.date().equals(LocalDate.of(2026, 7, 11))).findFirst().orElseThrow().turnover()).isFalse();
    }

    @Test
    void findApplicableLengthOfStayDiscount_selectsMaxApplicableTier() {
        when(lengthOfStayDiscountRepository.findByListingIdOrderByMinNightsAsc(listingId))
            .thenReturn(List.of(
                LengthOfStayDiscount.builder().minNights(7).discountPercent(new BigDecimal("10")).build(),
                LengthOfStayDiscount.builder().minNights(14).discountPercent(new BigDecimal("15")).build(),
                LengthOfStayDiscount.builder().minNights(30).discountPercent(new BigDecimal("25")).build()
            ));

        Optional<LengthOfStayDiscount> result = hostCalendarService.findApplicableLengthOfStayDiscount(listingId, 30);

        assertThat(result).isPresent();
        assertThat(result.get().getMinNights()).isEqualTo(30);
        assertThat(result.get().getDiscountPercent()).isEqualTo(new BigDecimal("25"));
    }

    @Test
    void findApplicableLengthOfStayDiscount_returnsEmptyWhenNoMatch() {
        when(lengthOfStayDiscountRepository.findByListingIdOrderByMinNightsAsc(listingId))
            .thenReturn(List.of(
                LengthOfStayDiscount.builder().minNights(7).discountPercent(new BigDecimal("10")).build()
            ));

        Optional<LengthOfStayDiscount> result = hostCalendarService.findApplicableLengthOfStayDiscount(listingId, 3);

        assertThat(result).isEmpty();
    }

    @Test
    void createSeasonalTemplate_rejectsOverlappingTemplates() {
        Listing listing = createListing();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(seasonalPricingTemplateRepository.findByListingIdOrderByStartDateAsc(listingId))
            .thenReturn(List.of(
                SeasonalPricingTemplate.builder()
                    .listing(listing)
                    .name("Existing")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 15))
                    .multiplier(new BigDecimal("1.20"))
                    .build()
            ));

        CreateSeasonalPricingTemplateRequest request = new CreateSeasonalPricingTemplateRequest(
            "Overlap", LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 20), new BigDecimal("1.30")
        );

        assertThatThrownBy(() -> hostCalendarService.createSeasonalPricingTemplate(hostId, listingId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot overlap");
    }

    @Test
    void createLengthOfStayDiscount_rejectsInvalidPercent() {
        Listing listing = createListing();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        CreateLengthOfStayDiscountRequest request = new CreateLengthOfStayDiscountRequest(7, new BigDecimal("110"));

        assertThatThrownBy(() -> hostCalendarService.createLengthOfStayDiscount(hostId, listingId, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("between 0 and 100");
    }

    private Listing createListing() {
        Listing listing = Listing.builder()
            .id(listingId)
            .hostId(hostId)
            .title("Test Listing")
            .description("Test")
            .category(ListingCategory.builder().name("Test").icon("pi pi-home").build())
            .propertyType(Listing.PropertyType.APARTMENT)
            .maxGuests(4)
            .bedrooms(2)
            .beds(2)
            .bathrooms(1)
            .nightlyPrice(new BigDecimal("100.00"))
            .status(Listing.Status.PUBLISHED)
            .build();
        ListingPolicy policy = ListingPolicy.builder().listing(listing).minNights(2).instantBook(false).build();
        listing.setPolicy(policy);
        return listing;
    }
}
