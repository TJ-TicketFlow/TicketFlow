package com.ticketflow.service;

import com.ticketflow.dto.ConcertNoticeMessage;
import com.ticketflow.dto.SeatEventMessage;
import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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
    private final SimpMessagingTemplate messagingTemplate;

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
        if (priceInfo.contains("스탠딩")) {
            return "STANDING"; // 앞서 seatmap.js 조건문과 맞추기 위해 STANDING 리턴
        } else {
            return "SEAT_A";
        }
    }

    /**
     * 1. 좌석 조회 (DB에 좌석이 없을 경우 13행 18열 자동 동적 생성 로직 통합)
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
     * 2. 좌석 선택 (1 = 가능, 0 = 불가능) 및 실시간 소켓 브로드캐스팅
     */
    public void selectSeat(String seatId, Long userNo) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        if (seat.getSeatStatus() == 0) {
            throw new RuntimeException("이미 선택된 좌석");
        }

        // 좌석 선점
        seat.setSeatStatus((short) 0);
        seatRepository.save(seat);

        String concertId = seat.getConcert().getConcertId();

        // 🔔 같은 공연 페이지를 보고 있는 다른 사용자들에게 실시간으로 좌석 잠금을 알림
        messagingTemplate.convertAndSend(
                "/topic/seat/" + concertId,
                new SeatEventMessage("SELECTED", seatId, userNo)
        );

        // 🔔 마지막 남은 좌석이었다면 마감(매진) 공지를 한 번 더 보냄
        notifyIfSoldOut(concertId);
    }

    /**
     * 3. 좌석 취소 및 실시간 소켓 브로드캐스팅
     */
    public void cancelSeat(String seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        seat.setSeatStatus((short) 1);
        seatRepository.save(seat);

        // 🔔 좌석이 다시 풀렸음을 실시간으로 알림 (다른 사용자가 선택 가능해짐)
        messagingTemplate.convertAndSend(
                "/topic/seat/" + seat.getConcert().getConcertId(),
                new SeatEventMessage("CANCELLED", seatId, null)
        );
    }

    /**
     * 4. 예약 상태 변경 및 실시간 소켓 연동
     */
    public void updateSeatStatus(String seatId, Short status) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new RuntimeException("좌석 없음"));

        seat.setSeatStatus(status);
        seatRepository.save(seat);

        String concertId = seat.getConcert().getConcertId();
        String type = (status != null && status == 0) ? "SELECTED" : "CANCELLED";

        messagingTemplate.convertAndSend(
                "/topic/seat/" + concertId,
                new SeatEventMessage(type, seatId, null)
        );

        if ("SELECTED".equals(type)) {
            notifyIfSoldOut(concertId);
        }
    }

    /**
     * 해당 공연의 선택 가능한(seatStatus=1) 좌석이 0개가 되면 매진 공지를 실시간 전송
     */
    private void notifyIfSoldOut(String concertId) {
        long remaining = seatRepository.countByConcert_ConcertIdAndSeatStatus(concertId, (short) 1);

        if (remaining == 0) {
            messagingTemplate.convertAndSend(
                    "/topic/concert/" + concertId + "/notice",
                    new ConcertNoticeMessage("SOLD_OUT", "전 좌석이 마감되었습니다.", 0)
            );
        }
    }

    /**
     * 5. 특정 좌석 등급의 가격 계산
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
            String grade = data[0].trim();
            int amount = Integer.parseInt(data[1].trim());

            if (grade.equals(seatClass)) {
                return amount;
            }
        }

        throw new RuntimeException("해당 좌석 등급 없음");
    }

    /**
     * 6. ID 기반 단일 좌석 데이터 단독 조회
     */
    @Transactional(readOnly = true)
    public Seat getSeatById(String realDbSeatId) {
        return seatRepository.findById(realDbSeatId)
                .orElseThrow(() -> new RuntimeException("해당 좌석 데이터를 찾을 수 없습니다. ID: " + realDbSeatId));
    }

    /**
     * 7. 단일 좌석 정보 강제 업데이트 및 엔티티 저장
     */
    public Seat saveSeat(Seat seat) {
        return seatRepository.save(seat);
    }

    // =========================================================================
    // 🌟 8. 프론트엔드의 최종 예매 데이터를 받아 DB 결제 가선점 임시 장부 생성 및 저장 로직
    // =========================================================================
    public Long processBookingAndGetReservationKey(Map<String, Object> bookingData, Long userNo) {
        String concertId = bookingData.get("concertId").toString();
        String ticketType = bookingData.get("ticketType").toString();

        // 프론트엔드로부터 전송받은 총 금액 안전 변환
        Long totalPrice = Double.valueOf(bookingData.get("totalPrice").toString()).longValue();

        User user = userRepository.findById(userNo).orElseThrow(() -> new RuntimeException("유저 찾을 수 없음"));
        Concert concert = concertRepository.findById(concertId).orElseThrow(() -> new RuntimeException("공연 찾을 수 없음"));

        int totalTicketCount = 0;
        Seat representativeSeat = null;

        // 결제 취소 시 복구 처리를 위해 식별할 데이터 바구니 설계
        List<String> realSeatIds = new java.util.ArrayList<>();
        List<String> formattedSeatsForUser = new java.util.ArrayList<>();

        // 8-A. 지정석(SEAT) 배치 형태의 예매 데이터 가선점 처리 로직
        if ("SEAT".equals(ticketType)) {
            List<String> seatIds = (List<String>) bookingData.get("selectedSeats");
            totalTicketCount = seatIds.size();

            for (String frontendSeatId : seatIds) {
                // 프론트 임시 ID인 "SEAT_R1_C1" 단어를 DB 실제 매핑 PK 양식인 "공연ID_R1_C1"로 변환
                String dbSeatId = frontendSeatId.replace("SEAT", concertId);

                Seat seat = seatRepository.findById(dbSeatId)
                        .orElseThrow(() -> new RuntimeException("유효하지 않은 좌석 번호입니다: " + dbSeatId));

                if (seat.getSeatStatus() == 0) {
                    throw new RuntimeException("이미 누군가 예매한 좌석입니다: " + dbSeatId);
                }

                // 좌석 사용 불가 상태로 잠금 확정
                seat.setSeatStatus((short) 0);

                try {
                    seat = seatRepository.save(seat);
                    seatRepository.flush();

                    realSeatIds.add(seat.getSeatId());
                    formattedSeatsForUser.add(seat.getSeatRow() + "열 " + seat.getSeatCol() + "번");

                    // 🔔 해당 좌석이 선점 처리되었음을 실시간 소켓으로 동시 공유하여 중복 클릭 원천 방지
                    messagingTemplate.convertAndSend(
                            "/topic/seat/" + concertId,
                            new SeatEventMessage("SELECTED", seat.getSeatId(), userNo)
                    );

                } catch (Exception e) {
                    throw new RuntimeException("동시 예매 충돌이 발생했습니다: " + dbSeatId);
                }
                if (representativeSeat == null) representativeSeat = seat;
            }
            // 🔔 일괄 좌석 선택에 따른 최종 매진 여부 재스크리닝 후 공지 처리
            notifyIfSoldOut(concertId);
        }
        // 8-B. 스탠딩(STANDING) 수량 지정 선택 형태의 예매 가선점 처리 로직
        else if ("STANDING".equals(ticketType)) {
            Map<String, Integer> quantities = (Map<String, Integer>) bookingData.get("quantities");
            for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
                String grade = entry.getKey();
                int qty = entry.getValue();
                totalTicketCount += qty;

                for (int i = 0; i < qty; i++) {
                    String shortGrade = grade.length() > 3 ? grade.substring(0, 3) : grade;
                    String tempSeatId = concertId + "_S_" + shortGrade + "_" + i;

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

        // 8-C. 가선점 및 결제 대기용 연관 장부 엔티티 조립 (SelectedSeat, Reservation 영수증)
        SelectedSeat selectedSeat = SelectedSeat.builder()
                .user(user)
                .seat(representativeSeat)
                .concert(concert)
                .price(totalPrice)
                .seatState((short) 1) // 1 = 결제 승인 대기 상태값 지정
                .build();

        SelectedSeat savedSelectedSeat = selectedSeatRepository.save(selectedSeat);
        String seatsDisplayHtml = "선택된 티켓";

        if ("SEAT".equals(ticketType)) {
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
                    standingDetails.add(gradeName + " " + entry.getValue() + "장");
                }
            }
            seatsDisplayHtml = String.join(", ", standingDetails);
        }

        Reservation reservation = Reservation.builder()
                .selectedSeat(savedSelectedSeat)
                .reservationCount(totalTicketCount)
                .reservationDate(concert.getConcertStartDate() != null ? concert.getConcertStartDate() : java.time.LocalDate.now())
                .sessionTime(concert.getConcertTime() != null ? concert.getConcertTime() : "시간 미정")
                .selectedSeatsText(seatsDisplayHtml)
                .reservedSeatIds(String.join(",", realSeatIds))
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        return savedReservation.getReservationKey();
    }

    // SeatService.java 내부 최하단에 추가

    /**
     * ↩️ 사용자가 결제 단계에서 뒤로가기 또는 페이지 이탈 시
     * 선점되었던 임시 예약 장부(Reservation) 및 선점 좌석(SelectedSeat) 데이터를 안전하게 파기합니다.
     */
    /**
     * ↩️ 임시 선점 데이터를 파기하고, 실시간 웹소켓 공지를 위한 래핑 데이터를 반환합니다.
     */
    @Transactional
    public Map<String, Object> releaseTemporarySeatsWithInfo(Long reservationKey) {
        System.out.println("🔄 [서비스 로직] 임시 선점 데이터 파기 시작. 가선점 키: " + reservationKey);
        Map<String, Object> resultInfo = new HashMap<>();

        // 1. 가선점 장부(Reservation) 조회
        Reservation reservation = reservationRepository.findById(reservationKey).orElse(null);

        if (reservation != null) {
            // 2. 웹소켓 전송을 위한 데이터 사전 추출
            String concertId = "";
            if (reservation.getSelectedSeat() != null && reservation.getSelectedSeat().getConcert() != null) {
                concertId = reservation.getSelectedSeat().getConcert().getConcertId();
            }

            if (reservation.getReservedSeatIds() != null && !concertId.isEmpty()) {
                String[] seatIds = reservation.getReservedSeatIds().split(",");
                resultInfo.put("concertId", concertId);
                resultInfo.put("seatIds", seatIds);
            }

            // 3. 연관 데이터 데이터베이스에서 연쇄 삭제
            SelectedSeat selectedSeat = reservation.getSelectedSeat();
            reservationRepository.delete(reservation);

            if (selectedSeat != null) {
                selectedSeatRepository.delete(selectedSeat);
            }

            System.out.println("✅ [서비스 로직] 데이터베이스에서 선점 데이터 파기 완료.");
            return resultInfo;
        }

        return null;
    }

}