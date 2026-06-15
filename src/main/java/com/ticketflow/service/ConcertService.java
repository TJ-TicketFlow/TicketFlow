package com.ticketflow.service;

import com.ticketflow.entity.Concert;
import com.ticketflow.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 데이터를 읽기만 하므로 성능 최적화
public class ConcertService {

    private final ConcertRepository concertRepository;

    // 모든 공연을 조회 (JOIN FETCH가 적용된 Repository 메서드 호출)
    public List<Concert> getAllConcerts() {
        return concertRepository.findAll();
    }

    // 장르별 조회
    public List<Concert> getConcertsByGenre(String genre) {
        return concertRepository.findByConcertGenre(genre);
    }

    public Concert findById(String id) {
        return concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공연을 찾을 수 없습니다. ID: " + id));
    }
}