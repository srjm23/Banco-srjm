package com.bancoprogramacao.api.dto;

public record OperationResponse(
        String message,
        AccountResponse account,
        TransactionResponse transaction
) {
}

