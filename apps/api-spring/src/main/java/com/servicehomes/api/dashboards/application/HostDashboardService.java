package com.servicehomes.api.dashboards.application;

import com.servicehomes.api.dashboards.application.dto.HostDashboardResponse;
import com.servicehomes.api.dashboards.application.dto.ListingPerformanceDto;
import com.servicehomes.api.dashboards.application.dto.ReservationPipelineDto;
import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.listings.domain.ListingPhoto;
import com.servicehomes.api.listings.domain.ListingRepository;
import com.servicehomes.api.messaging.domain.MessageRepository;
import com.servicehomes.api.messaging.domain.MessageThread;
import com.servicehomes.api.messaging.domain.MessageThreadRepository;
import com.servicehomes.api.reservations.domain.Reservation;
import com.servicehomes.api.reservations.domain.ReservationRepository;
import com.servicehomes.api.reviews.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HostDashboardService {

    private final ReservationRepository reservationRepository;
    private final ListingRepository listingRepository;
    private final MessageThreadRepository messageThreadRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;

    public HostDashboardResponse getDashboard(UUID hostId) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);

        List<Reservation> hostReservations = reservationRepository.findByHostId(hostId, Pageable.unpaged()).getContent();

        List<ReservationPipelineDto> upcoming = hostReservations.stream()
            .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED)
            .filter(r -> !r.getCheckIn().isBefore(today))
            .sorted(java.util.Comparator.comparing(Reservation::getCheckIn))
            .limit(10)
            .map(this::toPipelineDto)
            .toList();

        List<ReservationPipelineDto> pending = hostReservations.stream()
            .filter(r -> r.getStatus() == Reservation.Status.PENDING)
            .sorted(java.util.Comparator.comparing(Reservation::getCreatedAt).reversed())
            .map(this::toPipelineDto)
            .toList();

        double occupancyRate = calculateOccupancyRate(hostReservations, today, thirtyDaysLater);
        BigDecimal mockEarnings = calculateMockEarnings(hostReservations, today);
        List<ListingPerformanceDto> performance = buildListingPerformance(hostId);
        long unreadThreads = countUnreadThreads(hostId);

        return new HostDashboardResponse(upcoming, pending, occupancyRate, mockEarnings, performance, unreadThreads);
    }

    private double calculateOccupancyRate(List<Reservation> reservations, LocalDate windowStart, LocalDate windowEnd) {
        long totalNights = 30L;
        long blockedNights = reservations.stream()
            .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED || r.getStatus() == Reservation.Status.PENDING)
            .mapToLong(r -> countOverlappingNights(r, windowStart, windowEnd))
            .sum();
        return totalNights == 0 ? 0.0 : Math.min(100.0, (blockedNights * 100.0) / totalNights);
    }

    private long countOverlappingNights(Reservation r, LocalDate windowStart, LocalDate windowEnd) {
        LocalDate start = r.getCheckIn().isBefore(windowStart) ? windowStart : r.getCheckIn();
        LocalDate end = r.getCheckOut().isAfter(windowEnd) ? windowEnd : r.getCheckOut();
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
        return Math.max(0, days);
    }

    private BigDecimal calculateMockEarnings(List<Reservation> reservations, LocalDate today) {
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();
        return reservations.stream()
            .filter(r -> r.getStatus() == Reservation.Status.CONFIRMED || r.getStatus() == Reservation.Status.COMPLETED)
            .filter(r -> {
                LocalDate ci = r.getCheckIn();
                return ci.getMonthValue() == currentMonth && ci.getYear() == currentYear;
            })
            .map(Reservation::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ListingPerformanceDto> buildListingPerformance(UUID hostId) {
        List<Listing> listings = listingRepository.findByHostId(hostId, Pageable.unpaged()).getContent();
        return listings.stream()
            .map(listing -> {
                long bookingCount = reservationRepository.countByListing_IdAndStatusIn(
                    listing.getId(),
                    List.of(Reservation.Status.CONFIRMED, Reservation.Status.COMPLETED)
                );
                Double avgRating = reviewRepository.calculateAverageRatingByListingId(listing.getId());
                long reviewCount = reviewRepository.countReviewsByListingId(listing.getId());
                return new ListingPerformanceDto(
                    listing.getId(),
                    listing.getTitle(),
                    coverUrl(listing),
                    bookingCount,
                    avgRating,
                    reviewCount
                );
            })
            .toList();
    }

    private long countUnreadThreads(UUID hostId) {
        List<MessageThread> threads = messageThreadRepository.findByGuestIdOrHostId(hostId, hostId);
        return threads.stream()
            .filter(t -> messageRepository.countByThreadIdAndSenderIdNotAndReadAtIsNull(t.getId(), hostId) > 0)
            .count();
    }

    private ReservationPipelineDto toPipelineDto(Reservation r) {
        var l = r.getListing();
        return new ReservationPipelineDto(
            r.getId(),
            l.getId(),
            l.getTitle(),
            coverUrl(l),
            l.getLocation() != null ? l.getLocation().getCity() : null,
            l.getLocation() != null ? l.getLocation().getCountry() : null,
            r.getGuestId(),
            "Guest",
            r.getCheckIn(),
            r.getCheckOut(),
            r.getGuests(),
            r.getTotalNights(),
            r.getTotalAmount(),
            r.getStatus().name()
        );
    }

    private String coverUrl(Listing listing) {
        return listing.getPhotos().stream()
            .filter(ListingPhoto::isCover)
            .findFirst()
            .map(ListingPhoto::getUrl)
            .orElse(listing.getPhotos().isEmpty() ? null : listing.getPhotos().get(0).getUrl());
    }
}
