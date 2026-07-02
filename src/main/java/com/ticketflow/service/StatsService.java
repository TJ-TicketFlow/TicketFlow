package com.ticketflow.service;

import com.ticketflow.entity.SelectedSeat;
import com.ticketflow.entity.Stats;
import com.ticketflow.entity.User;
import com.ticketflow.repository.SelectedSeatRepository;
import com.ticketflow.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final SelectedSeatRepository seatRepository;
    private final StatsRepository statsRepository;

    @Transactional
    public void updateStats(String concertId) {
        // 1. 해당 공연의 결제 완료(상태 2) 좌석 리스트 조회
        List<SelectedSeat> completedSeats = seatRepository.findByConcert_ConcertIdAndSeatState(concertId, (short) 2);
        int total = completedSeats.size();

        // 2. Stats 엔티티 조회
        Stats stats = statsRepository.findByConcert_ConcertId(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Stats 데이터를 찾을 수 없습니다."));

        // 3. 예매 데이터가 없는 경우 초기화 후 종료
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
            return;
        }

        // 4. 계산 변수 초기화
        int male = 0, female = 0;
        int age10 = 0, age20 = 0, age30 = 0, age40 = 0, age50 = 0;
        int currentYear = LocalDate.now().getYear();

        // 5. 집계 로직
        for (SelectedSeat ss : completedSeats) {
            User user = ss.getUser();
            if (user == null) continue;

            // 성별 집계
            if (user.getUserSex() != null) {
                if (user.getUserSex() == 1) male++; else female++;
            }
            // 연령대 집계
            if (user.getUserBirth() != null) {
                int age = currentYear - user.getUserBirth().getYear() + 1;
                if (age < 20) age10++;
                else if (age < 30) age20++;
                else if (age < 40) age30++;
                else if (age < 50) age40++;
                else age50++;
            }
        }

        // 6. 예매율 및 비율 계산 (float 정밀도 적용)
        Integer totalSeats = stats.getConcert().getTotalSeats();
        float reservationRate = (totalSeats != null && totalSeats > 0) ? ((float) total / totalSeats) * 100.0f : 0.0f;

        // 7. 결과 반영
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
    }
}