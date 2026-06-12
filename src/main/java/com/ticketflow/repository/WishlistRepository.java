package com.ticketflow.repository;

import com.ticketflow.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUser_UserId(String userId);
    Optional<Wishlist> findByUser_UserIdAndConcert_ConcertId(String userId, Long concertId);
    boolean existsByUser_UserIdAndConcert_ConcertId(String userId, Long concertId);
}
