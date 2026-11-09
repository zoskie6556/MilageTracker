package com.fueltrack.service;

public class AiReadingException extends RuntimeException {

    public AiReadingException(String message) {
        super(message);
    }

    public AiReadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
