package com.ticketflow.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserUpdateDto {
    private String name;
    private String phone;
    private String address;
    private String currentPassword;
    private String newPassword;
    private String newPasswordConfirm;  // 새 비밀번호 확인용 (서비스 레이어에서 검증)
}
