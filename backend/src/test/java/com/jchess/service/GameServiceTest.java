package com.jchess.service;

import com.jchess.ai.HybridChessAiService;
import com.jchess.api.dto.*;
import com.jchess.domain.exception.*;
import com.jchess.domain.model.GameEndReason;
import com.jchess.domain.model.GameMode;
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

    @Autowired
    private HybridChessAiService aiService;

    private GameService gameService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneId.of("UTC"));
        gameService = new GameService(gameRepository, gameMoveRepository, aiService, clock);
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

    @Test
    @DisplayName("[REQ-UAT-02] 5단계 난이도 설정 및 기본값(600) 생성 검증")
    void createGameWithAiLevels() {
        CreateGameRequest reqDefault = new CreateGameRequest(GameMode.PVC, null, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse resDefault = gameService.createGame(reqDefault, "player-1", "Alice");
        assertThat(resDefault.aiLevel()).isEqualTo(600);

        CreateGameRequest req1300 = new CreateGameRequest(GameMode.PVC, 1300, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse res1300 = gameService.createGame(req1300, "player-1", "Alice");
        assertThat(res1300.aiLevel()).isEqualTo(1300);
    }

    @Test
    @DisplayName("[REQ-UAT-03] 무승부 합의(agreeDraw) 및 AI 무승부 형세 평가 검증")
    void drawAgreementAndAiEvaluation() {
        CreateGameRequest request = new CreateGameRequest(GameMode.PVC, 600, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");

        // 초기 FEN(동등 형세)에서 ELO 600 AI는 무승부 수락
        boolean shouldAccept = gameService.evaluateAiDrawOffer(created.gameId(), "player-1");
        assertThat(shouldAccept).isTrue();

        GameSnapshotResponse snapshot = gameService.agreeDraw(created.gameId(), "player-1");
        assertThat(snapshot.gameStatus()).isEqualTo(GameStatus.DRAW);
        assertThat(snapshot.result()).isEqualTo(GameResult.DRAW);
        assertThat(snapshot.endReason()).isEqualTo(GameEndReason.AGREED_DRAW);
    }

    @Test
    @DisplayName("[REQ-UAT-04] 추천TIP(Hint) 요청 시 최선의 수 반환 및 3회 제한 카운트다운 검증")
    void requestHintSuccessAndQuotaExceeded() {
        CreateGameRequest request = new CreateGameRequest(GameMode.PVC, 600, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");

        // 1회차 힌트 요청 (3 -> 2)
        HintResponse hint1 = gameService.requestHint(created.gameId(), "player-1");
        assertThat(hint1.from()).isNotBlank();
        assertThat(hint1.to()).isNotBlank();
        assertThat(hint1.remainingHints()).isEqualTo(2);

        // 2회차 (2 -> 1)
        HintResponse hint2 = gameService.requestHint(created.gameId(), "player-1");
        assertThat(hint2.remainingHints()).isEqualTo(1);

        // 3회차 (1 -> 0)
        HintResponse hint3 = gameService.requestHint(created.gameId(), "player-1");
        assertThat(hint3.remainingHints()).isEqualTo(0);

        // 4회차 초과 요청 시 예외 발생 검증
        assertThatThrownBy(() -> gameService.requestHint(created.gameId(), "player-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3회의 추천TIP");
    }

    @Test
    @DisplayName("[REQ-UAT-06] 무르기(Undo) 실행 시 2수 롤백 및 AI 레벨별 차등 횟수(10회) 관리 검증")
    void undoMoveSuccess() {
        CreateGameRequest request = new CreateGameRequest(GameMode.PVC, 600, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(request, "player-1", "Alice");

        // ELO 600 초보자 난이도 -> maxUndos=10, remainingUndos=10
        GameSnapshotResponse snap0 = gameService.getGameSnapshot(created.gameId());
        assertThat(snap0.maxUndos()).isEqualTo(10);
        assertThat(snap0.remainingUndos()).isEqualTo(10);

        // 1. 백 플레이어 착수 (e2 -> e4)
        PlayMoveRequest move1 = new PlayMoveRequest("e2", "e4", null);
        gameService.playMove(created.gameId(), move1, "player-1", null, "req-1");

        // 2. 흑 AI 착수 (e7 -> e5)
        PlayMoveRequest move2 = new PlayMoveRequest("e7", "e5", null);
        gameService.playMove(created.gameId(), move2, "ai-stockfish", null, "req-2");

        // 3. 무르기 실행 -> 백 턴으로 복귀, FEN 초기화, 남은 무르기 9회
        UndoResponse undoRes = gameService.undoMove(created.gameId(), "player-1");
        assertThat(undoRes.success()).isTrue();
        assertThat(undoRes.remainingUndos()).isEqualTo(9);
        assertThat(undoRes.maxUndos()).isEqualTo(10);
        assertThat(undoRes.snapshot().turn()).isEqualTo(PieceColor.WHITE);
        assertThat(undoRes.snapshot().fen()).isEqualTo(com.jchess.domain.engine.FenParser.INITIAL_FEN);
    }


}
