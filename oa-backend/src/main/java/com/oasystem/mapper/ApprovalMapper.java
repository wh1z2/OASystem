package com.oasystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oasystem.entity.Approval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审批工单 Mapper 接口
 */
@Mapper
public interface ApprovalMapper extends BaseMapper<Approval> {

    /**
     * 根据状态查询工单列表
     */
    @Select("SELECT * FROM oa_approval WHERE status = #{status} ORDER BY create_time DESC")
    List<Approval> selectByStatus(@Param("status") Integer status);

    /**
     * 根据申请人ID查询
     */
    @Select("SELECT * FROM oa_approval WHERE applicant_id = #{applicantId} ORDER BY create_time DESC")
    List<Approval> selectByApplicantId(@Param("applicantId") Long applicantId);

    /**
     * 根据当前审批人ID查询（待办列表）
     */
    @Select("SELECT * FROM oa_approval WHERE current_approver_id = #{approverId} AND status = 1 ORDER BY create_time DESC")
    List<Approval> selectByCurrentApproverId(@Param("approverId") Long approverId);

    /**
     * 查询用户的待办工单数量
     */
    @Select("SELECT COUNT(*) FROM oa_approval WHERE current_approver_id = #{approverId} AND status = 1")
    Long countTodoByApproverId(@Param("approverId") Long approverId);

    /**
     * 查询全系统待办工单数量（admin 统计用）
     */
    @Select("SELECT COUNT(*) FROM oa_approval WHERE status = 1")
    Long countAllTodos();

    /**
     * 查询用户已通过的工单数量
     */
    @Select("SELECT COUNT(*) FROM oa_approval WHERE applicant_id = #{applicantId} AND status = 2")
    Long countApprovedByApplicantId(@Param("applicantId") Long applicantId);

    /**
     * 查询用户已打回的工单数量
     */
    @Select("SELECT COUNT(*) FROM oa_approval WHERE applicant_id = #{applicantId} AND status = 3")
    Long countRejectedByApplicantId(@Param("applicantId") Long applicantId);

    /**
     * 查询用户发起的工单总数
     */
    @Select("SELECT COUNT(*) FROM oa_approval WHERE applicant_id = #{applicantId}")
    Long countMyApprovalsByApplicantId(@Param("applicantId") Long applicantId);

    /**
     * 按类型统计用户发起的工单分布
     */
    @Select("SELECT type, COUNT(*) as count FROM oa_approval WHERE applicant_id = #{applicantId} GROUP BY type")
    List<java.util.Map<String, Object>> countTypeDistributionByApplicantId(@Param("applicantId") Long applicantId);
}
