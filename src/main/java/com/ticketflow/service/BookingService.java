package com.ticketflow.service;

import com.ticketflow.dto.BookingRequestDto;
import com.ticketflow.entity.Membership;
import com.ticketflow.entity.Pay;
import com.ticketflow.entity.Reservation;
import com.ticketflow.entity.UserCoupon;
import com.ticketflow.repository.MembershipRepository;
import com.ticketflow.repository.PayRepository;
import com.ticketflow.repository.ReservationRepository;
import com.ticketflow.repository.UserCouponRepository;
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
    // 💡 2. 내 사용 가능한 쿠폰 목록 가져오기 (새로 추가됨!)
    // ==========================================
    public List<Map<String, Object>> getMyAvailableCoupons(String userId) {
        // 1. 상태가 0(미사용)인 쿠폰만 찾아옵니다.
        // (주의: UserCouponRepository에 findByUserNoAndUserCouponStatus 메서드가 만들어져 있어야 합니다!)
        List<UserCoupon> allCoupons = userCouponRepository.findByUser_UserId(userId);

        // 2. 화면에 던져줄 상자를 준비합니다.
        List<Map<String, Object>> resultList = new ArrayList<>();

        // 3. 가져온 쿠폰들을 하나씩 꺼내보면서 검사합니다.
        for (UserCoupon uc : allCoupons) {

            // ⭐️ 핵심: 여기서 조건을 겁니다! "쿠폰 상태가 0(미사용)일 때만 상자에 담아라!"
            if (uc.getUserCouponStatus() != null && uc.getUserCouponStatus() == 0) {

                Map<String, Object> map = new HashMap<>();
                map.put("userCouponId", uc.getUserCouponId());
                map.put("name", uc.getCoupon().getCouponName());
                map.put("couponDiscountRate", uc.getCoupon().getCouponDiscountRate());

                resultList.add(map); // 조건에 맞는 것만 상자에 들어갑니다.
            }
        }

        return resultList;
    }

    // ==========================================
    // 💡 3. 결제 창 띄우기 및 쿠폰 사용 처리 (수정됨!)
    // ==========================================
    @Transactional
    public String createTemporaryPayment(BookingRequestDto requestDto) {

        // 1. DTO에 있는 예약 번호(숫자)를 가지고, 진짜 예약 장부(Reservation 객체)를 DB에서 찾아옵니다.
//        Reservation reservation = reservationRepository.findById(requestDto.getReservationKey())
//                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        Long incomingCouponId = requestDto.getUserCouponId();
        UserCoupon selectedCoupon = null; // 기본값은 '쿠폰 안 씀(null)'

        // 만약 사용자가 쿠폰을 선택해서 ID가 넘어왔다면?
        if (incomingCouponId != null) {
            // DB에서 해당 번호의 쿠폰 객체를 통째로 찾아옵니다.
            selectedCoupon = userCouponRepository.findById(incomingCouponId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 쿠폰을 찾을 수 없습니다."));

            // ⭐️ 핵심: 결제를 진행하므로 해당 쿠폰의 상태를 '1 (사용 완료)'로 변경합니다!
            // 이 메서드가 끝날 때 JPA가 알아서 DB에 UPDATE 쿼리를 날려줍니다.
            selectedCoupon.setUserCouponStatus(1);
        }

        // 2. 빌더를 써서 새로운 결제 장부를 조립합니다.
        Pay newPayment = Pay.builder()
                //.reservation(reservation)
                .payName(requestDto.getPayName())
                .payAmount(requestDto.getPayAmount())
                .userCoupon(selectedCoupon) // 진짜 쿠폰 객체 연결
                .buyerName(requestDto.getBuyerName())
                .buyerEmail(requestDto.getBuyerEmail())
                .payDelName(requestDto.getPayDelName())
                .payDelCall(requestDto.getPayDelCall())
                .payDelPostcode(requestDto.getPayDelPostcode())
                .payDelAddr(requestDto.getPayDelAddr())
                .build();

        // 3. 완성된 장부를 DB에 저장합니다.
        payRepository.save(newPayment);

        return getLemonSqueezyUrl(newPayment);
    }

    // ==========================================
    // 4. 레몬스퀴지 통신 로직 (기존 그대로 유지)
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
        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

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
                                long webhookAmount) { // ⭐️ 결제 금액을 인자로 받습니다!

        Pay payment = payRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("주문 내역 없음"));

        // 1. 내가 DB에 저장해둔 결제 예정 금액 (단위 주의: 센트(cent) 등)
        long expectedAmount = payment.getPayAmount();

        // 2. 금액 비교 (오차가 있으면 결제 완료로 인정하지 않음!)
        if (expectedAmount != webhookAmount) {
            payment.setPayStatus("FAILED"); // 금액 불일치! 보안 위협으로 간주
            System.err.println("🚨 금액 불일치 경고! DB가격: " + expectedAmount + ", 결제된 가격: " + webhookAmount);
            return; // 여기서 로직 종료
        }

        // 3. 금액이 일치하면 정상 처리
        payment.setPayStatus("PAID");
        payment.setLsOrderId(lsOrderId);
        payment.setCurrency(currency);
        payment.setLsCustomerId(lsCustomerId);
        payment.setReceiptUrl(receiptUrl);
        payment.setLsWebhookEventId(lsWebhookEventId);

        // (payMethod는 임의로 고정하거나 비워둡니다)
        payment.setPayMethod("LemonSqueezy");

        System.out.println("✅ 결제 완료 및 상세 정보 업데이트 성공! 주문번호: " + merchantUid);
        System.out.println("🧾 영수증 주소: " + receiptUrl);
    }
}