package com.servicehomes.api.listings.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.listings.application.dto.AvailabilityRuleDto;
import com.servicehomes.api.listings.application.dto.ListingAvailabilityResponse;
import com.servicehomes.api.listings.application.dto.ListingCalendarResponse;
import com.servicehomes.api.listings.application.dto.UpdateListingAvailabilityRequest;
import com.servicehomes.api.listings.domain.LengthOfStayDiscount;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingAvailabilityRule;
import com.servicehomes.api.listings.domain.ListingAvailabilityRuleRepository;
import com.servicehomes.api.listings.domain.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final ListingRepository listingRepository;
    private final ListingAvailabilityRuleRepository availabilityRuleRepository;
    private final HostCalendarService hostCalendarService;
    private final EventPublisher eventPublisher;

    public ListingAvailabilityResponse getAvailability(UUID hostId, UUID listingId) {
        Listing listing = requireOwnedListing(hostId, listingId);
        return toAvailabilityResponse(listing, availabilityRuleRepository.findByListingIdOrderByStartDateAsc(listingId));
    }

    @Transactional
    public ListingAvailabilityResponse updateAvailability(
        UUID hostId,
        UUID listingId,
        UpdateListingAvailabilityRequest request
    ) {
        Listing listing = requireOwnedListing(hostId, listingId);
        List<ListingAvailabilityRule> replacementRules = buildRules(listing, request.rules());

        listing.getAvailabilityRules().clear();
        listing.getAvailabilityRules().addAll(replacementRules);
        listingRepository.save(listing);

        eventPublisher.publish(
            "listing_availability_updated",
            "listing",
            listingId.toString(),
            Map.of(
                "listingId", listingId.toString(),
                "hostId", hostId.toString(),
                "ruleCount", Integer.toString(replacementRules.size())
            )
        );

        return toAvailabilityResponse(listing, replacementRules);
    }

    public ListingCalendarResponse getCalendar(UUID hostId, UUID listingId, LocalDate startDate, LocalDate endDate) {
        Listing listing = requireOwnedListing(hostId, listingId);
        LocalDate resolvedStart = startDate != null ? startDate : LocalDate.now();
        LocalDate resolvedEnd = endDate != null ? endDate : resolvedStart.plusDays(41);
        if (resolvedEnd.isBefore(resolvedStart)) {
            throw new IllegalArgumentException("Calendar end date must be on or after the start date");
        }

        List<ListingAvailabilityRule> rules = availabilityRuleRepository
            .findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(
                listingId,
                resolvedStart,
                resolvedEnd
            );

        List<ListingCalendarResponse.CalendarDayDto> days = new ArrayList<>();
        int defaultMinNights = defaultMinNights(listing);
        BigDecimal baseNightlyPrice = listing.getNightlyPrice();
        for (LocalDate cursor = resolvedStart; !cursor.isAfter(resolvedEnd); cursor = cursor.plusDays(1)) {
            ListingAvailabilityRule minRule = findApplicableRule(rules, ListingAvailabilityRule.RuleType.MIN_NIGHTS_OVERRIDE, cursor);
            ListingAvailabilityRule priceRule = findApplicableRule(rules, ListingAvailabilityRule.RuleType.PRICE_OVERRIDE, cursor);
            boolean blocked = findApplicableRule(rules, ListingAvailabilityRule.RuleType.BLOCKED_DATE, cursor) != null;

            days.add(new ListingCalendarResponse.CalendarDayDto(
                cursor,
                blocked,
                minRule != null ? minRule.getValue().intValue() : defaultMinNights,
                priceRule != null ? priceRule.getValue() : baseNightlyPrice,
                minRule != null,
                priceRule != null
            ));
        }

        return new ListingCalendarResponse(listingId, resolvedStart, resolvedEnd, days);
    }

    public StayEvaluation evaluateStay(Listing listing, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        LocalDate lastNight = checkOut.minusDays(1);
        List<ListingAvailabilityRule> rules = availabilityRuleRepository
            .findByListingIdAndEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(
                listing.getId(),
                checkIn,
                lastNight
            );

        int effectiveMinNights = defaultMinNights(listing);
        int priceOverrideNights = 0;
        BigDecimal subtotal = BigDecimal.ZERO;

        for (LocalDate cursor = checkIn; cursor.isBefore(checkOut); cursor = cursor.plusDays(1)) {
            if (findApplicableRule(rules, ListingAvailabilityRule.RuleType.BLOCKED_DATE, cursor) != null) {
                throw new IllegalArgumentException("Dates are not available for this listing");
            }

            ListingAvailabilityRule minRule = findApplicableRule(rules, ListingAvailabilityRule.RuleType.MIN_NIGHTS_OVERRIDE, cursor);
            if (minRule != null) {
                effectiveMinNights = Math.max(effectiveMinNights, minRule.getValue().intValue());
            }

            ListingAvailabilityRule priceRule = findApplicableRule(rules, ListingAvailabilityRule.RuleType.PRICE_OVERRIDE, cursor);
            BigDecimal nightlyPrice = priceRule != null ? priceRule.getValue() : listing.getNightlyPrice();
            subtotal = subtotal.add(nightlyPrice);
            if (priceRule != null) {
                priceOverrideNights++;
            }
        }

        if (nights < effectiveMinNights) {
            throw new IllegalArgumentException("Minimum nights requirement not met for selected dates");
        }

        Optional<LengthOfStayDiscount> losDiscount = hostCalendarService.findApplicableLengthOfStayDiscount(listing.getId(), nights);
        if (losDiscount.isPresent()) {
            BigDecimal discountPercent = losDiscount.get().getDiscountPercent();
            BigDecimal discountFactor = BigDecimal.ONE.subtract(
                discountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            );
            subtotal = subtotal.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageNightlyPrice = subtotal.divide(BigDecimal.valueOf(nights), 2, RoundingMode.HALF_UP);
        return new StayEvaluation(nights, averageNightlyPrice, subtotal, effectiveMinNights, priceOverrideNights);
    }

    public record StayEvaluation(
        int totalNights,
        BigDecimal averageNightlyPrice,
        BigDecimal subtotal,
        int minimumNightsRequired,
        int priceOverrideNights
    ) {}

    private Listing requireOwnedListing(UUID hostId, UUID listingId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
        if (!listing.getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        return listing;
    }

    private List<ListingAvailabilityRule> buildRules(
        Listing listing,
        List<UpdateListingAvailabilityRequest.AvailabilityRuleInput> inputs
    ) {
        validateRules(inputs);
        return inputs.stream()
            .map(input -> ListingAvailabilityRule.builder()
                .listing(listing)
                .ruleType(parseRuleType(input.ruleType()))
                .startDate(input.startDate())
                .endDate(input.endDate())
                .value(sanitizeRuleValue(parseRuleType(input.ruleType()), input.value()))
                .build())
            .sorted(Comparator.comparing(ListingAvailabilityRule::getStartDate).thenComparing(ListingAvailabilityRule::getEndDate))
            .collect(Collectors.toList());
    }

    private void validateRules(List<UpdateListingAvailabilityRequest.AvailabilityRuleInput> inputs) {
        Map<ListingAvailabilityRule.RuleType, List<UpdateListingAvailabilityRequest.AvailabilityRuleInput>> groupedRules = inputs.stream()
            .peek(this::validateRule)
            .collect(Collectors.groupingBy(input -> parseRuleType(input.ruleType())));

        groupedRules.values().forEach(rules -> {
            List<UpdateListingAvailabilityRequest.AvailabilityRuleInput> orderedRules = rules.stream()
                .sorted(Comparator.comparing(UpdateListingAvailabilityRequest.AvailabilityRuleInput::startDate)
                    .thenComparing(UpdateListingAvailabilityRequest.AvailabilityRuleInput::endDate))
                .toList();

            for (int index = 1; index < orderedRules.size(); index++) {
                UpdateListingAvailabilityRequest.AvailabilityRuleInput previous = orderedRules.get(index - 1);
                UpdateListingAvailabilityRequest.AvailabilityRuleInput current = orderedRules.get(index);
                if (!current.startDate().isAfter(previous.endDate())) {
                    throw new IllegalArgumentException("Availability rules of the same type cannot overlap");
                }
            }
        });
    }

    private void validateRule(UpdateListingAvailabilityRequest.AvailabilityRuleInput input) {
        ListingAvailabilityRule.RuleType ruleType = parseRuleType(input.ruleType());
        if (input.endDate().isBefore(input.startDate())) {
            throw new IllegalArgumentException("Availability rule end date must be on or after the start date");
        }

        if (ruleType == ListingAvailabilityRule.RuleType.BLOCKED_DATE) {
            return;
        }

        if (input.value() == null) {
            throw new IllegalArgumentException("Availability override values are required");
        }

        if (ruleType == ListingAvailabilityRule.RuleType.MIN_NIGHTS_OVERRIDE) {
            if (input.value().scale() > 0 && input.value().stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("Minimum nights overrides must be whole numbers");
            }
            if (input.value().intValue() <= 0) {
                throw new IllegalArgumentException("Minimum nights overrides must be greater than zero");
            }
        }

        if (ruleType == ListingAvailabilityRule.RuleType.PRICE_OVERRIDE && input.value().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price overrides cannot be negative");
        }
    }

    private ListingAvailabilityRule.RuleType parseRuleType(String rawRuleType) {
        try {
            return ListingAvailabilityRule.RuleType.valueOf(rawRuleType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported availability rule type: " + rawRuleType);
        }
    }

    private BigDecimal sanitizeRuleValue(ListingAvailabilityRule.RuleType ruleType, BigDecimal value) {
        if (ruleType == ListingAvailabilityRule.RuleType.BLOCKED_DATE) {
            return null;
        }
        return value;
    }

    private ListingAvailabilityResponse toAvailabilityResponse(Listing listing, List<ListingAvailabilityRule> rules) {
        List<AvailabilityRuleDto> ruleDtos = rules.stream()
            .sorted(Comparator.comparing(ListingAvailabilityRule::getStartDate).thenComparing(ListingAvailabilityRule::getEndDate))
            .map(this::toRuleDto)
            .toList();

        return new ListingAvailabilityResponse(
            listing.getId(),
            listing.getNightlyPrice(),
            defaultMinNights(listing),
            ruleDtos
        );
    }

    private AvailabilityRuleDto toRuleDto(ListingAvailabilityRule rule) {
        return new AvailabilityRuleDto(
            rule.getId(),
            rule.getRuleType().name(),
            rule.getStartDate(),
            rule.getEndDate(),
            rule.getValue()
        );
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
}
