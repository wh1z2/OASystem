package com.oasystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 表单模板创建请求 DTO
 */
@Data
public class FormTemplateCreateRequest {

    /**
     * 表单名称
     */
    @NotBlank(message = "表单名称不能为空")
    private String name;

    /**
     * 表单编码
     */
    @NotBlank(message = "表单编码不能为空")
    private String code;

    /**
     * 表单描述
     */
    private String description;

    /**
     * 字段配置（JSON数组）
     */
    private List<Map<String, Object>> fieldsConfig;

    /**
     * 流程配置
     */
    private String flowConfig;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;
}
