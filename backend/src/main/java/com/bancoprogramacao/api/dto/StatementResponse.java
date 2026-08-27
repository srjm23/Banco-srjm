package com.bancoprogramacao.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StatementResponse(
        String bankName,
        Instant generatedAt,
        AccountResponse account,
        BigDecimal currentBalance,
        List<TransactionResponse> transactions
) {
}

