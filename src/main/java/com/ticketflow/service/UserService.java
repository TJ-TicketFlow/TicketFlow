package com.ticketflow.service;

import com.ticketflow.dto.RegisterRequestDto;
import com.ticketflow.dto.UserUpdateDto;
import com.ticketflow.entity.Coupon;
import com.ticketflow.entity.User;
import com.ticketflow.entity.UserCoupon;
import com.ticketflow.repository.CouponRepository;
import com.ticketflow.repository.UserCouponRepository;
import com.ticketflow.repository.UserRepository;
import com.ticketflow.repository.WishlistRepository;
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
    private final WishlistRepository wishlistRepository;
    private final EmailVerificationService emailVerificationService;

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
        // 🌟 [추가] 이메일 인증 여부를 서버가 직접 재확인 (기존에는 프론트 JS 변수만 확인해서
        // /register API를 직접 호출하면 인증 절차를 건너뛸 수 있었던 문제를 수정)
        if (!emailVerificationService.isVerified(dto.getEmail())) {
            throw new IllegalArgumentException("이메일 인증을 먼저 완료해주세요.");
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
        // 🌟 [추가] 가입에 사용한 인증 상태는 소모(삭제)해서 재사용을 막습니다.
        emailVerificationService.consumeVerification(dto.getEmail());
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

    // 🌟 [추가] 회원가입(RegisterRequestDto)과 동일한 비밀번호 규칙 - 영문+숫자+특수문자 포함 8자 이상
    private static final java.util.regex.Pattern PASSWORD_PATTERN = java.util.regex.Pattern.compile(
            "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    );

    /**
     * 비밀번호 변경 (마이페이지 - 현재 비밀번호 확인 필요)
     * @throws IllegalArgumentException 현재 비밀번호 불일치 또는 새 비밀번호 형식/확인 오류
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
        // 🌟 [추가] 회원가입 때와 동일한 비밀번호 강도 규칙 강제
        if (!PASSWORD_PATTERN.matcher(dto.getNewPassword()).matches()) {
            throw new IllegalArgumentException("새 비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 8자 이상이어야 합니다.");
        }
        // 🌟 [추가] "새 비밀번호 확인" 입력칸이 실제로는 아무 검증도 안 되던 문제 수정
        if (dto.getNewPasswordConfirm() == null || !dto.getNewPassword().equals(dto.getNewPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }
        // 🌟 [추가] 기존 비밀번호와 동일한 값으로 "변경"하는 것을 방지 (선택적이지만 흔한 관례)
        if (passwordEncoder.matches(dto.getNewPassword(), user.getUserPw())) {
            throw new IllegalArgumentException("현재 비밀번호와 다른 새 비밀번호를 입력해주세요.");
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

        // 🌟 0. [추가] 위시리스트(찜) 전체 삭제
        // 예매/결제 내역은 법적/통계 목적상 남겨두지만, 위시리스트는 순수하게 개인화된
        // 데이터이고 탈퇴 이후에는 아무 의미가 없으므로 탈퇴 시 함께 삭제합니다.
        wishlistRepository.deleteByUser_UserId(userId);

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