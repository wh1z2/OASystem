-- 修复财务部、人事部审批规则的 approver_value 数据错误
-- 原数据将用户ID误存为角色ID，导致角色解析失败
UPDATE oa_approver_rule SET approver_value = '[2]' WHERE id IN (3, 4);
