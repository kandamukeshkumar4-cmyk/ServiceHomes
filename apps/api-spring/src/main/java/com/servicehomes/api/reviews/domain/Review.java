package com.servicehomes.api.reviews.domain;

import com.servicehomes.api.listings.domain.Listing;
import com.servicehomes.api.reservations.domain.Reservation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewer_role", nullable = false, length = 16)
    private ReviewerRole reviewerRole;

    @Column(nullable = false)
    private int rating;

    @Column(name = "cleanliness_rating")
    private Integer cleanlinessRating;

    @Column(name = "accuracy_rating")
    private Integer accuracyRating;

    @Column(name = "communication_rating")
    private Integer communicationRating;

    @Column(name = "location_rating")
    private Integer locationRating;

    @Column(name = "value_rating")
    private Integer valueRating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "host_response", columnDefinition = "TEXT")
    private String hostResponse;

    @Column(name = "visible_at", nullable = false)
    private Instant visibleAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 32)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.APPROVED;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "moderated_by")
    private UUID moderatedBy;

    @Column(name = "moderation_notes", columnDefinition = "TEXT")
    private String moderationNotes;

    @Column(name = "report_count", nullable = false)
    @Builder.Default
    private int reportCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum ReviewerRole {
        GUEST,
        HOST
    }

    public enum ModerationStatus {
        APPROVED,
        HIDDEN
    }
}
