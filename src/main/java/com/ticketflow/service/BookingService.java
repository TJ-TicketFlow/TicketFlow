package com.ticketflow.service;

import com.ticketflow.dto.BookingRequestDto;
import com.ticketflow.entity.Pay;
import com.ticketflow.entity.Reservation;
import com.ticketflow.repository.PayRepository;
import com.ticketflow.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class BookingService {
    private final PayRepository payRepository;
    private final ReservationRepository reservationRepository; // Reservation(예약) 테이블 조종기 추가

    @Value("${lemonsqueezy.api-key2}")
    private String apiKey;

    @Value("${lemonsqueezy.store-id2}")
    private String storeId;

    private String variantId = "1792135";

    @Transactional
    public String createTemporaryPayment(BookingRequestDto requestDto) {

        // 1. DTO에 있는 예약 번호(숫자)를 가지고, 진짜 예약 장부(Reservation 객체)를 DB에서 찾아옵니다.
//        Reservation reservation = reservationRepository.findById(requestDto.getReservationKey())
//                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        // 2. 빌더를 써서 새로운 결제 장부를 조립합니다. (순서는 내 맘대로!)
        Pay newPayment = Pay.builder()
                //.reservation(reservation) // ⭐️ 찾아온 예약 장부 전체를 통째로 넣습니다.
                .payName(requestDto.getPayName())
                .payAmount(requestDto.getPayAmount())
                .buyerName(requestDto.getBuyerName())
                .buyerEmail(requestDto.getBuyerEmail())
                .payDelName(requestDto.getPayDelName())
                .payDelCall(requestDto.getPayDelCall())
                .payDelPostcode(requestDto.getPayDelPostcode())
                .payDelAddr(requestDto.getPayDelAddr())
                // merchantUid, payStatus, createdAt 등은 적지 않습니다! (자동으로 들어감)
                .build();

        // 3. 완성된 장부를 DB에 저장합니다.
        payRepository.save(newPayment);

        return getLemonSqueezyUrl(newPayment);
    }

    private String getLemonSqueezyUrl(Pay payment) {
        RestTemplate restTemplate = new RestTemplate(); // 우체부 소환!

        // [편지 봉투 설정] 헤더에 인증키(API Key)와 데이터 형식을 적어줍니다.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/vnd.api+json");
        headers.set("Content-Type", "application/vnd.api+json");

        // [편지 내용물 작성] 레몬스퀴지가 원하는 특별한 모양(JSON)으로 박스를 포장합니다.
        // Map은 자바에서 데이터를 "키: 값" 형태로 묶어주는 상자입니다.
        Map<String, Object> body = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        Map<String, Object> checkoutData = new HashMap<>();
        Map<String, Object> custom = new HashMap<>();
        Map<String, Object> relationships = new HashMap<>();

        // 나중에 결제 완료(Webhook) 알림이 올 때, 이게 누구 결제인지 알아야 하므로
        // 주문번호(merchantUid)를 custom(사용자 정의) 데이터에 몰래 끼워 넣습니다.
        custom.put("merchant_uid", payment.getMerchantUid());
        checkoutData.put("custom", custom);

        // (선택사항) 결제창에 구매자 이름과 이메일을 미리 채워주고 싶다면 여기에 넣습니다.
        checkoutData.put("name", payment.getBuyerName());
        checkoutData.put("email", payment.getBuyerEmail());

        attributes.put("checkout_data", checkoutData);
        attributes.put("custom_price", payment.getPayAmount());

        // 상점과 상품 정보를 연결합니다.
        relationships.put("store", Map.of("data", Map.of("type", "stores", "id", storeId)));
        relationships.put("variant", Map.of("data", Map.of("type", "variants", "id", variantId)));

        // 준비된 박스들을 가장 큰 data 상자에 차곡차곡 넣습니다.
        data.put("type", "checkouts");
        data.put("attributes", attributes);
        data.put("relationships", relationships);
        body.put("data", data);

        // [우체국에 접수] 봉투(headers)와 내용물(body)을 하나로 합칩니다.
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // [발송 및 답장 받기] POST 방식으로 레몬스퀴지 주소로 보내고, 답장(Map)을 받습니다!
        String apiUrl = "https://api.lemonsqueezy.com/v1/checkouts";
        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        // [답장에서 URL만 쏙 빼내기]
        // 레몬스퀴지가 준 복잡한 답장 속에서 data -> attributes -> url을 찾아 꺼냅니다.
        Map<String, Object> responseBody = response.getBody();
        Map<String, Object> responseData = (Map<String, Object>) responseBody.get("data");
        Map<String, Object> responseAttributes = (Map<String, Object>) responseData.get("attributes");

        // 드디어 진짜 결제창 URL 획득!!
        String realCheckoutUrl = (String) responseAttributes.get("url");

        return realCheckoutUrl;
    }
}
