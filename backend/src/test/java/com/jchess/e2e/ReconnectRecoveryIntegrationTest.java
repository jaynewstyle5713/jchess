package com.jchess.e2e;

import com.jchess.api.dto.*;
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
class ReconnectRecoveryIntegrationTest {

    private WebTestClient client;

    @BeforeEach
    void setUp(ApplicationContext context) {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    @DisplayName("대국 도중 연결이 끊어졌다가 재접속 시 최신 게임 상태가 완벽히 복원되고 다음 수가 정상 처리된다")
    void reconnectRestoresFullGameState() {
        // 1. 대국 생성 및 참가
        CreateGameResponse created = client.post().uri("/api/v1/games")
                .header("X-Player-Id", "p-white").header("X-Player-Name", "WhitePlayer")
                .exchange().expectStatus().isCreated().expectBody(CreateGameResponse.class).returnResult().getResponseBody();

        String gameId = created.gameId();
        client.post().uri("/api/v1/games/" + gameId + "/join")
                .header("X-Player-Id", "p-black").header("X-Player-Name", "BlackPlayer")
                .exchange().expectStatus().isOk();

        // 2. 백 착수: d2 -> d4
        client.post().uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", "p-white")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PlayMoveRequest("d2", "d4", null))
                .exchange().expectStatus().isOk();

        // 3. 재접속(Reconnect): GET /api/v1/games/{gameId}
        GameSnapshotResponse snapshot = client.get().uri("/api/v1/games/" + gameId)
                .exchange().expectStatus().isOk()
                .expectBody(GameSnapshotResponse.class)
                .returnResult().getResponseBody();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.gameId()).isEqualTo(gameId);
        assertThat(snapshot.turn()).isEqualTo(PieceColor.BLACK);
        assertThat(snapshot.fen()).contains("3P4"); // d4 폰 전진 (3칸 빈칸 + P + 4칸 빈칸)
        assertThat(snapshot.lastMove().from()).isEqualTo("d2");
        assertThat(snapshot.lastMove().to()).isEqualTo("d4");

        // 4. 재접속 상태에서 흑이 다음 수 착수: d7 -> d5
        GameSnapshotResponse nextSnap = client.post().uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", "p-black")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PlayMoveRequest("d7", "d5", null))
                .exchange().expectStatus().isOk()
                .expectBody(GameSnapshotResponse.class)
                .returnResult().getResponseBody();

        assertThat(nextSnap.turn()).isEqualTo(PieceColor.WHITE);
        assertThat(nextSnap.lastMove().from()).isEqualTo("d7");
        assertThat(nextSnap.lastMove().to()).isEqualTo("d5");
    }
}
