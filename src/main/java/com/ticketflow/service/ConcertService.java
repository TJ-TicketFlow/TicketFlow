package com.ticketflow.service;

import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Stats; // Stats 엔티티 import 확인 필요
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
            Concert concert = (Concert) obj[0];
            int ranking = ((Number) obj[1]).intValue();
            map.put("concert", concert);
            map.put("ranking", ranking);
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * [랭킹 페이지용] 특정 장르의 예매율 기반 랭킹 조회
     */
    public List<Map<String, Object>> getRankedConcertsByGenre(String genre) {
        List<Object[]> results = concertRepository.findConcertsByGenreRanking(genre);

        return results.stream().map(obj -> {
            Map<String, Object> map = new HashMap<>();
            Concert concert = (Concert) obj[0];
            int ranking = ((Number) obj[1]).intValue();
            map.put("concert", concert);
            map.put("ranking", ranking);
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * [상세페이지용] 예매자 통계 데이터 가공
     */
    public Map<String, List<?>> getStatsData(String concertId) {
        Concert concert = findById(concertId);

        // stats가 비어있거나 null이면 차트를 띄우지 않기 위해 null 반환
        if (concert.getStats() == null || concert.getStats().isEmpty()) {
            return null;
        }

        // Stats 엔티티의 필드명에 맞게 정확한 getter 호출
        Stats stats = concert.getStats().get(0);

        Map<String, List<?>> data = new HashMap<>();

        // 성별 데이터 [남성%, 여성%]
        data.put("genderData", Arrays.asList(stats.getMaleRatio(), stats.getFemaleRatio()));

        // 연령 데이터 [10대, 20대, 30대, 40대, 50대]
        data.put("ageData", Arrays.asList(
                stats.getAge10sRatio(),
                stats.getAge20sRatio(),
                stats.getAge30sRatio(),
                stats.getAge40sRatio(),
                stats.getAge50sRatio()
        ));

        return data;
    }

    /**
     * [기존 유지] 특정 날짜를 받아 해당 요일에 맞는 시간만 반환
     */
    public List<String> findSessionsByDate(String id, String selectedDate) {
        Concert concert = findById(id);
        String allTimes = concert.getConcertTime();

        if (allTimes == null || allTimes.isEmpty()) return Collections.emptyList();

        LocalDate date = LocalDate.parse(selectedDate);
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        return Arrays.stream(allTimes.split(","))
                .map(String::trim)
                .filter(time -> {
                    if (time.contains("~")) {
                        String rangePart = time.split("\\(")[0];
                        return rangePart.contains(dayOfWeek.substring(0, 1));
                    }
                    return time.startsWith(dayOfWeek);
                })
                .collect(Collectors.toList());
    }

    /**
     * 진행 예정 공연 조회 (오늘 포함 이후)
     */
    public List<Concert> getUpcomingConcerts() {
        LocalDate today = LocalDate.now();
        return getAllConcerts().stream()
                .filter(c -> !c.getConcertEndDate().isBefore(today))
                .collect(Collectors.toList());
    }

    /**
     * 지난 공연 조회 (오늘 이전)
     */
    public List<Concert> getPastConcerts() {
        LocalDate today = LocalDate.now();
        return getAllConcerts().stream()
                .filter(c -> c.getConcertEndDate().isBefore(today))
                .collect(Collectors.toList());
    }
}