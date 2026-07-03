package com.ticketflow.service;

import com.ticketflow.entity.Concert;
import com.ticketflow.entity.SelectedSeat;
import com.ticketflow.entity.Stats;
import com.ticketflow.entity.User;
import com.ticketflow.repository.ConcertRepository;
import com.ticketflow.repository.SelectedSeatRepository;
import com.ticketflow.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    private final SelectedSeatRepository seatRepository;
    private final StatsRepository statsRepository;
    private final ConcertRepository concertRepository;

    @Transactional
    public void updateStats(String concertId) {
        log.info("통계 업데이트 시작: concertId = {}", concertId);

        // 1. 좌석 리스트 조회
        List<SelectedSeat> completedSeats = seatRepository.findByConcert_ConcertIdAndSeatState(concertId, (short) 2);
        int total = completedSeats.size();
        log.info("예매 완료된 좌석 수: {}", total);

        // 2. Stats 엔티티 조회 (없으면 생성)
        Stats stats = statsRepository.findByConcert_ConcertId(concertId)
                .orElseGet(() -> {
                    log.info("Stats 데이터 없음, 새로 생성합니다.");
                    Concert concert = concertRepository.findById(concertId)
                            .orElseThrow(() -> new IllegalArgumentException("해당 공연 정보를 찾을 수 없습니다."));
                    return Stats.builder().concert(concert).build();
                });

        // 3. 예매 데이터가 없는 경우 비율 0으로 초기화 후 저장
        if (total == 0) {
            stats.setTotalReservations(0L);
            stats.setMaleRatio(0.0f);
            stats.setFemaleRatio(0.0f);
            stats.setAge10sRatio(0.0f);
            stats.setAge20sRatio(0.0f);
            stats.setAge30sRatio(0.0f);
            stats.setAge40sRatio(0.0f);
            stats.setAge50sRatio(0.0f);
            stats.setReservationRate(0.0f);
            statsRepository.save(stats);
            log.info("예매 데이터 없음, Stats 0으로 초기화 완료.");
            return;
        }

        // 4. 계산 변수 및 집계 로직
        int male = 0, female = 0;
        int age10 = 0, age20 = 0, age30 = 0, age40 = 0, age50 = 0;
        int currentYear = LocalDate.now().getYear();

        for (SelectedSeat ss : completedSeats) {
            User user = ss.getUser();
            if (user == null) continue;

            if (user.getUserSex() != null) {
                if (user.getUserSex() == 1) male++; else female++;
            }
            if (user.getUserBirth() != null) {
                int age = currentYear - user.getUserBirth().getYear() + 1;
                if (age < 20) age10++;
                else if (age < 30) age20++;
                else if (age < 40) age30++;
                else if (age < 50) age40++;
                else age50++;
            }
        }

        // 5. 결과 반영
        Integer totalSeats = stats.getConcert().getTotalSeats();
        float reservationRate = (totalSeats != null && totalSeats > 0) ? ((float) total / totalSeats) * 100.0f : 0.0f;

        stats.setTotalReservations((long) total);
        stats.setMaleRatio((float) male * 100.0f / total);
        stats.setFemaleRatio((float) female * 100.0f / total);
        stats.setAge10sRatio((float) age10 * 100.0f / total);
        stats.setAge20sRatio((float) age20 * 100.0f / total);
        stats.setAge30sRatio((float) age30 * 100.0f / total);
        stats.setAge40sRatio((float) age40 * 100.0f / total);
        stats.setAge50sRatio((float) age50 * 100.0f / total);
        stats.setReservationRate(reservationRate);

        statsRepository.save(stats);
        log.info("통계 업데이트 완료. 예매율: {}%", reservationRate);
    }
}