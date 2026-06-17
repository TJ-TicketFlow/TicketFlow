package com.ticketflow.repository;

import com.ticketflow.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    // 유저의 ID(userId)와 공연의 ID(concertId)를 기준으로 찜 여부 확인
    boolean existsByUser_UserIdAndConcert_ConcertId(String userId, String concertId);

    // 유저의 ID와 공연의 ID를 기준으로 찜 삭제
    void deleteByUser_UserIdAndConcert_ConcertId(String userId, String concertId);

    // 상세 조회 시 사용 (필요한 경우)
    Optional<Wishlist> findByUser_UserIdAndConcert_ConcertId(String userId, String concertId);

    // 1. 유저 ID로 찜 목록 전체 조회 (List 반환)
    List<Wishlist> findByUser_UserId(String userId);

    // 2. 유저 ID로 찜 개수 조회 (count 반환)
    long countByUser_UserId(String userId);
}