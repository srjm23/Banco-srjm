package com.bancoprogramacao.api.dto;

import com.bancoprogramacao.api.domain.PixKeyType;
import java.time.Instant;

public record PixKeyResponse(PixKeyType type, String value, Instant createdAt) {
}
