-- ============================================
-- 默认审批人配置机制数据库迁移脚本
-- ============================================

-- 1. 扩展部门表，增加上级部门字段
ALTER TABLE sys_dept ADD COLUMN parent_id BIGINT NULL COMMENT '上级部门ID' AFTER id;
ALTER TABLE sys_dept ADD KEY idx_parent_id (parent_id);

-- 更新示例部门数据，建立汇报关系
UPDATE sys_dept SET parent_id = NULL WHERE id = 4;
UPDATE sys_dept SET parent_id = 4 WHERE id = 1;
UPDATE sys_dept SET parent_id = 4 WHERE id = 2;
UPDATE sys_dept SET parent_id = 4 WHERE id = 3;

-- 2. 创建审批规则表
CREATE TABLE IF NOT EXISTS oa_approver_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    strategy_type TINYINT NOT NULL DEFAULT 1 COMMENT '策略类型：1=按部门角色, 3=固定人员',
    match_conditions JSON COMMENT '匹配条件JSON',
    approver_type TINYINT NOT NULL DEFAULT 1 COMMENT '审批人类型：1=指定用户, 2=指定角色',
    approver_value VARCHAR(500) COMMENT '审批人值JSON',
    priority INT NOT NULL DEFAULT 100 COMMENT '优先级（越小越优先）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    description VARCHAR(500) COMMENT '规则描述',
    scope_type TINYINT NOT NULL DEFAULT 1 COMMENT '作用范围：1=全局, 2=指定部门, 3=指定角色',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_strategy_type (strategy_type),
    KEY idx_status (status),
    KEY idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='默认审批人规则表';

-- 3. 插入默认规则（v1.0 仅策略1和策略3）
INSERT INTO oa_approver_rule (name, strategy_type, match_conditions, approver_type, approver_value, priority, status, description, scope_type) VALUES
('技术部日常审批规则', 1, '{"deptIds":[1], "types":[1,4]}', 2, '[2]', 10, 1, '技术部请假或加班由部门经理审批', 2),
('财务类审批规则', 3, '{"types":[2,3]}', 1, '[2]', 20, 1, '报销和采购由张经理审批（固定人员）', 1);

SELECT 'Migration completed!' AS result;
