package com.project.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitGlobalFilter implements GlobalFilter, Ordered {
    private final int requestsPerSecond;
    private final int burstCapacity;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap();

    public RateLimitGlobalFilter(
            @Value("${rate-limit.requests-per-second}") int requestsPerSecond,
            @Value("${rate-limit.burst-capacity}") int burstCapacity
    ) {
        this.requestsPerSecond = requestsPerSecond;
        this.burstCapacity = burstCapacity;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientKey = resolveClientKey(exchange);
        TokenBucket bucket = buckets.computeIfAbsent(
                clientKey,
                k -> new TokenBucket(requestsPerSecond, burstCapacity)
        );

        if (!bucket.tryConsume()) {
            log.warn("Превышен лимит запросов для клиента: {}", clientKey);
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Превышен лимит запросов"
            );
        }

        return chain.filter(exchange);
    }

    private String resolveClientKey(ServerWebExchange exchange) {
        return Objects.requireNonNull(exchange.getRequest()
                        .getRemoteAddress())
                .getAddress()
                .getHostAddress();
    }

    @Override
    public int getOrder() {
        return -40;
    }

    static class TokenBucket {
        private final int refillRatePerSecond;
        private final int capacity;

        private int tokens;
        private Instant lastRefill;

        public TokenBucket(int refillRatePerSecond, int capacity) {
            this.refillRatePerSecond = refillRatePerSecond;
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefill = Instant.now();
        }

        synchronized boolean tryConsume() {
            refill();

            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            Instant now = Instant.now();
            long secondElapsed = now.getEpochSecond() - lastRefill.getEpochSecond();
            if (secondElapsed > 0) {
                int refill = (int) (secondElapsed * refillRatePerSecond);
                tokens = Math.min(capacity, tokens + refill);
                lastRefill = now;
            }
        }
    }
}
