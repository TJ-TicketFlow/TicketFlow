package com.ticketflow.service;

import com.ticketflow.dto.RegisterRequestDto;
import com.ticketflow.dto.UserUpdateDto;
import com.ticketflow.entity.Coupon;
import com.ticketflow.entity.User;
import com.ticketflow.entity.UserCoupon;
import com.ticketflow.repository.CouponRepository;
import com.ticketflow.repository.UserCouponRepository;
import com.ticketflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;

    // ───────────────────────────────────────────────
    // 회원가입
    // ───────────────────────────────────────────────

    /**
     * 아이디 중복 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isUserIdDuplicated(String userId) {
        return userRepository.existsByUserId(userId);
    }

    /**
     * 이메일 중복 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isEmailDuplicated(String email) {
        return userRepository.existsByUserEmail(email);
    }

    /**
     * 회원가입 처리
     * @throws IllegalArgumentException 아이디 또는 이메일 중복 시
     */
    @Transactional
    public void register(RegisterRequestDto dto) {
        if (isUserIdDuplicated(dto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (isEmailDuplicated(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 생년월일 파싱 (YYYY-MM-DD, 선택)
        LocalDate birth = null;
        if (dto.getBirth() != null && !dto.getBirth().isBlank()) {
            try {
                birth = LocalDate.parse(dto.getBirth());
            } catch (Exception ignored) { /* 형식 오류 시 null 저장 */ }
        }

        // 전화번호 기본값 처리 (필수 필드이므로 빈 문자열 대신 공백 방지)
        String phone = dto.getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            phone = "000-0000-0000";
        }

        User user = User.builder()
                .userId(dto.getUserId())
                .userPw(passwordEncoder.encode(dto.getPassword()))
                .userEmail(dto.getEmail())
                .userName(dto.getName())
                .userBirth(birth)
                .userAddress(dto.getAddress())
                .userPhoneNumber(phone)
                .userSex(dto.getGenderInt())
                .build();

        userRepository.save(user);
        issueWelcomeCoupon(user);
    }

    private void issueWelcomeCoupon(User user) {
        Coupon coupon = couponRepository.findByCouponName("신규가입 웰컴 쿠폰")
                .orElseThrow(() -> new IllegalStateException("쿠폰 마스터를 찾을 수 없음: 신규가입 웰컴 쿠폰"));

        UserCoupon userCoupon = UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .userCouponStatus(0)
                .userCouponExpireAt(LocalDateTime.now().plusDays(coupon.getCouponValidDays()))
                .build();
        userCouponRepository.save(userCoupon);
    }

    // ───────────────────────────────────────────────
    // 회원정보 수정
    // ───────────────────────────────────────────────

    /**
     * 비밀번호 변경 (마이페이지 - 현재 비밀번호 확인 필요)
     * @throws IllegalArgumentException 현재 비밀번호 불일치 또는 새 비밀번호 미입력
     */
    @Transactional
    public void updatePassword(String userId, UserUpdateDto dto) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호를 입력해주세요.");
        }
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getUserPw())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new IllegalArgumentException("새 비밀번호를 입력해주세요.");
        }

        user.setUserPw(passwordEncoder.encode(dto.getNewPassword()));
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdraw(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 🌟 1. 절대 중복되지 않는 고유 꼬리표 만들기 (예: _1730000000000)
        String delSuffix = "_" + System.currentTimeMillis();

        // 🌟 2. 아이디와 이메일에 회원번호+꼬리표를 붙여서 유니크 충돌 완벽 방지!
        // 결과 예시: del_15_1730000000000 (약 20자)
        user.setUserId("del_" + user.getUserNo() + delSuffix);

        // 결과 예시: del_15_1730000000000@x.com (약 26자 -> 50자 제한 안전!)
        user.setUserEmail("del_" + user.getUserNo() + delSuffix + "@x.com");

        // 🌟 3. 나머지 개인정보 마스킹 (법적 의무)
        user.setUserName("탈퇴회원");
        user.setUserPhoneNumber("000-0000-0000");
        user.setUserPw(""); // 비밀번호 무효화

        // userRepository.delete(user); // <--- 이건 꼭 지우거나 주석 처리하세요!
    }

    /**
     * 사용자 조회 (마이페이지 등)
     */
    @Transactional(readOnly = true)
    public User findByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}