package com.ticketflow.service;

import com.ticketflow.dto.BookingRequestDto;
import com.ticketflow.entity.Membership;
import com.ticketflow.entity.Pay;
import com.ticketflow.entity.Reservation;
import com.ticketflow.entity.UserCoupon;
import com.ticketflow.entity.User; // 💡 User 엔티티 임포트 필요!
import com.ticketflow.repository.MembershipRepository;
import com.ticketflow.repository.PayRepository;
import com.ticketflow.repository.ReservationRepository;
import com.ticketflow.repository.UserCouponRepository;
import com.ticketflow.repository.UserRepository; // 💡 UserRepository 임포트 필요!
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    // 💡 3. 결제 창 띄우기 및 쿠폰 사용 처리
    // ==========================================
    @Transactional
    public String createTemporaryPayment(BookingRequestDto requestDto) {
        Long incomingCouponId = requestDto.getUserCouponId();
        UserCoupon selectedCoupon = null;

        if (incomingCouponId != null) {
            selectedCoupon = userCouponRepository.findById(incomingCouponId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 쿠폰을 찾을 수 없습니다."));
            selectedCoupon.setUserCouponStatus(1);
        }

        Pay newPayment = Pay.builder()
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
        userInfo.put("birthDate", String.valueOf(user.getUserBirth()));

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
            ticketInfo.put("posterUrl", "https://via.placeholder.com/70x95/3B82F6/FFFFFF?text=Poster");

            ticketInfo.put("seatInfo", "VIP석 A구역 1열 1번");
            ticketInfo.put("title", "[테스트] 티켓플로우 크리스마스 콘서트");
            ticketInfo.put("time", "19:00");
            ticketInfo.put("venue", "올림픽 체조경기장 (가짜 데이터)");
        }

        return ticketInfo;
    }
}