package com.jchess.api.controller;

import com.jchess.api.dto.*;
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
class GameControllerTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp(ApplicationContext context) {
        webTestClient = WebTestClient.bindToApplicationContext(context).build();
    }

    @Test
    @DisplayName("POST /api/v1/games 대국 생성 API가 201 Created를 반환한다.")
    void createGameEndpoint() {
        CreateGameRequest request = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");

        CreateGameResponse response = webTestClient.post()
                .uri("/api/v1/games")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Player-Id", "user-1")
                .header("X-Player-Name", "Alice")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateGameResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.gameId()).isNotNull();
        assertThat(response.status()).isEqualTo(GameStatus.WAITING_FOR_OPPONENT);
        assertThat(response.whitePlayer().id()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("대국 생성 -> 참가 -> 수 진행 -> 조회 전체 REST 흐름 검증")
    void fullGameRestFlow() {
        CreateGameResponse created = webTestClient.post()
                .uri("/api/v1/games")
                .header("X-Player-Id", "user-white")
                .header("X-Player-Name", "Alice")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateGameResponse.class)
                .returnResult()
                .getResponseBody();

        String gameId = created.gameId();

        JoinGameResponse joined = webTestClient.post()
                .uri("/api/v1/games/" + gameId + "/join")
                .header("X-Player-Id", "user-black")
                .header("X-Player-Name", "Bob")
                .exchange()
                .expectStatus().isOk()
                .expectBody(JoinGameResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(joined.status()).isEqualTo(GameStatus.ACTIVE);

        PlayMoveRequest move1 = new PlayMoveRequest("e2", "e4", null);
        GameSnapshotResponse moveResult = webTestClient.post()
                .uri("/api/v1/games/" + gameId + "/moves")
                .header("X-Player-Id", "user-white")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(move1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GameSnapshotResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(moveResult.turn()).isEqualTo(PieceColor.BLACK);
        assertThat(moveResult.lastMove().from()).isEqualTo("e2");
        assertThat(moveResult.lastMove().to()).isEqualTo("e4");

        GameSnapshotResponse snapshot = webTestClient.get()
                .uri("/api/v1/games/" + gameId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GameSnapshotResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(snapshot.gameId()).isEqualTo(gameId);
        assertThat(snapshot.gameStatus()).isEqualTo(GameStatus.ACTIVE);
        assertThat(snapshot.turn()).isEqualTo(PieceColor.BLACK);
    }

    @Test
    @DisplayName("존재하지 않는 게임 조회 시 404 Not Found와 표준 ApiErrorResponse를 반환한다.")
    void gameNotFoundReturns404() {
        ApiErrorResponse error = webTestClient.get()
                .uri("/api/v1/games/non-existent-game-id")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(error).isNotNull();
        assertThat(error.code()).isEqualTo("GAME_NOT_FOUND");
        assertThat(error.message()).contains("대국을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("차례가 아닐 때 수 요청 시 400 Bad Request와 NOT_YOUR_TURN 에러 코드를 반환한다.")
    void notYourTurnReturns400() {
        CreateGameResponse created = webTestClient.post()
                .uri("/api/v1/games")
                .header("X-Player-Id", "user-white")
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreateGameResponse.class)
                .returnResult()
                .getResponseBody();

        webTestClient.post()
                .uri("/api/v1/games/" + created.gameId() + "/join")
                .header("X-Player-Id", "user-black")
                .exchange()
                .expectStatus().isOk();

        PlayMoveRequest move = new PlayMoveRequest("e7", "e5", null);
        ApiErrorResponse error = webTestClient.post()
                .uri("/api/v1/games/" + created.gameId() + "/moves")
                .header("X-Player-Id", "user-black")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(move)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ApiErrorResponse.class)
                .returnResult()
                .getResponseBody();

        assertThat(error).isNotNull();
        assertThat(error.code()).isEqualTo("NOT_YOUR_TURN");
    }
}


