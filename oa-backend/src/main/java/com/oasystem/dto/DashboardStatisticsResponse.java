package com.oasystem.dto;

import lombok.Data;

import java.util.List;

/**
 * 工作台统计数据响应DTO
 */
@Data
public class DashboardStatisticsResponse {

    /**
     * 待办事项数量
     */
    private Long pendingCount;

    /**
     * 已通过数量
     */
    private Long approvedCount;

    /**
     * 已拒绝（已打回）数量
     */
    private Long rejectedCount;

    /**
     * 我的申请总数
     */
    private Long myApprovalCount;

    /**
     * 已办事项数量
     */
    private Long doneCount;

    /**
     * 审批类型分布统计
     */
    private List<TypeDistributionItem> approvalTypeDistribution;

    /**
     * 类型分布项
     */
    @Data
    public static class TypeDistributionItem {
        /**
         * 类型编码（对应表单模板编码）
         */
        private String type;

        /**
         * 类型名称
         */
        private String typeName;

        /**
         * 数量
         */
        private Long count;
    }
}
