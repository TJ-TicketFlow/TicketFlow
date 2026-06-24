package com.ticketflow.service;

import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatService {

    private final SeatRepository seatRepository;
    private final ConcertRepository concertRepository;

    /**
     * 공연 가격정보 기반 좌석 타입 판단
     * 예: VIP 200000,R 150000,S 100000 또는 스탠딩 99000
     */
    @Transactional(readOnly = true)
    public String getSeatLayoutType(String concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new RuntimeException("공연 없음"));

        String priceInfo = concert.getConcertPriceInfo();

        if (priceInfo == null || priceInfo.trim().isEmpty()) {
            return "SEAT_A"; // 가격 정보가 없으면 안전하게 기본 좌석형 리턴
        }

        // 🌟 대소문자 무관하게 비교하기 위해 대문자로 변환 처리
        String upperPriceInfo = priceInfo.toUpperCase();

        // 🌟 "스탠딩"이나 "GA"라는 단어가 포함되어 있다면 배치도가 없는 스탠딩형으로 판단
        if (upperPriceInfo.contains("스탠딩") || upperPriceInfo.contains("GA")) {
            return "STANDING"; // 앞서 seatmap.js 조건문과 맞추기 위해 "STANDING" 또는 "SEAT_B"를 리턴
        } else {
            return "SEAT_A";
        }
    }

    /**
     * 1. 좌석 조회 (💡 수정: DB에 좌석이 없을 경우 13행 18열 자동 동적 생성 로직 통합)
     */
    public List<Seat> getSeats(String concertId) {
        // 1-1. 먼저 해당 공연의 좌석이 DB에 존재하는지 파악합니다.
        List<Seat> seats = seatRepository.findByConcert_ConcertId(concertId);

        // 1-2. 만약 조회된 좌석 개수가 0개라면? 아직 배치도가 생성이 안 된 공연이므로 즉석에서 생성합니다!
        if (seats.isEmpty()) {
            System.out.println("⚠️ [SeatService] " + concertId + " 공연의 좌석 데이터가 없어 13행 18열 동적 생성을 시작합니다.");

            // 실제 Concert 정보가 DB에 존재치 않는 경우 예외 처리 및 연관관계 매핑용 객체 확보
            Concert concert = concertRepository.findById(concertId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 공연입니다: " + concertId));

            // 지정석 배치 규격에 맞춰 234개 좌석 밀어 넣기
            for (int row = 1; row <= 13; row++) {
                for (int col = 1; col <= 18; col++) {
                    Seat seat = new Seat();

                    // PK인 seatId 중복 방지를 위해 공연 ID를 접두어로 결합 (예: PF277688_R1_C1)
                    seat.setSeatId(concertId + "_R" + row + "_C" + col);
                    seat.setConcert(concert);
                    seat.setSeatClass("STANDARD");
                    seat.setSeatStatus((short) 1); // 1: 사용가능(일반석 기본값)
                    seat.setSeatRow(String.valueOf(row));
                    seat.setSeatCol(String.valueOf(col));

                    seatRepository.save(seat);
                }
            }

            // 데이터 쓰기 작업(인서트)이 끝났으므로 다시 정상 조회하여 리스트를 채웁니다.
            seats = seatRepository.findByConcert_ConcertId(concertId);
            System.out.println("✅ [SeatService] " + concertId + " 공연의 좌석 234개 실시간 동적 생성 완료.");
        }

        return seats;
    }

    /**
     * 2. 좌석 선택 (1 = 가능, 0 = 불가능)
     */
    public void selectSeat(String seatId, Long userNo) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        if (seat.getSeatStatus() == 0) {
            throw new RuntimeException("이미 선택된 좌석");
        }

        // 좌석 선점 (Dirty Checking으로 인해 save 생략 가능하나 명시 유지)
        seat.setSeatStatus((short) 0);
        seatRepository.save(seat);
    }

    /**
     * 3. 좌석 취소
     */
    public void cancelSeat(String seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        seat.setSeatStatus((short) 1);
        seatRepository.save(seat);
    }

    /**
     * 4. 예약 상태 변경
     */
    public void updateSeatStatus(String seatId, Short status) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        seat.setSeatStatus(status);
        seatRepository.save(seat);
    }

    /**
     * 5. 가격 계산
     */
    @Transactional(readOnly = true)
    public int calculatePrice(String concertId, String seatClass) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new RuntimeException("공연 없음"));

        String priceInfo = concert.getConcertPriceInfo();
        if (priceInfo == null) {
            throw new RuntimeException("가격 정보 없음");
        }

        String[] prices = priceInfo.split(",");
        for (String price : prices) {
            String[] data = price.split(":");
            String grade = data[0].trim(); // 공백 방지용 trim 추가
            int amount = Integer.parseInt(data[1].trim());

            if (grade.equals(seatClass)) {
                return amount;
            }
        }

        throw new RuntimeException("해당 좌석 등급 없음");
    }
}