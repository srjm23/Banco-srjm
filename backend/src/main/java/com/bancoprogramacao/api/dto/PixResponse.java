package com.bancoprogramacao.api.dto;

import java.util.UUID;

public record PixResponse(
        String message,
        UUID transferId,
        AccountResponse sourceAccount,
        AccountResponse destinationAccount,
        TransactionResponse debitTransaction,
        TransactionResponse creditTransaction
) {
}

