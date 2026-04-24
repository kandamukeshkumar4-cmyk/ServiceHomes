package com.servicehomes.api.search.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "search.index.leader", havingValue = "true", matchIfMissing = true)
public class SearchIndexRefresher {

    private static final long SEARCH_INDEX_REFRESH_LOCK_ID = 871_320_264_917_301L;

    private final JdbcTemplate jdbcTemplate;
    private final SearchService searchService;

    @Scheduled(fixedDelayString = "${search.index.refresh-delay-ms:60000}")
    public void refreshSearchIndex() {
        if (!tryAcquireRefreshLock()) {
            log.debug("Skipping search materialized view refresh; another instance holds the refresh lock");
            return;
        }

        try {
            refreshMaterializedView();
            searchService.invalidateSearchCache();
        } finally {
            releaseRefreshLock();
        }
    }

    private void refreshMaterializedView() {
        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY search_listings_materialized");
            log.debug("Search materialized view refreshed concurrently");
        } catch (Exception concurrentFailure) {
            log.warn("Failed to refresh search materialized view concurrently, falling back to non-concurrent refresh", concurrentFailure);
            try {
                jdbcTemplate.execute("REFRESH MATERIALIZED VIEW search_listings_materialized");
                log.info("Search materialized view refreshed without concurrency");
            } catch (Exception fallbackFailure) {
                log.error("Failed to refresh search materialized view", fallbackFailure);
            }
        }
    }

    private boolean tryAcquireRefreshLock() {
        Boolean acquired = jdbcTemplate.queryForObject(
            "SELECT pg_try_advisory_lock(?)",
            Boolean.class,
            SEARCH_INDEX_REFRESH_LOCK_ID
        );
        return Boolean.TRUE.equals(acquired);
    }

    private void releaseRefreshLock() {
        try {
            jdbcTemplate.queryForObject(
                "SELECT pg_advisory_unlock(?)",
                Boolean.class,
                SEARCH_INDEX_REFRESH_LOCK_ID
            );
        } catch (Exception e) {
            log.warn("Failed to release search materialized view refresh lock", e);
        }
    }
}
