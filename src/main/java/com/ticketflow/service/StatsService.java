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
        List<SelectedSeat> completedSeats = seatRepository.findByConcert_ConcertIdAndSeatState(concertId, (short) 2);
        int total = completedSeats.size();
        if (total == 0) return;

        int male = 0, female = 0;
        int age10 = 0, age20 = 0, age30 = 0, age40 = 0, age50 = 0;
        int currentYear = LocalDate.now().getYear();

        for (SelectedSeat ss : completedSeats) {
            User user = ss.getUser();
            // 성별 집계 (1: 남성, 0: 여성)
            if (user.getUserSex() != null) {
                if (user.getUserSex() == 1) male++;
                else female++;
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

        Stats stats = statsRepository.findByConcert_ConcertId(concertId)
                .orElseThrow(() -> new IllegalArgumentException("Stats 데이터를 찾을 수 없습니다."));

        stats.setMaleRatio((float) male * 100 / total);
        stats.setFemaleRatio((float) female * 100 / total);
        stats.setAge10sRatio((float) age10 * 100 / total);
        stats.setAge20sRatio((float) age20 * 100 / total);
        stats.setAge30sRatio((float) age30 * 100 / total);
        stats.setAge40sRatio((float) age40 * 100 / total);
        stats.setAge50sRatio((float) age50 * 100 / total);

        statsRepository.save(stats);
    }
}