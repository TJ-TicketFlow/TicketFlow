package com.ticketflow.service;

import com.ticketflow.entity.User;
import com.ticketflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthRecoveryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    // 비밀번호 재설정용 1회용 토큰 저장소 (token → {userId, expiry})
    private final Map<String, ResetToken> resetTokenStore = new ConcurrentHashMap<>();

    // ───────────────────────────────────────────────
    // 아이디 찾기
    // ───────────────────────────────────────────────

    /**
     * 이름 + 이메일이 일치하는 회원인지 빠르게 확인만 한다 (메일 발송 X).
     * 인증 화면으로 즉시 전환하기 위한 용도 — DB 조회만 하므로 빠름.
     * @throws IllegalArgumentException 일치하는 회원이 없을 때
     */
    @Transactional(readOnly = true)
    public void checkFindIdEligible(String name, String email) {
        if (!userRepository.existsByUserNameAndUserEmail(name, email)) {
            throw new IllegalArgumentException("입력하신 정보와 일치하는 회원이 없습니다.");
        }
    }

    /**
     * 이름 + 이메일 재확인 후 인증번호 발송.
     * 인증 화면이 뜬 뒤 JS(AJAX)에서 비동기로 호출 — 화면 전환과 메일 발송을 분리.
     * @throws IllegalArgumentException 일치하는 회원이 없을 때
     */
    @Transactional(readOnly = true)
    public void sendFindIdCode(String name, String email) {
        checkFindIdEligible(name, email);
        emailVerificationService.sendCode(email);
    }

    /**
     * 인증번호를 검증하고, 통과하면 이름+이메일로 아이디를 반환한다.
     * @throws IllegalArgumentException 인증번호 불일치 또는 회원 없음
     */
    @Transactional(readOnly = true)
    public String confirmFindId(String name, String email, String code) {
        if (!emailVerificationService.verify(email, code)) {
            throw new IllegalArgumentException("인증번호가 올바르지 않거나 만료되었습니다.");
        }
        User user = userRepository.findByUserNameAndUserEmail(name, email)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원 정보가 없습니다."));
        return user.getUserId();
    }

    // ───────────────────────────────────────────────
    // 비밀번호 찾기
    // ───────────────────────────────────────────────

    /**
     * 아이디 + 이메일이 일치하는 회원인지 빠르게 확인만 한다 (메일 발송 X).
     * @throws IllegalArgumentException 일치하는 회원이 없을 때
     */
    @Transactional(readOnly = true)
    public void checkFindPasswordEligible(String userId, String email) {
        if (!userRepository.existsByUserIdAndUserEmail(userId, email)) {
            throw new IllegalArgumentException("입력하신 정보와 일치하는 회원이 없습니다.");
        }
    }

    /**
     * 아이디 + 이메일 재확인 후 인증번호 발송.
     * 인증 화면이 뜬 뒤 JS(AJAX)에서 비동기로 호출.
     * @throws IllegalArgumentException 일치하는 회원이 없을 때
     */
    @Transactional(readOnly = true)
    public void sendFindPasswordCode(String userId, String email) {
        checkFindPasswordEligible(userId, email);
        emailVerificationService.sendCode(email);
    }

    /**
     * 인증번호를 검증하고, 통과하면 비밀번호 재설정용 1회용 토큰을 발급한다.
     * 발급된 토큰은 다음 단계(새 비밀번호 입력)에서만 사용 가능하다.
     * @throws IllegalArgumentException 인증번호 불일치 시
     */
    public String confirmFindPasswordCode(String userId, String email, String code) {
        if (!emailVerificationService.verify(email, code)) {
            throw new IllegalArgumentException("인증번호가 올바르지 않거나 만료되었습니다.");
        }
        String token = generateToken();
        resetTokenStore.put(token, new ResetToken(userId, LocalDateTime.now().plusMinutes(10)));
        return token;
    }

    /**
     * 발급된 토큰을 검증하고 새 비밀번호로 변경한다.
     * @throws IllegalArgumentException 토큰 불일치/만료, 비밀번호 형식 오류, 회원 없음
     */
    @Transactional
    public void resetPassword(String token, String userId, String newPassword, String confirmPassword) {
        ResetToken entry = resetTokenStore.get(token);
        if (entry == null || !entry.userId().equals(userId)
                || LocalDateTime.now().isAfter(entry.expiry())) {
            resetTokenStore.remove(token);
            throw new IllegalArgumentException("인증이 만료되었습니다. 처음부터 다시 시도해주세요.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setUserPw(passwordEncoder.encode(newPassword));
        resetTokenStore.remove(token);
    }

    private String generateToken() {
        SecureRandom rnd = new SecureRandom();
        byte[] bytes = new byte[24];
        rnd.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private record ResetToken(String userId, LocalDateTime expiry) {}
}