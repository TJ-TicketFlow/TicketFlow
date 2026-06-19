package com.ticketflow.config;

import com.ticketflow.entity.*;
import com.ticketflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DummyDataInitializer implements CommandLineRunner {

    // 💡 1. 필요한 모든 창고(Repository)들을 빠짐없이 불러옵니다.
    private final UserRepository userRepository;
    private final HallRepository hallRepository;
    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final MembershipRepository membershipRepository;
    private final SelectedSeatRepository selectedSeatRepository;
    private final ReservationRepository reservationRepository;
    private final PayRepository payRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // 🚨 2. 이미 데이터가 들어있다면(중복 생성 방지), 그냥 넘어갑니다!
        if (concertRepository.count() > 0) {
            System.out.println("✅ 더미 데이터가 이미 존재하여 생성을 건너뜁니다.");
            return;
        }

        System.out.println("🚀 다양하고 풍성한 테스트용 더미 데이터 생성을 시작합니다...");

        // ==========================================
        // [기본 세팅] 1번 유저 확인
        // ==========================================
        User testUser = userRepository.findById(1L).orElse(null);
        if (testUser == null) {
            System.out.println("🚨 1번 유저가 없습니다! 먼저 유저 회원가입/생성을 해주세요.");
            return;
        }

        // ==========================================
        // [데이터 1] 공연장 생성
        // ==========================================
        Hall hall = Hall.builder()
                .hallId(1L) // 💡 주석(//)을 지워서 1번이라고 직접 알려줍니다!
                .hallName("올림픽 체조경기장 (KSPO DOME)")
                .hallAddress("서울특별시 송파구 올림픽로 424")
                .build();
        hallRepository.save(hall);

        // ==========================================
        // [데이터 2] 콘서트 2개 생성 (미래 공연 1개, 과거 공연 1개)
        // ==========================================
        Concert futureConcert = Concert.builder()
                .concertId("C_XMAS_2026")
                .hall(hall)
                .concertName("[테스트] 2026 티켓플로우 크리스마스 콘서트")
                .concertStartDate(LocalDate.of(2026, 12, 24))
                .concertEndDate(LocalDate.of(2026, 12, 25))
                .concertTime("19:00")
                .concertPosterUrl("https://dummyimage.com/210x297/3B82F6/FFFFFF.png&text=X-MAS+CONCERT")
                .concertStatus("TICKETING")
                .build();
        concertRepository.save(futureConcert);

        Concert pastConcert = Concert.builder()
                .concertId("C_SUMMER_2026")
                .hall(hall)
                .concertName("[테스트] 한여름 밤의 라이브")
                .concertStartDate(LocalDate.of(2026, 5, 10)) // 이미 지난 날짜
                .concertEndDate(LocalDate.of(2026, 5, 11))
                .concertTime("18:00")
                .concertPosterUrl("https://dummyimage.com/210x297/ef4444/FFFFFF.png&text=SUMMER+LIVE")
                .concertStatus("ENDED")
                .build();
        concertRepository.save(pastConcert);

        // ==========================================
        // [데이터 3] 쿠폰 및 멤버십 생성
        // ==========================================
        Coupon coupon = Coupon.builder()
                .couponName("크리스마스 특별 10% 할인")
                .couponDiscountRate(10)
                .couponValidDays(30)
                .build();
        couponRepository.save(coupon);

        UserCoupon unusedCoupon = UserCoupon.builder()
                .user(testUser)
                .coupon(coupon)
                .userCouponStatus(0) // 0: 미사용 (사용 가능)
                .userCouponIssuedAt(LocalDateTime.now())
                .userCouponExpireAt(LocalDateTime.now().plusDays(30))
                .build();
        userCouponRepository.save(unusedCoupon);

        Membership membership = Membership.builder()
                .user(testUser)
                .membershipCustomerId("CUST_TEST_001")
                .membershipSubId("SUB_TEST_001")
                .membershipVariantId("VAR_TEST_001")
                .membershipStatus("ACTIVE")
                .membershipStartDate(LocalDate.now())
                .membershipPeriodEnd(LocalDateTime.now().plusYears(1))
                .build();
        membershipRepository.save(membership);

        // ==========================================
        // [데이터 4] 다양한 좌석 생성
        // ==========================================
        // 미래 공연 좌석 (VIP석 2개, 일반석 1개)
        Seat seatVip1 = seatRepository.save(Seat.builder().seatId("S_XMAS_VIP_1").concert(futureConcert).seatClass("VIP").seatRow("1").seatCol("1").seatStatus((short) 0).build()); // 이미 팔린 좌석
        Seat seatVip2 = seatRepository.save(Seat.builder().seatId("S_XMAS_VIP_2").concert(futureConcert).seatClass("VIP").seatRow("1").seatCol("2").seatStatus((short) 1).build()); // 취소되어 다시 풀린 좌석
        Seat seatA1 = seatRepository.save(Seat.builder().seatId("S_XMAS_A_1").concert(futureConcert).seatClass("A석").seatRow("5").seatCol("10").seatStatus((short) 0).build()); // 결제 실패로 묶인 좌석

        // 과거 공연 좌석
        Seat pastSeat = seatRepository.save(Seat.builder().seatId("S_SUM_VIP_1").concert(pastConcert).seatClass("VIP").seatRow("1").seatCol("1").seatStatus((short) 0).build()); // 관람 완료된 좌석

        // ==========================================
        // [데이터 5] 다양한 결제 내역(Pay) 세트 생성
        // ==========================================

        // 1. 정상적으로 예매가 완료된 내역 (VIP석 15만원)
        createFullBookingData(testUser, futureConcert, seatVip1, "PAID", "결제완료 (정상)", 150000L, null);

        // 2. 예매했다가 고객이 변심해서 취소한 내역 (VIP석 15만원 -> 취소됨)
        createFullBookingData(testUser, futureConcert, seatVip2, "CANCELLED", "결제취소 (변심)", 150000L, null);

        // 3. 결제창에서 카드 잔액 부족 등으로 실패한 내역 (A석 9만원)
        createFullBookingData(testUser, futureConcert, seatA1, "FAILED", "결제실패 (잔액부족)", 90000L, null);

        // 4. 이미 과거에 성공적으로 관람까지 완료한 내역
        createFullBookingData(testUser, pastConcert, pastSeat, "PAID", "관람완료 (과거공연)", 120000L, null);

        // ==========================================
        // 🚨 [여기로 이동!] 2번 유저(남의 계정) 강제 생성 및 예약건 만들기
        // ==========================================
        User stranger = userRepository.findById(2L).orElseGet(() -> {
            return userRepository.save(User.builder()
                    .userId("stranger")
                    .userName("김도둑")
                    .userEmail("thief@test.com")
                    .userPhoneNumber("010-9999-9999") // 💡 이 줄을 추가해 주세요!
                    .userPw("1234")                   // 💡 (안전빵) 비밀번호도 필드도 추가!
                    .build());
        });

        Seat strangerSeat = seatRepository.save(Seat.builder().seatId("S_XMAS_B_1").concert(futureConcert).seatClass("B석").seatRow("10").seatCol("1").seatStatus((short) 1).build());

        SelectedSeat strangerSelected = selectedSeatRepository.save(SelectedSeat.builder()
                .user(stranger).seat(strangerSeat).seatState((short) 1).price(50000L).concert(futureConcert).build());

        Reservation strangerRes = reservationRepository.save(Reservation.builder()
                .selectedSeat(strangerSelected).reservationDate(futureConcert.getConcertStartDate()).reservationCount(1).sessionTime(futureConcert.getConcertTime()).build());

        System.out.println("🚨 [테스트용] 김도둑(남)의 예약 번호: " + strangerRes.getReservationKey());
        // ==========================================
        // 💡 [여기서부터 새로 추가!] 김도둑의 '결제 완료'된 예매 내역 만들기
        // ==========================================
        // 1. 김도둑 전용 C석 좌석 하나 만들기
        Seat strangerSeat2 = seatRepository.save(Seat.builder()
                .seatId("S_XMAS_C_1")
                .concert(futureConcert)
                .seatClass("C석")
                .seatRow("15")
                .seatCol("1")
                .seatStatus((short) 0) // 팔림
                .build());

        // 2. 김도둑의 예매 내역(마이페이지용) 생성!
        createFullBookingData(stranger, futureConcert, strangerSeat2, "PAID", "김도둑의 C석 예매건", 50000L, null);
        // ==========================================

        System.out.println("🎉 다양한 테스트용 더미 데이터가 완벽하게 생성되었습니다!");
    }

    // ==========================================
    // 💡 여러 단계의 복잡한 데이터를 한 번에 예쁘게 묶어주는 마법의 메서드
    // ==========================================
    private void createFullBookingData(User user, Concert concert, Seat seat, String payStatus, String payName, Long amount, UserCoupon coupon) {

        // 1. 좌석 선택 내역 만들기
        SelectedSeat selectedSeat = SelectedSeat.builder()
                .user(user)
                .seat(seat)
                .seatState((short) 2) // 2: 선택완료 상태
                .price(amount)
                .concert(concert)
                .build();
        selectedSeatRepository.save(selectedSeat);

        // 2. 예약 장부 만들기
        Reservation reservation = Reservation.builder()
                .selectedSeat(selectedSeat)
                .reservationDate(concert.getConcertStartDate()) // 공연 날짜로 예약일 세팅
                .reservationCount(1)
                .sessionTime(concert.getConcertTime()) // 공연 시간으로 세션 타임 세팅
                .build();
        reservationRepository.save(reservation);

        // 3. 최종 결제 영수증(Pay) 만들기
        Pay pay = Pay.builder()
                .reservation(reservation)
                .payName(payName)
                .payAmount(amount)
                .payStatus(payStatus) // "PAID", "CANCELLED", "FAILED" 등
                .buyerName(user.getUserName() != null ? user.getUserName() : "홍길동")
                .buyerEmail(user.getUserEmail() != null ? user.getUserEmail() : "test@test.com")
                .userCoupon(coupon) // 쓴 쿠폰이 있다면 여기에 기록
                .payCreatedAt(LocalDateTime.now().minusDays(2)) // 이틀 전에 결제했다고 가정
                .build();
        payRepository.save(pay);
    }
}