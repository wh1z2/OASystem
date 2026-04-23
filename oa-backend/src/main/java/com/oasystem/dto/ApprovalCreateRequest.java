package com.oasystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 审批工单创建请求DTO
 */
@Data
public class ApprovalCreateRequest {

    /**
     * 审批标题
     */
    @NotBlank(message = "审批标题不能为空")
    private String title;

    /**
     * 审批类型（对应表单模板编码，如 LEAVE_FORM）
     */
    @NotBlank(message = "审批类型不能为空")
    private String type;

    /**
     * 优先级：0低，1普通，2紧急
     */
    private Integer priority = 1;

    /**
     * 申请内容
     */
    private String content;

    /**
     * 表单数据（JSON对象）
     */
    private Map<String, Object> formData;

    /**
     * 当前审批人ID（提交时指定）
     */
    private Long currentApproverId;
}
