package com.bancoprogramacao.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank(message = "A conta é obrigatória.")
        String account,

        @NotBlank(message = "O código de barras é obrigatório.")
        @Pattern(regexp = "\\d{44}", message = "O código de barras deve possuir exatamente 44 dígitos.")
        String barcode,

        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 13, fraction = 2, message = "Informe um valor com até duas casas decimais.")
        BigDecimal amount,

        @NotBlank(message = "A descrição do pagamento é obrigatória.")
        @Size(max = 120, message = "A descrição deve possuir no máximo 120 caracteres.")
        String description,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 128, message = "A senha deve possuir entre 6 e 128 caracteres.")
        String password
) {
}
