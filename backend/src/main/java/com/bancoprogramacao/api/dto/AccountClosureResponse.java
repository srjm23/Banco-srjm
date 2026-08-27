package com.bancoprogramacao.api.dto;

public record AccountClosureResponse(
        String message,
        AccountResponse account
) {
}
