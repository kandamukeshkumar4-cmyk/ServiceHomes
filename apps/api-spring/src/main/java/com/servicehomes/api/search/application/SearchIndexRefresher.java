package com.servicehomes.api.search.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchIndexRefresher {

    private final JdbcTemplate jdbcTemplate;
    private final SearchService searchService;

    @Scheduled(fixedDelay = 60_000)
    public void refreshSearchIndex() {
        try {
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY search_listings_materialized");
            log.debug("Search materialized view refreshed");
        } catch (Exception e) {
            log.warn("Failed to refresh search materialized view concurrently, falling back to non-concurrent refresh", e);
            try {
                jdbcTemplate.execute("REFRESH MATERIALIZED VIEW search_listings_materialized");
                log.info("Search materialized view refreshed (non-concurrent)");
            } catch (Exception ex) {
                log.error("Failed to refresh search materialized view", ex);
            }
        }
        searchService.invalidateSearchCache();
    }
}
