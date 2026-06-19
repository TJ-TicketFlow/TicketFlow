package com.ticketflow.repository;

import com.ticketflow.entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StatsRepository extends JpaRepository<Stats, Long> {
    // 특정 공연의 통계 데이터를 조회하기 위한 메서드
    Optional<Stats> findByConcert_ConcertId(String concertId);
}