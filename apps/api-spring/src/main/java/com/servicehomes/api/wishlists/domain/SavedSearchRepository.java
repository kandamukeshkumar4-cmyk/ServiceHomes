package com.servicehomes.api.wishlists.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SavedSearchRepository extends JpaRepository<SavedSearch, UUID> {
    List<SavedSearch> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("""
        SELECT s FROM SavedSearch s
        WHERE s.notifyNewResults = true
          AND (s.lastNotifiedAt IS NULL OR s.lastNotifiedAt <= :dueBefore)
        ORDER BY s.createdAt ASC
        """)
    List<SavedSearch> findDueForNotification(@Param("dueBefore") Instant dueBefore);
}
