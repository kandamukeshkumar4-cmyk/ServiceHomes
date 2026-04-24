package com.servicehomes.api.search.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
        try {
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                if (!tryAcquireRefreshLock(connection)) {
                    log.debug("Skipping search materialized view refresh; another instance holds the refresh lock");
                    return null;
                }

                try {
                    refreshMaterializedView(connection);
                    searchService.invalidateSearchCache();
                } finally {
                    releaseRefreshLock(connection);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to run search materialized view refresh", e);
        }
    }

    private void refreshMaterializedView(Connection connection) {
        try {
            execute(connection, "REFRESH MATERIALIZED VIEW CONCURRENTLY search_listings_materialized");
            log.debug("Search materialized view refreshed concurrently");
        } catch (Exception concurrentFailure) {
            log.warn("Failed to refresh search materialized view concurrently, falling back to non-concurrent refresh", concurrentFailure);
            try {
                execute(connection, "REFRESH MATERIALIZED VIEW search_listings_materialized");
                log.info("Search materialized view refreshed without concurrency");
            } catch (Exception fallbackFailure) {
                log.error("Failed to refresh search materialized view", fallbackFailure);
            }
        }
    }

    private boolean tryAcquireRefreshLock(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            statement.setLong(1, SEARCH_INDEX_REFRESH_LOCK_ID);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean(1);
            }
        } catch (Exception e) {
            log.warn("Failed to acquire search materialized view refresh lock", e);
            return false;
        }
    }

    private void releaseRefreshLock(Connection connection) {
        try {
            try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_unlock(?)")) {
                statement.setLong(1, SEARCH_INDEX_REFRESH_LOCK_ID);
                statement.executeQuery();
            }
        } catch (Exception e) {
            log.warn("Failed to release search materialized view refresh lock", e);
        }
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
