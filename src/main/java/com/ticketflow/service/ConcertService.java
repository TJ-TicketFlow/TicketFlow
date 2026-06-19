package com.ticketflow.service;

import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
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
    private final WishlistRepository wishlistRepository; // 의존성 주입 확인
    private final UserRepository userRepository;         // User 엔티티 조회를 위해 추가
    private final SelectedSeatRepository selectedSeatRepository;
    private final SeatRepository seatRepository;

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

    // [위시리스트] 찜 토글 로직
    @Transactional
    public boolean toggleWishlist(String concertId, String userId) {
        // Repository 호출 시 변수명 소문자 wishlistRepository 사용
        boolean isAlreadyLiked = wishlistRepository.existsByUser_UserIdAndConcert_ConcertId(userId, concertId);

        if (isAlreadyLiked) {
            wishlistRepository.deleteByUser_UserIdAndConcert_ConcertId(userId, concertId);
            updateWishlistCount(concertId, -1);
            return false;
        } else {
            // User와 Concert 엔티티 조회
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
            Concert concert = findById(concertId);

            // Wishlist 엔티티 생성
            Wishlist wishlist = Wishlist.builder()
                    .user(user)
                    .concert(concert)
                    .build();

            wishlistRepository.save(wishlist);
            updateWishlistCount(concertId, 1);
            return true;
        }
    }

    // [위시리스트] 상태 확인
    public boolean isLiked(String concertId, String userId) {
        return wishlistRepository.existsByUser_UserIdAndConcert_ConcertId(userId, concertId);
    }

    // [위시리스트] 개수 조회
    public int getWishlistCount(String concertId) {
        // 1. Repository에 countByConcert_ConcertId 메서드가 있어야 함
        return (int) wishlistRepository.countByConcert_ConcertId(concertId);
    }

    // [내부 호출] 공연 엔티티의 카운트 업데이트
    @Transactional
    public void updateWishlistCount(String concertId, int delta) {
        Concert concert = findById(concertId);
        concert.setConcertWishlistCount(concert.getConcertWishlistCount() + delta);
    }

    // --- 기존 코드 (통계, 랭킹, 세션 등 유지) ---

    public Map<String, List<?>> getStatsData(String concertId) {
        Concert concert = findById(concertId);
        if (concert.getStats() == null || concert.getStats().isEmpty()) return null;
        Stats stats = concert.getStats().get(0);
        Map<String, List<?>> data = new HashMap<>();
        data.put("genderData", Arrays.asList(stats.getMaleRatio(), stats.getFemaleRatio()));
        data.put("ageData", Arrays.asList(stats.getAge10sRatio(), stats.getAge20sRatio(), stats.getAge30sRatio(), stats.getAge40sRatio(), stats.getAge50sRatio()));
        return data;
    }

    public List<Map<String, Object>> getRankedConcerts() {
        List<Object[]> results = concertRepository.findConcertsByRanking();
        return results.stream().map(obj -> {
            Map<String, Object> map = new HashMap<>();
            map.put("concert", (Concert) obj[0]);
            map.put("ranking", ((Number) obj[1]).intValue());
            return map;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRankedConcertsByGenre(String genre) {
        List<Object[]> results = concertRepository.findConcertsByGenreRanking(genre);
        return results.stream().map(obj -> {
            Map<String, Object> map = new HashMap<>();
            map.put("concert", (Concert) obj[0]);
            map.put("ranking", ((Number) obj[1]).intValue());
            return map;
        }).collect(Collectors.toList());
    }

    public List<String> findSessionsByDate(String id, String selectedDate) {
        Concert concert = findById(id);
        LocalDate date = LocalDate.parse(selectedDate);

        // 1. 공연 기간 검증 (선택한 날짜가 범위 내에 없으면 바로 종료)
        if (date.isBefore(concert.getConcertStartDate()) || date.isAfter(concert.getConcertEndDate())) {
            return Collections.emptyList();
        }

        String allTimes = concert.getConcertTime();
        if (allTimes == null || allTimes.isEmpty()) return Collections.emptyList();

        // 2. 선택한 날짜의 요일 구하기 (예: "금요일")
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

        return Arrays.stream(allTimes.split(","))
                .map(String::trim)
                .filter(time -> {
                    // time 예시: "금요일(20:00)" 또는 "토요일~일요일(14:00)"
                    // 요일 부분만 추출 (괄호 앞 부분)
                    String targetPart = time.contains("(") ? time.split("\\(")[0] : time;

                    // 해당 요일이 포함되어 있는지 확인
                    return targetPart.contains(dayOfWeek);
                })
                .collect(Collectors.toList());
    }

    public List<Concert> getUpcomingConcerts() {
        LocalDate today = LocalDate.now();
        return getAllConcerts().stream().filter(c -> !c.getConcertEndDate().isBefore(today)).collect(Collectors.toList());
    }

    public List<Concert> getPastConcerts() {
        LocalDate today = LocalDate.now();
        return getAllConcerts().stream().filter(c -> c.getConcertEndDate().isBefore(today)).collect(Collectors.toList());
    }
    // ConcertService.java 내부
    public List<Concert> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // Repository에 있는 메서드 호출 (LIKE %keyword% 방식)
        return concertRepository.findByConcertNameContaining(keyword);
    }
    public boolean isAllSoldOut(String concertId) {
        // 1. 해당 공연의 전체 좌석 수 (Seat 엔티티 기준)
        long totalSeats = seatRepository.countByConcert_ConcertId(concertId);

        // 2. 해당 공연의 예매된 좌석 수 (SelectedSeat 엔티티 기준)
        long reservedSeats = selectedSeatRepository.countByConcert_ConcertId(concertId);

        // 3. 남은 자리가 없는지 확인 (0보다 크면 매진 아님)
        return totalSeats > 0 && reservedSeats >= totalSeats;
    }

    public boolean isSessionSoldOut(String concertId, String sessionTime) {
        // 특정 회차의 좌석 수 대비 예매된 좌석 수 계산이 필요합니다.
        // 회차(sessionTime)별로 좌석이 구분되어 있다면 이 로직을 사용하세요.
        long totalSeats = seatRepository.countByConcert_ConcertId(concertId); // 필요 시 회차 조건 추가
        long reservedSeats = selectedSeatRepository.countByConcert_ConcertIdAndSessionTime(concertId, sessionTime);

        return reservedSeats >= totalSeats;
    }

}