package com.servicehomes.api.dashboards.application;

import com.servicehomes.api.dashboards.application.dto.GuestDashboardResponse;
import com.servicehomes.api.dashboards.application.dto.TripDto;
import com.servicehomes.api.listings.domain.ListingPhoto;
import com.servicehomes.api.messaging.domain.MessageRepository;
import com.servicehomes.api.messaging.domain.MessageThread;
import com.servicehomes.api.messaging.domain.MessageThreadRepository;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import com.servicehomes.api.reviews.domain.Review;
import com.servicehomes.api.reviews.domain.ReviewRepository;
import com.servicehomes.api.saved.domain.SavedListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestDashboardService {

    private final ReservationRepository reservationRepository;
    private final SavedListingRepository savedListingRepository;
    private final MessageThreadRepository messageThreadRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;

    public GuestDashboardResponse getDashboard(UUID guestId) {
        LocalDate today = LocalDate.now();
        List<Reservation> guestReservations = reservationRepository.findByGuestId(guestId, Pageable.unpaged()).getContent();

        List<TripDto> upcoming = guestReservations.stream()
            .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED)
            .filter(r -> !r.getCheckIn().isBefore(today))
            .sorted(java.util.Comparator.comparing(Reservation::getCheckIn))
            .map(r -> toTripDto(r, false))
            .toList();

        List<TripDto> past = guestReservations.stream()
            .filter(r -> r.getCheckOut().isBefore(today))
            .sorted(java.util.Comparator.comparing(Reservation::getCheckOut).reversed())
            .map(r -> toTripDto(r, canReview(r)))
            .toList();

        long savedCount = savedListingRepository.countByGuestId(guestId);
        long unreadThreads = countUnreadThreads(guestId);

        return new GuestDashboardResponse(upcoming, past, savedCount, unreadThreads);
    }

    private boolean canReview(Reservation r) {
        if (r.getStatus() != Reservation.Status.COMPLETED) {
            return false;
        }
        return !reviewRepository.existsByReservation_IdAndReviewerRole(r.getId(), Review.ReviewerRole.GUEST);
    }

    private long countUnreadThreads(UUID guestId) {
        List<MessageThread> threads = messageThreadRepository.findByGuestIdOrHostId(guestId, guestId);
        return threads.stream()
            .filter(t -> messageRepository.countByThreadIdAndSenderIdNotAndReadAtIsNull(t.getId(), guestId) > 0)
            .count();
    }

    private TripDto toTripDto(Reservation r, boolean canReview) {
        var l = r.getListing();
        return new TripDto(
            r.getId(),
            l.getId(),
            l.getTitle(),
            coverUrl(l),
            l.getLocation() != null ? l.getLocation().getCity() : null,
            l.getLocation() != null ? l.getLocation().getCountry() : null,
            l.getHostId(),
            "Host",
            r.getCheckIn(),
            r.getCheckOut(),
            r.getGuests(),
            r.getTotalNights(),
            r.getTotalAmount(),
            r.getStatus().name(),
            canReview
        );
    }

    private String coverUrl(com.servicehomes.api.listings.domain.Listing listing) {
        return listing.getPhotos().stream()
            .filter(ListingPhoto::isCover)
            .findFirst()
            .map(ListingPhoto::getUrl)
            .orElse(listing.getPhotos().isEmpty() ? null : listing.getPhotos().get(0).getUrl());
    }
}
