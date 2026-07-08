package com.ticketflow.service;

import ai.onnxruntime.OrtException;
import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Stats;
import com.ticketflow.repository.ConcertRepository;
import com.ticketflow.repository.StatsRepository;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcertPredictService {

    private final ConcertRepository concertRepository;
    private final StatsRepository statsRepository;

    private OrtEnvironment env;
    private OrtSession session;

    @PostConstruct
    public void init() {
        try {
            this.env = OrtEnvironment.getEnvironment();
            Resource resource = new ClassPathResource("concert_predict_model.onnx");
            byte[] modelBytes = StreamUtils.copyToByteArray(resource.getInputStream());
            this.session = env.createSession(modelBytes);
            log.info("[ONNX 1.17.1] 모델 로드 완료!");
        } catch (Exception e) {
            log.error("모델 로딩 실패: {}", e.getMessage(), e);
        }
    }

    public float predictSoldOutRate(String concertId) {
        if (session == null) return 77.0f;

        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("공연 없음"));

        Optional<Stats> statsOptional = statsRepository.findTopByConcertOrderByStatsIdDesc(concert);

        float[] features = statsOptional.map(s -> new float[] {
                s.getMaleRatio() != null ? s.getMaleRatio() : 0.0f,
                s.getFemaleRatio() != null ? s.getFemaleRatio() : 0.0f,
                s.getAge10sRatio() != null ? s.getAge10sRatio() : 0.0f,
                s.getAge20sRatio() != null ? s.getAge20sRatio() : 0.0f,
                s.getAge30sRatio() != null ? s.getAge30sRatio() : 0.0f,
                s.getAge40sRatio() != null ? s.getAge40sRatio() : 0.0f,
                s.getAge50sRatio() != null ? s.getAge50sRatio() : 0.0f
        }).orElse(new float[] { 50.0f, 50.0f, 10.0f, 30.0f, 30.0f, 20.0f, 10.0f });

        try {
            long[] shape = new long[]{1, 7};
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(features), shape)) {
                String inputName = session.getInputNames().iterator().next();

                try (OrtSession.Result results = session.run(Collections.singletonMap(inputName, inputTensor))) {
                    if (results == null || results.size() == 0) return 77.0f;

                    Object rawValue = results.get(0).getValue();
                    log.info("AI 반환 객체 타입: {}", rawValue.getClass().getName());

                    float predictedRate;

                    if (rawValue instanceof float[][]) {
                        predictedRate = ((float[][]) rawValue)[0][0];
                    } else if (rawValue instanceof Object[]) {
                        Object[] objArray = (Object[]) rawValue;
                        Object inner = objArray[0];
                        if (inner instanceof float[]) {
                            predictedRate = ((float[]) inner)[0];
                        } else if (inner instanceof Float) {
                            predictedRate = (Float) inner;
                        } else {
                            predictedRate = Float.parseFloat(inner.toString());
                        }
                    } else if (rawValue instanceof float[]) {
                        predictedRate = ((float[]) rawValue)[0];
                    } else if (rawValue instanceof Float) {
                        predictedRate = (Float) rawValue;
                    } else {
                        predictedRate = 77.0f;
                    }

                    log.info("▶ AI 최종 예측 예매율: {}%", predictedRate);
                    return predictedRate;
                }
            }
        } catch (Exception e) {
            log.error("AI 추론 오류: {}", e.getMessage());
            return 77.0f;
        }
    }

    @PreDestroy
    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception e) { log.error("리소스 해제 오류: {}", e.getMessage()); }
    }
}