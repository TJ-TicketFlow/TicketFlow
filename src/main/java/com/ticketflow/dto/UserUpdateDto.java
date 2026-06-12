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
}
