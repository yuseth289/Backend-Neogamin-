package com.neogaming.common.exception;

import org.springframework.http.HttpStatus;

public class AIServiceException extends NeoGamingException {

    public AIServiceException(String message) {
        super(message, HttpStatus.BAD_GATEWAY, "AI_SERVICE_ERROR");
    }

    public AIServiceException(String message, String errorCode) {
        super(message, HttpStatus.BAD_GATEWAY, errorCode);
    }
}
