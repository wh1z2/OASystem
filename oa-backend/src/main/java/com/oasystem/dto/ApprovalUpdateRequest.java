package com.oasystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 审批工单更新请求DTO
 */
@Data
public class ApprovalUpdateRequest {

    /**
     * 审批标题
     */
    @NotBlank(message = "审批标题不能为空")
    private String title;

    /**
     * 优先级：0低，1普通，2紧急
     */
    private Integer priority = 1;

    /**
     * 申请内容
     */
    private String content;

    /**
     * 表单数据（JSON格式）
     */
    private String formData;

    /**
     * 当前审批人ID
     */
    private Long currentApproverId;
}
