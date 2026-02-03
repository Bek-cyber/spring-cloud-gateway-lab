package com.project.apigateway.error;

import lombok.Value;

@Value
public class GatewayErrorResponse {
    String errorCode;
    String message;
    String traceId;
}
