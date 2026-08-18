package org.ai.clinic.example.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class RateLimitFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 20;

    private final ConcurrentHashMap<String, RateEntry> clients = new ConcurrentHashMap<>();

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                            @NonNull FilterChain filterChain) throws ServletException, IOException {
                String clientIp = request.getRemoteAddr();
                long now = System.currentTimeMillis();
                RateEntry fresh = new RateEntry(now, new AtomicInteger(1));
                RateEntry entry = clients.merge(clientIp, fresh, (prev, value) -> {
                    if (now - prev.windowStart > 60_000) {
                        return value;
                    }
                    prev.count.incrementAndGet();
                    return prev;
                });

                if (entry.count.get() > MAX_REQUESTS_PER_MINUTE) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"detail\":\"Too many requests. Please wait a moment.\"}");
                    return;
                }
                filterChain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns("/clinic/*");
        registration.setOrder(1);
        return registration;
    }

    private record RateEntry(long windowStart, AtomicInteger count) {}
}
