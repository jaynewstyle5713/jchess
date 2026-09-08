package com.jchess.service;

import com.jchess.api.dto.CreateGameRequest;
import com.jchess.api.dto.CreateGameResponse;
import com.jchess.api.dto.GameSnapshotResponse;
import com.jchess.api.dto.PlayMoveRequest;
import com.jchess.api.dto.TimeControlDto;
import com.jchess.domain.exception.ChessException;
import com.jchess.domain.exception.GameVersionConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GameConcurrencyTest {

    @Autowired
    private GameService gameService;

    @Test
    @DisplayName("동일한 버전에 대해 동시에 두 개의 수 요청이 올 경우 하나만 성공하고 다른 하나는 버전 충돌로 거부된다.")
    void concurrentMoveRequests() throws InterruptedException {
        CreateGameRequest createReq = new CreateGameRequest(new TimeControlDto(10, 0), "WHITE");
        CreateGameResponse created = gameService.createGame(createReq, "user-white", "Alice");
        gameService.joinGame(created.gameId(), "user-black", "Bob");

        GameSnapshotResponse snapshot = gameService.getGameSnapshot(created.gameId());
        long currentVersion = snapshot.gameVersion();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        PlayMoveRequest move1 = new PlayMoveRequest("e2", "e4", null);
        PlayMoveRequest move2 = new PlayMoveRequest("e2", "e3", null);

        executor.submit(() -> {
            try {
                latch.await();
                gameService.playMove(created.gameId(), move1, "user-white", currentVersion, "req-1");
                successCount.incrementAndGet();
            } catch (GameVersionConflictException | org.springframework.dao.OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                System.out.println("Thread 1 unexpected exception: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                latch.await();
                gameService.playMove(created.gameId(), move2, "user-white", currentVersion, "req-2");
                successCount.incrementAndGet();
            } catch (GameVersionConflictException | org.springframework.dao.OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                System.out.println("Thread 2 unexpected exception: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        latch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }
}
