package com.jchess.e2e;

import com.jchess.api.dto.*;
import com.jchess.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FinalAcceptanceIntegrationTest {

    private WebTestClient client;

    @Autowired
    private com.jchess.ai.AsyncAiMoveExecutor aiMoveExecutor;

    @BeforeEach
    void setUp(ApplicationContext context) {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    @DisplayName("[인수 1] 시스템 상태(Actuator Health) 점검 - 헬스체크 UP 확인")
    void scenario1_healthCheck() {
        client.get().uri("/actuator/health").exchange()
                .expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("[인수 2] PVP 2인 대국 (생성 -> 참가 -> Scholar's Mate 4수 체크메이트 종료)")
    void scenario2_pvpCheckmateFlow() {
        CreateGameRequest createReq = new CreateGameRequest(GameMode.PVP, 2000, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = client.post().uri("/api/v1/games")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Player-Id", "user-alice").bodyValue(createReq)
                .exchange().expectStatus().isCreated().expectBody(CreateGameResponse.class).returnResult().getResponseBody();

        String gameId = created.gameId();
        client.post().uri("/api/v1/games/" + gameId + "/join")
                .header("X-Player-Id", "user-bob").exchange().expectStatus().isOk();

        // 1. e4 e5, 2. Bc4 Nc6, 3. Qh5 Nf6, 4. Qxf7# (Scholar's Mate)
        playMove(gameId, "user-alice", "e2", "e4");
        playMove(gameId, "user-bob", "e7", "e5");
        playMove(gameId, "user-alice", "f1", "c4");
        playMove(gameId, "user-bob", "b8", "c6");
        playMove(gameId, "user-alice", "d1", "h5");
        playMove(gameId, "user-bob", "g8", "f6");
        GameSnapshotResponse finalSnap = playMove(gameId, "user-alice", "h5", "f7");

        assertThat(finalSnap.gameStatus()).isEqualTo(GameStatus.CHECKMATE);
        assertThat(finalSnap.result()).isEqualTo(GameResult.WHITE_WON);
        assertThat(finalSnap.endReason()).isEqualTo(GameEndReason.CHECKMATE);
        assertThat(finalSnap.isCheck()).isTrue();
    }

    @Test
    @DisplayName("[인수 3] PVC AI 대국 (생성 즉시 ACTIVE -> 플레이어 착수 -> 비동기 AI 자동 응수)")
    void scenario3_pvcAiMoveFlow() {
        CreateGameRequest createReq = new CreateGameRequest(GameMode.PVC, 2000, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = client.post().uri("/api/v1/games")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Player-Id", "user-alice").bodyValue(createReq)
                .exchange().expectStatus().isCreated().expectBody(CreateGameResponse.class).returnResult().getResponseBody();

        String gameId = created.gameId();
        assertThat(created.status()).isEqualTo(GameStatus.ACTIVE);

        GameSnapshotResponse snap = playMove(gameId, "user-alice", "e2", "e4");
        assertThat(snap.turn()).isEqualTo(PieceColor.BLACK);

        aiMoveExecutor.triggerAiMoveIfApplicable(snap);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            GameSnapshotResponse latest = client.get().uri("/api/v1/games/" + gameId)
                    .exchange().expectStatus().isOk().expectBody(GameSnapshotResponse.class).returnResult().getResponseBody();
            assertThat(latest.turn()).isEqualTo(PieceColor.WHITE);
            assertThat(latest.lastMove()).isNotNull();
            assertThat(latest.lastMove().from()).isNotEqualTo("e2");
        });
    }

    @Test
    @DisplayName("[인수 4] 보안 및 불법 수 차단 검증 (제3자 침입 403 & 불법 수 400 차단)")
    void scenario4_securityAndIllegalMoveRejection() {
        CreateGameRequest createReq = new CreateGameRequest(GameMode.PVP, 2000, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = client.post().uri("/api/v1/games")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Player-Id", "user-alice").bodyValue(createReq)
                .exchange().expectStatus().isCreated().expectBody(CreateGameResponse.class).returnResult().getResponseBody();

        String gameId = created.gameId();
        client.post().uri("/api/v1/games/" + gameId + "/join").header("X-Player-Id", "user-bob").exchange().expectStatus().isOk();

        // 1. 비인가 사용자 접근 차단 (403)
        client.post().uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", "intruder-charlie").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PlayMoveRequest("e2", "e4", null)).exchange().expectStatus().isForbidden();

        // 2. 폰 3칸 이동 불법 수 차단 (400)
        client.post().uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", "user-alice").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PlayMoveRequest("e2", "e5", null)).exchange().expectStatus().isBadRequest();
    }

    private GameSnapshotResponse playMove(String gameId, String playerId, String from, String to) {
        return client.post().uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", playerId).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PlayMoveRequest(from, to, null))
                .exchange().expectStatus().isOk().expectBody(GameSnapshotResponse.class).returnResult().getResponseBody();
    }
}
