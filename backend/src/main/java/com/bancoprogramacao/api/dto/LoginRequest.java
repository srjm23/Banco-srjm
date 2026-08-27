package com.bancoprogramacao.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "A conta é obrigatória.")
        String account,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 128, message = "A senha deve possuir entre 6 e 128 caracteres.")
        String password
) {
}
