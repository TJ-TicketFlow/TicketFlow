package com.ticketflow.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용 가능합니다.")
    private String userId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    private String birth;           // YYYY-MM-DD (선택)

    // HTML의 th:field="*{gender}" 와 매핑될 필드 추가
    private String gender;

    private String zipcode;
    private String address;
    private String addressDetail;

    private String phonePrefix;
    private String phoneMid;
    private String phoneEnd;

    /** 전화번호 조합 */
    public String getPhoneNumber() {
        if (phonePrefix == null || phoneMid == null || phoneEnd == null) return null;
        return phonePrefix + "-" + phoneMid + "-" + phoneEnd;
    }

    /** * HTML에서 넘어온 성별 문자열을 DB(User 엔티티) 형식에 맞게 변환
     * 0: 여성, 1: 남성, null: 선택안함
     */
    public Integer getGenderInt() {
        if ("male".equals(this.gender)) {
            return 1;
        } else if ("female".equals(this.gender)) {
            return 0;
        }
        return null; // "none" 이거나 값이 없을 경우 null 반환
    }
}