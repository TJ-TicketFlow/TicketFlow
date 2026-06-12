package com.ticketflow.controller;

import com.ticketflow.dto.RegisterRequestDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String user_id, @RequestParam String password,
                        HttpSession session, RedirectAttributes rttr) {
        if (DummyData.DUMMY_USER.get("id").equals(user_id) &&
                DummyData.DUMMY_USER.get("password").equals(password)) {
            session.setAttribute("logged_in", true);
            session.setAttribute("user_id", user_id);
            return "redirect:/mypage/benefits";
        } else {
            rttr.addFlashAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "redirect:/login?error";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterRequestDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(RedirectAttributes rttr) {
        rttr.addFlashAttribute("message", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/login";
    }

    @GetMapping("/find-id")
    public String findId(Model model) {
        model.addAttribute("step", "input");
        return "auth/find_id";
    }

    @PostMapping("/find-id/verify")
    public String findIdVerify(@RequestParam String name, @RequestParam String email,
                               HttpSession session, Model model) {
        if (DummyData.DUMMY_USER.get("name").equals(name) &&
                DummyData.DUMMY_USER.get("email").equals(email)) {
            session.setAttribute("find_id_verified", true);
            model.addAttribute("step", "verify");
            return "auth/find_id";
        } else {
            model.addAttribute("error", "입력하신 정보와 일치하는 회원이 없습니다.");
            model.addAttribute("step", "input");
            return "auth/find_id";
        }
    }

    @PostMapping("/find-id/result")
    public String findIdResult(@RequestParam String code, HttpSession session, Model model) {
        if ("123456".equals(code) || Boolean.TRUE.equals(session.getAttribute("find_id_verified"))) {
            model.addAttribute("step", "result");
            model.addAttribute("foundId", DummyData.DUMMY_USER.get("id"));
            return "auth/find_id";
        }
        model.addAttribute("error", "인증번호가 올바르지 않습니다.");
        model.addAttribute("step", "verify");
        return "auth/find_id";
    }

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

    @RequestMapping(value = "/find-password/reset", method = {RequestMethod.GET, RequestMethod.POST})
    public String findPasswordReset(Model model, RedirectAttributes rttr,
                                    @RequestParam(required = false) String newPassword) {
        if (newPassword != null) {
            rttr.addFlashAttribute("message", "비밀번호가 성공적으로 변경되었습니다.");
            return "redirect:/login";
        }
        model.addAttribute("step", "reset");
        return "auth/find_password";
    }
}
