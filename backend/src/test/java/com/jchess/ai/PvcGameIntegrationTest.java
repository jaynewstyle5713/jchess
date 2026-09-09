package com.jchess.ai;

import com.jchess.api.dto.CreateGameRequest;
import com.jchess.api.dto.CreateGameResponse;
import com.jchess.api.dto.GameSnapshotResponse;
import com.jchess.api.dto.PlayMoveRequest;
import com.jchess.domain.model.GameMode;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceColor;
import com.jchess.service.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class PvcGameIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private AsyncAiMoveExecutor aiMoveExecutor;

    @Test
    @DisplayName("PVC 대국 생성 시 즉시 ACTIVE 상태가 되고 상대방이 Stockfish AI로 설정된다")
    void createPvcGameStartsImmediately() {
        CreateGameRequest request = new CreateGameRequest(GameMode.PVC, 2000, null, "WHITE");
        CreateGameResponse response = gameService.createGame(request, "user-alice", "Alice");

        assertThat(response.gameId()).isNotNull();
        assertThat(response.gameMode()).isEqualTo(GameMode.PVC);
        assertThat(response.aiLevel()).isEqualTo(2000);
        assertThat(response.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(response.whitePlayer().id()).isEqualTo("user-alice");
        assertThat(response.blackPlayer().id()).isEqualTo("ai-stockfish");
        assertThat(response.blackPlayer().name()).contains("Stockfish AI");
    }

    @Test
    @DisplayName("플레이어가 착수하면 AsyncAiMoveExecutor가 AI 차례를 감지하여 합법적인 AI 수를 자동 확정한다")
    void playerMoveTriggersAsyncAiMove() {
        CreateGameRequest request = new CreateGameRequest(GameMode.PVC, 2000, null, "WHITE");
        CreateGameResponse created = gameService.createGame(request, "user-alice", "Alice");
        String gameId = created.gameId();

        // 1. 백(플레이어) 착수: e2 -> e4
        PlayMoveRequest moveRequest = new PlayMoveRequest("e2", "e4", null);
        GameSnapshotResponse playerSnapshot = gameService.playMove(gameId, moveRequest, "user-alice", 0L, "req-1");

        assertThat(playerSnapshot.turn()).isEqualTo(PieceColor.BLACK);
        assertThat(playerSnapshot.lastMove().from()).isEqualTo("e2");
        assertThat(playerSnapshot.lastMove().to()).isEqualTo("e4");

        // 2. 비동기 AI 착수 트리거
        aiMoveExecutor.triggerAiMoveIfApplicable(playerSnapshot);

        // 3. 비동기 AI가 흑 수를 착수하여 턴이 다시 WHITE로 넘어오는지 대기 및 검증 (최대 5초)
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            GameSnapshotResponse latest = gameService.getGameSnapshot(gameId);
            assertThat(latest.turn()).isEqualTo(PieceColor.WHITE);
            assertThat(latest.gameVersion()).isGreaterThanOrEqualTo(2L);
            assertThat(latest.lastMove()).isNotNull();
            assertThat(latest.lastMove().from()).isNotEqualTo("e2");
        });
    }
}
