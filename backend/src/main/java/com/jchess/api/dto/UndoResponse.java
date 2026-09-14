package com.jchess.api.dto;

public record UndoResponse(
        boolean success,
        int remainingUndos,
        int maxUndos,
        String message,
        GameSnapshotResponse snapshot
) {}
