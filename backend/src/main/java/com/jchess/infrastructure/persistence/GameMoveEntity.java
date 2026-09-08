package com.jchess.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "game_moves", indexes = {
        @Index(name = "idx_game_moves_game_id", columnList = "gameId")
})
public class GameMoveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String gameId;

    @Column(nullable = false)
    private int moveNumber;

    @Column(nullable = false, length = 16)
    private String playerColor;

    @Column(nullable = false, length = 4)
    private String fromSquare;

    @Column(nullable = false, length = 4)
    private String toSquare;

    @Column(length = 16)
    private String promotionPiece;

    @Column(length = 32)
    private String moveNotation;

    @Column(nullable = false, length = 128)
    private String fenAfterMove;

    @Column(nullable = false)
    private Instant playedAt;

    public GameMoveEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public int getMoveNumber() { return moveNumber; }
    public void setMoveNumber(int moveNumber) { this.moveNumber = moveNumber; }

    public String getPlayerColor() { return playerColor; }
    public void setPlayerColor(String playerColor) { this.playerColor = playerColor; }

    public String getFromSquare() { return fromSquare; }
    public void setFromSquare(String fromSquare) { this.fromSquare = fromSquare; }

    public String getToSquare() { return toSquare; }
    public void setToSquare(String toSquare) { this.toSquare = toSquare; }

    public String getPromotionPiece() { return promotionPiece; }
    public void setPromotionPiece(String promotionPiece) { this.promotionPiece = promotionPiece; }

    public String getMoveNotation() { return moveNotation; }
    public void setMoveNotation(String moveNotation) { this.moveNotation = moveNotation; }

    public String getFenAfterMove() { return fenAfterMove; }
    public void setFenAfterMove(String fenAfterMove) { this.fenAfterMove = fenAfterMove; }

    public Instant getPlayedAt() { return playedAt; }
    public void setPlayedAt(Instant playedAt) { this.playedAt = playedAt; }
}
