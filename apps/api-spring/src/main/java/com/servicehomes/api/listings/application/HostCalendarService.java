package com.servicehomes.api.listings.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.application.dto.*;
import com.servicehomes.api.listings.domain.*;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostCalendarService {

    private final ListingRepository listingRepository;
    private final ListingAvailabilityRuleRepository availabilityRuleRepository;
    private final SeasonalPricingTemplateRepository seasonalPricingTemplateRepository;
    private final LengthOfStayDiscountRepository lengthOfStayDiscountRepository;
    private final WeekendMultiplierRepository weekendMultiplierRepository;
    private final TurnoverDayRepository turnoverDayRepository;
    private final ReservationRepository reservationRepository;
    private final EventPublisher eventPublisher;

    public HostCalendarResponse getCalendar(UUID hostId, UUID listingId, LocalDate startDate, LocalDate endDate) {
        Listing listing = requireOwnedListing(hostId, listingId);
        LocalDate resolvedStart = startDate != null ? startDate : LocalDate.now();
        LocalDate resolvedEnd = endDate != null ? endDate : resolvedStart.plusDays(41);
        if (resolvedEnd.isBefore(resolvedStart)) {
            throw new IllegalArgumentException("Calendar end date must be on or after the start date");
        }

        List<ListingAvailabilityRule> rules = availabilityRuleRepository
            .findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(
                listingId, resolvedStart, resolvedEnd
            );

        List<SeasonalPricingTemplate> templates = seasonalPricingTemplateRepository
            .findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
                listingId, resolvedStart, resolvedEnd
            );

        Optional<WeekendMultiplier> weekendMultiplierOpt = weekendMultiplierRepository.findByListingId(listingId);
        Optional<TurnoverDay> turnoverDayOpt = turnoverDayRepository.findByListingId(listingId);

        Set<LocalDate> blockedDates = rules.stream()
            .filter(r -> r.getRuleType() == ListingAvailabilityRule.RuleType.BLOCKED_DATE)
            .flatMap(r -> r.getStartDate().datesUntil(r.getEndDate().plusDays(1)))
            .collect(Collectors.toSet());

        Set<LocalDate> turnoverDates = computeTurnoverDates(listingId, resolvedStart, resolvedEnd, turnoverDayOpt);

        List<HostCalendarDayDto> days = new ArrayList<>();
        BigDecimal basePrice = listing.getNightlyPrice();

        for (LocalDate cursor = resolvedStart; !cursor.isAfter(resolvedEnd); cursor = cursor.plusDays(1)) {
            boolean blocked = blockedDates.contains(cursor);
            boolean turnover = turnoverDates.contains(cursor);

            int minNights = defaultMinNights(listing);
            ListingAvailabilityRule minRule = findApplicableRule(rules, ListingAvailabilityRule.RuleType.MIN_NIGHTS_OVERRIDE, cursor);
            if (minRule != null) {
                minNights = minRule.getValue().intValue();
            }

            BigDecimal priceOverride = null;
            ListingAvailabilityRule priceRule = findApplicableRule(rules, ListingAvailabilityRule.RuleType.PRICE_OVERRIDE, cursor);
            boolean hasPriceOverride = priceRule != null;
            if (hasPriceOverride) {
                priceOverride = priceRule.getValue();
            }

            BigDecimal seasonalMultiplier = BigDecimal.ONE;
            SeasonalPricingTemplate template = findApplicableTemplate(templates, cursor);
            boolean hasSeasonalTemplate = template != null;
            if (hasSeasonalTemplate) {
                seasonalMultiplier = template.getMultiplier();
            }

            BigDecimal weekendMultiplier = BigDecimal.ONE;
            boolean hasWeekendMultiplier = false;
            if (weekendMultiplierOpt.isPresent()) {
                WeekendMultiplier wm = weekendMultiplierOpt.get();
                DayOfWeek dow = cursor.getDayOfWeek();
                if (dow == DayOfWeek.FRIDAY) {
                    weekendMultiplier = wm.getFridayMultiplier();
                    hasWeekendMultiplier = weekendMultiplier.compareTo(BigDecimal.ONE) != 0;
                } else if (dow == DayOfWeek.SATURDAY) {
                    weekendMultiplier = wm.getSaturdayMultiplier();
                    hasWeekendMultiplier = weekendMultiplier.compareTo(BigDecimal.ONE) != 0;
                } else if (dow == DayOfWeek.SUNDAY) {
                    weekendMultiplier = wm.getSundayMultiplier();
                    hasWeekendMultiplier = weekendMultiplier.compareTo(BigDecimal.ONE) != 0;
                }
            }

            BigDecimal finalNightlyPrice;
            if (hasPriceOverride) {
                finalNightlyPrice = priceOverride;
            } else {
                finalNightlyPrice = basePrice
                    .multiply(seasonalMultiplier)
                    .multiply(weekendMultiplier)
                    .setScale(2, RoundingMode.HALF_UP);
            }

            days.add(new HostCalendarDayDto(
                cursor,
                blocked,
                turnover,
                minNights,
                basePrice,
                seasonalMultiplier,
                weekendMultiplier,
                priceOverride,
                finalNightlyPrice,
                hasPriceOverride,
                hasSeasonalTemplate,
                hasWeekendMultiplier
            ));
        }

        return new HostCalendarResponse(listingId, resolvedStart, resolvedEnd, days);
    }

    @Transactional
    public SeasonalPricingTemplateDto createSeasonalPricingTemplate(
        UUID hostId,
        UUID listingId,
        CreateSeasonalPricingTemplateRequest request
    ) {
        Listing listing = requireOwnedListing(hostId, listingId);
        validateTemplateDates(listingId, request.startDate(), request.endDate(), null);

        SeasonalPricingTemplate template = SeasonalPricingTemplate.builder()
            .listing(listing)
            .name(request.name())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .multiplier(request.multiplier())
            .build();

        SeasonalPricingTemplate saved = seasonalPricingTemplateRepository.save(template);

        eventPublisher.publish(
            "seasonal_pricing_template_created",
            "listing",
            listingId.toString(),
            Map.of(
                "eventId", UUID.randomUUID().toString(),
                "timestamp", java.time.Instant.now().toString(),
                "listingId", listingId.toString(),
                "hostId", hostId.toString(),
                "templateId", saved.getId().toString(),
                "name", request.name(),
                "startDate", request.startDate().toString(),
                "endDate", request.endDate().toString(),
                "multiplier", request.multiplier().toString()
            )
        );

        return toTemplateDto(saved);
    }

    @Transactional
    public SeasonalPricingTemplateDto updateSeasonalPricingTemplate(
        UUID hostId,
        UUID listingId,
        UUID templateId,
        CreateSeasonalPricingTemplateRequest request
    ) {
        requireOwnedListing(hostId, listingId);
        SeasonalPricingTemplate template = seasonalPricingTemplateRepository
            .findByIdAndListingId(templateId, listingId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        validateTemplateDates(listingId, request.startDate(), request.endDate(), templateId);

        template.setName(request.name());
        template.setStartDate(request.startDate());
        template.setEndDate(request.endDate());
        template.setMultiplier(request.multiplier());

        return toTemplateDto(seasonalPricingTemplateRepository.save(template));
    }

    @Transactional
    public void deleteSeasonalPricingTemplate(UUID hostId, UUID listingId, UUID templateId) {
        requireOwnedListing(hostId, listingId);
        seasonalPricingTemplateRepository.deleteByIdAndListingId(templateId, listingId);
    }

    public List<SeasonalPricingTemplateDto> getSeasonalPricingTemplates(UUID hostId, UUID listingId) {
        requireOwnedListing(hostId, listingId);
        return seasonalPricingTemplateRepository.findByListingIdOrderByStartDateAsc(listingId)
            .stream()
            .map(this::toTemplateDto)
            .toList();
    }

    @Transactional
    public LengthOfStayDiscountDto createLengthOfStayDiscount(
        UUID hostId,
        UUID listingId,
        CreateLengthOfStayDiscountRequest request
    ) {
        Listing listing = requireOwnedListing(hostId, listingId);
        if (request.minNights() <= 0) {
            throw new IllegalArgumentException("Minimum nights must be greater than zero");
        }
        if (request.discountPercent().compareTo(BigDecimal.ZERO) < 0 || request.discountPercent().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount percent must be between 0 and 100");
        }

        LengthOfStayDiscount discount = LengthOfStayDiscount.builder()
            .listing(listing)
            .minNights(request.minNights())
            .discountPercent(request.discountPercent())
            .build();

        LengthOfStayDiscount saved = lengthOfStayDiscountRepository.save(discount);

        eventPublisher.publish(
            "length_of_stay_discount_created",
            "listing",
            listingId.toString(),
            Map.of(
                "eventId", UUID.randomUUID().toString(),
                "timestamp", java.time.Instant.now().toString(),
                "listingId", listingId.toString(),
                "hostId", hostId.toString(),
                "discountId", saved.getId().toString(),
                "minNights", String.valueOf(request.minNights()),
                "discountPercent", request.discountPercent().toString()
            )
        );

        return toDiscountDto(saved);
    }

    @Transactional
    public LengthOfStayDiscountDto updateLengthOfStayDiscount(
        UUID hostId,
        UUID listingId,
        UUID discountId,
        CreateLengthOfStayDiscountRequest request
    ) {
        requireOwnedListing(hostId, listingId);
        LengthOfStayDiscount discount = lengthOfStayDiscountRepository
            .findByIdAndListingId(discountId, listingId)
            .orElseThrow(() -> new IllegalArgumentException("Discount not found"));

        if (request.minNights() <= 0) {
            throw new IllegalArgumentException("Minimum nights must be greater than zero");
        }
        if (request.discountPercent().compareTo(BigDecimal.ZERO) < 0 || request.discountPercent().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Discount percent must be between 0 and 100");
        }

        discount.setMinNights(request.minNights());
        discount.setDiscountPercent(request.discountPercent());

        return toDiscountDto(lengthOfStayDiscountRepository.save(discount));
    }

    @Transactional
    public void deleteLengthOfStayDiscount(UUID hostId, UUID listingId, UUID discountId) {
        requireOwnedListing(hostId, listingId);
        lengthOfStayDiscountRepository.deleteByIdAndListingId(discountId, listingId);
    }

    public List<LengthOfStayDiscountDto> getLengthOfStayDiscounts(UUID hostId, UUID listingId) {
        requireOwnedListing(hostId, listingId);
        return lengthOfStayDiscountRepository.findByListingIdOrderByMinNightsAsc(listingId)
            .stream()
            .map(this::toDiscountDto)
            .toList();
    }

    @Transactional
    public WeekendMultiplierDto updateWeekendMultiplier(
        UUID hostId,
        UUID listingId,
        UpdateWeekendMultiplierRequest request
    ) {
        Listing listing = requireOwnedListing(hostId, listingId);

        WeekendMultiplier wm = weekendMultiplierRepository.findByListingId(listingId)
            .orElse(WeekendMultiplier.builder().listing(listing).build());

        wm.setFridayMultiplier(request.fridayMultiplier());
        wm.setSaturdayMultiplier(request.saturdayMultiplier());
        wm.setSundayMultiplier(request.sundayMultiplier());

        WeekendMultiplier saved = weekendMultiplierRepository.save(wm);

        eventPublisher.publish(
            "weekend_multiplier_updated",
            "listing",
            listingId.toString(),
            Map.of(
                "eventId", UUID.randomUUID().toString(),
                "timestamp", java.time.Instant.now().toString(),
                "listingId", listingId.toString(),
                "hostId", hostId.toString(),
                "fridayMultiplier", request.fridayMultiplier().toString(),
                "saturdayMultiplier", request.saturdayMultiplier().toString(),
                "sundayMultiplier", request.sundayMultiplier().toString()
            )
        );

        return toWeekendMultiplierDto(saved);
    }

    public Optional<WeekendMultiplierDto> getWeekendMultiplier(UUID hostId, UUID listingId) {
        requireOwnedListing(hostId, listingId);
        return weekendMultiplierRepository.findByListingId(listingId)
            .map(this::toWeekendMultiplierDto);
    }

    @Transactional
    public TurnoverDayDto updateTurnoverDay(
        UUID hostId,
        UUID listingId,
        UpdateTurnoverDayRequest request
    ) {
        Listing listing = requireOwnedListing(hostId, listingId);
        if (request.bufferDays() < 0) {
            throw new IllegalArgumentException("Buffer days cannot be negative");
        }

        TurnoverDay td = turnoverDayRepository.findByListingId(listingId)
            .orElse(TurnoverDay.builder().listing(listing).build());

        td.setBufferDays(request.bufferDays());

        TurnoverDay saved = turnoverDayRepository.save(td);

        eventPublisher.publish(
            "turnover_day_updated",
            "listing",
            listingId.toString(),
            Map.of(
                "eventId", UUID.randomUUID().toString(),
                "timestamp", java.time.Instant.now().toString(),
                "listingId", listingId.toString(),
                "hostId", hostId.toString(),
                "bufferDays", String.valueOf(request.bufferDays())
            )
        );

        return toTurnoverDayDto(saved);
    }

    public Optional<TurnoverDayDto> getTurnoverDay(UUID hostId, UUID listingId) {
        requireOwnedListing(hostId, listingId);
        return turnoverDayRepository.findByListingId(listingId)
            .map(this::toTurnoverDayDto);
    }

    public Optional<LengthOfStayDiscount> findApplicableLengthOfStayDiscount(UUID listingId, int nights) {
        return lengthOfStayDiscountRepository.findByListingIdOrderByMinNightsAsc(listingId)
            .stream()
            .filter(d -> d.getMinNights() <= nights)
            .max(Comparator.comparingInt(LengthOfStayDiscount::getMinNights));
    }

    private Set<LocalDate> computeTurnoverDates(
        UUID listingId,
        LocalDate rangeStart,
        LocalDate rangeEnd,
        Optional<TurnoverDay> turnoverDayOpt
    ) {
        if (turnoverDayOpt.isEmpty() || turnoverDayOpt.get().getBufferDays() <= 0) {
            return Collections.emptySet();
        }

        int buffer = turnoverDayOpt.get().getBufferDays();
        LocalDate queryStart = rangeStart.minusDays(buffer);
        LocalDate queryEnd = rangeEnd;

        List<Reservation> reservations = reservationRepository.findConfirmedByListingIdAndDateRange(
            listingId,
            queryStart,
            queryEnd
        );

        Set<LocalDate> turnoverDates = new HashSet<>();
        for (Reservation r : reservations) {
            LocalDate checkout = r.getCheckOut();
            for (int i = 1; i <= buffer; i++) {
                LocalDate turnoverDate = checkout.plusDays(i);
                if (!turnoverDate.isBefore(rangeStart) && !turnoverDate.isAfter(rangeEnd)) {
                    turnoverDates.add(turnoverDate);
                }
            }
        }
        return turnoverDates;
    }

    private void validateTemplateDates(UUID listingId, LocalDate startDate, LocalDate endDate, UUID excludeId) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Template end date must be on or after the start date");
        }

        List<SeasonalPricingTemplate> existing = seasonalPricingTemplateRepository.findByListingIdOrderByStartDateAsc(listingId);
        for (SeasonalPricingTemplate t : existing) {
            if (excludeId != null && t.getId().equals(excludeId)) {
                continue;
            }
            if (!startDate.isAfter(t.getEndDate()) && !endDate.isBefore(t.getStartDate())) {
                throw new IllegalArgumentException("Seasonal pricing templates cannot overlap");
            }
        }
    }

    private Listing requireOwnedListing(UUID hostId, UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        return listing;
    }

    private int defaultMinNights(Listing listing) {
        return listing.getPolicy() != null ? listing.getPolicy().getMinNights() : 1;
    }

    private ListingAvailabilityRule findApplicableRule(
        List<ListingAvailabilityRule> rules,
        ListingAvailabilityRule.RuleType ruleType,
        LocalDate date
    ) {
        return rules.stream()
            .filter(rule -> rule.getRuleType() == ruleType)
            .filter(rule -> !date.isBefore(rule.getStartDate()) && !date.isAfter(rule.getEndDate()))
            .findFirst()
            .orElse(null);
    }

    private SeasonalPricingTemplate findApplicableTemplate(List<SeasonalPricingTemplate> templates, LocalDate date) {
        return templates.stream()
            .filter(t -> !date.isBefore(t.getStartDate()) && !date.isAfter(t.getEndDate()))
            .findFirst()
            .orElse(null);
    }

    private SeasonalPricingTemplateDto toTemplateDto(SeasonalPricingTemplate t) {
        return new SeasonalPricingTemplateDto(t.getId(), t.getName(), t.getStartDate(), t.getEndDate(), t.getMultiplier());
    }

    private LengthOfStayDiscountDto toDiscountDto(LengthOfStayDiscount d) {
        return new LengthOfStayDiscountDto(d.getId(), d.getMinNights(), d.getDiscountPercent());
    }

    private WeekendMultiplierDto toWeekendMultiplierDto(WeekendMultiplier wm) {
        return new WeekendMultiplierDto(wm.getId(), wm.getFridayMultiplier(), wm.getSaturdayMultiplier(), wm.getSundayMultiplier());
    }

    private TurnoverDayDto toTurnoverDayDto(TurnoverDay td) {
        return new TurnoverDayDto(td.getId(), td.getBufferDays());
    }
}
