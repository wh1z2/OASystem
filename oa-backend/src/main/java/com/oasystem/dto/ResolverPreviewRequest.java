package com.oasystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批规则预览请求DTO
 */
@Data
public class ResolverPreviewRequest {

    @NotNull(message = "申请人ID不能为空")
    private Long applicantId;

    @NotBlank(message = "审批类型不能为空")
    private String type;
}
