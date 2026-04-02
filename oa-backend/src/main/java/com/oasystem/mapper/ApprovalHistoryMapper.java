package com.oasystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oasystem.entity.ApprovalHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审批历史记录 Mapper 接口
 */
@Mapper
public interface ApprovalHistoryMapper extends BaseMapper<ApprovalHistory> {

    /**
     * 根据工单ID查询历史记录
     */
    @Select("SELECT * FROM oa_approval_history WHERE approval_id = #{approvalId} ORDER BY create_time ASC")
    List<ApprovalHistory> selectByApprovalId(@Param("approvalId") Long approvalId);

    /**
     * 根据审批人ID查询历史记录（已办列表）
     */
    @Select("SELECT * FROM oa_approval_history WHERE approver_id = #{approverId} ORDER BY create_time DESC")
    List<ApprovalHistory> selectByApproverId(@Param("approverId") Long approverId);
}
