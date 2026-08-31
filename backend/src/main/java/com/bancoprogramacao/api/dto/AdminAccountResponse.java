package com.bancoprogramacao.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import com.bancoprogramacao.api.domain.AccountStatus;

public record AdminAccountResponse(
        String accountReference,
        String holderName,
        String email,
        String phone,
        String maskedCpf,
        AccountStatus status,
        Instant createdAt,
        BigDecimal balance
) {
}
