package com.ticketflow.service;

import com.ticketflow.dto.BookingRequestDto;
import com.ticketflow.entity.*;
import com.ticketflow.repository.MembershipRepository;
import com.ticketflow.repository.PayRepository;
import com.ticketflow.repository.ReservationRepository;
import com.ticketflow.repository.UserCouponRepository;
import com.ticketflow.repository.UserRepository; // 💡 UserRepository 임포트 필요!
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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

    // 💡 1. 회원 정보를 찾기 위해 UserRepository를 추가합니다!
    private final UserRepository userRepository;

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
            // 💡 [핵심 1] 에러의 원인이었던 Map.class를 String.class로 바꿨습니다!
            ResponseEntity<String> response = restTemplate.exchange(apiURL, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            System.out.println("✅ 네이버 채점 응답 원본: " + responseBody);

            // 💡 [핵심 2] JSON 객체 대신 무식하고 안전하게 텍스트로 잘라냅니다.
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
                                long webhookAmount) {

        Pay payment = payRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("주문 내역 없음"));

        long expectedAmount = payment.getPayAmount();

        if (expectedAmount != webhookAmount) {
            payment.setPayStatus("FAILED");
            System.err.println("🚨 금액 불일치 경고! DB가격: " + expectedAmount + ", 결제된 가격: " + webhookAmount);
            return;
        }

        payment.setPayStatus("PAID");
        payment.setLsOrderId(lsOrderId);
        payment.setCurrency(currency);
        payment.setLsCustomerId(lsCustomerId);
        payment.setReceiptUrl(receiptUrl);
        payment.setLsWebhookEventId(lsWebhookEventId);
        payment.setPayMethod("LemonSqueezy");

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
    }

    // ==========================================
    // 💡 6. [새로 추가] 결제 화면용: 구매자 정보 포장하기
    // ==========================================
    public Map<String, Object> getUserInfoMap(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Map<String, Object> userInfo = new HashMap<>();
        // 주의: 엔티티의 Getter 이름이 다르면 (예: getName()) 아래 부분을 본인 코드에 맞게 수정하세요!
        userInfo.put("name", user.getUserName());
        userInfo.put("email", user.getUserEmail());
        userInfo.put("phone", user.getUserPhoneNumber());
        Object birth = user.getUserBirth();
        userInfo.put("birthDate", birth != null ? String.valueOf(birth) : "-");

        return userInfo;
    }

    // ==========================================
    // 💡 7. [수정] 결제 화면용: 티켓 정보 포장하기 (가짜 데이터 방어막 추가!)
    // ==========================================
    public Map<String, Object> getTicketInfoMap(Long reservationKey) {
        Map<String, Object> ticketInfo = new HashMap<>();

        // 1. 일단 DB에서 예약 정보를 찾아봅니다. (Optional을 써서 에러를 막습니다)
        Optional<Reservation> optionalReservation = reservationRepository.findById(reservationKey);

        if (optionalReservation.isPresent()) {
            // ----------------------------------------------------
            // ✅ DB에 진짜 데이터가 있을 때 (정상 로직)
            // ----------------------------------------------------
            Reservation reservation = optionalReservation.get();
            ticketInfo.put("count", reservation.getReservationCount());
            ticketInfo.put("date", String.valueOf(reservation.getReservationDate()));

            try {
                ticketInfo.put("price", reservation.getSelectedSeat().getPrice());

                // 💡 [추가] 진짜 DB에서 포스터 이미지 주소 꺼내기
                ticketInfo.put("posterUrl", reservation.getSelectedSeat().getSeat().getConcert().getConcertPosterUrl());

                ticketInfo.put("seatInfo", reservation.getSelectedSeat().getSeat().getSeatClass() + " " +
                        reservation.getSelectedSeat().getSeat().getSeatRow() + "열 " +
                        reservation.getSelectedSeat().getSeat().getSeatCol() + "번");
                ticketInfo.put("title", reservation.getSelectedSeat().getSeat().getConcert().getConcertName());
                ticketInfo.put("time", reservation.getSelectedSeat().getSeat().getConcert().getConcertTime());
                ticketInfo.put("venue", reservation.getSelectedSeat().getSeat().getConcert().getHall().getHallName());
            } catch (Exception e) {
                System.err.println("🚨 조인 오류 발생 (일부 데이터 임시 처리)");
                ticketInfo.put("price", 20000);
                ticketInfo.put("title", "데이터 연결 오류");
                ticketInfo.put("posterUrl", ""); // 에러 시 빈칸
            }
        } else {
            System.out.println("🚨 예약 데이터가 DB에 없습니다. 테스트용 가짜 데이터를 화면에 띄웁니다.");
            ticketInfo.put("count", 2);
            ticketInfo.put("date", "2026-12-24");
            ticketInfo.put("price", 55000);

            // 💡 [추가] 가짜 데이터에도 임시 이미지 주소를 하나 넣어줍니다.
            ticketInfo.put("posterUrl", "https://dummyimage.com/210x297/3b82f6/fff.png&text=Poster");

            ticketInfo.put("seatInfo", "VIP석 A구역 1열 1번");
            ticketInfo.put("title", "[테스트] 티켓플로우 크리스마스 콘서트");
            ticketInfo.put("time", "19:00");
            ticketInfo.put("venue", "올림픽 체조경기장 (가짜 데이터)");
        }

        return ticketInfo;
    }

    // ==========================================
    // 💡 8. [새로 추가] 문자 아이디로 회원 고유 번호(user_no) 찾기
    // ==========================================
    public Long getUserNoById(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 엔티티에 만들어두신 getter 이름에 맞춰주세요! (예: getUserNo())
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

            // 💡 핵심 2: 받아온 문자열 (예: {"key":"요청키값"}) 에서 키값만 잘라냅니다.
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

    // BookingService.java 내부에 추가 (기존 코드 유지)

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
            if (pay.getReservation() != null
                    && pay.getReservation().getSelectedSeat() != null
                    && pay.getReservation().getSelectedSeat().getConcert() != null) {

                Concert concert = pay.getReservation().getSelectedSeat().getConcert();

                // 1) getConcertStartDate()가 LocalDate를 반환하므로, null인지 먼저 체크합니다.
                if (concert.getConcertStartDate() != null) {

                    // 2) .toString()을 붙여서 날짜를 문자열("2026-05-18")로 바꿔줍니다!
                    String startDateStr = concert.getConcertStartDate().toString();
                    String timeStr = concert.getConcertTime(); // 시간은 String이라고 가정합니다.

                    date = startDateStr;

                    // 3) 시간 데이터가 있다면 날짜 뒤에 띄어쓰기 한 칸 하고 붙여줍니다.
                    if (timeStr != null && !timeStr.isBlank()) {
                        date += " " + timeStr; // 결과 예시: "2026-05-18 18:00"
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
            String statusStr = "진행중";
            if ("PAID".equals(pay.getPayStatus())) statusStr = "예매완료";
            else if ("FAILED".equals(pay.getPayStatus())) statusStr = "취소/환불";
            map.put("status", statusStr);

            return map;
        });
    }
}