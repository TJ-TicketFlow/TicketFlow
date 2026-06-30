package com.ticketflow;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication()
public class TicketflowApplication {

    @PostConstruct
    public void setTimezone() {
        // 자바(JVM) 전체의 기본 시계를 무조건 '아시아/서울'로 강제 고정합니다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.out.println("✅ 서버 시간 설정 완료: " + new java.util.Date());
    }

    public static void main(String[] args) {
        SpringApplication.run(TicketflowApplication.class, args);
    }
}
