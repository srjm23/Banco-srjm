package com.bancoprogramacao.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminOverviewResponse(
        long totalCustomers,
        BigDecimal totalBalance,
        List<AdminAccountResponse> accounts
) {
}
