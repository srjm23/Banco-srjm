package com.bancoprogramacao.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record DepositRequest(
        @NotBlank(message = "A conta é obrigatória.")
        String account,

        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 13, fraction = 2, message = "Informe um valor com até duas casas decimais.")
        BigDecimal amount
) {
}

