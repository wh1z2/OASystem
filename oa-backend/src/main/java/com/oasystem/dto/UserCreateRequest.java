package com.oasystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户创建请求DTO
 */
@Data
public class UserCreateRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

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

    /**
     * 角色ID
     */
    @NotNull(message = "角色不能为空")
    private Long roleId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status = 1;
}
