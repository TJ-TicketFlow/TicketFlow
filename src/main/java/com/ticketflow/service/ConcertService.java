package com.ticketflow.service;

import com.ticketflow.dto.ConcertResponseDto;
import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
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
    private final ReservationRepository reservationRepository;

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

    public boolean isSessionSoldOut(String concertId, String sessionTime, LocalDate date) {
        // DB에 저장된 실제 좌석 등급 이름("일반석", "스탠딩" 등)을 정확히 넣으세요.
        long reservedGeneral = reservationRepository.countBySeatClass(concertId, sessionTime, date, "일반석");
        long reservedStanding = reservationRepository.countBySeatClass(concertId, sessionTime, date, "스탠딩");

        // 총 좌석수를 구하는 로직 (각 클래스별로 좌석 개수를 미리 알고 있다면 하드코딩해도 됩니다)
        long totalGeneral = 200;
        long totalStanding = 400;

        return (reservedGeneral >= totalGeneral) || (reservedStanding >= totalStanding);
    }

    public List<ConcertResponseDto> getPopularConcerts(int limit) {
        LocalDate today = LocalDate.now();

        // 1. 모든 공연을 가져와서 필터링 및 정렬 수행
        return concertRepository.findAll().stream()
                // 1) 지난 공연 제외 (종료일이 오늘 이전인 것 제외)
                .filter(c -> !c.getConcertEndDate().isBefore(today))
                // 2) 정렬: 찜 개수 내림차순, 같다면 종료일 오름차순(임박순)
                .sorted(Comparator.comparing(Concert::getConcertWishlistCount).reversed()
                        .thenComparing(Concert::getConcertEndDate))
                // 3) 개수 제한
                .limit(limit)
                .map(ConcertResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<ConcertResponseDto> getRecommendedConcerts(String userId) {
        // 1. 유저가 선호하는 장르 목록 추출 (기존 로직 유지)
        List<String> preferredGenres = wishlistRepository.findByUser_UserId(userId).stream()
                .map(wish -> wish.getConcert().getConcertGenre())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(","))) // 쉼표 기준 분리
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .collect(Collectors.groupingBy(g -> g, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (preferredGenres.isEmpty()) {
            // 선호 장르가 없으면 인기순으로 기본 추천
            return concertRepository.findPopularAndUpcoming(PageRequest.of(0, 3)).stream()
                    .map(ConcertResponseDto::new).collect(Collectors.toList());
        }

        // 2. 서비스단에서 유연한 필터링 수행 (방법 B)
        LocalDate today = LocalDate.now();
        return concertRepository.findAll().stream() // 전체 공연을 가져옴 (데이터가 아주 많다면 JPQL로 페이징 필요)
                .filter(c -> !c.getConcertEndDate().isBefore(today)) // 마감 안 된 공연
                .filter(c -> {
                    String[] concertGenres = c.getConcertGenre().split(","); // DB의 다중 장르 분리
                    return Arrays.stream(concertGenres)
                            .map(String::trim)
                            .anyMatch(preferredGenres::contains); // 취향 장르가 하나라도 포함되면 True
                })
                .limit(3)
                .map(ConcertResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<String> findAvailableDates(String id) {
        Concert concert = findById(id);
        LocalDate startDate = concert.getConcertStartDate();
        LocalDate endDate = concert.getConcertEndDate();
        String allTimes = concert.getConcertTime();

        if (allTimes == null || allTimes.isEmpty()) return Collections.emptyList();

        List<String> availableDates = new ArrayList<>();

        // 공연 기간(startDate ~ endDate)을 하루씩 증가시키며 루프
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);

            // 해당 요일에 공연이 있는지 확인 (기존 findSessionsByDate와 동일한 로직)
            boolean hasPerformance = Arrays.stream(allTimes.split(","))
                    .map(String::trim)
                    .anyMatch(time -> {
                        String targetPart = time.contains("(") ? time.split("\\(")[0] : time;
                        return targetPart.contains(dayOfWeek);
                    });

            if (hasPerformance) {
                availableDates.add(date.toString()); // "2026-06-20" 형식으로 저장
            }
        }
        return availableDates;
    }

}