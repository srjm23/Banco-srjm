package com.bancoprogramacao.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountCreateRequest(
        @NotBlank(message = "O nome do titular é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome deve possuir entre 3 e 150 caracteres.")
        String holderName,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 128, message = "A senha deve possuir entre 6 e 128 caracteres.")
        String password,

        boolean administrator,

        @DecimalMin(value = "0.01", message = "O depósito inicial deve ser maior que zero.")
        @Digits(integer = 13, fraction = 2, message = "Informe um valor com até duas casas decimais.")
        BigDecimal initialDeposit
) {
}
