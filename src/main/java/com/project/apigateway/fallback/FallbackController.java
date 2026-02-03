package com.project.apigateway.fallback;

import com.project.apigateway.error.GatewayErrorCode;
import com.project.apigateway.error.GatewayErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.awt.*;

@Slf4j
@RestController
public class FallbackController {

    @RequestMapping(value = "/__fallback/mobile-bff", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<GatewayErrorResponse>> mobileBffFallback() {
        String traceId = MDC.get("traceId");

        log.warn("CircuitBreaker fallback triggered for mobile-bff");

        return Mono.just(
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new GatewayErrorResponse(
                                GatewayErrorCode.DOWNSTREAM_UNAVAILABLE.name(),
                                "Сервис временно недоступен",
                                traceId
                        ))
        );
    }
}
