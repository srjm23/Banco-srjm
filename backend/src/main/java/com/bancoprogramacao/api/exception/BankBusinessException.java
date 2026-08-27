package com.bancoprogramacao.api.exception;

import org.springframework.http.HttpStatus;

public class BankBusinessException extends RuntimeException {

    private final HttpStatus status;

    public BankBusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

