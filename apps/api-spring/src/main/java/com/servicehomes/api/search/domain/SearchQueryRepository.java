package com.servicehomes.api.search.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SearchQueryRepository extends JpaRepository<SearchQuery, UUID> {
}
