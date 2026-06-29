package com.ticketflow.service;

import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatService {

    private final SeatRepository seatRepository;
    private final ConcertRepository concertRepository;
    private final UserRepository userRepository;
    private final SelectedSeatRepository selectedSeatRepository;
    private final ReservationRepository reservationRepository;

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

        // "스탠딩"이라는 단어가 명확히 포함된 경우에만 배치도가 없는 스탠딩형으로 판단
        // 과거에는 "GA"라는 문자열도 체크했지만, "GA석"처럼 좌석 등급명에
        //    "GA"라는 두 글자가 포함되기만 해도 STANDING으로 오판하는 버그가 있어 제거함.
        //    (예: "VIP석 99,000원, GA석 88,000원" → 지정석 공연인데도 스탠딩으로 잘못 분류됨)
        if (priceInfo.contains("전석")) {
            return "SEAT_A"; // 앞서 seatmap.js 조건문과 맞추기 위해 "STANDING" 또는 "SEAT_B"를 리턴
        } else {
            return "STANDING";
        }
    }

    /**
     * 1. 좌석 조회 (수정: DB에 좌석이 없을 경우 13행 18열 자동 동적 생성 로직 통합)
     */
    public List<Seat> getSeats(String concertId) {
        // 1-1. 먼저 해당 공연의 좌석이 DB에 존재하는지 파악합니다.
        List<Seat> seats = seatRepository.findByConcert_ConcertId(concertId);

        // 1-2. 만약 조회된 좌석 개수가 0개라면? 아직 배치도가 생성이 안 된 공연이므로 즉석에서 생성합니다!
        if (seats.isEmpty()) {
            System.out.println("[SeatService] " + concertId + " 공연의 좌석 데이터가 없어 13행 18열 동적 생성을 시작합니다.");

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
            System.out.println("[SeatService] " + concertId + " 공연의 좌석 234개 실시간 동적 생성 완료.");
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

    @Transactional(readOnly = true)
    public Seat getSeatById(String realDbSeatId) {
        // seatRepository(Spring Data JPA)를 이용해 primary key(ID)로 좌석을 찾습니다.
        return seatRepository.findById(realDbSeatId)
                .orElseThrow(() -> new RuntimeException("해당 좌석 데이터를 찾을 수 없습니다. ID: " + realDbSeatId));
    }

    // 🎯 SeatService 클래스 내부에 아래 메서드를 추가해 주세요.
    @Transactional
    public Seat saveSeat(Seat seat) {
        return seatRepository.save(seat);
    }

    // ===================================================
    // 🌟 프론트엔드의 최종 예매 데이터를 받아 DB 장부 생성 (유저 표시 문구 최적화 버전)
    // ===================================================
    @Transactional
    public Long processBookingAndGetReservationKey(Map<String, Object> bookingData, Long userNo) {
        String concertId = bookingData.get("concertId").toString();
        String ticketType = bookingData.get("ticketType").toString();
        // 프론트엔드에서 연산해온 총 가격
        Long totalPrice = Double.valueOf(bookingData.get("totalPrice").toString()).longValue();

        User user = userRepository.findById(userNo).orElseThrow(() -> new RuntimeException("유저 찾을 수 없음"));
        Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new RuntimeException("공연 찾을 수 없음"));

        int totalTicketCount = 0;
        Seat representativeSeat = null;

        // 백엔드가 취소할 때 기억할 진짜 좌석 ID들을 담을 바구니
        List<String> realSeatIds = new java.util.ArrayList<>();

        //유저에게 보여줄 깔끔한 좌석 명칭("1열 1번")을 담을 바구니
        List<String> formattedSeatsForUser = new java.util.ArrayList<>();

        if ("SEAT".equals(ticketType)) {
            List<String> seatIds = (List<String>) bookingData.get("selectedSeats");
            totalTicketCount = seatIds.size();

            for (String frontendSeatId : seatIds) {

                // 프론트가 보낸 "SEAT_R1_C1"에서 "SEAT" 글자를 "공연ID(예: PF1234)"로 바꾸기
                // 결과물: "PF1234_R1_C1" (DB에 들어있는 진짜 아이디와 완벽 일치)
                String dbSeatId = frontendSeatId.replace("SEAT", concertId);

                Seat seat = seatRepository.findById(dbSeatId)
                        .orElseThrow(() -> new RuntimeException("유효하지 않은 좌석 번호입니다: " + dbSeatId));

                if (seat.getSeatStatus() == 0) {
                    throw new RuntimeException("이미 누군가 예매한 좌석입니다: " + dbSeatId);
                }

                // 좌석 잠금 처리
                seat.setSeatStatus((short) 0);

                try {
                    seat = seatRepository.save(seat);
                    seatRepository.flush();

                    realSeatIds.add(seat.getSeatId()); // 💡 취소용 진짜 ID(예: PF277688_R1_C1)는 여기에 안전하게 보존

                    // 엔티티가 가진 순수 숫자 데이터만 꺼내서 "1열 1번" 형태로 깨끗하게 조립합니다.
                    formattedSeatsForUser.add(seat.getSeatRow() + "열 " + seat.getSeatCol() + "번");

                } catch (Exception e) {
                    throw new RuntimeException("동시 예매 충돌: " + dbSeatId);
                }
                if (representativeSeat == null) representativeSeat = seat;
            }
        }
        // 💡 2. 스탠딩(STANDING) 수량형일 경우의 처리 로직
        else if ("STANDING".equals(ticketType)) {
            Map<String, Integer> quantities = (Map<String, Integer>) bookingData.get("quantities");
            for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
                String grade = entry.getKey();
                int qty = entry.getValue();
                totalTicketCount += qty;

                for (int i = 0; i < qty; i++) {
                    String shortGrade = grade.length() > 3 ? grade.substring(0, 3) : grade;
                    String finalshortGrade = shortGrade.replaceAll("석|스", "").trim();
                    long timeSeq = System.currentTimeMillis() % 10000;
                    String tempSeatId = concertId + "_S_" + finalshortGrade + timeSeq;

                    Seat seat = new Seat();
                    seat.setSeatId(tempSeatId);
                    seat.setConcert(concert);
                    seat.setSeatStatus((short) 0);
                    seat.setSeatClass(grade);
                    seat.setSeatRow("0");
                    seat.setSeatCol("0");

                    seat = seatRepository.save(seat);
                    realSeatIds.add(seat.getSeatId());

                    if (representativeSeat == null) representativeSeat = seat;
                }
            }
        }

        // 💡 3. 최종 예약 장부(SelectedSeat, Reservation) 조립 및 저장
        SelectedSeat selectedSeat = SelectedSeat.builder()
                .user(user)
                .seat(representativeSeat)
                .concert(concert)
                .price(totalPrice)
                .seatState((short) 1) // 1 = 결제 대기 상태
                .build();

        SelectedSeat savedSelectedSeat = selectedSeatRepository.save(selectedSeat);

        String seatsDisplayHtml = "선택된 티켓";

        if ("SEAT".equals(ticketType)) {
            //위에서 에러 없이 정밀하게 담아둔 "1열 1번, 1열 2번" 문자열을 그대로 합쳐줍니다
            seatsDisplayHtml = String.join(", ", formattedSeatsForUser);

        } else if ("STANDING".equals(ticketType)) {
            Map<String, Integer> quantities = (Map<String, Integer>) bookingData.get("quantities");
            List<String> standingDetails = new java.util.ArrayList<>();
            if (quantities != null) {
                for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
                    String gradeName = entry.getKey();
                    if ("GENERAL".equalsIgnoreCase(gradeName)) {
                        gradeName = "일반";
                    }
                    standingDetails.add(gradeName + entry.getValue() + "장");
                }
            }
            seatsDisplayHtml = String.join(", ", standingDetails);
        }

        Reservation reservation = Reservation.builder()
                .selectedSeat(savedSelectedSeat)
                .reservationCount(totalTicketCount)
                .reservationDate(concert.getConcertStartDate() != null ? concert.getConcertStartDate() : java.time.LocalDate.now())
                .sessionTime(concert.getConcertTime() != null ? concert.getConcertTime() : "시간 미정")
                .selectedSeatsText(seatsDisplayHtml) //여기에 완전 깨끗한 글자가 들어갑니다
                .reservedSeatIds(String.join(",", realSeatIds)) // 진짜 식별자 ID 목록도 컴마로저장
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        return savedReservation.getReservationKey();
    }


}