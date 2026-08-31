package com.bancoprogramacao.api.dto;

import com.bancoprogramacao.api.domain.PixKeyType;
import jakarta.validation.constraints.NotNull;

public record PixKeyCreateRequest(
        @NotNull(message = "O tipo da chave PIX é obrigatório.") PixKeyType type
) {
}
