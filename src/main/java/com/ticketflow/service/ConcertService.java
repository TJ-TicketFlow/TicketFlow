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
     * [수정됨] 특정 날짜를 받아 해당 요일에 맞는 시간만 반환
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
                    // 1. "토요일 ~ 일요일(17:00)" 형태 처리: 요일이 포함되어 있는지 확인
                    if (time.contains("~")) {
                        // 예: "토요일 ~ 일요일" 부분을 잘라내어 해당 요일이 있는지 확인
                        String rangePart = time.split("\\(")[0]; // "토요일 ~ 일요일"
                        return rangePart.contains(dayOfWeek.substring(0, 1)); // '토', '일' 등 첫 글자 비교
                    }
                    // 2. 단일 요일 형태 처리: "금요일(20:00)"
                    return time.startsWith(dayOfWeek);
                })
                .collect(Collectors.toList());
    }
}