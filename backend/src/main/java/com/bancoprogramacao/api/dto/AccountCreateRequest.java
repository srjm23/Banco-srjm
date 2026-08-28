package com.bancoprogramacao.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
        @NotBlank(message = "O nome do titular é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome deve possuir entre 3 e 150 caracteres.")
        String holderName,

        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail deve possuir no máximo 254 caracteres.")
        String email,

        @Pattern(regexp = "\\d{11}", message = "O telefone deve possuir exatamente 11 dígitos, incluindo o DDD.")
        String phone,

        @Pattern(regexp = "\\d{11}", message = "O CPF deve possuir exatamente 11 dígitos.")
        String cpf,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 128, message = "A senha deve possuir entre 6 e 128 caracteres.")
        String password,

        boolean administrator,

        @Size(max = 128, message = "O token administrativo é inválido.")
        String administratorToken
) {
}
