package com.ticketflow.service;

import com.ticketflow.entity.Concert;
import com.ticketflow.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;

    public List<Concert> getAllConcerts() {
        return concertRepository.findAll();
    }

    public List<Concert> getConcertsByGenre(String genre) {
        return concertRepository.findByConcertGenre(genre);
    }

    public Concert findById(String id) {
        return concertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 공연을 찾을 수 없습니다."));
    }

    /**
     * [랭킹 페이지용] 예측 매진율 기반 랭킹 조회
     */
    public List<Map<String, Object>> getRankedConcerts() {
        List<Object[]> results = concertRepository.findConcertsByRanking();

        return results.stream().map(obj -> {
            Map<String, Object> map = new HashMap<>();

            // 데이터 순서: obj[0] = Concert 객체, obj[1] = ranking (Integer)
            Concert concert = (Concert) obj[0];

            // Integer를 캐스팅할 때 발생할 수 있는 오류 방지
            int ranking = ((Number) obj[1]).intValue();

            map.put("concert", concert);
            map.put("ranking", ranking);
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * [기존 유지] 특정 날짜를 받아 해당 요일에 맞는 시간만 반환
     */
    public List<String> findSessionsByDate(String id, String selectedDate) {
        Concert concert = findById(id);
        String allTimes = concert.getConcertTime(); // "금요일(20:00), 토요일 ~ 일요일(17:00)"

        if (allTimes == null || allTimes.isEmpty()) return Collections.emptyList();

        LocalDate date = LocalDate.parse(selectedDate);
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        return Arrays.stream(allTimes.split(","))
                .map(String::trim)
                .filter(time -> {
                    // 1. "토요일 ~ 일요일(17:00)" 형태 처리
                    if (time.contains("~")) {
                        String rangePart = time.split("\\(")[0];
                        return rangePart.contains(dayOfWeek.substring(0, 1));
                    }
                    // 2. 단일 요일 형태 처리: "금요일(20:00)"
                    return time.startsWith(dayOfWeek);
                })
                .collect(Collectors.toList());
    }
}