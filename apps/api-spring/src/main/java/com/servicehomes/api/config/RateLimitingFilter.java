package com.servicehomes.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_UPLOAD_REQUESTS_PER_MINUTE = 10;

    private final Map<String, WindowedCounter> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        long now = System.currentTimeMillis();

        int limit = path.contains("/upload-by-url") || path.contains("/presigned-url")
            ? MAX_UPLOAD_REQUESTS_PER_MINUTE
            : MAX_REQUESTS_PER_MINUTE;

        WindowedCounter counter = counters.computeIfAbsent(clientIp + ":" + path, k -> new WindowedCounter());
        counter.increment(now, limit);

        if (counter.isExceeded(limit)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isEmpty()) {
            return xForwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class WindowedCounter {
        private volatile long windowStart = System.currentTimeMillis();
        private volatile int count = 0;

        synchronized void increment(long now, int limit) {
            if (now - windowStart > 60_000) {
                windowStart = now;
                count = 0;
            }
            count++;
        }

        boolean isExceeded(int limit) {
            return count > limit;
        }
    }
}
