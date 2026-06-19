package com.ticketflow.controller;

import com.ticketflow.dto.RegisterRequestDto;
import com.ticketflow.entity.User;
import com.ticketflow.service.EmailVerificationService;
import com.ticketflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

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
            RedirectAttributes rttr) {

        // 비밀번호 확인 일치 여부 (클라이언트 JS가 우회되는 경우에 대비한 서버단 검증)
        if (form.getConfirmPassword() != null
                && form.getPassword() != null
                && !form.getConfirmPassword().equals(form.getPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "비밀번호가 일치하지 않습니다.");
        }

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
        // 이메일 인증 완료 여부 (클라이언트 emailVerified 플래그만으로는 우회 가능하므로 서버에서 재확인)
        if (!emailVerificationService.isVerified(form.getEmail())) {
            bindingResult.reject("emailNotVerified", "이메일 인증을 완료해주세요.");
            return "auth/register";
        }

        try {
            userService.register(form);
        } catch (IllegalArgumentException e) {
            bindingResult.reject("registerError", e.getMessage());
            return "auth/register";
        }

        emailVerificationService.clearVerified(form.getEmail());
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

    @PostMapping("/find-id/verify")
    public String findIdVerify(@RequestParam String name,
                               @RequestParam String email,
                               Model model) {
        Optional<User> user = userService.findByNameAndEmail(name, email);
        if (user.isEmpty()) {
            model.addAttribute("step", "input");
            model.addAttribute("errorMessage", "입력하신 정보와 일치하는 회원이 없습니다.");
            return "auth/find_id";
        }

        emailVerificationService.sendCode(email);
        model.addAttribute("step", "verify");
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        return "auth/find_id";
    }

    @PostMapping("/find-id/result")
    public String findIdResult(@RequestParam String name,
                               @RequestParam String email,
                               @RequestParam String code,
                               Model model) {
        if (!emailVerificationService.verify(email, code)) {
            model.addAttribute("step", "verify");
            model.addAttribute("name", name);
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", "인증번호가 올바르지 않거나 만료되었습니다.");
            return "auth/find_id";
        }

        Optional<User> user = userService.findByNameAndEmail(name, email);
        if (user.isEmpty()) {
            // 인증 코드 발송 이후 정보가 바뀌는 등 극히 예외적인 경우에 대한 안전장치
            model.addAttribute("step", "input");
            model.addAttribute("errorMessage", "회원 정보를 찾을 수 없습니다. 다시 시도해주세요.");
            return "auth/find_id";
        }

        model.addAttribute("step", "result");
        model.addAttribute("foundId", user.get().getUserId());
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

    @PostMapping("/find-password/verify")
    public String findPasswordVerify(@RequestParam String userId,
                                     @RequestParam String email,
                                     Model model) {
        Optional<User> user = userService.findByUserIdAndEmail(userId, email);
        if (user.isEmpty()) {
            model.addAttribute("step", "input");
            model.addAttribute("errorMessage", "입력하신 정보와 일치하는 회원이 없습니다.");
            return "auth/find_password";
        }

        emailVerificationService.sendCode(email);
        model.addAttribute("step", "verify");
        model.addAttribute("userId", userId);
        model.addAttribute("email", email);
        return "auth/find_password";
    }

    @PostMapping("/find-password/reset")
    public String findPasswordReset(@RequestParam String userId,
                                    @RequestParam String email,
                                    @RequestParam String code,
                                    @RequestParam String newPassword,
                                    @RequestParam String confirmPassword,
                                    Model model) {

        if (newPassword == null || newPassword.length() < 8) {
            model.addAttribute("step", "verify");
            model.addAttribute("userId", userId);
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", "비밀번호는 8자 이상이어야 합니다.");
            return "auth/find_password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("step", "verify");
            model.addAttribute("userId", userId);
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
            return "auth/find_password";
        }
        if (!emailVerificationService.verify(email, code)) {
            model.addAttribute("step", "verify");
            model.addAttribute("userId", userId);
            model.addAttribute("email", email);
            model.addAttribute("errorMessage", "인증번호가 올바르지 않거나 만료되었습니다.");
            return "auth/find_password";
        }

        try {
            userService.resetPassword(userId, newPassword);
        } catch (IllegalArgumentException e) {
            model.addAttribute("step", "input");
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/find_password";
        }

        model.addAttribute("step", "done");
        return "auth/find_password";
    }
}
