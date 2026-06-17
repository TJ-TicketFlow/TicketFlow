package com.ticketflow.controller;


import com.ticketflow.entity.Seat;
import com.ticketflow.service.SeatService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/seat")
public class SeatController {


    private final SeatService seatService;


    /*
        1. 좌석 조회


        GET
        /seat/{concertId}


        예:
        /seat/PF277688

    */
    @GetMapping("/api/{concertId}")
    public ResponseEntity<List<Seat>> getSeats(
            @PathVariable String concertId
    ) {


        List<Seat> seats =
                seatService
                        .getSeats(
                                concertId
                        );


        return ResponseEntity
                .ok(seats);


    }


    /*
        2. 좌석 선택


        POST
        /seat/select


        body

        {
            "seatId":"A1",
            "userNo":1
        }

    */
    @PostMapping("/select")
    public ResponseEntity<String> selectSeat(
            @RequestBody Map<String, Object> data
    ) {


        String seatId =
                data.get("seatId")
                        .toString();


        Long userNo =
                Long.valueOf(
                        data.get("userNo")
                                .toString()
                );


        seatService
                .selectSeat(
                        seatId,
                        userNo
                );


        return ResponseEntity
                .ok(
                        "좌석 선택 완료"
                );


    }


    /*
        3. 좌석 취소


        POST
        /seat/cancel


        body

        {
            "seatId":"A1"
        }

    */
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelSeat(
            @RequestBody Map<String, Object> data
    ) {


        String seatId =
                data.get("seatId")
                        .toString();


        seatService
                .cancelSeat(
                        seatId
                );


        return ResponseEntity
                .ok(
                        "좌석 취소 완료"
                );


    }


    /*
        4. 좌석 상태 변경


        PUT
        /seat/status


        body

        {
          "seatId":"A1",
          "status":0
        }

    */
    @PutMapping("/status")
    public ResponseEntity<String> updateSeatStatus(
            @RequestBody Map<String, Object> data
    ) {


        String seatId =
                data.get("seatId")
                        .toString();


        Short status =
                Short.valueOf(
                        data.get("status")
                                .toString()
                );


        seatService
                .updateSeatStatus(
                        seatId,
                        status
                );


        return ResponseEntity
                .ok(
                        "상태 변경 완료"
                );


    }


    /*
        5. 가격 조회


        GET

        /seat/price/{concertId}/{seatClass}


        예:

        /seat/price/PF277688/VIP

    */
    @GetMapping(
            "/price/{concertId}/{seatClass}"
    )
    public ResponseEntity<String> getPrice(
            @PathVariable String concertId,
            @PathVariable String seatClass
    ) {


        String price =
                seatService
                        .calculatePrice(
                                concertId,
                                seatClass
                        );


        return ResponseEntity
                .ok(
                        price
                );


    }


}