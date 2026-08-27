package com.bancoprogramacao.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminAccountResponse(
        String accountReference,
        String holderName,
        Instant createdAt,
        BigDecimal balance
) {
}
