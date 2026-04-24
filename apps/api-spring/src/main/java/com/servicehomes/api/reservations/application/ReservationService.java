package com.servicehomes.api.reservations.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.application.AvailabilityService;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingPhoto;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reservations.application.dto.*;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;
    private final AvailabilityService availabilityService;

    public QuoteResponse quote(QuoteRequest request) {
        Listing listing = listingRepository.findById(request.listingId())
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        AvailabilityService.StayEvaluation stayEvaluation = availabilityService.evaluateStay(
            listing,
            request.checkIn(),
            request.checkOut()
        );

        return toQuoteResponse(listing, stayEvaluation);
    }

    @Transactional
    public ReservationDto create(UUID guestId, CreateReservationRequest request) {
        Listing listing = listingRepository.findByIdWithLock(request.listingId())
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        if (listing.getStatus() != Listing.Status.PUBLISHED) {
            throw new IllegalArgumentException("Listing is not available for booking");
        }

        if (listing.getHostId().equals(guestId)) {
            throw new IllegalArgumentException("Cannot book your own listing");
        }

        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        if (request.guests() > listing.getMaxGuests()) {
            throw new IllegalArgumentException("Exceeds maximum guest capacity");
        }

        if (listing.getPolicy() != null && listing.getPolicy().getMaxNights() != null && nights > listing.getPolicy().getMaxNights()) {
            throw new IllegalArgumentException("Maximum nights exceeded");
        }

        long overlaps = listingRepository.countOverlappingReservations(listing.getId(), checkIn, checkOut);
        if (overlaps > 0) {
            throw new IllegalArgumentException("Dates are not available for this listing");
        }

        AvailabilityService.StayEvaluation stayEvaluation = availabilityService.evaluateStay(listing, checkIn, checkOut);
        QuoteResponse quote = toQuoteResponse(listing, stayEvaluation);

        Reservation.Status status = listing.getPolicy() != null && listing.getPolicy().isInstantBook()
            ? Reservation.Status.CONFIRMED
            : Reservation.Status.PENDING;

        Reservation reservation = Reservation.builder()
            .listing(listing)
            .guestId(guestId)
            .checkIn(checkIn)
            .checkOut(checkOut)
            .guests(request.guests())
            .totalNights(quote.totalNights())
            .nightlyPrice(quote.nightlyPrice())
            .cleaningFee(quote.cleaningFee())
            .serviceFee(quote.serviceFee())
            .totalAmount(quote.totalAmount())
            .status(status)
            .build();

        Reservation saved;
        try {
            saved = reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Dates are not available for this listing");
        }
        String eventName = status == Reservation.Status.CONFIRMED ? "reservation_confirmed" : "reservation_created";
        eventPublisher.publish(eventName, "reservation", saved.getId().toString(),
            java.util.Map.of(
                "listingId", listing.getId().toString(),
                "guestId", guestId.toString(),
                "totalAmount", quote.totalAmount().toString(),
                "status", status.name()
            ));
        return toDto(saved);
    }

    @Transactional
    public ReservationDto cancelByGuest(UUID guestId, UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getGuestId().equals(guestId)) {
            throw new IllegalArgumentException("Not authorized");
        }

        if (!isGuestCancellable(reservation.getStatus())) {
            throw new IllegalArgumentException("Reservation cannot be cancelled");
        }

        return transitionStatus(
            reservation,
            Reservation.Status.CANCELLED_BY_GUEST,
            "reservation_cancelled",
            Map.of(
                "by", "guest",
                "listingId", reservation.getListing().getId().toString(),
                "guestId", reservation.getGuestId().toString(),
                "status", Reservation.Status.CANCELLED_BY_GUEST.name()
            )
        );
    }

    @Transactional
    public ReservationDto cancelByHost(UUID hostId, UUID reservationId) {
        Reservation reservation = getHostReservation(hostId, reservationId);
        if (reservation.getStatus() == Reservation.Status.PENDING || !isHostCancellable(reservation.getStatus())) {
            throw new IllegalArgumentException("Reservation cannot be cancelled");
        }

        return transitionStatus(
            reservation,
            Reservation.Status.CANCELLED_BY_HOST,
            "reservation_cancelled",
            Map.of(
                "by", "host",
                "listingId", reservation.getListing().getId().toString(),
                "guestId", reservation.getGuestId().toString(),
                "status", Reservation.Status.CANCELLED_BY_HOST.name()
            )
        );
    }

    @Transactional
    public ReservationDto acceptByHost(UUID hostId, UUID reservationId) {
        Reservation reservation = getHostReservation(hostId, reservationId);
        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalArgumentException("Only pending reservations can be accepted");
        }

        return transitionStatus(
            reservation,
            Reservation.Status.CONFIRMED,
            "reservation_confirmed",
            Map.of(
                "listingId", reservation.getListing().getId().toString(),
                "guestId", reservation.getGuestId().toString(),
                "totalAmount", reservation.getTotalAmount().toString(),
                "status", Reservation.Status.CONFIRMED.name(),
                "decisionBy", "host"
            )
        );
    }

    @Transactional
    public ReservationDto declineByHost(UUID hostId, UUID reservationId) {
        Reservation reservation = getHostReservation(hostId, reservationId);
        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new IllegalArgumentException("Only pending reservations can be declined");
        }

        return transitionStatus(
            reservation,
            Reservation.Status.DECLINED,
            "reservation_declined",
            Map.of(
                "listingId", reservation.getListing().getId().toString(),
                "guestId", reservation.getGuestId().toString(),
                "status", Reservation.Status.DECLINED.name(),
                "decisionBy", "host"
            )
        );
    }

    public Page<ReservationDto> listByGuest(UUID guestId, Pageable pageable) {
        return reservationRepository.findByGuestId(guestId, pageable)
            .map(this::toDto);
    }

    public Page<ReservationDto> listByHost(UUID hostId, Pageable pageable) {
        return reservationRepository.findByHostId(hostId, pageable)
            .map(this::toDto);
    }

    public ReservationDto getById(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        return toDto(reservation);
    }

    private Reservation getHostReservation(UUID hostId, UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getListing().getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }
        return reservation;
    }

    private boolean isGuestCancellable(Reservation.Status status) {
        return status == Reservation.Status.PENDING || status == Reservation.Status.CONFIRMED;
    }

    private boolean isHostCancellable(Reservation.Status status) {
        return status == Reservation.Status.CONFIRMED;
    }

    private ReservationDto transitionStatus(
        Reservation reservation,
        Reservation.Status status,
        String eventName,
        Map<String, String> payload
    ) {
        reservation.setStatus(status);
        eventPublisher.publish(eventName, "reservation", reservation.getId().toString(), payload);
        return toDto(reservation);
    }

    private ReservationDto toDto(Reservation r) {
        Listing l = r.getListing();
        String hostName = userRepository.findById(l.getHostId())
            .map(User::getProfile)
            .map(Profile::getDisplayName)
            .orElse("Host");
        String guestName = userRepository.findById(r.getGuestId())
            .map(User::getProfile)
            .map(Profile::getDisplayName)
            .orElse("Guest");

        String coverUrl = l.getPhotos().stream()
            .filter(ListingPhoto::isCover)
            .findFirst()
            .map(ListingPhoto::getUrl)
            .orElse(l.getPhotos().isEmpty() ? null : l.getPhotos().get(0).getUrl());

        return new ReservationDto(
            r.getId(),
            l.getId(),
            l.getTitle(),
            coverUrl,
            l.getLocation() != null ? l.getLocation().getCity() : null,
            l.getLocation() != null ? l.getLocation().getCountry() : null,
            r.getGuestId(),
            r.getCheckIn(),
            r.getCheckOut(),
            r.getGuests(),
            r.getTotalNights(),
            r.getNightlyPrice(),
            r.getCleaningFee(),
            r.getServiceFee(),
            r.getTotalAmount(),
            r.getStatus().name(),
            hostName,
            guestName
        );
    }

    private QuoteResponse toQuoteResponse(Listing listing, AvailabilityService.StayEvaluation stayEvaluation) {
        BigDecimal cleaningFee = listing.getCleaningFee() != null ? listing.getCleaningFee() : BigDecimal.ZERO;
        BigDecimal serviceFee = listing.getServiceFee() != null ? listing.getServiceFee() : BigDecimal.ZERO;
        BigDecimal total = stayEvaluation.subtotal().add(cleaningFee).add(serviceFee);

        return new QuoteResponse(
            stayEvaluation.totalNights(),
            stayEvaluation.averageNightlyPrice(),
            stayEvaluation.subtotal(),
            cleaningFee,
            serviceFee,
            total
        );
    }
}
