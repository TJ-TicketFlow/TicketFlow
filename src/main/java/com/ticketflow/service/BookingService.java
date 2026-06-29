package com.ticketflow.service;

import com.ticketflow.dto.BookingRequestDto;
import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final PayRepository payRepository;
    private final ReservationRepository reservationRepository;
    private final UserCouponRepository userCouponRepository;
    private final MembershipRepository membershipRepository;
    private final SeatRepository seatRepository;

    // 💡 1. 회원 정보를 찾기 위해 UserRepository를 추가합니다!
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;

    @Value("${lemonsqueezy.api-key2}")
    private String apiKey;

    @Value("${lemonsqueezy.store-id2}")
    private String storeId;

    @Value("${lemonsqueezy.variant-Id2}")
    private String variantId;

    @Value("${naverCaptcha.client-id}")
    private String clientId;

    @Value("${naverCaptcha.client-secret}")
    private String clientSecret;

    // ==========================================
    // 💡 1. 멤버십 상태 확인 (기존 기능)
    // ==========================================
    public boolean checkActiveMembership(String userId) {
        Optional<Membership> latestMembership = membershipRepository
                .findTopByUser_UserIdOrderByMembershipCreatedAtDesc(userId);

        if (latestMembership.isPresent()) {
            Membership membership = latestMembership.get();
            return "ACTIVE".equals(membership.getMembershipStatus());
        }
        return false;
    }

    // ==========================================
    // 💡 2. 내 사용 가능한 쿠폰 목록 가져오기
    // ==========================================
    public List<Map<String, Object>> getMyAvailableCoupons(String userId) {
        List<UserCoupon> allCoupons = userCouponRepository.findByUser_UserId(userId);
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (UserCoupon uc : allCoupons) {
            if (uc.getUserCouponStatus() != null && uc.getUserCouponStatus() == 0) {
                Map<String, Object> map = new HashMap<>();
                map.put("userCouponId", uc.getUserCouponId());
                map.put("name", uc.getCoupon().getCouponName());
                map.put("couponDiscountRate", uc.getCoupon().getCouponDiscountRate());
                resultList.add(map);
            }
        }
        return resultList;
    }

    // ==========================================
    // 💡 3. 결제 창 띄우기
    // ==========================================
    @Transactional
    public String createTemporaryPayment(BookingRequestDto requestDto) {

        // ----------------------------------------------------
        // 🚨 [새로 추가된 핵심 방어막] 레몬스퀴지로 넘어가기 전 최종 이중결제 체크!
        // ----------------------------------------------------
        if (isAlreadyPaid(requestDto.getReservationKey())) {
            System.err.println("🚨 API 이중 결제 시도 거부: 예약번호 " + requestDto.getReservationKey());
            throw new IllegalStateException("이미 결제가 완료된 예매건입니다.");
        }
        // ----------------------------------------------------
        // 🛡️ [매크로 방어 1단계] 네이버 캡차 채점 로직
        // ----------------------------------------------------

        RestTemplate restTemplate = new RestTemplate();
        // 채점을 요청하는 네이버 주소 (code=1은 결과 확인을 의미함)
        String apiURL = "https://openapi.naver.com/v1/captcha/nkey?code=1&key="
                + requestDto.getCaptchaKey()
                + "&value=" + requestDto.getCaptchaValue();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", this.clientId);
        headers.set("X-Naver-Client-Secret", this.clientSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiURL, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            System.out.println("✅ 네이버 채점 응답 원본: " + responseBody);

            // JSON 객체 대신 무식하고 안전하게 텍스트로 잘라냅니다.
            // 네이버가 {"result":true, ...} 라고 보내주면 통과입니다.
            if (responseBody == null || !responseBody.contains("\"result\":true")) {
                throw new IllegalArgumentException("자동주문 방지 글자가 틀렸습니다. 다시 확인해주세요.");
            }
        } catch (Exception e) {
            // 🚨 네이버 통신 오류 등
            throw new IllegalArgumentException("캡차 검증에 실패했습니다: " + e.getMessage());
        }
        System.out.println("✅ 네이버 캡차 검증 완벽하게 통과!");

        Reservation reservation = reservationRepository.findById(requestDto.getReservationKey())
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        Long incomingCouponId = requestDto.getUserCouponId();
        UserCoupon selectedCoupon = null;

        if (incomingCouponId != null) {
            selectedCoupon = userCouponRepository.findById(incomingCouponId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 쿠폰을 찾을 수 없습니다."));
        }

        Pay newPayment = Pay.builder()
                .reservation(reservation)
                .payName(requestDto.getPayName())
                .payAmount(requestDto.getPayAmount())
                .userCoupon(selectedCoupon)
                .buyerName(requestDto.getBuyerName())
                .buyerEmail(requestDto.getBuyerEmail())
                .payDelName(requestDto.getPayDelName())
                .payDelCall(requestDto.getPayDelCall())
                .payDelPostcode(requestDto.getPayDelPostcode())
                .payDelAddr(requestDto.getPayDelAddr())
                .build();

        payRepository.save(newPayment);
        return getLemonSqueezyUrl(newPayment);
    }

    // ==========================================
    // 4. 레몬스퀴지 통신 로직
    // ==========================================
    private String getLemonSqueezyUrl(Pay payment) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/vnd.api+json");
        headers.set("Content-Type", "application/vnd.api+json");

        Map<String, Object> body = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> checkoutData = new HashMap<>();
        Map<String, Object> custom = new HashMap<>();
        Map<String, Object> relationships = new HashMap<>();

        custom.put("merchant_uid", payment.getMerchantUid());
        checkoutData.put("custom", custom);
        checkoutData.put("name", payment.getBuyerName());
        checkoutData.put("email", payment.getBuyerEmail());

        attributes.put("checkout_data", checkoutData);
        long finalPriceForLemonSqueezy = payment.getPayAmount() * 100;
        attributes.put("custom_price", finalPriceForLemonSqueezy);

        relationships.put("store", Map.of("data", Map.of("type", "stores", "id", storeId)));
        relationships.put("variant", Map.of("data", Map.of("type", "variants", "id", variantId)));

        data.put("type", "checkouts");
        data.put("attributes", attributes);
        data.put("relationships", relationships);
        body.put("data", data);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String apiUrl = "https://api.lemonsqueezy.com/v1/checkouts";
        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, Map.class);

        Map<String, Object> responseBody = response.getBody();
        Map<String, Object> responseData = (Map<String, Object>) responseBody.get("data");
        Map<String, Object> responseAttributes = (Map<String, Object>) responseData.get("attributes");

        return (String) responseAttributes.get("url");
    }

    // ==========================================
    // 5. 레몬스퀴지 웹훅
    // ==========================================
    @Transactional
    public void completePayment(String merchantUid, String lsOrderId,
                                String currency, String lsCustomerId,
                                String receiptUrl, String lsWebhookEventId,
                                long webhookAmount, String payStatus, String failReason) {

        // 1. 주문 번호로 DB에서 결제 대기 중인 장부를 찾습니다.
        Pay payment = payRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("주문 내역 없음"));

        // ----------------------------------------------------
        // 🚨 [1단계 방어막] 레몬스퀴지 자체에서 결제가 실패(failed)해서 넘어온 경우
        // ----------------------------------------------------
        if ("failed".equalsIgnoreCase(payStatus)) {
            payment.setPayStatus("FAILED");
            payment.setPayFailReason(failReason != null ? failReason : "카드 한도 초과 또는 잔액 부족 등 결제 실패");

            // 결제가 실패했으므로 묶여있던 좌석을 다른 사람도 살 수 있게 즉시 풀어줍니다.
            if (payment.getReservation() != null) {
                releaseUnpaidSeat(payment.getReservation().getReservationKey());
            }
            System.out.println("❌ 레몬스퀴지 결제 실패 기록 완료: " + payment.getPayFailReason());
            return; // 실패했으므로 아래 성공 로직을 타지 않고 여기서 함수를 끝냅니다.
        }

        // ----------------------------------------------------
        // 🚨 [2단계 방어막] 금액 위조 검사 (기존 코드 유지)
        // ----------------------------------------------------
        long expectedAmount = payment.getPayAmount();
        if (expectedAmount != webhookAmount) {
            payment.setPayStatus("FAILED");
            payment.setPayFailReason("🚨 서버 금액(" + expectedAmount + "원)과 실결제 금액(" + webhookAmount + "원)이 일치하지 않음 (위조 위험)");

            if (payment.getReservation() != null) {
                releaseUnpaidSeat(payment.getReservation().getReservationKey());
            }
            System.err.println("🚨 금액 불일치 사고 발생! 실패 사유 기록 완료.");
            return;
        }

        payment.setPayStatus("PAID");
        payment.setLsOrderId(lsOrderId);
        payment.setCurrency(currency);
        payment.setLsCustomerId(lsCustomerId);
        payment.setReceiptUrl(receiptUrl);
        payment.setLsWebhookEventId(lsWebhookEventId);
        payment.setPayMethod("신용/체크카드");

        if (payment.getUserCoupon() != null) {
            // 이 결제에 쿠폰이 쓰였다면, 상태를 1(사용함)로 바꿔줍니다!
            payment.getUserCoupon().setUserCouponStatus(1);
            System.out.println("✅ 쿠폰 사용 완료 처리됨!");
        }

        try {
            // 1. 장부에 연결해둔 예약 정보를 꺼냅니다.
            Reservation reservation = payment.getReservation();

            // 2. 예약에 연결된 임시 선택 좌석(Selected_Seat)을 꺼냅니다.
            var selectedSeat = reservation.getSelectedSeat();

            // 3. 임시 선택 좌석에 연결된 진짜 물리 좌석(Seat)을 꺼냅니다.
            var seat = selectedSeat.getSeat();

            // --------------------------------------------------
            // 💡 여기서 상태 값을 바꿔줍니다! (@Transactional 덕분에 자동 저장됨)
            // --------------------------------------------------

            // ① Selected_Seat 테이블 상태 변경: 1(결제중) -> 2(선택완료)
            selectedSeat.setSeatState((short) 2);

            // ② Seat 테이블 상태 변경: 1(사용가능) -> 0(사용불가=팔림)
            seat.setSeatStatus((short) 0);

            System.out.println("✅ " + seat.getSeatId() + "번 좌석 완벽하게 예매 확정(DB 업데이트) 완료!");

        } catch (Exception e) {
            System.err.println("🚨 좌석 확정 로직을 처리할 수 없습니다: " + e.getMessage());
        }

        System.out.println("✅ 결제 완료 및 상세 정보 업데이트 성공! 주문번호: " + merchantUid);

        sendBookingCompleteEmail(payment);
    }

    // ==========================================
    // 💡 6. [새로 추가] 결제 화면용: 구매자 정보 포장하기
    // ==========================================
    public Map<String, Object> getUserInfoMap(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("name", user.getUserName());
        userInfo.put("email", user.getUserEmail());
        userInfo.put("phone", user.getUserPhoneNumber());
        Object birth = user.getUserBirth();
        userInfo.put("birthDate", birth != null ? String.valueOf(birth) : "-");

        return userInfo;
    }

    // ==========================================
    // 💡 [수정] 결제 화면용: 티켓 정보 포장하기 (+ 보안 검증 추가)
    // ==========================================
    // 💡 기존 파라미터(Long reservationKey) 옆에 로그인한 유저의 번호(Long currentUserNo)를 추가로 받습니다.
    public Map<String, Object> getTicketInfoMap(Long reservationKey, Long currentUserNo) {
        Map<String, Object> ticketInfo = new HashMap<>();

        // 1. DB에서 예약 정보를 찾습니다. (없으면 바로 에러 발생)
        Reservation reservation = reservationRepository.findById(reservationKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약 정보입니다."));

        // ----------------------------------------------------
        // 🚨 [핵심 보안 방어막] 로그인한 사람과 예약한 사람이 같은지 검사!
        // ----------------------------------------------------
        Long ownerNo = reservation.getSelectedSeat().getUser().getUserNo();

        if (!ownerNo.equals(currentUserNo)) {
            // 주인이 다르면 가차 없이 에러를 던져버립니다.
            System.err.println("🚨 비정상적인 접근 감지: 로그인유저(" + currentUserNo + ")가 남의 예약건(" + ownerNo + ")에 접근 시도함.");
            throw new IllegalStateException("본인의 예매 내역만 결제할 수 있습니다.");
        }
        // ----------------------------------------------------
        // 🚨 [새로 추가된 핵심 방어막] 이미 돈을 지불한 예약건인지 검사!
        // ----------------------------------------------------
        if (isAlreadyPaid(reservationKey)) {
            System.err.println("🚨 이중 결제 시도 차단: 예약번호(" + reservationKey + ")는 이미 결제가 완료된 상태입니다.");
            // 이미 결제가 끝났다면 에러를 던져서 결제창 화면이 열리지 못하게 막아버립니다.
            throw new IllegalStateException("이미 결제가 완료된 예매건입니다. 마이페이지에서 확인해주세요.");
        }
        // ----------------------------------------------------

        // 2. 모든 검사를 통과했다면 안전하게 데이터를 채워서 넘겨줍니다.
        ticketInfo.put("count", reservation.getReservationCount());
        ticketInfo.put("date", String.valueOf(reservation.getReservationDate()));

        try {
            ticketInfo.put("price", reservation.getSelectedSeat().getPrice());
            ticketInfo.put("posterUrl", reservation.getSelectedSeat().getSeat().getConcert().getConcertPosterUrl());
            String allSeatsText = reservation.getSelectedSeatsText();
            if (allSeatsText == null || allSeatsText.isBlank()) {
                // 🌟 스탠딩(행이 0)일 때 방어 로직 추가
                if ("0".equals(reservation.getSelectedSeat().getSeat().getSeatRow())) {
                    allSeatsText = reservation.getSelectedSeat().getSeat().getSeatClass() + " " + reservation.getReservationCount() + "장";
                } else {
                    allSeatsText = reservation.getSelectedSeat().getSeat().getSeatClass() + " " +
                            reservation.getSelectedSeat().getSeat().getSeatRow() + "열 " +
                            reservation.getSelectedSeat().getSeat().getSeatCol() + "번";
                }
            }
            ticketInfo.put("seatInfo", allSeatsText);
            ticketInfo.put("title", reservation.getSelectedSeat().getSeat().getConcert().getConcertName());
            ticketInfo.put("time", reservation.getSelectedSeat().getSeat().getConcert().getConcertTime());
            ticketInfo.put("venue", reservation.getSelectedSeat().getSeat().getConcert().getHall().getHallName());
        } catch (Exception e) {
            System.err.println("🚨 조인 오류 발생 (일부 데이터 임시 처리)");
            ticketInfo.put("price", 0);
            ticketInfo.put("title", "데이터 연결 오류");
        }

        return ticketInfo;
    }

    // ==========================================
    // 💡 8. [새로 추가] 문자 아이디로 회원 고유 번호(user_no) 찾기
    // ==========================================
    public Long getUserNoById(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        return user.getUserNo();
    }

    // ==========================================
    // 💡 [네이버 캡차 1] 캡차 열쇠 발급받기
    // ==========================================
    public String getNaverCaptchaKey() {
        RestTemplate restTemplate = new RestTemplate();
        String apiURL = "https://openapi.naver.com/v1/captcha/nkey?code=0";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", this.clientId);
        headers.set("X-Naver-Client-Secret", this.clientSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiURL, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            System.out.println("✅ 네이버 응답 성공: " + responseBody);

            // 💡 핵심 : 받아온 문자열 (예: {"key":"요청키값"}) 에서 키값만 잘라냅니다.
            // (JSON 파싱 라이브러리인 Jackson을 써도 되지만, 에러 방지를 위해 가장 원초적인 방법으로 자릅니다)
            if (responseBody != null && responseBody.contains("\"key\"")) {
                int startIndex = responseBody.indexOf("\"key\":\"") + 7;
                int endIndex = responseBody.indexOf("\"", startIndex);
                return responseBody.substring(startIndex, endIndex);
            }
            return null;

        } catch (Exception e) {
            System.err.println("🚨 네이버 캡차 키 발급 실패: " + e.getMessage());
            return null;
        }
    }

    // 💡 1. [핵심] 예쁜 예매번호 생성기 (엔티티 수정 X)
    public String generateReadableOrderNo(Long payNo) {
        // TF-000087 형태로 만들어줍니다.
        return String.format("TF-%06d", payNo);
    }

    // 💡 2. 내 예매 내역 가져오기 (화면 전달용)
    public Page<Map<String, Object>> getMyTicketHistory(String userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {

        // 1. 날짜를 시간(00:00:00 ~ 23:59:59)까지 꽉 채워줍니다.
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 2. Repository에서 List가 아닌 Page 객체로 받아옵니다.
        Page<Pay> payPage = payRepository.findMyPaymentsByDateRange(userId, startDateTime, endDateTime, pageable);

        // 3. Page 안의 데이터(Pay)를 화면에 뿌리기 좋은 Map으로 변환해서 그대로 다시 Page로 묶어 반환합니다.
        return payPage.map(pay -> {
            Map<String, Object> map = new HashMap<>();

            map.put("booking_no", pay.getPayNo());
            map.put("display_no", generateReadableOrderNo(pay.getPayNo()));

            // 공연명
            String showName = "알 수 없는 공연";
            if (pay.getReservation() != null && pay.getReservation().getSelectedSeat() != null && pay.getReservation().getSelectedSeat().getSeat() != null && pay.getReservation().getSelectedSeat().getSeat().getConcert() != null) {
                showName = pay.getReservation().getSelectedSeat().getSeat().getConcert().getConcertName();
            }
            map.put("show_name", showName);

            // 관람일시
            String date = "-";
            if (pay.getReservation() != null) {
                Reservation reservation = pay.getReservation();

                // 1) reservationDate(LocalDate)가 null인지 확인하고 문자열로 바꿉니다.
                if (reservation.getReservationDate() != null) {
                    String dateStr = reservation.getReservationDate().toString();
                    date = dateStr;

                    // 2) sessionTime(String)을 확인하고 뒤에 띄어쓰기와 함께 붙여줍니다.
                    // 🚨 주의: 엔티티에 만들어두신 getter 이름에 맞게 getSessionTime()을 수정해 주세요!
                    String timeStr = reservation.getSessionTime();
                    if (timeStr != null && !timeStr.isBlank()) {
                        date += " " + timeStr; // 결과 예시: "2026-06-18 19:00"
                    }
                }
            }
            map.put("date", date);

            // 매수
            int count = 1;
            if (pay.getReservation() != null && pay.getReservation().getReservationCount() != null) {
                count = pay.getReservation().getReservationCount();
            }
            map.put("count", count);
            // 만약 개별 좌석 수를 세어야 한다면 이 방식이 더 정확할 수도 있습니다!
            // int count = pay.getReservation().getSelectedSeats().size();

            // 예매상태
            // 💡 [수정] 예매상태 결정 로직 (+ 관람완료 처리 추가)
            String statusStr = "진행중";
            String currentStatus = pay.getPayStatus();

            if ("PAID".equals(currentStatus)) {
                statusStr = "예매완료"; // 일단 기본은 예매완료로 설정

                // 🚨 방어막: 예약 날짜 정보가 잘 있는지 확인
                if (pay.getReservation() != null && pay.getReservation().getReservationDate() != null) {
                    LocalDate rDate = pay.getReservation().getReservationDate();
                    String rTimeStr = pay.getReservation().getSessionTime();

                    try {
                        LocalDateTime concertDateTime;
                        // "19:00" 처럼 시간 글자가 있다면 날짜와 합쳐서 정확한 '관람일시' 생성
                        if (rTimeStr != null && !rTimeStr.isBlank()) {
                            concertDateTime = LocalDateTime.of(rDate, LocalTime.parse(rTimeStr));
                        } else {
                            // 시간 정보가 없으면 그날 자정(밤 12시)을 기준으로 잡음
                            concertDateTime = rDate.atTime(LocalTime.MAX);
                        }

                        // 💡 현재 시간이 관람일시를 지나버렸다면? -> 관람완료!
                        if (LocalDateTime.now().isAfter(concertDateTime)) {
                            statusStr = "관람완료";
                        }
                    } catch (Exception e) {
                        // 만약 DB에 시간이 "오후 7시" 처럼 글자로 들어있어서 파싱 에러가 난다면,
                        // 안전하게 '날짜'만 비교해서 어제 이전이면 관람완료로 처리!
                        if (LocalDate.now().isAfter(rDate)) {
                            statusStr = "관람완료";
                        }
                    }
                }

            } else if ("CANCELLED".equals(currentStatus)) {
                statusStr = "결제 취소";
            } else if ("FAILED".equals(currentStatus)) {
                statusStr = "결제 실패";
            }
            map.put("status", statusStr);

            return map;
        });
    }

    // ==========================================
    // 💡 9. 예매 상세 내역 단건 조회 (상세 페이지용)
    // ==========================================
    public Map<String, Object> getTicketDetail(Long payNo, Long currentUserNo) {
        // 1. 넘어온 결제 번호(payNo)로 DB에서 데이터를 찾습니다.
        Pay pay = payRepository.findById(payNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 예매 내역을 찾을 수 없습니다."));

        // ----------------------------------------------------
        // 🚨 [핵심 보안 방어막] 이 영수증의 주인이 로그인한 사람이 맞는지 대조
        // ----------------------------------------------------
        if (pay.getReservation() != null && pay.getReservation().getSelectedSeat() != null) {
            Long ownerNo = pay.getReservation().getSelectedSeat().getUser().getUserNo();

            if (!ownerNo.equals(currentUserNo)) {
                System.err.println("🚨 경고: 유저(" + currentUserNo + ")가 남의 영수증(" + payNo + ", 주인:" + ownerNo + ") 조회를 시도함.");
                throw new IllegalStateException("본인의 예매 내역만 조회할 수 있습니다.");
            }
        }

        Map<String, Object> map = new HashMap<>();

        // 2. 기본 결제/예매자 정보
        map.put("display_no", generateReadableOrderNo(pay.getPayNo())); // TF-000001
        map.put("buyer", pay.getBuyerName() != null ? pay.getBuyerName() : "알 수 없음");

        // 결제일시 (날짜만 잘라서 보여주기 위해 포맷팅)
        String orderDate = "-";
        if (pay.getPayCreatedAt() != null) {
            // "2026-06-18 14:30:00" 형태에서 앞의 날짜와 시간만 예쁘게 자릅니다.
            orderDate = pay.getPayCreatedAt().toString().substring(0, 16).replace("T", " ");
        }
        map.put("order_date", orderDate);

        // 3. 공연, 좌석, 예약 정보 깊게 탐색하기
        if (pay.getReservation() != null && pay.getReservation().getSelectedSeat() != null
                && pay.getReservation().getSelectedSeat().getSeat() != null) {

            Reservation reservation = pay.getReservation();
            Seat seat = reservation.getSelectedSeat().getSeat();
            Concert concert = seat.getConcert();

            // 공연명
            map.put("title", concert != null ? concert.getConcertName() : "알 수 없는 공연");

            // 포스터 이미지 URL 가져오기
            String posterUrl = "";
            if (concert != null && concert.getConcertPosterUrl() != null) {
                posterUrl = concert.getConcertPosterUrl(); // 엔티티 Getter 이름 확인!
            }
            map.put("poster_url", posterUrl);

            // 공연장 (Hall)
            String venue = "-";
            if (concert != null && concert.getHall() != null) {
                venue = concert.getHall().getHallName();
            }
            map.put("venue", venue);

            // ==============================================================
            // 🌟 [핵심 수정] 영수증에 적어둔 전체 좌석 텍스트를 꺼냅니다!
            // ==============================================================
            String allSeatsText = reservation.getSelectedSeatsText();

            // 방어 로직: 텍스트가 텅 비어있을 경우 (예전 예매건 등)
            if (allSeatsText == null || allSeatsText.isBlank()) {
                // 🌟 스탠딩(행이 0)일 때 방어 로직 추가
                if ("0".equals(seat.getSeatRow())) {
                    allSeatsText = seat.getSeatClass() + " " + reservation.getReservationCount() + "장";
                } else {
                    allSeatsText = seat.getSeatClass() + " " + seat.getSeatRow() + "열 " + seat.getSeatCol() + "번";
                }
            }

            map.put("seat", allSeatsText);
            // ==============================================================

            // 관람일시 (List 페이지와 동일한 방식 적용)
            String viewDate = "-";
            if (reservation.getReservationDate() != null) {
                viewDate = reservation.getReservationDate().toString();
                String sessionTime = reservation.getSessionTime(); // 주의: Getter 이름 확인!
                if (sessionTime != null && !sessionTime.isBlank()) {
                    viewDate += " " + sessionTime;
                }
            }
            map.put("date", viewDate);
            map.put("count", reservation.getReservationCount());

            // 💡 취소 마감시간 (관람일 하루 전 17:00로 계산)
            if (reservation.getReservationDate() != null) {
                map.put("cancel_deadline", reservation.getReservationDate().minusDays(1).toString() + " 17:00");
            } else {
                map.put("cancel_deadline", "-");
            }

        } else {
            // 에러 방지용 기본값
            map.put("title", "데이터 연결 오류");
            map.put("venue", "-");
            map.put("seat", "-");
            map.put("date", "-");
            map.put("count", 0);
            map.put("cancel_deadline", "-");
        }

        // 4. 기타 부가 정보
        String delivery_status = "모바일 티켓";
        if(pay.getPayDelPostcode()!=null && pay.getPayDelAddr() != null) delivery_status = "배송";
        map.put("delivery", delivery_status);
        map.put("pay_method", pay.getPayMethod() != null ? pay.getPayMethod() : "결제 대기");

        // 가격 (150000 -> "150,000" 형태로 콤마 찍기)
        java.text.DecimalFormat df = new java.text.DecimalFormat("###,###");
        map.put("total_price", df.format(pay.getPayAmount() != null ? pay.getPayAmount() : 0));

        // 결제 상태
        String statusStr = "진행중";
        String currentStatus = pay.getPayStatus();

        if ("PAID".equals(currentStatus)) {
            statusStr = "예매완료";

            if (pay.getReservation() != null && pay.getReservation().getReservationDate() != null) {
                LocalDate rDate = pay.getReservation().getReservationDate();
                String rTimeStr = pay.getReservation().getSessionTime();

                try {
                    LocalDateTime concertDateTime;
                    if (rTimeStr != null && !rTimeStr.isBlank()) {
                        concertDateTime = LocalDateTime.of(rDate, LocalTime.parse(rTimeStr));
                    } else {
                        concertDateTime = rDate.atTime(LocalTime.MAX);
                    }

                    if (LocalDateTime.now().isAfter(concertDateTime)) {
                        statusStr = "관람완료";
                    }
                } catch (Exception e) {
                    if (LocalDate.now().isAfter(rDate)) {
                        statusStr = "관람완료";
                    }
                }
            }
        } else if ("CANCELLED".equals(currentStatus)) {
            statusStr = "결제 취소";
        } else if ("FAILED".equals(currentStatus)) {
            statusStr = "결제 실패";
        }
        map.put("status", statusStr);
        map.put("pay_no", pay.getPayNo());

        return map;
    }

    // ==========================================
    // 💡 10. 예매 취소 수수료 계산기
    // ==========================================
    public int calculateCancelFee(Pay pay, LocalDateTime cancelTime) {

        // 1. 기준 날짜들 가져오기
        LocalDateTime bookedTime = pay.getPayCreatedAt(); // 예매(결제)한 시간
        LocalDate concertDate = pay.getReservation().getReservationDate(); // 공연 날짜

        // 취소 마감시간: 관람일 하루 전 17시
        LocalDateTime deadline = concertDate.atTime(17, 0).minusDays(1);

        // 🚨 방어 로직: 취소 마감시간이 지났으면 에러 발생!
        if (cancelTime.isAfter(deadline)) {
            throw new IllegalArgumentException("취소 마감시간이 지나 취소할 수 없습니다.");
        }

        // 💡 규칙 1: 예매 당일 밤 12시 이전 취소 시 무조건 수수료 0원
        if (bookedTime.toLocalDate().isEqual(cancelTime.toLocalDate())) {
            return 0;
        }

        // 2. 날짜 차이 계산하기 (며칠 남았나? 며칠 지났나?)
        long daysToConcert = java.time.temporal.ChronoUnit.DAYS.between(cancelTime.toLocalDate(), concertDate);
        long daysSinceBooking = java.time.temporal.ChronoUnit.DAYS.between(bookedTime.toLocalDate(), cancelTime.toLocalDate());

        // 계산을 위한 가격 데이터 준비
        int totalPayAmount = pay.getPayAmount().intValue();
        int count = pay.getReservation().getReservationCount();
        int ticketPrice = totalPayAmount / count; // 티켓 1장당 가격
        int fee = 0;

        // 💡 핵심 규칙: 관람일 10일 이내라면, '예매 후 7일 이내' 규칙보다 우선 적용됩니다.
        if (daysToConcert <= 9) {

            if (daysToConcert >= 7) { // 9일 전 ~ 7일 전
                fee = (int) (totalPayAmount * 0.1); // 10%
            } else if (daysToConcert >= 3) { // 6일 전 ~ 3일 전
                fee = (int) (totalPayAmount * 0.2); // 20%
            } else { // 2일 전 ~ 마감일시
                fee = (int) (totalPayAmount * 0.3); // 30%
            }

        } else {
            // 관람일이 10일 이상 남았을 때
            if (daysSinceBooking <= 7) {
                // 💡 규칙 2: 예매 후 7일 이내 (수수료 없음)
                fee = 0;
            } else {
                // 💡 규칙 3: 예매 후 8일 이상 지남 (장당 4,000원, 단 10% 이내)
                int maxFeePerTicket = (int) (ticketPrice * 0.1);
                int feePerTicket = Math.min(4000, maxFeePerTicket); // 4000원과 10% 중 더 작은 금액 선택
                fee = feePerTicket * count; // 티켓 장수만큼 곱해줌
            }
        }

        return fee;
    }

    // ==========================================
    // 💡 11. 예매 취소 및 좌석 원상복구 로직
    // ==========================================
    @Transactional
    public void cancelTicket(Long payNo) {

        Pay pay = payRepository.findById(payNo)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        if (!"PAID".equals(pay.getPayStatus())) {
            throw new IllegalStateException("이미 취소되었거나 결제 완료 상태가 아닙니다.");
        }

        // 1. 수수료 계산 및 환불 금액 확정
        LocalDateTime now = LocalDateTime.now();
        int cancelFee = calculateCancelFee(pay, now);
        long refundAmount = pay.getPayAmount() - cancelFee;

        System.out.println("✅ 취소수수료: " + cancelFee + "원, 실제 환불될 금액: " + refundAmount + "원");

        // 🚨 레몬스퀴지 환불 API 연동이 필요하다면 이 자리에서 호출합니다!
        callLemonSqueezyRefund(pay.getLsOrderId(), refundAmount, pay.getPayAmount());

        // 2. 결제 상태 취소로 변경
        pay.setPayStatus("CANCELLED");

        // 3. 사용했던 쿠폰 돌려주기 (다시 쓸 수 있게 상태를 0으로)
        if (pay.getUserCoupon() != null) {
            pay.getUserCoupon().setUserCouponStatus(0);
        }

        // 4. 좌석을 다시 [예매 가능] 상태로 풀어주기
        Reservation reservation = pay.getReservation();
        if (reservation != null && reservation.getSelectedSeat() != null) {
            reservation.getSelectedSeat().setSeatState((short) 0); // 껍데기 해제

            // 🌟 [수정된 부분] 저장해둔 진짜 ID들을 꺼내서 모조리 1(가능)로 돌려놓습니다!
            String idsStr = reservation.getReservedSeatIds();
            if (idsStr != null && !idsStr.isEmpty()) {
                String[] seatIds = idsStr.split(",");
                for (String sId : seatIds) {
                    Seat seatToFree = seatRepository.findById(sId).orElse(null);
                    if (seatToFree != null) {
                        seatToFree.setSeatStatus((short) 1); // 싹 다 해제!
                    }
                }
                System.out.println("✅ 예매된 모든 좌석(" + idsStr + ") 취소 완료 및 상태 복구!");
            }
        }
    }

    // ==========================================
    // 💡 12. 취소 수수료 및 환불 금액 미리보기
    // ==========================================
    public Map<String, Object> getCancelFeeInfo(Long payNo) {
        Pay pay = payRepository.findById(payNo)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역을 찾을 수 없습니다."));

        if (!"PAID".equals(pay.getPayStatus())) {
            throw new IllegalStateException("이미 취소되었거나 취소할 수 없는 상태입니다.");
        }

        // 이전에 만든 calculateCancelFee 메서드 활용
        int fee = calculateCancelFee(pay, LocalDateTime.now());
        long refundAmount = pay.getPayAmount() - fee;

        Map<String, Object> result = new HashMap<>();
        result.put("cancelable", true);
        result.put("fee", fee);
        result.put("refundAmount", refundAmount);

        return result;
    }

    // ==========================================
    // 💡 13. 레몬스퀴지 환불(Refund) API 통신
    // ==========================================
    private void callLemonSqueezyRefund(String lsOrderId, long refundAmount, long originalAmount) {
        if (lsOrderId == null || lsOrderId.isBlank()) {
            System.out.println("🚨 레몬스퀴지 주문 번호가 없어서 환불 API를 호출할 수 없습니다. (더미 데이터일 확률 높음)");
            return;
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/vnd.api+json");
        headers.set("Content-Type", "application/vnd.api+json");

        Map<String, Object> body = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();

        if (refundAmount < originalAmount) {
            attributes.put("amount", refundAmount * 100);
            data.put("attributes", attributes);
        }
        // 2. 수수료가 0원인 '전액 환불'일 경우, attributes를 아예 세팅하지 않습니다!
        // (금액을 안 보내면 레몬스퀴지가 알아서 1원 단위 오차 없이 100% 전액 환불 처리합니다.)

        data.put("type", "orders"); // 'refunds'가 아니라 'orders' 타입으로 보냅니다.
        data.put("id", lsOrderId);  // 어떤 주문을 취소할지 주문 번호를 직접 명시합니다.
        data.put("attributes", attributes);

        body.put("data", data);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // 💡 [핵심 수정 2] 환불 요청 주소를 최신 규격으로 변경
            // 예: https://api.lemonsqueezy.com/v1/orders/12345/refund
            String apiUrl = "https://api.lemonsqueezy.com/v1/orders/" + lsOrderId + "/refund";

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, Map.class);
            System.out.println("✅ 레몬스퀴지 결제 취소(환불) 완벽하게 성공! 환불 요청 금액: " + refundAmount);

        } catch (Exception e) {
            System.err.println("🚨 레몬스퀴지 환불 API 호출 실패: " + e.getMessage());
            // 결제사 통신에 실패하면 우리 DB 취소도 멈추도록 에러를 던집니다.
            throw new IllegalStateException("결제사(레몬스퀴지) 환불 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    // ==========================================
    // 💡 [새로 추가] 이중 결제 확인용 헬퍼 메서드
    // ==========================================
    public boolean isAlreadyPaid(Long reservationKey) {
        // PayRepository에 추가했던 메서드를 사용하여 PAID(결제완료)된 내역이 있는지 검사합니다.
        return payRepository.existsByReservation_ReservationKeyAndPayStatus(reservationKey, "PAID");
    }

    // ==========================================
    // 💡 14. 결제 이탈 시 좌석 락(Lock) 해제하기
    // ==========================================
    @Transactional
    public void releaseUnpaidSeat(Long reservationKey) {
        Reservation reservation = reservationRepository.findById(reservationKey).orElse(null);
        if (reservation == null || reservation.getSelectedSeat() == null) return;

        SelectedSeat selectedSeat = reservation.getSelectedSeat();

        // 3. 결제가 안 된 좀비 좌석이라면 다시 세상에 풀어줍니다!
        if (selectedSeat.getSeatState() == 1 || selectedSeat.getSeatState() == 2) {
            selectedSeat.setSeatState((short) 0);

            // 🌟 [수정된 부분] 여기도 똑같이 모든 좌석 해제!
            String idsStr = reservation.getReservedSeatIds();
            if (idsStr != null && !idsStr.isEmpty()) {
                String[] seatIds = idsStr.split(",");
                for (String sId : seatIds) {
                    Seat seatToFree = seatRepository.findById(sId).orElse(null);
                    if (seatToFree != null) {
                        seatToFree.setSeatStatus((short) 1);
                    }
                }
            }
            System.out.println("✅ 결제창 이탈! 좌석(" + idsStr + ") 다시 예매 가능 상태로 풀림.");
        }
    }

    // ==========================================
    // 💡 예매 완료 이메일 발송 메서드
    // ==========================================
    public void sendBookingCompleteEmail(Pay payment) {
        try {
            // 편지 봉투(MimeMessage)를 하나 만듭니다.
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            // 1. 누구에게 보낼 것인가? (결제할 때 입력한 구매자 이메일)
            helper.setTo(payment.getBuyerEmail());

            // 2. 이메일 제목
            helper.setSubject("[TicketFlow] 예매가 성공적으로 완료되었습니다!");

            // 3. 이메일 내용 (HTML 형식으로 예쁘게 꾸밀 수 있습니다)
            String showName = payment.getReservation().getSelectedSeat().getSeat().getConcert().getConcertName();
            String seatInfo = payment.getReservation().getSelectedSeatsText();
            if (seatInfo == null || seatInfo.isBlank()) {
                seatInfo = payment.getReservation().getSelectedSeat().getSeat().getSeatClass() + " "
                        + payment.getReservation().getSelectedSeat().getSeat().getSeatRow() + "열 "
                        + payment.getReservation().getSelectedSeat().getSeat().getSeatCol() + "번";
            }
            // HTML 문법을 사용해서 내용을 작성합니다.
            String htmlContent = "<h3>🎉 예매가 완료되었습니다!</h3>"
                    + "<p><b>구매자명:</b> " + payment.getBuyerName() + "</p>"
                    + "<p><b>예매번호:</b> TF-0000" + payment.getPayNo() + "</p>"
                    + "<p><b>공연명:</b> " + showName + "</p>"
                    + "<p><b>좌석:</b> " + seatInfo + "</p>"
                    + "<p><b>결제금액:</b> " + payment.getPayAmount() + "원</p>"
                    + "<br><p>마이페이지에서 상세 내역을 확인하실 수 있습니다. 감사합니다!</p>";

            // true를 적어주면 단순 텍스트가 아니라 HTML 디자인이 적용됩니다.
            helper.setText(htmlContent, true);

            // 4. 전송!
            javaMailSender.send(mimeMessage);
            System.out.println("✅ 예매 완료 이메일 발송 성공! (수신자: " + payment.getBuyerEmail() + ")");

        } catch (Exception e) {
            System.err.println("🚨 이메일 발송 실패: " + e.getMessage());
        }
    }

    // ==========================================
    // 💡 15. 결제창 타이머 동기화를 위한 남은 시간 계산기
    // ==========================================
    public long getRemainingSeconds(Long reservationKey) {
        Reservation reservation = reservationRepository.findById(reservationKey)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        // 🌟 예약 장부가 DB에 생성된 진짜 시간을 가져옵니다.
        java.time.LocalDateTime createdAt = reservation.getReservationCreatedAt();

        // 혹시라도 생성 시간이 기록 안 되어 있다면 안전하게 기본값 30분(1800초)을 줍니다.
        if (createdAt == null) {
            return 1800L;
        }

        // 만료 시간 = 생성 시간 + 30분
        java.time.LocalDateTime expiresAt = createdAt.plusMinutes(30);

        // 현재 시간과 만료 시간 사이의 남은 초 계산
        long remainingSeconds = java.time.Duration.between(java.time.LocalDateTime.now(), expiresAt).getSeconds();

        // 30분이 이미 지났다면 마이너스 대신 0초를 반환합니다.
        return remainingSeconds > 0 ? remainingSeconds : 0;
    }
}