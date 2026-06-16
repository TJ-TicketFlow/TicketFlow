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
    Optional<Concert> findById(@Param("id") Long id);

    @Query("SELECT c FROM Concert c JOIN FETCH c.hall WHERE c.concertGenre = :genre")
    List<Concert> findByConcertGenre(@Param("genre") String genre);

    @Query("SELECT c FROM Concert c JOIN FETCH c.hall WHERE c.concertName LIKE %:keyword%")
    List<Concert> findByConcertNameContaining(@Param("keyword") String keyword);
}