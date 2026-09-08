package com.jchess.api.dto;

import com.jchess.domain.model.GameEndReason;
import com.jchess.domain.model.GameResult;

import java.time.Instant;

public record GameEndedPayload(
        GameResult result,
        GameEndReason reason,
        String finalFen,
        Instant endedAt
) {}
