package com.ticketflow.repository;

import com.ticketflow.entity.Concert;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT c FROM Concert c JOIN FETCH c.hall WHERE c.concertGenre LIKE %:genre%")
    List<Concert> findByConcertGenre(@Param("genre") String genre);


    /**
     * 예측 매진율 기반 순위 조회
     * RANK() 함수를 사용하여 동일 순위 발생 시 다음 순위를 건너뜁니다 (예: 1, 2, 2, 4)
     * Concert와 Stats를 JOIN하여 매진율 데이터를 가져옵니다.
     */
    // ConcertRepository.java
    /**
     * 중복을 방지하기 위해 DISTINCT를 사용하고,
     * Stats 데이터가 1:N이라면 가장 최신 데이터 하나만 조인하도록 수정해야 합니다.
     */
    @Query(value = "SELECT DISTINCT c, RANK() OVER (ORDER BY s.predictSoldOutRate DESC) as ranking " +
            "FROM Concert c " +
            "JOIN c.stats s " +
            "WHERE s.id = (SELECT MAX(s2.id) FROM Stats s2 WHERE s2.concert.concertId = c.concertId)")
    List<Object[]> findConcertsByRanking();

    // ConcertRepository.java
    @Query("SELECT c, RANK() OVER (ORDER BY s.reservationRate DESC) as ranking " +
            "FROM Concert c JOIN c.stats s WHERE c.concertGenre = :genre")
    List<Object[]> findConcertsByGenreRanking(@Param("genre") String genre);

    // 1. 인기 공연 조회 (Wishlist 테이블과 조인)
    @Query("SELECT c FROM Concert c JOIN Wishlist w ON c.concertId = w.concert.concertId " +
            "GROUP BY c.concertId ORDER BY COUNT(w) DESC")
    List<Concert> findTopPopular(Pageable pageable);

    // 2. 선호 장르 공연 조회
    @Query("SELECT c FROM Concert c WHERE c.concertGenre IN :genres ORDER BY c.concertStartDate ASC")
    List<Concert> findByGenreInOrderByStartDateAsc(@Param("genres") List<String> genres, Pageable pageable);

    @Query("SELECT c FROM Concert c WHERE c.concertEndDate >= CURRENT_DATE " +
            "ORDER BY c.concertWishlistCount DESC, c.concertEndDate ASC")
    List<Concert> findPopularAndUpcoming(Pageable pageable);
}