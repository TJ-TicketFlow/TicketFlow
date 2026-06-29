package com.ticketflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 좌석 실시간 알림(좌석 선점/취소, 매진 공지)을 위한 STOMP 기반 WebSocket 설정.
 *
 * - 클라이언트 접속 주소(핸드셰이크): /ws-seat (SockJS 폴백 포함)
 * - 서버 → 클라이언트 브로드캐스트 prefix: /topic
 *   예) /topic/seat/{concertId}        -> 특정 공연의 좌석 잠금/해제 이벤트
 *       /topic/concert/{concertId}/notice -> 매진(마감) 등 공지 이벤트
 *
 * 별도의 메시지 브로커(RabbitMQ 등) 없이 동작하는 인메모리 SimpleBroker를 사용합니다.
 * (단일 인스턴스 배포 기준. 추후 서버를 여러 대로 스케일아웃하면 RabbitMQ/Redis 같은
 *  외부 브로커로 교체하는 것을 고려해야 합니다.)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트로 보내는 메시지는 전부 /topic 으로 시작하는 주소를 사용
        registry.enableSimpleBroker("/topic");
        // 클라이언트 -> 서버로 보내는 메시지가 있다면 /app 으로 시작 (지금은 사용하지 않지만 확장 대비로 열어둠)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-seat")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 구형 브라우저/네트워크 환경 대비 SockJS 폴백 지원
    }
}
