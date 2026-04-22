package com.servicehomes.api.reservations.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.identity.domain.Profile;
import com.servicehomes.api.identity.domain.User;
import com.servicehomes.api.identity.domain.UserRepository;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingPhoto;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.reservations.application.dto.*;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    public QuoteResponse quote(QuoteRequest request) {
        Listing listing = listingRepository.findById(request.listingId())
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        int nights = (int) ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        BigDecimal subtotal = listing.getNightlyPrice().multiply(BigDecimal.valueOf(nights));
        BigDecimal cleaningFee = listing.getCleaningFee() != null ? listing.getCleaningFee() : BigDecimal.ZERO;
        BigDecimal serviceFee = listing.getServiceFee() != null ? listing.getServiceFee() : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(cleaningFee).add(serviceFee);

        return new QuoteResponse(nights, listing.getNightlyPrice(), subtotal, cleaningFee, serviceFee, total);
    }

    @Transactional
    public ReservationDto create(UUID guestId, CreateReservationRequest request) {
        Listing listing = listingRepository.findById(request.listingId())
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

        if (listing.getPolicy() != null) {
            if (nights < listing.getPolicy().getMinNights()) {
                throw new IllegalArgumentException("Minimum nights requirement not met");
            }
            if (listing.getPolicy().getMaxNights() != null && nights > listing.getPolicy().getMaxNights()) {
                throw new IllegalArgumentException("Maximum nights exceeded");
            }
        }

        long overlaps = listingRepository.countOverlappingReservations(listing.getId(), checkIn, checkOut);
        if (overlaps > 0) {
            throw new IllegalArgumentException("Dates are not available for this listing");
        }

        QuoteResponse quote = quote(new QuoteRequest(request.listingId(), checkIn, checkOut, request.guests()));

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

        Reservation saved = reservationRepository.save(reservation);
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

        if (reservation.getStatus() == Reservation.Status.CANCELLED_BY_GUEST ||
            reservation.getStatus() == Reservation.Status.CANCELLED_BY_HOST ||
            reservation.getStatus() == Reservation.Status.COMPLETED) {
            throw new IllegalArgumentException("Reservation cannot be cancelled");
        }

        reservation.setStatus(Reservation.Status.CANCELLED_BY_GUEST);
        eventPublisher.publish("reservation_cancelled", "reservation", reservationId.toString(),
            java.util.Map.of("by", "guest", "listingId", reservation.getListing().getId().toString()));
        return toDto(reservation);
    }

    @Transactional
    public ReservationDto cancelByHost(UUID hostId, UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getListing().getHostId().equals(hostId)) {
            throw new IllegalArgumentException("Not authorized");
        }

        if (reservation.getStatus() == Reservation.Status.CANCELLED_BY_GUEST ||
            reservation.getStatus() == Reservation.Status.CANCELLED_BY_HOST ||
            reservation.getStatus() == Reservation.Status.COMPLETED) {
            throw new IllegalArgumentException("Reservation cannot be cancelled");
        }

        reservation.setStatus(Reservation.Status.CANCELLED_BY_HOST);
        eventPublisher.publish("reservation_cancelled", "reservation", reservationId.toString(),
            java.util.Map.of("by", "host", "listingId", reservation.getListing().getId().toString()));
        return toDto(reservation);
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

    private ReservationDto toDto(Reservation r) {
        Listing l = r.getListing();
        String hostName = userRepository.findById(l.getHostId())
            .map(User::getProfile)
            .map(Profile::getDisplayName)
            .orElse("Host");

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
            hostName
        );
    }
}
