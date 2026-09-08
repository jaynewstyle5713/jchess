package com.jchess.api.dto;

import java.time.Instant;

public record EventEnvelope<T>(
    String eventId,
    String gameId,
    Long gameVersion,
    String eventType,
    Instant occurredAt,
    T payload
) {
    public static <T> EventEnvelope<T> of(String eventId, String gameId, Long gameVersion, String eventType, T payload) {
        return new EventEnvelope<>(eventId, gameId, gameVersion, eventType, Instant.now(), payload);
    }
}
