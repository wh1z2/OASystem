package com.oasystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
     * 审批类型：1请假，2报销，3采购，4加班，5出差
     */
    @NotNull(message = "审批类型不能为空")
    private Integer type;

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
