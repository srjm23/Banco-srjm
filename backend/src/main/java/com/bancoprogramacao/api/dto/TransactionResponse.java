package com.bancoprogramacao.api.dto;

import com.bancoprogramacao.api.domain.TransactionDirection;
import com.bancoprogramacao.api.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        Long id,
        TransactionDirection direction,
        TransactionType transactionType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyAccount,
        UUID transferId,
        String description,
        Instant createdAt
) {
}

