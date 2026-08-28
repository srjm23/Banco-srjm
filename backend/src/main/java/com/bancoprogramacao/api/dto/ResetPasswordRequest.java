package com.bancoprogramacao.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "O token é obrigatório.") String token,
        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 6, max = 128, message = "A senha deve possuir entre 6 e 128 caracteres.") String newPassword
) {
}
