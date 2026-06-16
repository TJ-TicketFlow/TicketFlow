package com.ticketflow.controller;

import com.ticketflow.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    /**
     * POST /register/send-code
     * 이메일로 인증번호 발송
     */
    @PostMapping("/register/send-code")
    public ResponseEntity<Map<String, Object>> sendCode(@RequestParam String email) {
        try {
            emailVerificationService.sendCode(email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증번호를 발송했습니다."
            ));
        } catch (Exception e) {
            log.error("인증번호 발송 실패: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요."
            ));
        }
    }

    /**
     * POST /register/verify-code
     * 인증번호 검증
     */
    @PostMapping("/register/verify-code")
    public ResponseEntity<Map<String, Object>> verifyCode(
            @RequestParam String email,
            @RequestParam String code) {

        boolean ok = emailVerificationService.verify(email, code);
        if (ok) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증되었습니다."
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "인증번호가 올바르지 않거나 만료되었습니다."
            ));
        }
    }
}