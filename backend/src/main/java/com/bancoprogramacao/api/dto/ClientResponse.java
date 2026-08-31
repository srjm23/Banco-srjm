package com.bancoprogramacao.api.dto;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Instant createdAt
) {
}
