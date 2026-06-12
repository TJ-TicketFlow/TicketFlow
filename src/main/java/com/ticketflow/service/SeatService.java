package com.ticketflow.service;

import com.ticketflow.entity.Concert;
import com.ticketflow.repository.ConcertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final ConcertRepository concertRepository;


    /*
        공연 조회
     */
    public Concert getConcert(
            Long concertId
    ) {

        return concertRepository
                .findById(concertId)
                .orElseThrow(
                        () ->
                                new RuntimeException(
                                        "공연 없음"
                                )
                );

    }


    /*
        공연ID → 좌석배치도 결정
     */
    public String getSeatMapType(
            Long concertId
    ) {

        Concert concert =
                getConcert(
                        concertId
                );

        Long hallId =
                concert
                        .getHall()
                        .getHallId();



        if (hallId == 1L) {

            // 선착순
            return "FIRST_COME";

        }

        if (hallId == 2L) {

            // 배치도 A
            return "LAYOUT_A";

        }

        return "LAYOUT_B";

    }


    /*
        실제 좌석 배치도 반환
     */
    public List<List<String>>
    getSeatLayout(
            Long concertId
    ) {

        String seatMap =
                getSeatMapType(
                        concertId
                );



        if (
                seatMap.equals(
                        "LAYOUT_A"
                )
        ) {

            return List.of(

                    List.of(
                            "A",
                            "A",
                            "A",
                            "N"
                    ),

                    List.of(
                            "A",
                            "A",
                            "A",
                            "A"
                    ),

                    List.of(
                            "A",
                            "A",
                            "A",
                            "A"
                    )

            );

        }



        if (
                seatMap.equals(
                        "LAYOUT_B"
                )
        ) {

            return List.of(

                    List.of(
                            "A",
                            "A"
                    ),

                    List.of(
                            "A",
                            "N"
                    ),

                    List.of(
                            "A",
                            "A"
                    )

            );

        }



        // 선착순

        return List.of();

    }



    /*
        좌석 선택
     */
    public void selectSeat(
            Long concertId,
            String seatId,
            Long userNo
    ) {

    }



    /*
        요금 계산
     */
    public int calculatePrice(
            Long concertId,
            String seatId
    ) {

        return 150000;

    }



    /*
        좌석 상태
     */
    public Object getSeatStatus(
            Long concertId
    ) {

        return List.of(
                Map.of(
                        "seatId",
                        "A1",
                        "state",
                        "RESERVED"
                )
        );

    }



    public boolean isSelected(
            Long concertId,
            String seatId
    ) {

        return false;

    }



    public void cancelSeat(
            Long concertId,
            String seatId,
            Long userNo
    ) {

    }



    public int getRemainSeat(
            Long concertId
    ) {

        return 100;

    }



    public void sendPaymentInfo(
            Map<String,Object> data
    ) {

    }

}