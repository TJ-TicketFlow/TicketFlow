package com.ticketflow.controller;

import com.ticketflow.dto.RegisterRequestDto;
import com.ticketflow.service.AuthRecoveryService;
import com.ticketflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthRecoveryService authRecoveryService;

    // ─────────────────────────────────────────
    // 아이디 중복확인 API (register.html fetch 호출)
    // ─────────────────────────────────────────
    @GetMapping("/api/check-userid")
    @ResponseBody
    public Map<String, Object> checkUserId(@RequestParam String userId) {
        boolean dup = userService.isUserIdDuplicated(userId);
        return Map.of("duplicated", dup);
    }

    // ─────────────────────────────────────────
    // 로그인
    // ─────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // ─────────────────────────────────────────
    // 회원가입
    // ─────────────────────────────────────────
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterRequestDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") RegisterRequestDto form,
            BindingResult bindingResult,
            RedirectAttributes rttr,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        if (userService.isUserIdDuplicated(form.getUserId())) {
            bindingResult.rejectValue("userId", "duplicate", "이미 사용 중인 아이디입니다.");
            return "auth/register";
        }
        if (userService.isEmailDuplicated(form.getEmail())) {
            bindingResult.rejectValue("email", "duplicate", "이미 사용 중인 이메일입니다.");
            return "auth/register";
        }

        try {
            userService.register(form);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }

        rttr.addFlashAttribute("successMessage", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/login";
    }

    // ─────────────────────────────────────────
    // 아이디 찾기
    // ─────────────────────────────────────────
    @GetMapping("/find-id")
    public String findId(Model model) {
        model.addAttribute("step", "input");
        return "auth/find_id";
    }

    /**
     * 1단계: 이름+이메일 존재 여부만 빠르게 확인 후 즉시 인증 화면으로 전환.
     * (메일 발송은 여기서 하지 않음 → 화면 전환이 느려지지 않음)
     */
    @PostMapping("/find-id/verify")
    public String findIdVerify(@RequestParam String name,
                               @RequestParam String email,
                               Model model) {
        try {
            authRecoveryService.checkFindIdEligible(name, email);
            model.addAttribute("step", "verify");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
        } catch (IllegalArgumentException e) {
            model.addAttribute("step", "input");
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/find_id";
    }

    /**
     * 인증 화면이 뜬 뒤 JS(fetch)가 호출 — 실제 메일 발송.
     * 발송 성공/실패 여부를 JSON으로 반환해 화면에 "발송되었습니다" 표시.
     */
    @PostMapping("/find-id/send-code")
    @ResponseBody
    public Map<String, Object> findIdSendCode(@RequestParam String name,
                                              @RequestParam String email) {
        try {
            authRecoveryService.sendFindIdCode(name, email);
            return Map.of("success", true, "message", "인증번호를 발송했습니다.");
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("success", false, "message", "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 2단계: 인증번호 검증 후 아이디 노출
     */
    @PostMapping("/find-id/result")
    public String findIdResult(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String code,
                               Model model) {
        try {
            String foundId = authRecoveryService.confirmFindId(name, email, code);
            model.addAttribute("step", "result");
            model.addAttribute("foundId", foundId);
        } catch (IllegalArgumentException e) {
            model.addAttribute("step", "verify");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/find_id";
    }

    // ─────────────────────────────────────────
    // 비밀번호 찾기
    // ─────────────────────────────────────────
    @GetMapping("/find-password")
    public String findPassword(Model model) {
        model.addAttribute("step", "input");
        return "auth/find_password";
    }

    /**
     * 1단계: 아이디+이메일 존재 여부만 빠르게 확인 후 즉시 인증 화면으로 전환.
     */
    @PostMapping("/find-password/verify")
    public String findPasswordVerify(@RequestParam String userId,
                                     @RequestParam String email,
                                     Model model) {
        try {
            authRecoveryService.checkFindPasswordEligible(userId, email);
            model.addAttribute("step", "verify");
            model.addAttribute("userId", userId);
            model.addAttribute("email", email);
        } catch (IllegalArgumentException e) {
            model.addAttribute("step", "input");
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/find_password";
    }

    /**
     * 인증 화면이 뜬 뒤 JS(fetch)가 호출 — 실제 메일 발송.
     */
    @PostMapping("/find-password/send-code")
    @ResponseBody
    public Map<String, Object> findPasswordSendCode(@RequestParam String userId,
                                                    @RequestParam String email) {
        try {
            authRecoveryService.sendFindPasswordCode(userId, email);
            return Map.of("success", true, "message", "인증번호를 발송했습니다.");
        } catch (IllegalArgumentException e) {
            return Map.of("success", false, "message", e.getMessage());
        } catch (Exception e) {
            return Map.of("success", false, "message", "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 2단계: 인증번호 검증 → 통과 시 새 비밀번호 입력 화면(reset)으로 전환.
     * 통과 시점에 1회용 토큰을 발급해 다음 단계로 넘겨준다.
     */
    @PostMapping("/find-password/confirm-code")
    public String findPasswordConfirmCode(@RequestParam String userId,
                                          @RequestParam String email,
                                          @RequestParam String code,
                                          Model model) {
        try {
            String token = authRecoveryService.confirmFindPasswordCode(userId, email, code);
            model.addAttribute("step", "reset");
            model.addAttribute("token", token);
            model.addAttribute("userId", userId);
        } catch (IllegalArgumentException e) {
            model.addAttribute("step", "verify");
            model.addAttribute("userId", userId);
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/find_password";
    }

    /**
     * 3단계: 토큰 검증 + 새 비밀번호로 변경
     */
    @PostMapping("/find-password/reset")
    public String findPasswordReset(@RequestParam String token,
                                    @RequestParam String userId,
                                    @RequestParam String newPassword,
                                    @RequestParam String confirmPassword,
                                    Model model) {
        try {
            authRecoveryService.resetPassword(token, userId, newPassword, confirmPassword);
            model.addAttribute("step", "done");
        } catch (IllegalArgumentException e) {
            model.addAttribute("step", "reset");
            model.addAttribute("token", token);
            model.addAttribute("userId", userId);
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "auth/find_password";
    }
}