package com.servicehomes.api.wishlists.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {
    @EntityGraph(attributePaths = {"listing", "listing.photos"})
    List<WishlistItem> findByWishlistIdOrderBySortOrderAscAddedAtAsc(UUID wishlistId);

    @EntityGraph(attributePaths = {"listing", "listing.photos"})
    Page<WishlistItem> findByWishlistIdOrderBySortOrderAscAddedAtAsc(UUID wishlistId, Pageable pageable);
    boolean existsByWishlistIdAndListingId(UUID wishlistId, UUID listingId);
    Optional<WishlistItem> findByWishlistIdAndId(UUID wishlistId, UUID itemId);

    @EntityGraph(attributePaths = {"listing", "listing.photos"})
    Optional<WishlistItem> findByWishlistIdAndListingId(UUID wishlistId, UUID listingId);
    long countByWishlistId(UUID wishlistId);

    @Query("""
        SELECT COALESCE(MAX(item.sortOrder), -1) + 1
        FROM WishlistItem item
        WHERE item.wishlist.id = :wishlistId
        """)
    int findNextSortOrderByWishlistId(@Param("wishlistId") UUID wishlistId);

    @Query("""
        SELECT item.wishlist.id AS wishlistId, COUNT(item) AS itemCount
        FROM WishlistItem item
        WHERE item.wishlist.id IN :wishlistIds
        GROUP BY item.wishlist.id
        """)
    List<WishlistItemCount> countByWishlistIds(@Param("wishlistIds") Collection<UUID> wishlistIds);

    @Query("""
        SELECT item.wishlist.id
        FROM WishlistItem item
        WHERE item.wishlist.id IN :wishlistIds
          AND item.listing.id = :listingId
        """)
    List<UUID> findWishlistIdsContainingListing(@Param("wishlistIds") Collection<UUID> wishlistIds, @Param("listingId") UUID listingId);

    void deleteByWishlistIdAndId(UUID wishlistId, UUID itemId);
    void deleteByWishlistIdAndListingId(UUID wishlistId, UUID listingId);

    interface WishlistItemCount {
        UUID getWishlistId();
        long getItemCount();
    }
}
