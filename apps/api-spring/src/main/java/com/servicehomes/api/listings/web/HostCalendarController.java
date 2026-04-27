package com.servicehomes.api.listings.web;

import com.servicehomes.api.identity.application.CurrentUserService;
import com.servicehomes.api.listings.application.HostCalendarService;
import com.servicehomes.api.listings.application.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/host/listings/{listingId}/calendar")
@RequiredArgsConstructor
public class HostCalendarController {

    private final CurrentUserService currentUserService;
    private final HostCalendarService hostCalendarService;

    @GetMapping
    public ResponseEntity<HostCalendarResponse> getCalendar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.getCalendar(hostId, listingId, startDate, endDate));
    }

    @PostMapping("/seasonal-templates")
    public ResponseEntity<SeasonalPricingTemplateDto> createSeasonalTemplate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @Valid @RequestBody CreateSeasonalPricingTemplateRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.createSeasonalPricingTemplate(hostId, listingId, request));
    }

    @PutMapping("/seasonal-templates/{templateId}")
    public ResponseEntity<SeasonalPricingTemplateDto> updateSeasonalTemplate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @PathVariable UUID templateId,
        @Valid @RequestBody CreateSeasonalPricingTemplateRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.updateSeasonalPricingTemplate(hostId, listingId, templateId, request));
    }

    @DeleteMapping("/seasonal-templates/{templateId}")
    public ResponseEntity<Void> deleteSeasonalTemplate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @PathVariable UUID templateId
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        hostCalendarService.deleteSeasonalPricingTemplate(hostId, listingId, templateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/seasonal-templates")
    public ResponseEntity<List<SeasonalPricingTemplateDto>> getSeasonalTemplates(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.getSeasonalPricingTemplates(hostId, listingId));
    }

    @PostMapping("/length-of-stay-discounts")
    public ResponseEntity<LengthOfStayDiscountDto> createLengthOfStayDiscount(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @Valid @RequestBody CreateLengthOfStayDiscountRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.createLengthOfStayDiscount(hostId, listingId, request));
    }

    @PutMapping("/length-of-stay-discounts/{discountId}")
    public ResponseEntity<LengthOfStayDiscountDto> updateLengthOfStayDiscount(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @PathVariable UUID discountId,
        @Valid @RequestBody CreateLengthOfStayDiscountRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.updateLengthOfStayDiscount(hostId, listingId, discountId, request));
    }

    @DeleteMapping("/length-of-stay-discounts/{discountId}")
    public ResponseEntity<Void> deleteLengthOfStayDiscount(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @PathVariable UUID discountId
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        hostCalendarService.deleteLengthOfStayDiscount(hostId, listingId, discountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/length-of-stay-discounts")
    public ResponseEntity<List<LengthOfStayDiscountDto>> getLengthOfStayDiscounts(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.getLengthOfStayDiscounts(hostId, listingId));
    }

    @PutMapping("/weekend-multiplier")
    public ResponseEntity<WeekendMultiplierDto> updateWeekendMultiplier(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @Valid @RequestBody UpdateWeekendMultiplierRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.updateWeekendMultiplier(hostId, listingId, request));
    }

    @GetMapping("/weekend-multiplier")
    public ResponseEntity<WeekendMultiplierDto> getWeekendMultiplier(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return hostCalendarService.getWeekendMultiplier(hostId, listingId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/turnover-day")
    public ResponseEntity<TurnoverDayDto> updateTurnoverDay(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId,
        @Valid @RequestBody UpdateTurnoverDayRequest request
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return ResponseEntity.ok(hostCalendarService.updateTurnoverDay(hostId, listingId, request));
    }

    @GetMapping("/turnover-day")
    public ResponseEntity<TurnoverDayDto> getTurnoverDay(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID listingId
    ) {
        UUID hostId = currentUserService.requireUserId(jwt);
        return hostCalendarService.getTurnoverDay(hostId, listingId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
