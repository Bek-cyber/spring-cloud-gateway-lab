package com.project.apigateway.error;

public enum GatewayErrorCode {
    UNAUTHORIZED,
    FORBIDDEN,
    DOWNSTREAM_UNAVAILABLE,
    RATE_LIMIT_EXCEEDED,
    INTERNAL_ERROR
}
