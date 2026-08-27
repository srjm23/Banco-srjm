package com.bancoprogramacao.api.dto;

import com.bancoprogramacao.api.domain.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record AccountStatusUpdateRequest(
        @NotNull(message = "O status é obrigatório.")
        AccountStatus status
) {
}

