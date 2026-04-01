package com.oasystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应DTO
 */
@Data
public class UserInfoResponse {

    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private Long roleId;
    private String roleName;
    private String roleLabel;
    private Long deptId;
    private Integer status;
    private LocalDateTime createTime;
}
