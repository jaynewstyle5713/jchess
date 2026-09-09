package com.jchess.e2e;

import com.jchess.api.dto.*;
import com.jchess.domain.model.GameEndReason;
import com.jchess.domain.model.GameMode;
import com.jchess.domain.model.GameResult;
import com.jchess.domain.model.GameStatus;
import com.jchess.domain.model.PieceColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PvpGameE2ETest {

    private WebTestClient client;

    @BeforeEach
    void setUp(ApplicationContext context) {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    @DisplayName("PVP 대국 생성 -> 참가 -> 수 교환 -> 기권까지의 전체 대국 라이프사이클 E2E 검증")
    void fullPvpGameLifecycle() {
        // 1. 대국 생성 (Alice - White)
        CreateGameRequest createRequest = new CreateGameRequest(GameMode.PVP, 2000, new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = client.post()
                .uri("/api/v1/games")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Player-Id", "user-alice")
                .header("X-Player-Name", "Alice")
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateGameResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();
        String gameId = created.gameId();
        assertThat(created.status()).isEqualTo(GameStatus.WAITING_FOR_OPPONENT);
        assertThat(created.whitePlayer().id()).isEqualTo("user-alice");
        assertThat(created.blackPlayer()).isNull();

        // 2. 상대방 참가 (Bob - Black)
        JoinGameResponse joined = client.post()
                .uri("/api/v1/games/" + gameId + "/join")
                .header("X-Player-Id", "user-bob")
                .header("X-Player-Name", "Bob")
                .exchange()
                .expectStatus().isOk()
                .expectBody(JoinGameResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(joined).isNotNull();
        assertThat(joined.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(joined.blackPlayer().id()).isEqualTo("user-bob");

        // 3. 백 착수: e2 -> e4
        PlayMoveRequest move1 = new PlayMoveRequest("e2", "e4", null);
        GameSnapshotResponse snap1 = client.post()
                .uri("/api/v1/games/" + gameId + "/moves")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Player-Id", "user-alice")
                .bodyValue(move1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GameSnapshotResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(snap1.turn()).isEqualTo(PieceColor.BLACK);
        assertThat(snap1.lastMove().from()).isEqualTo("e2");
        assertThat(snap1.lastMove().to()).isEqualTo("e4");

        // 4. 흑 착수: e7 -> e5
        PlayMoveRequest move2 = new PlayMoveRequest("e7", "e5", null);
        GameSnapshotResponse snap2 = client.post()
                .uri("/api/v1/games/" + gameId + "/moves")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Player-Id", "user-bob")
                .bodyValue(move2)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GameSnapshotResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(snap2.turn()).isEqualTo(PieceColor.WHITE);
        assertThat(snap2.lastMove().from()).isEqualTo("e7");
        assertThat(snap2.lastMove().to()).isEqualTo("e5");

        // 5. 백 기물 전개: g1 -> f3
        PlayMoveRequest move3 = new PlayMoveRequest("g1", "f3", null);
        GameSnapshotResponse snap3 = client.post()
                .uri("/api/v1/games/" + gameId + "/moves")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Player-Id", "user-alice")
                .bodyValue(move3)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GameSnapshotResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(snap3.turn()).isEqualTo(PieceColor.BLACK);
        assertThat(snap3.lastMove().from()).isEqualTo("g1");
        assertThat(snap3.lastMove().to()).isEqualTo("f3");

        // 6. 흑 기권(Resign)
        GameSnapshotResponse snapResign = client.post()
                .uri("/api/v1/games/" + gameId + "/resign")
                .header("X-Player-Id", "user-bob")
                .exchange()
                .expectStatus().isOk()
                .expectBody(GameSnapshotResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(snapResign.gameStatus()).isEqualTo(GameStatus.RESIGNED);
        assertThat(snapResign.result()).isEqualTo(GameResult.WHITE_WON);
        assertThat(snapResign.endReason()).isEqualTo(GameEndReason.RESIGNATION);
    }
}
