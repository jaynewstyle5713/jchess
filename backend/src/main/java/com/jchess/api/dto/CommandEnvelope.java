package com.jchess.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommandEnvelope<T>(
    @NotBlank String type,
    @NotBlank String requestId,
    @NotBlank String gameId,
    @NotNull Long expectedGameVersion,
    T payload
) {}
