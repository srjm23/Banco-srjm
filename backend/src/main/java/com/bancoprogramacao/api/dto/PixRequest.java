package com.bancoprogramacao.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PixRequest(
        @NotBlank(message = "A conta de origem é obrigatória.")
        String sourceAccount,

        @NotBlank(message = "A conta de destino é obrigatória.")
        String destinationAccount,

        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 13, fraction = 2, message = "Informe um valor com até duas casas decimais.")
        BigDecimal amount,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 128, message = "A senha deve possuir entre 6 e 128 caracteres.")
        String password
) {
}
