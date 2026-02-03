package com.project.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class TraceLoggingGlobalFilter implements GlobalFilter, Ordered {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(MDC_KEY, traceId);

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TRACE_ID_HEADER, traceId)
                .build();

        response.getHeaders().add(TRACE_ID_HEADER, traceId);

        log.info("Входящий запрос: {} {}", request.getMethod(), request.getURI().getRawPath());

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doOnSuccess(unused -> logResponse(response, startTime))
                .doOnError(error -> logError(response, startTime, error))
                .doFinally(signal -> MDC.remove(MDC_KEY));
    }

    private void logResponse(ServerHttpResponse response, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        var status = response.getStatusCode() != null
                ? response.getStatusCode().value()
                : null;

        log.info("Ответ Gateway: status={} latency={}ms", status, duration);
    }

    private void logError(ServerHttpResponse response, long startTime, Throwable error) {
        long duration = System.currentTimeMillis() - startTime;
        var status = response.getStatusCode() != null
                ? response.getStatusCode().value()
                : null;

        log.error("Ошибка Gateway: status={} latency={}ms message={}",
                status, duration, error.getMessage(), error);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
