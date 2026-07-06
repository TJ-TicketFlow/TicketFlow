package com.ticketflow.service;

import com.ticketflow.dto.ConcertResponseDto;
import com.ticketflow.dto.ConcertSearchDto;
import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final SelectedSeatRepository selectedSeatRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ObjectMapper objectMapper;
    private final co.elastic.clients.elasticsearch.ElasticsearchClient elasticsearchClient;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

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
            Wishlist wishlist = Wishlist.builder().user(user).concert(concert).build();
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

    // ConcertService.java

    // ConcertService.java
    public List<Map<String, Object>> getRankedConcerts() {
        List<Object[]> results = concertRepository.findConcertsWithLatestStats();

        // 필터링 및 정렬
        List<Map<String, Object>> rankedList = results.stream()
                .filter(obj -> obj[1] != null && ((Stats) obj[1]).getReservationRate() > 0)
                .sorted((o1, o2) -> Float.compare(((Stats) o2[1]).getReservationRate(), ((Stats) o1[1]).getReservationRate()))
                .map(obj -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("concert", (Concert) obj[0]);
                    map.put("stats", (Stats) obj[1]);
                    return map;
                })
                .collect(Collectors.toList());

        // [중요] 여기서 'ranking' 키를 추가해야 합니다!
        for (int i = 0; i < rankedList.size(); i++) {
            rankedList.get(i).put("ranking", i + 1);
        }

        return rankedList;
    }

    public List<Map<String, Object>> getRankedConcertsByGenre(String genre) {
        // 1. 장르별 데이터 조회
        List<Object[]> results = concertRepository.findConcertsByGenreWithLatestStats(genre);

        if (results == null || results.isEmpty()) return Collections.emptyList();

        // 2. Stream 처리 및 리스트 변환
        List<Map<String, Object>> rankedList = results.stream()
                .filter(obj -> obj[1] != null && ((Stats) obj[1]).getReservationRate() > 0)
                .sorted((o1, o2) -> Double.compare(
                        ((Stats) o2[1]).getReservationRate(),
                        ((Stats) o1[1]).getReservationRate()))
                .map(obj -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("concert", (Concert) obj[0]);
                    map.put("stats", (Stats) obj[1]); // [추가] stats 정보도 반드시 넣어줘야 합니다!
                    return map;
                })
                .collect(Collectors.toList());

        // 3. [핵심] 여기서 랭킹(1, 2, 3...)을 부여합니다.
        for (int i = 0; i < rankedList.size(); i++) {
            rankedList.get(i).put("ranking", i + 1);
        }

        return rankedList;
    }

    public List<String> findSessionsByDate(String id, String selectedDate) {
        Concert concert = findById(id);
        LocalDate date = LocalDate.parse(selectedDate);
        if (date.isBefore(concert.getConcertStartDate()) || date.isAfter(concert.getConcertEndDate())) return Collections.emptyList();
        String allTimes = concert.getConcertTime();
        if (allTimes == null || allTimes.isEmpty()) return Collections.emptyList();

        // 2. 선택한 날짜의 요일 구하기 (예: "금요일")
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.KOREAN);
        return Arrays.stream(allTimes.split(",")).map(String::trim).filter(time -> {
            String targetPart = time.contains("(") ? time.split("\\(")[0] : time;
            return targetPart.contains(dayOfWeek);
        }).collect(Collectors.toList());
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
        if (keyword == null || keyword.trim().isEmpty()) return Collections.emptyList();
        try {
            String url = "http://elasticsearch:9200/concerts/_search";
            String queryJson = String.format("{\"query\": {\"query_string\": {\"default_field\": \"concertName\", \"query\": \"*%s*\"}}}", keyword);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(queryJson, headers);
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = response.getBody();
            Map<String, Object> hitsContainer = (Map<String, Object>) body.get("hits");
            List<Map<String, Object>> hits = (List<Map<String, Object>>) hitsContainer.get("hits");
            List<String> concertIds = hits.stream()
                    .map(hit -> (String) ((Map<String, Object>) hit.get("_source")).get("concertId"))
                    .collect(Collectors.toList());
            return concertIds.isEmpty() ? Collections.emptyList() : concertRepository.findAllById(concertIds);
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
        return (reservedGeneral >= 200) || (reservedStanding >= 400);
    }

    public List<ConcertResponseDto> getPopularConcerts(int limit) {
        LocalDate today = LocalDate.now();

        return concertRepository.findAll().stream()
                .filter(c -> !c.getConcertEndDate().isBefore(today))
                // [수정] 예매율 대신 예매된 좌석 수로 필터링
                .filter(c -> {
                    long totalSeats = seatRepository.countByConcert_ConcertId(c.getConcertId());
                    long reservedSeats = selectedSeatRepository.countByConcert_ConcertId(c.getConcertId());
                    // 예매율이 0.1% 이상인 것만 (즉, 하나라도 예매된 경우)
                    return totalSeats > 0 && reservedSeats > 0;
                })
                .sorted(Comparator.comparing(Concert::getConcertWishlistCount).reversed())
                .limit(limit)
                .map(ConcertResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<ConcertResponseDto> getRecommendedConcerts(String userId) {
        // 1. 유저가 선호하는 장르 목록 추출 (기존 로직 유지)
        List<String> preferredGenres = wishlistRepository.findByUser_UserId(userId).stream()
                .map(wish -> wish.getConcert().getConcertGenre())
                .filter(Objects::nonNull)
                .flatMap(g -> Arrays.stream(g.split(",")))
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
        return concertRepository.findAll().stream()
                .filter(c -> !c.getConcertEndDate().isBefore(today))
                .filter(c -> Arrays.stream(c.getConcertGenre().split(",")).map(String::trim).anyMatch(preferredGenres::contains))
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
            boolean hasPerformance = Arrays.stream(allTimes.split(",")).map(String::trim).anyMatch(time -> {
                String targetPart = time.contains("(") ? time.split("\\(")[0] : time;
                return targetPart.contains(dayOfWeek);
            });
            if (hasPerformance) availableDates.add(date.toString());
        }
        return availableDates;
    }

    @Transactional
    public void saveConcert(Concert concert) {
        try {
            String name = concert.getConcertName();
            String[] words = name.split("\\s+");
            StringBuilder inputList = new StringBuilder();

            for (int i = 0; i < words.length; i++) {
                StringBuilder suffix = new StringBuilder();
                for (int j = i; j < words.length; j++) {
                    suffix.append(words[j]).append((j == words.length - 1) ? "" : " ");
                }
                inputList.append("\"").append(suffix.toString().trim()).append("\"");
                if (i < words.length - 1) inputList.append(",");
            }

            String url = "http://elasticsearch:9200/concerts/_doc/" + concert.getConcertId();

            // ★ 핵심 수정: concertName 대신 위에서 만든 inputList 변수를 삽입
            String jsonString = String.format(
                    "{\"concertId\":\"%s\", \"concertName\":\"%s\", \"suggest\":{\"input\":[%s]}}",
                    concert.getConcertId(), concert.getConcertName(), inputList.toString()
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, new org.springframework.http.HttpEntity<>(jsonString, headers), String.class);

        } catch (Exception e) {
            System.err.println("★ 저장 실패: " + e.getMessage());
        }
    }

    public List<String> autocomplete(String query) {
        try {
            String url = "http://elasticsearch:9200/concerts/_search";
            String jsonQuery = String.format(
                    "{\"suggest\": {\"concert-suggest\": {\"prefix\": \"%s\", \"completion\": {\"field\": \"suggest\"}}}}",
                    query
            );

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(org.springframework.http.MediaType.APPLICATION_JSON));

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(jsonQuery, headers);

            // 결과값을 일단 Map으로 받습니다.
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

            if (response == null || !response.containsKey("suggest")) return Collections.emptyList();

            // [구조 분석]
            // response -> "suggest" -> "concert-suggest" (List) -> [0] -> "options" (List)
            Map<String, Object> suggestContainer = (Map<String, Object>) response.get("suggest");
            List<Map<String, Object>> suggestList = (List<Map<String, Object>>) suggestContainer.get("concert-suggest");

            if (suggestList == null || suggestList.isEmpty()) return Collections.emptyList();

            List<Map<String, Object>> options = (List<Map<String, Object>>) suggestList.get(0).get("options");

            return options.stream()
                    .map(opt -> (String) opt.get("text"))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            // 어떤 데이터 구조에서 에러가 나는지 확인하기 위해 로그 출력
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // [핵심] 엘라스틱서치 초기화 자동화 (재시도 로직 포함)
    @EventListener(ContextRefreshedEvent.class)
    public void initializeElasticsearch() {
        new Thread(() -> {
            boolean elasticReady = false;
            int retries = 0;
            while (!elasticReady && retries < 10) {
                try {
                    Thread.sleep(10000);
                    org.springframework.http.ResponseEntity<String> ping =
                            restTemplate.getForEntity("http://elasticsearch:9200/", String.class);
                    if (ping.getStatusCode().is2xxSuccessful()) elasticReady = true;
                } catch (Exception e) {
                    retries++;
                    System.out.println("★ 엘라스틱서치 대기 중... (" + retries + "/10회)");
                }
            }

            if (elasticReady) {
                try {
                    String indexUrl = "http://elasticsearch:9200/concerts";
                    try {
                        restTemplate.exchange(indexUrl, org.springframework.http.HttpMethod.HEAD, null, String.class);
                    } catch (Exception e) {
                        createIndex(indexUrl);
                    }
                } catch (Exception e) {
                    System.err.println("★ 초기화 실패: " + e.getMessage());
                }
            }
        }).start();
    }

    private void createIndex(String url) {
        try {
            String mappingJson = "{\"mappings\": {\"properties\": {\"concertId\": { \"type\": \"keyword\" },\"concertName\": { \"type\": \"text\" },\"suggest\": { \"type\": \"completion\" }}}}";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            restTemplate.put(url, new org.springframework.http.HttpEntity<>(mappingJson, headers));
            syncAllConcertsToElasticsearch();
        } catch (Exception e) {
            System.err.println("★ 인덱스 생성 실패: " + e.getMessage());
        }
    }

    public void syncAllConcertsToElasticsearch() {
        try {
            String countUrl = "http://elasticsearch:9200/concerts/_count";
            org.springframework.http.ResponseEntity<Map> countResponse = restTemplate.getForEntity(countUrl, Map.class);
            Integer count = (Integer) countResponse.getBody().get("count");

            if (count == null || count == 0) {
                List<Concert> allConcerts = concertRepository.findAll();
                if (allConcerts.isEmpty()) return;

                StringBuilder bulkBody = new StringBuilder();
                for (Concert concert : allConcerts) {
                    bulkBody.append("{\"index\":{\"_id\":\"").append(concert.getConcertId()).append("\"}}\n");
                    bulkBody.append("{\"concertId\":\"").append(concert.getConcertId())
                            .append("\", \"concertName\":\"").append(concert.getConcertName())
                            .append("\", \"suggest\":{\"input\":[\"").append(concert.getConcertName()).append("\"]}}\n");
                }

                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

                restTemplate.postForEntity("http://elasticsearch:9200/concerts/_bulk",
                        new org.springframework.http.HttpEntity<>(bulkBody.toString(), headers), String.class);

                System.out.println("★ 대량 데이터 동기화 완료! " + allConcerts.size() + "건 입력됨.");
            }
        } catch (Exception e) {
            System.err.println("★ 동기화 실패: " + e.getMessage());
        }
    }
}