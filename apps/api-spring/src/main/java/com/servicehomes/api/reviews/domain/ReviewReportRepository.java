package com.servicehomes.api.reviews.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewReportRepository extends JpaRepository<ReviewReport, UUID> {

    boolean existsByReview_IdAndReporterId(UUID reviewId, UUID reporterId);

    Page<ReviewReport> findByStatusOrderByCreatedAtDesc(ReviewReport.Status status, Pageable pageable);

    List<ReviewReport> findByReview_IdAndStatus(UUID reviewId, ReviewReport.Status status);
}
