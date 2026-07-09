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


    private final Map<String, LocalDateTime> verifiedEmails = new ConcurrentHashMap<>();

    // 인증 성공 후 회원가입을 완료할 때까지 유효 시간 (이 시간 안에 가입을 마쳐야 함)
    private static final long VERIFIED_VALID_MINUTES = 30;

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

        verifiedEmails.put(email, LocalDateTime.now().plusMinutes(VERIFIED_VALID_MINUTES));
        return true;
    }

    /**
     * 이 이메일이 최근에 인증번호 검증을 통과했는지 서버 기준으로 확인합니다.
     */
    public boolean isVerified(String email) {
        LocalDateTime expiry = verifiedEmails.get(email);
        if (expiry == null) return false;
        if (LocalDateTime.now().isAfter(expiry)) {
            verifiedEmails.remove(email);
            return false;
        }
        return true;
    }

    /**
     * 회원가입이 완료된 뒤에는 인증 상태를 다시 쓸 수 없도록 소모(삭제)합니다.
     */
    public void consumeVerification(String email) {
        verifiedEmails.remove(email);
    }

    private String generateCode() {
        SecureRandom rnd = new SecureRandom();
        return String.format("%06d", rnd.nextInt(1_000_000));
    }

    private record VerificationEntry(String code, LocalDateTime expiry) {
    }
}