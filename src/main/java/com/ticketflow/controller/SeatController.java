package com.ticketflow.controller;

import com.ticketflow.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seat")
public class SeatController {

    private final SeatService seatService;


    @GetMapping("/{concertId}")
    public String seatPage(

            @PathVariable String concertId,
            Model model

    ){

        model.addAttribute(
                "concertId",
                concertId
        );


        return "concert/seatmap";

    }

    // 1. 좌석 선택
    @PostMapping("/select")
    @ResponseBody
    public ResponseEntity<?> selectSeat(
            @RequestParam Long concertId,
            @RequestParam String seatId,
            @RequestParam Long userNo
    ) {

        seatService.selectSeat(
                concertId,
                seatId,
                userNo
        );

        return ResponseEntity.ok("선택 완료");
    }


    // 2. 요금 자동 계산
    @GetMapping("/select/pay")
    @ResponseBody
    public ResponseEntity<Integer> calculatePrice(
            @RequestParam String concertId,
            @RequestParam String seatId
    ) {

        int total =
                seatService.calculatePrice(
                        concertId,
                        seatId
                );

        return ResponseEntity.ok(total);
    }


    // 3. 공연장 좌석현황 업데이트
    @GetMapping("/status/{concertId}")
    @ResponseBody
    public ResponseEntity<?> getSeatStatus(
            @PathVariable Long concertId
    ) {

        return ResponseEntity.ok(
                seatService.getSeatStatus(
                        concertId
                )
        );
    }


    // 4. 좌석 중복 선택 방지
    @GetMapping("/selected")
    @ResponseBody
    public ResponseEntity<Boolean> checkSelected(
            @RequestParam Long concertId,
            @RequestParam String seatId
    ) {

        boolean selected =
                seatService.isSelected(
                        concertId,
                        seatId
                );

        return ResponseEntity.ok(
                selected
        );
    }


    // 5. 좌석 선택 취소
    @DeleteMapping("/select")
    @ResponseBody
    public ResponseEntity<?> cancelSeat(
            @RequestParam Long concertId,
            @RequestParam String seatId,
            @RequestParam Long userNo
    ) {

        seatService.cancelSeat(
                concertId,
                seatId,
                userNo
        );

        return ResponseEntity.ok(
                "취소 완료"
        );
    }


    // 6. 잔여 좌석수 표시
    @GetMapping("/remain")
    @ResponseBody
    public ResponseEntity<Integer> remainSeat(
            @RequestParam Long concertId
    ) {

        return ResponseEntity.ok(
                seatService.getRemainSeat(
                        concertId
                )
        );
    }


    // 7. 총 예매금액 + 선택좌석 전달
    @PostMapping("/payment")
    @ResponseBody
    public ResponseEntity<?> sendPaymentInfo(
            @RequestBody Map<String,Object> data
    ) {

        seatService.sendPaymentInfo(
                data
        );

        return ResponseEntity.ok(
                "결제 데이터 전달 완료"
        );
    }

}