package com.jchess.security;

import com.jchess.api.dto.CreateGameResponse;
import com.jchess.api.dto.PlayMoveRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityAuthorizationTest {

    private WebTestClient client;

    @BeforeEach
    void setUp(ApplicationContext context) {
        client = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    @DisplayName("대국 참가자가 아닌 제3자가 수 요청을 보내면 403 Forbidden으로 거부된다")
    void unauthorizedPlayerCannotMakeMove() {
        CreateGameResponse created = client.post().uri("/api/v1/games")
                .header("X-Player-Id", "user-alice").header("X-Player-Name", "Alice")
                .exchange().expectStatus().isCreated().expectBody(CreateGameResponse.class).returnResult().getResponseBody();

        String gameId = created.gameId();
        client.post().uri("/api/v1/games/" + gameId + "/join")
                .header("X-Player-Id", "user-bob").header("X-Player-Name", "Bob")
                .exchange().expectStatus().isOk();

        // 제3자(hacker-charlie)가 수 조작 시도
        PlayMoveRequest move = new PlayMoveRequest("e2", "e4", null);
        client.post().uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", "hacker-charlie")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(move)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("자신의 차례가 아닐 때 착수를 시도하면 400 Bad Request로 거부된다")
    void notYourTurnMoveIsRejected() {
        CreateGameResponse created = client.post().uri("/api/v1/games")
                .header("X-Player-Id", "user-alice").header("X-Player-Name", "Alice")
                .exchange().expectStatus().isCreated().expectBody(CreateGameResponse.class).returnResult().getResponseBody();

        String gameId = created.gameId();
        client.post().uri("/api/v1/games/" + gameId + "/join")
                .header("X-Player-Id", "user-bob").header("X-Player-Name", "Bob")
                .exchange().expectStatus().isOk();

        // 흑(Bob)이 백 차례일 때 먼저 두려고 시도
        PlayMoveRequest move = new PlayMoveRequest("e7", "e5", null);
        client.post().uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", "user-bob")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(move)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("이미 기권으로 종료된 대국에 대해 추가 착수를 시도하면 400 Bad Request로 거부된다")
    void cannotMoveInFinishedGame() {
        CreateGameResponse created = client.post().uri("/api/v1/games")
                .header("X-Player-Id", "user-alice").header("X-Player-Name", "Alice")
                .exchange().expectStatus().isCreated().expectBody(CreateGameResponse.class).returnResult().getResponseBody();

        String gameId = created.gameId();
        client.post().uri("/api/v1/games/" + gameId + "/join")
                .header("X-Player-Id", "user-bob").header("X-Player-Name", "Bob")
                .exchange().expectStatus().isOk();

        // 백 기권
        client.post().uri("/api/v1/games/" + gameId + "/resign")
                .header("X-Player-Id", "user-alice")
                .exchange().expectStatus().isOk();

        // 종료 후 수 착수 시도
        PlayMoveRequest move = new PlayMoveRequest("e7", "e5", null);
        client.post().uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", "user-bob")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(move)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
