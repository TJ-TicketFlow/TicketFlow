package com.ticketflow.service;


import com.ticketflow.entity.Concert;
import com.ticketflow.entity.Seat;
import com.ticketflow.repository.ConcertRepository;
import com.ticketflow.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;



@Service
@RequiredArgsConstructor
@Transactional
public class SeatService {



    private final SeatRepository seatRepository;

    private final ConcertRepository concertRepository;

    /*
    공연 가격정보 기반 좌석 타입 판단
*/
    public String getSeatLayoutType(String concertId){


        Concert concert =
                concertRepository
                        .findById(concertId)
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "공연 없음"
                                        )
                        );


        String priceInfo =
                concert.getConcertPriceInfo();



    /*
       예:
       VIP 200000,R 150000,S 100000

       또는

       스탠딩 99000
    */

        if (priceInfo.contains("스탠딩"))
                return "SEAT_B";
            else
                return "SEAT_A";


    }


    /*
        1. 좌석 조회
    */
    @Transactional(readOnly = true)
    public List<Seat> getSeats(
            String concertId
    ){


        return seatRepository
                .findByConcert_ConcertId(
                        concertId
                );


    }


    /*
        2. 좌석 선택
    */
    public void selectSeat(
            String seatId,
            Long userNo
    ){



        Seat seat =
                seatRepository
                        .findById(
                                seatId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "좌석 없음"
                                        )
                        );



        /*
            1 = 가능
            0 = 불가능
        */
        if(
                seat.getSeatStatus() == 0
        ){

            throw new RuntimeException(
                    "이미 선택된 좌석"
            );

        }



        // 좌석 선점
        seat.setSeatStatus(
                (short)0
        );



        seatRepository.save(
                seat
        );


    }









    /*
        3. 좌석 취소
    */
    public void cancelSeat(
            String seatId
    ){



        Seat seat =
                seatRepository
                        .findById(
                                seatId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "좌석 없음"
                                        )
                        );



        seat.setSeatStatus(
                (short)1
        );



        seatRepository.save(
                seat
        );


    }

    /*
        4. 예약 상태 변경
    */
    public void updateSeatStatus(
            String seatId,
            Short status
    ){



        Seat seat =
                seatRepository
                        .findById(
                                seatId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "좌석 없음"
                                        )
                        );



        seat.setSeatStatus(
                status
        );


        seatRepository.save(
                seat
        );


    }

    /*
    5. 가격 계산
*/
    public int calculatePrice(
            String concertId,
            String seatClass
    ){


        Concert concert =
                concertRepository
                        .findById(
                                concertId
                        )
                        .orElseThrow(
                                () ->
                                        new RuntimeException(
                                                "공연 없음"
                                        )
                        );


        String priceInfo =
                concert.getConcertPriceInfo();



        if(priceInfo == null){
            throw new RuntimeException(
                    "가격 정보 없음"
            );
        }



        String[] prices =
                priceInfo.split(",");



        for(String price : prices){


            String[] data =
                    price.split(":");



            String grade =
                    data[0];


            int amount =
                    Integer.parseInt(
                            data[1]
                    );



            if(
                    grade.equals(seatClass)
            ){

                return amount;

            }

        }



        throw new RuntimeException(
                "해당 좌석 등급 없음"
        );


    }}