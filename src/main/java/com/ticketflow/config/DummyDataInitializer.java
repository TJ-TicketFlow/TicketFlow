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

    // 💡 필요한 창고(Repository)들을 모두 불러옵니다.
    // (혹시 안 만들어진 Repository가 있다면 인터페이스만 간단히 만들어주세요!)
    private final UserRepository userRepository;
    private final HallRepository hallRepository;
    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final MembershipRepository membershipRepository;
    private final SelectedSeatRepository selectedSeatRepository;
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // 🚨 이미 데이터가 들어있다면(중복 생성 방지), 그냥 넘어갑니다!
        if (concertRepository.count() > 0) {
            System.out.println("✅ 더미 데이터가 이미 존재하여 생성을 건너뜁니다.");
            return;
        }

        System.out.println("🚀 테스트용 더미 데이터 생성을 시작합니다...");

        // ==========================================
        // 0. 테스트용 기준 유저 가져오기 (이미 DB에 있는 1번 회원이라고 가정)
        // ==========================================
        User testUser = userRepository.findById(1L).orElse(null);
        if (testUser == null) {
            System.out.println("🚨 1번 유저가 없습니다! 먼저 유저 회원가입/생성을 해주세요.");
            return;
        }

        // ==========================================
        // 1. 공연장(Hall) 생성
        // ==========================================
        Hall hall = Hall.builder()
                .hallId(1L) // 자동생성(Identity)이면 주석처리
                .hallName("올림픽 체조경기장 (KSPO DOME)")
                .hallAddress("서울특별시 송파구 올림픽로 424")
                .build();
        hallRepository.save(hall);

        // ==========================================
        // 2. 콘서트(Concert) 생성
        // ==========================================
        Concert concert = Concert.builder()
                .concertId("C_XMAS_2026") // VARCHAR 타입이라 직접 지정
                .hall(hall) // 💡 ERD에 맞게 엔티티 참조로 변경 (hall_id 대신)
                .concertName("[테스트] 티켓플로우 크리스마스 콘서트")
                .concertStartDate(LocalDate.of(2026, 12, 24))
                .concertEndDate(LocalDate.of(2026, 12, 25))
                .concertTime("19:00")
                .concertPosterUrl("https://via.placeholder.com/210x297/3B82F6/FFFFFF?text=X-MAS+CONCERT")
                .concertStatus("TICKETING")
                .build();
        concertRepository.save(concert);

        // ==========================================
        // 3. 좌석(Seat) 생성
        // ==========================================
        Seat seat1 = Seat.builder()
                .seatId("S_VIP_A_1_1")
                .concert(concert)
                .seatClass("VIP")
                .seatRow("1")
                .seatCol("1")
                .seatStatus((short) 1) // 1: 사용가능
                .build();
        seatRepository.save(seat1);

        // ==========================================
        // 4. 쿠폰(Coupon) 및 유저 발급(UserCoupon) 생성
        // ==========================================
        Coupon coupon = Coupon.builder()
                .couponName("크리스마스 특별 10% 할인")
                .couponDiscountRate(10)
                .couponValidDays(30)
                .build();
        couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
                .user(testUser)
                .coupon(coupon)
                .userCouponStatus((Integer) 0) // 0: 미사용
                .userCouponIssuedAt(LocalDateTime.now())
                .userCouponExpireAt(LocalDateTime.now().plusDays(30))
                .build();
        userCouponRepository.save(userCoupon);

        // ==========================================
        // 5. 멤버십(Membership) 생성
        // ==========================================
        Membership membership = Membership.builder()
                .user(testUser)
                //.userNo(testUser.getUserNo())
                .membershipCustomerId("CUST_TEST_001")
                .membershipSubId("SUB_TEST_001")       // 💡 새로 추가!
                .membershipVariantId("VAR_TEST_001")
                .membershipStatus("ACTIVE")
                .membershipStartDate(LocalDate.now())
                .membershipPeriodEnd(LocalDateTime.now().plusYears(1))
                .build();
        membershipRepository.save(membership);

        // ==========================================
        // 6. 임시 선택 좌석(SelectedSeat) 생성 (장바구니 상태)
        // ==========================================
        SelectedSeat selectedSeat = SelectedSeat.builder()
                .user(testUser)
                .seat(seat1)
                .seatState((short) 1) // 1: 결제중 대기상태
                .price(150000L) // VIP석 15만원
                .concert(concert)
                .build();
        selectedSeatRepository.save(selectedSeat);

        // ==========================================
        // 7. 예약 장부(Reservation) 생성 (결제 전 임시 상태)
        // ==========================================
        Reservation reservation = Reservation.builder()
                .selectedSeat(selectedSeat)
                .reservationDate(LocalDate.now())
                .reservationCount(1)
                .build();
        reservationRepository.save(reservation);

        System.out.println("🎉 더미 데이터 생성이 완료되었습니다!");
        System.out.println("💡 테스트용 예약 번호(Reservation Key): " + reservation.getReservationKey());
    }
}