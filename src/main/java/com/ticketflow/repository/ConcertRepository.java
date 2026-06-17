package com.ticketflow.repository;

import com.ticketflow.entity.Concert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, String> {

    @Query("SELECT c FROM Concert c JOIN FETCH c.hall ORDER BY c.concertStartDate ASC")
    List<Concert> findAll();

    @Query("SELECT c FROM Concert c JOIN FETCH c.hall WHERE c.concertId = :id")
    Optional<Concert> findById(@Param("id") String id);

    @Query("SELECT c FROM Concert c JOIN FETCH c.hall WHERE c.concertGenre = :genre")
    List<Concert> findByConcertGenre(@Param("genre") String genre);

    @Query("SELECT c FROM Concert c JOIN FETCH c.hall WHERE c.concertName LIKE %:keyword%")
    List<Concert> findByConcertNameContaining(@Param("keyword") String keyword);

    /**
     * 예측 매진율 기반 순위 조회
     * RANK() 함수를 사용하여 동일 순위 발생 시 다음 순위를 건너뜁니다 (예: 1, 2, 2, 4)
     * Concert와 Stats를 JOIN하여 매진율 데이터를 가져옵니다.
     */
    // ConcertRepository.java
    @Query(value = "SELECT c, RANK() OVER (ORDER BY s.predictSoldOutRate DESC) as ranking " +
            "FROM Concert c " +
            "JOIN c.stats s") // 엔티티 관계(OneToMany 등)가 설정되어 있다면 바로 조인 가능
    List<Object[]> findConcertsByRanking();

    // ConcertRepository.java
    @Query("SELECT c, RANK() OVER (ORDER BY s.reservationRate DESC) as ranking " +
            "FROM Concert c JOIN c.stats s WHERE c.concertGenre = :genre")
    List<Object[]> findConcertsByGenreRanking(@Param("genre") String genre);
}