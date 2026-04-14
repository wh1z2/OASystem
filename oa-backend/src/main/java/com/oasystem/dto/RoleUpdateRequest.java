package com.oasystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 角色更新请求DTO
 */
@Data
public class RoleUpdateRequest {

    /**
     * 角色显示名
     */
    @NotBlank(message = "角色名称不能为空")
    private String label;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 权限编码列表
     */
    private List<String> permissions;
}
