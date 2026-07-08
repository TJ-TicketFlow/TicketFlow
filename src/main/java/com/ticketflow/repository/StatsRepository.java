package com.ticketflow.repository;

import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Stats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatsRepository extends JpaRepository<Stats, Long> {

    //공연 객체를 기준으로 가장 최근에 생성된(statsId가 가장 큰) 통계 1건만 조회합니다.
    Optional<Stats> findTopByConcertOrderByStatsIdDesc(Concert concert);
}