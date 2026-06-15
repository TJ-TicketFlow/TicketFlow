package com.ticketflow.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException e, Model model) {
        log.warn("잘못된 요청: {}", e.getMessage());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("status", 400);
        return "error/4xx";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleIllegalState(IllegalStateException e, Model model) {
        log.warn("상태 오류: {}", e.getMessage());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("status", 409);
        return "error/4xx";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception e, Model model) {
        log.error("서버 오류 발생", e);
        model.addAttribute("errorMessage", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        model.addAttribute("status", 500);
        return "error/5xx";
    }
}
