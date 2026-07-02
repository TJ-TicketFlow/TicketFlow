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

    @Transactional
    public boolean toggleWishlist(String concertId, String userId) {
        boolean isAlreadyLiked = wishlistRepository.existsByUser_UserIdAndConcert_ConcertId(userId, concertId);
        if (isAlreadyLiked) {
            wishlistRepository.deleteByUser_UserIdAndConcert_ConcertId(userId, concertId);
            updateWishlistCount(concertId, -1);
            return false;
        } else {
            User user = userRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
            Concert concert = findById(concertId);
            Wishlist wishlist = Wishlist.builder().user(user).concert(concert).build();
            wishlistRepository.save(wishlist);
            updateWishlistCount(concertId, 1);
            return true;
        }
    }

    public boolean isLiked(String concertId, String userId) {
        return wishlistRepository.existsByUser_UserIdAndConcert_ConcertId(userId, concertId);
    }

    public int getWishlistCount(String concertId) {
        return (int) wishlistRepository.countByConcert_ConcertId(concertId);
    }

    @Transactional
    public void updateWishlistCount(String concertId, int delta) {
        Concert concert = findById(concertId);
        concert.setConcertWishlistCount(concert.getConcertWishlistCount() + delta);
    }

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
        Map<String, Map<String, Object>> distinctMap = new LinkedHashMap<>();
        for (Object[] obj : results) {
            Concert concert = (Concert) obj[0];
            int ranking = ((Number) obj[1]).intValue();
            if (!distinctMap.containsKey(concert.getConcertId())) {
                Map<String, Object> map = new HashMap<>();
                map.put("concert", concert);
                map.put("ranking", ranking);
                distinctMap.put(concert.getConcertId(), map);
            }
        }
        return new ArrayList<>(distinctMap.values());
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
        if (date.isBefore(concert.getConcertStartDate()) || date.isAfter(concert.getConcertEndDate())) return Collections.emptyList();
        String allTimes = concert.getConcertTime();
        if (allTimes == null || allTimes.isEmpty()) return Collections.emptyList();
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
        long totalSeats = seatRepository.countByConcert_ConcertId(concertId);
        long reservedSeats = selectedSeatRepository.countByConcert_ConcertId(concertId);
        return totalSeats > 0 && reservedSeats >= totalSeats;
    }

    public boolean isSessionSoldOut(String concertId, String sessionTime, LocalDate date) {
        long reservedGeneral = reservationRepository.countBySeatClass(concertId, sessionTime, date, "일반석");
        long reservedStanding = reservationRepository.countBySeatClass(concertId, sessionTime, date, "스탠딩");
        return (reservedGeneral >= 200) || (reservedStanding >= 400);
    }

    public List<ConcertResponseDto> getPopularConcerts(int limit) {
        LocalDate today = LocalDate.now();
        return concertRepository.findAll().stream()
                .filter(c -> !c.getConcertEndDate().isBefore(today))
                .sorted(Comparator.comparing(Concert::getConcertWishlistCount).reversed().thenComparing(Concert::getConcertEndDate))
                .limit(limit)
                .map(ConcertResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<ConcertResponseDto> getRecommendedConcerts(String userId) {
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
            return concertRepository.findPopularAndUpcoming(PageRequest.of(0, 3)).stream()
                    .map(ConcertResponseDto::new).collect(Collectors.toList());
        }
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
            String url = "http://elasticsearch:9200/concerts/_doc/" + concert.getConcertId();
            String jsonString = String.format("{\"concertId\":\"%s\", \"concertName\":\"%s\", \"suggest\":{\"input\":[\"%s\"]}}",
                    concert.getConcertId(), concert.getConcertName(), concert.getConcertName());
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, new org.springframework.http.HttpEntity<>(jsonString, headers), String.class);
        } catch (Exception e) {
            System.err.println("★ 저장 실패: " + e.getMessage());
        }
    }

    public List<String> autocomplete(String query) {
        try {
            var response = elasticsearchClient.search(s -> s
                    .index("concerts")
                    .suggest(sg -> sg
                            .suggesters("concert-suggest", fs -> fs
                                    .completion(c -> c.field("suggest").skipDuplicates(true).size(10).fuzzy(f -> f.fuzziness("AUTO")))
                            )
                    ), ConcertSearchDto.class);
            return response.suggest().get("concert-suggest").get(0).completion().options().stream()
                    .map(option -> option.text())
                    .collect(Collectors.toList());
        } catch (Exception e) {
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