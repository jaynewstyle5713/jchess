package com.jchess.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TimeControlDto(
        @Min(1) @Max(180) int baseMinutes,
        @Min(0) @Max(60) int incrementSeconds
) {
    public static TimeControlDto standard() {
        return new TimeControlDto(10, 0);
    }
}
