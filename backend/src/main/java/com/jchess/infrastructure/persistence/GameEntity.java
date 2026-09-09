package com.jchess.infrastructure.persistence;

import com.jchess.domain.model.GameEndReason;
import com.jchess.domain.model.GameMode;
import com.jchess.domain.model.GameResult;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceColor;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Version
    @Column(nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GameStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private GameMode gameMode = GameMode.PVP;

    @Column
    private Integer aiLevel = 2000;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PieceColor currentTurn;

    @Column(nullable = false, length = 128)
    private String currentFen;

    @Column(length = 64)
    private String whitePlayerId;

    @Column(length = 64)
    private String whitePlayerName;

    @Column(length = 64)
    private String blackPlayerId;

    @Column(length = 64)
    private String blackPlayerName;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private GameResult result;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private GameEndReason endReason;

    @Column(nullable = false)
    private int baseMinutes;

    @Column(nullable = false)
    private int incrementSeconds;

    @Column(nullable = false)
    private long whiteRemainingTimeMs;

    @Column(nullable = false)
    private long blackRemainingTimeMs;

    @Column
    private Instant lastMoveAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public GameEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public GameMode getGameMode() { return gameMode; }
    public void setGameMode(GameMode gameMode) { this.gameMode = gameMode; }

    public Integer getAiLevel() { return aiLevel; }
    public void setAiLevel(Integer aiLevel) { this.aiLevel = aiLevel; }

    public PieceColor getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(PieceColor currentTurn) { this.currentTurn = currentTurn; }

    public String getCurrentFen() { return currentFen; }
    public void setCurrentFen(String currentFen) { this.currentFen = currentFen; }

    public String getWhitePlayerId() { return whitePlayerId; }
    public void setWhitePlayerId(String whitePlayerId) { this.whitePlayerId = whitePlayerId; }

    public String getWhitePlayerName() { return whitePlayerName; }
    public void setWhitePlayerName(String whitePlayerName) { this.whitePlayerName = whitePlayerName; }

    public String getBlackPlayerId() { return blackPlayerId; }
    public void setBlackPlayerId(String blackPlayerId) { this.blackPlayerId = blackPlayerId; }

    public String getBlackPlayerName() { return blackPlayerName; }
    public void setBlackPlayerName(String blackPlayerName) { this.blackPlayerName = blackPlayerName; }

    public GameResult getResult() { return result; }
    public void setResult(GameResult result) { this.result = result; }

    public GameEndReason getEndReason() { return endReason; }
    public void setEndReason(GameEndReason endReason) { this.endReason = endReason; }

    public int getBaseMinutes() { return baseMinutes; }
    public void setBaseMinutes(int baseMinutes) { this.baseMinutes = baseMinutes; }

    public int getIncrementSeconds() { return incrementSeconds; }
    public void setIncrementSeconds(int incrementSeconds) { this.incrementSeconds = incrementSeconds; }

    public long getWhiteRemainingTimeMs() { return whiteRemainingTimeMs; }
    public void setWhiteRemainingTimeMs(long whiteRemainingTimeMs) { this.whiteRemainingTimeMs = whiteRemainingTimeMs; }

    public long getBlackRemainingTimeMs() { return blackRemainingTimeMs; }
    public void setBlackRemainingTimeMs(long blackRemainingTimeMs) { this.blackRemainingTimeMs = blackRemainingTimeMs; }

    public Instant getLastMoveAt() { return lastMoveAt; }
    public void setLastMoveAt(Instant lastMoveAt) { this.lastMoveAt = lastMoveAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
