package com.ticketflow.service;
import com.ticketflow.entity.Pay;
import com.ticketflow.entity.User;
import com.ticketflow.repository.PayRepository; // 👈 레포지토리 임포트
import ai.onnxruntime.*;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CancelPredictionService {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final PayRepository payRepository; // 👈 의존성 주입

    // 생성자를 통해 의존성 주입과 모델 로딩을 동시에 처리합니다.
    public CancelPredictionService(PayRepository payRepository) throws Exception {
        this.payRepository = payRepository;
        this.env = OrtEnvironment.getEnvironment();
        String modelPath = "src/main/resources/ticket_cancellation_best_model.onnx";
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
    }

    public double calculatePerformanceCancelRate(List<Pay> concertPays) throws Exception {
        if (concertPays == null || concertPays.isEmpty()) {
            return 0.0;
        }

        double totalCancelProbability = 0.0;
        int totalTickets = concertPays.size();

        for (Pay pay : concertPays) {
            User buyer = pay.getReservation().getSelectedSeat().getUser();

            // 1. 프로모션(멤버십) 여부
            float isPromotion = (buyer.getMembership() != null && buyer.getMembership().equals("premium")) ? 1.0f : 0.0f;

            // 2. 리드타임
            long daysBetween = ChronoUnit.DAYS.between(
                    pay.getPayCreatedAt().toLocalDate(),
                    pay.getReservation().getSelectedSeat().getConcert().getConcertStartDate()
            );
            float leadTime = (float) Math.max(0, daysBetween);

            // 3. 예매 매수
            float ticketVolume = (float) pay.getReservation().getReservationCount();

            // 4. 유저 과거 취소율 계산
            long totalPays = payRepository.countTotalPaysByUserNo(buyer.getUserNo());
            float userCancelRate = 0.0f;

            if (totalPays > 0) {
                long cancelledPays = payRepository.countCancelledPaysByUserNo(buyer.getUserNo());
                userCancelRate = (float) cancelledPays / totalPays;
            }

            // 5. 총액
            float totalPrice = (float) pay.getPayAmount();

            // 6. 1장당 가격
            float pricePerTicket = totalPrice / ticketVolume;

            // 💡 1단계: 모델에 넣을 숫자 배열 만들기
            float[][] inputData = new float[][]{{
                    isPromotion, leadTime, ticketVolume, userCancelRate, totalPrice, pricePerTicket
            }};

            // 💡 2단계: 빠져있던 부분! 자바 배열을 ONNX 텐서로 변환하고 inputs 맵 만들기
            try (OnnxTensor tensor = OnnxTensor.createTensor(env, inputData)) {
                // 파이썬에서 정해준 이름표인 "float_input"을 붙여줍니다.
                Map<String, OnnxTensor> inputs = Collections.singletonMap("float_input", tensor);

                // 💡 3단계: ONNX 실행 및 결과 추출 (포장지 벗기기 적용됨)
                try (OrtSession.Result results = session.run(inputs)) {
                    // 결과 바구니에서 리스트를 꺼냅니다.
                    List<?> probList = (List<?>) results.get(1).getValue();

                    // 리스트의 첫 번째 항목을 ONNX 전용 상자인 'OnnxMap'으로 받습니다.
                    OnnxMap onnxMap = (OnnxMap) probList.get(0);

                    // ONNX 상자의 포장지를 벗기고 진짜 자바 전용 Map을 꺼냅니다.
                    @SuppressWarnings("unchecked")
                    Map<Long, Float> probMap = (Map<Long, Float>) onnxMap.getValue();

                    // 드디어 '1(취소됨)'의 확률 값을 찾아 더해줍니다!
                    totalCancelProbability += probMap.get(1L);
                }
            } // 텐서 자원을 닫아주는 try-with-resources 괄호입니다.
        }

        return (totalCancelProbability / totalTickets) * 100;
    }
}