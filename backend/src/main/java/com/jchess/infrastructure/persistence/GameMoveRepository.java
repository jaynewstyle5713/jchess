package com.jchess.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameMoveRepository extends JpaRepository<GameMoveEntity, Long> {
    List<GameMoveEntity> findByGameIdOrderByIdAsc(String gameId);
    List<GameMoveEntity> findByGameIdOrderByMoveNumberAsc(String gameId);
    Optional<GameMoveEntity> findTopByGameIdOrderByIdDesc(String gameId);
}

