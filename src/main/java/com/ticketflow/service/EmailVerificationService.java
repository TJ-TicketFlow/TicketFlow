package com.ticketflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final JavaMailSender mailSender;

    // 인증번호 저장 (email → {code, expiry})
    private final Map<String, VerificationEntry> store = new ConcurrentHashMap<>();

    /**
     * 6자리 인증번호 생성 후 이메일 발송
     */
    public void sendCode(String email) {
        String code = generateCode();
        store.put(email, new VerificationEntry(code, LocalDateTime.now().plusMinutes(3)));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("[티켓플로우] 이메일 인증번호");
        msg.setText(
                "안녕하세요, 티켓플로우입니다.\n\n" +
                        "이메일 인증번호: " + code + "\n\n" +
                        "인증번호는 3분간 유효합니다.\n" +
                        "본인이 요청하지 않은 경우 이 메일을 무시하세요."
        );
        mailSender.send(msg);
        log.info("인증번호 발송: {} → {}", email, code);
    }

    /**
     * 인증번호 검증
     */
    public boolean verify(String email, String code) {
        VerificationEntry entry = store.get(email);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiry())) {
            store.remove(email);
            return false;
        }
        if (!entry.code().equals(code)) return false;
        store.remove(email); // 사용 후 삭제
        return true;
    }

    private String generateCode() {
        SecureRandom rnd = new SecureRandom();
        return String.format("%06d", rnd.nextInt(1_000_000));
    }

    private record VerificationEntry(String code, LocalDateTime expiry) {}
}