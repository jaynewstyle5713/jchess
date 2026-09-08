package com.jchess.api.dto;

import com.jchess.domain.model.PieceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PlayMoveRequest(
        @NotBlank @Pattern(regexp = "^[a-h][1-8]$") String from,
        @NotBlank @Pattern(regexp = "^[a-h][1-8]$") String to,
        PieceType promotion
) {}
