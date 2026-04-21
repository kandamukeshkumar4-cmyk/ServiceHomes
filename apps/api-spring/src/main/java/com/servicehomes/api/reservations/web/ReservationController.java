package com.servicehomes.api.reservations.web;

import com.servicehomes.api.reservations.application.ReservationService;
import com.servicehomes.api.reservations.application.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/quote")
    public ResponseEntity<QuoteResponse> quote(@Valid @RequestBody QuoteRequest request) {
        return ResponseEntity.ok(reservationService.quote(request));
    }

    @PostMapping
    public ResponseEntity<ReservationDto> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateReservationRequest request
    ) {
        UUID guestId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(reservationService.create(guestId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ReservationDto>> myReservations(
        @AuthenticationPrincipal Jwt jwt,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID guestId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(reservationService.listByGuest(guestId, pageable));
    }

    @GetMapping("/host")
    public ResponseEntity<Page<ReservationDto>> hostReservations(
        @AuthenticationPrincipal Jwt jwt,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID hostId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(reservationService.listByHost(hostId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationDto> cancelByGuest(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID guestId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(reservationService.cancelByGuest(guestId, id));
    }

    @PostMapping("/{id}/cancel-by-host")
    public ResponseEntity<ReservationDto> cancelByHost(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable UUID id
    ) {
        UUID hostId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(reservationService.cancelByHost(hostId, id));
    }
}
