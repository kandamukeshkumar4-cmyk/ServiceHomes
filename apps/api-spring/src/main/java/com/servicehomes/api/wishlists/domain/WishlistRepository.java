package com.servicehomes.api.wishlists.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    List<Wishlist> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
    Optional<Wishlist> findByShareToken(String shareToken);

    @Query(value = """
        SELECT *
        FROM wishlists
        WHERE owner_id = :userId
           OR collaborator_ids @> jsonb_build_array(CAST(:userId AS text))
        ORDER BY updated_at DESC
        """, nativeQuery = true)
    List<Wishlist> findAccessibleByUserId(UUID userId);
}
