package com.jchess.service;

import com.jchess.api.dto.*;
import com.jchess.domain.exception.*;
import com.jchess.domain.model.GameEndReason;
import com.jchess.domain.model.GameResult;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceColor;
import com.jchess.infrastructure.persistence.GameMoveRepository;
import com.jchess.infrastructure.persistence.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GameServiceTest {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private GameMoveRepository gameMoveRepository;

    private GameService gameService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneId.of("UTC"));
        gameService = new GameService(gameRepository, gameMoveRepository, clock);
    }

    @Test
    @DisplayName("게임 생성 시 WAITING_FOR_OPPONENT 상태와 백 플레이어가 설정된다.")
    void createGameSuccess() {
        CreateGameRequest request = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse response = gameService.createGame(request, "player-1", "Alice");

        assertThat(response.gameId()).isNotNull();
        assertThat(response.status()).isEqualTo(GameStatus.WAITING_FOR_OPPONENT);
        assertThat(response.whitePlayer().id()).isEqualTo("player-1");
        assertThat(response.whitePlayer().name()).isEqualTo("Alice");
        assertThat(response.blackPlayer()).isNull();
    }

    @Test
    @DisplayName("상대방이 게임에 참가하면 ACTIVE 상태로 전이된다.")
    void joinGameSuccess() {
        CreateGameRequest request = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");

        JoinGameResponse joined = gameService.joinGame(created.gameId(), "player-2", "Bob");

        assertThat(joined.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(joined.whitePlayer().id()).isEqualTo("player-1");
        assertThat(joined.blackPlayer().id()).isEqualTo("player-2");
    }

    @Test
    @DisplayName("정상적인 수(e2 -> e4)를 두면 차례가 흑으로 넘어가고 FEN 및 기보가 저장된다.")
    void playValidMove() {
        CreateGameRequest request = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");
        gameService.joinGame(created.gameId(), "player-2", "Bob");

        PlayMoveRequest moveRequest = new PlayMoveRequest("e2", "e4", null);
        GameSnapshotResponse snapshot = gameService.playMove(created.gameId(), moveRequest, "player-1", null, "req-1");

        assertThat(snapshot.turn()).isEqualTo(PieceColor.BLACK);
        assertThat(snapshot.fen()).contains("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        assertThat(snapshot.lastMove().from()).isEqualTo("e2");
        assertThat(snapshot.lastMove().to()).isEqualTo("e4");
        assertThat(gameMoveRepository.findByGameIdOrderByMoveNumberAsc(created.gameId())).hasSize(1);
    }

    @Test
    @DisplayName("자신의 차례가 아닐 때 수를 두면 NotYourTurnException이 발생한다.")
    void notYourTurnThrowsException() {
        CreateGameRequest request = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");
        gameService.joinGame(created.gameId(), "player-2", "Bob");

        PlayMoveRequest moveRequest = new PlayMoveRequest("e7", "e5", null);
        assertThatThrownBy(() -> gameService.playMove(created.gameId(), moveRequest, "player-2", null, "req-1"))
                .isInstanceOf(NotYourTurnException.class);
    }

    @Test
    @DisplayName("불법 수를 두면 IllegalMoveException이 발생한다.")
    void illegalMoveThrowsException() {
        CreateGameRequest request = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");
        gameService.joinGame(created.gameId(), "player-2", "Bob");

        PlayMoveRequest moveRequest = new PlayMoveRequest("e2", "e5", null);
        assertThatThrownBy(() -> gameService.playMove(created.gameId(), moveRequest, "player-1", null, "req-1"))
                .isInstanceOf(IllegalMoveException.class);
    }

    @Test
    @DisplayName("참가자가 아닌 제3자가 수를 두면 UnauthorizedPlayerException이 발생한다.")
    void thirdPartyCannotPlayMove() {
        CreateGameRequest request = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");
        gameService.joinGame(created.gameId(), "player-2", "Bob");

        PlayMoveRequest moveRequest = new PlayMoveRequest("e2", "e4", null);
        assertThatThrownBy(() -> gameService.playMove(created.gameId(), moveRequest, "player-3", null, "req-1"))
                .isInstanceOf(UnauthorizedPlayerException.class);
    }

    @Test
    @DisplayName("기권(Resign) 시 상대방의 승리로 게임이 종료된다.")
    void resignGame() {
        CreateGameRequest request = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");
        gameService.joinGame(created.gameId(), "player-2", "Bob");

        GameSnapshotResponse snapshot = gameService.resign(created.gameId(), "player-1");

        assertThat(snapshot.gameStatus()).isEqualTo(GameStatus.RESIGNED);
        assertThat(snapshot.result()).isEqualTo(GameResult.BLACK_WON);
        assertThat(snapshot.endReason()).isEqualTo(GameEndReason.RESIGNATION);
    }
}
