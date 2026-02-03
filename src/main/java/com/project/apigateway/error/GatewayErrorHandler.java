package com.project.apigateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@Order(-1)
@RequiredArgsConstructor
public class GatewayErrorHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;
    private static final String INTERNAL_ERROR_CODE = "{\"errorCode\":\"INTERNAL_ERROR\"}";

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);
        GatewayErrorCode errorCode = resolveErrorCode(ex);

        String traceId = MDC.get("traceId");

        GatewayErrorResponse response = new GatewayErrorResponse(
                errorCode.name(),
                resolveMessage(errorCode),
                traceId
        );

        log.error("Gateway error: status={} code={} message={}",
                status, errorCode, ex.getMessage(), ex);

        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(response);
        } catch (Exception e) {
            body = INTERNAL_ERROR_CODE.getBytes(StandardCharsets.UTF_8);
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(body)));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private GatewayErrorCode resolveErrorCode(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            if (rse.getStatusCode().value() == 401) {
                return GatewayErrorCode.UNAUTHORIZED;
            }
            if (rse.getStatusCode().value() == 403) {
                return GatewayErrorCode.UNAUTHORIZED;
            }
            if (rse.getStatusCode().value() == 429) {
                return GatewayErrorCode.RATE_LIMIT_EXCEEDED;
            }
        }
        return GatewayErrorCode.INTERNAL_ERROR;
    }

    private String resolveMessage(GatewayErrorCode code) {
        return switch (code) {
            case UNAUTHORIZED -> "Требуется авторизация";
            case FORBIDDEN -> "Доступ запрещён";
            case DOWNSTREAM_UNAVAILABLE -> "Сервис временно недоступен";
            case RATE_LIMIT_EXCEEDED -> "Превышен лимит запросов";
            case INTERNAL_ERROR -> "Внутренняя ошибка Gateway";
        };
    }
}
