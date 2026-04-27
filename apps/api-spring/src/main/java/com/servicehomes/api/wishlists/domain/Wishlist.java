package com.servicehomes.api.wishlists.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wishlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_photo_url")
    private String coverPhotoUrl;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean publicList = false;

    @Column(name = "share_token", unique = true, length = 64)
    private String shareToken;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "collaborator_ids", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private List<UUID> collaboratorIds = new ArrayList<>();

    @OneToMany(mappedBy = "wishlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, addedAt ASC")
    @Builder.Default
    private List<WishlistItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
