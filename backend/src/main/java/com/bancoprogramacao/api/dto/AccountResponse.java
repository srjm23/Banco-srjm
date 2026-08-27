package com.bancoprogramacao.api.dto;

import com.bancoprogramacao.api.domain.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String number,
        String checkDigit,
        String accountReference,
        ClientResponse client,
        AccountStatus status,
        boolean administrator,
        BigDecimal balance,
        Instant createdAt,
        Instant closedAt
) {
}
