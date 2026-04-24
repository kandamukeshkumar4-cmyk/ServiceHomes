package com.servicehomes.api.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbHealthy = checkDatabase();
        Map<String, Object> body = Map.of(
            "status", dbHealthy ? "UP" : "DOWN",
            "database", dbHealthy ? "UP" : "DOWN"
        );
        return dbHealthy
            ? ResponseEntity.ok(body)
            : ResponseEntity.status(503).body(body);
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}
