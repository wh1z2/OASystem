package com.oasystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oasystem.entity.ApproverRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 审批规则Mapper
 */
@Mapper
public interface ApproverRuleMapper extends BaseMapper<ApproverRule> {

    /**
     * 查询所有启用的规则，按优先级升序排序
     */
    @Select("SELECT * FROM oa_approver_rule WHERE status = 1 ORDER BY priority ASC, id ASC")
    List<ApproverRule> selectEnabledRulesOrderedByPriority();
}
