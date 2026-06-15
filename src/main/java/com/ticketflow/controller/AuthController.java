package com.ticketflow.controller;

import com.ticketflow.dto.RegisterRequestDto;
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

    @PostMapping("/find-id/verify")
    public String findIdVerify(@RequestParam String name,
                               @RequestParam String email,
                               Model model) {
        // TODO: userService.findByNameAndEmail(name, email) 로 교체
        model.addAttribute("step", "verify");
        return "auth/find_id";
    }

    @PostMapping("/find-id/result")
    public String findIdResult(@RequestParam String code, Model model) {
        // TODO: 실제 인증번호 검증 로직 구현
        model.addAttribute("step", "result");
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
    public String findPasswordVerify(Model model) {
        model.addAttribute("step", "verify");
        return "auth/find_password";
    }

    @RequestMapping(value = "/find-password/reset",
            method = {RequestMethod.GET, RequestMethod.POST})
    public String findPasswordReset(
            @RequestParam(required = false) String newPassword,
            Model model,
            RedirectAttributes rttr) {

        if (newPassword != null && !newPassword.isBlank()) {
            // TODO: userService.resetPassword(token, newPassword)
            rttr.addFlashAttribute("successMessage", "비밀번호가 성공적으로 변경되었습니다.");
            return "redirect:/login";
        }
        model.addAttribute("step", "reset");
        return "auth/find_password";
    }
}