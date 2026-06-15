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



    /*
        좌석 배치도 페이지 이동

        예:
        /seat/1

        1번 공연 좌석 페이지
    */
    @GetMapping("/{concertId}")
    public String seatMap(

            @PathVariable Long concertId,

            Model model

    ){


        // 공연 ID 전달
        model.addAttribute(
                "concertId",
                concertId
        );



        // 공연장별 배치도 이름
        model.addAttribute(
                "seatMapType",
                seatService.getSeatMapType(
                        concertId
                )
        );



        // 실제 좌석 배열
        model.addAttribute(
                "layout",
                seatService.getSeatLayout(
                        concertId
                )
        );



        return "concert/seatmap";

    }





    // ==========================
    // 기존 API
    // ==========================



    // 좌석 선택
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


        return ResponseEntity.ok(
                "선택 완료"
        );

    }




    // 가격 계산
    @GetMapping("/select/pay")
    @ResponseBody
    public ResponseEntity<Integer> calculatePrice(

            @RequestParam Long concertId,
            @RequestParam String seatId

    ) {


        int total =
                seatService.calculatePrice(
                        concertId,
                        seatId
                );


        return ResponseEntity.ok(
                total
        );

    }




    // 좌석 상태 조회
    @GetMapping("/status/{concertId}")
    @ResponseBody
    public ResponseEntity<?> getSeatStatus(

            @PathVariable Long concertId

    ){


        return ResponseEntity.ok(

                seatService.getSeatStatus(
                        concertId
                )

        );

    }





    // 중복 선택 확인
    @GetMapping("/selected")
    @ResponseBody
    public ResponseEntity<Boolean> checkSelected(

            @RequestParam Long concertId,
            @RequestParam String seatId

    ){


        return ResponseEntity.ok(

                seatService.isSelected(
                        concertId,
                        seatId
                )

        );

    }





    // 선택 취소
    @DeleteMapping("/select")
    @ResponseBody
    public ResponseEntity<?> cancelSeat(

            @RequestParam Long concertId,
            @RequestParam String seatId,
            @RequestParam Long userNo

    ){


        seatService.cancelSeat(
                concertId,
                seatId,
                userNo
        );


        return ResponseEntity.ok(
                "취소 완료"
        );

    }





    // 남은 좌석
    @GetMapping("/remain")
    @ResponseBody
    public ResponseEntity<Integer> remainSeat(

            @RequestParam Long concertId

    ){


        return ResponseEntity.ok(

                seatService.getRemainSeat(
                        concertId
                )

        );

    }





    // 결제 전달
    @PostMapping("/payment")
    @ResponseBody
    public ResponseEntity<?> sendPaymentInfo(

            @RequestBody Map<String,Object> data

    ){


        seatService.sendPaymentInfo(
                data
        );


        return ResponseEntity.ok(
                "결제 데이터 전달 완료"
        );

    }


}