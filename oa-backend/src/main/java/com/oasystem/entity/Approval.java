package com.oasystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批工单实体类
 */
@Data
@TableName("oa_approval")
public class Approval {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 审批标题
     */
    private String title;

    /**
     * 审批类型：1请假，2报销，3采购，4加班，5出差
     */
    private Integer type;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 当前审批人ID
     */
    private Long currentApproverId;

    /**
     * 状态：0草稿，1审批中，2已通过，3已打回，4已撤销
     */
    private Integer status;

    /**
     * 优先级：0低，1普通，2紧急
     */
    private Integer priority;

    /**
     * 申请内容
     */
    private String content;

    /**
     * 表单数据（JSON格式）
     */
    private String formData;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
