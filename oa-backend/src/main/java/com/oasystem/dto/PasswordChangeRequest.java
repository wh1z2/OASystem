package com.oasystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 密码修改请求DTO
 */
@Data
public class PasswordChangeRequest {

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
