package com.oasystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 个人资料更新请求DTO
 */
@Data
public class ProfileUpdateRequest {

    /**
     * 真实姓名
     */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;
}
