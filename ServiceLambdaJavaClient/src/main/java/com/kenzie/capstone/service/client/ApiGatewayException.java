package com.kenzie.capstone.service.client;

public class ApiGatewayException extends RuntimeException {
    public ApiGatewayException(String errorMessage) {
        super(errorMessage);
    }

    public ApiGatewayException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }
}
