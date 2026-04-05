-- ========================================================
-- 权限系统优化 - 数据库变更脚本
-- 版本: V1.1
-- 说明: 增加代审批相关字段，支持管理员和部门经理代审批功能
-- ========================================================

-- --------------------------------------------------------
-- 1. 审批历史表增强 - 添加代审批相关字段
-- --------------------------------------------------------
ALTER TABLE oa_approval_history
    ADD COLUMN approval_type VARCHAR(20) DEFAULT 'DIRECT'
        COMMENT '审批类型：DIRECT直接审批/PROXY_ADMIN管理员代审批/PROXY_MANAGER经理代审批',
    ADD COLUMN is_proxy TINYINT DEFAULT 0
        COMMENT '是否为代审批：0否 1是',
    ADD COLUMN original_approver_id BIGINT
        COMMENT '原指定审批人ID（代审批时记录）',
    ADD COLUMN proxy_reason VARCHAR(200)
        COMMENT '代审批原因/备注';

-- 创建索引优化查询性能
CREATE INDEX idx_approval_type ON oa_approval_history(approval_type);
CREATE INDEX idx_is_proxy ON oa_approval_history(is_proxy);

-- --------------------------------------------------------
-- 2. 角色权限配置更新
-- --------------------------------------------------------

-- 2.1 查看当前权限配置（用于备份参考）
-- SELECT id, name, label, permissions FROM sys_role;

-- 2.2 更新管理员权限（增加全范围审批权限）
-- 管理员拥有：所有权限、基础审批权限、全范围审批权限、查看所有工单权限
UPDATE sys_role
SET permissions = '["all", "approval:execute", "approval:execute:all", "approval:view:all"]'
WHERE name = 'ADMIN';

-- 2.3 更新部门经理权限（增加本部门审批权限）
-- 部门经理拥有：基础审批权限、本部门审批权限、查看所有工单权限、用户查看权限、报表权限
UPDATE sys_role
SET permissions = '["approval:execute", "approval:execute:dept", "approval:view:all", "user_view", "report"]'
WHERE name = 'MANAGER';

-- 2.4 查看更新后的权限配置（确认更新结果）
-- SELECT id, name, label, permissions FROM sys_role;

-- --------------------------------------------------------
-- 3. 数据迁移说明
-- --------------------------------------------------------
-- 现有历史记录的 approval_type 默认为 'DIRECT'
-- 现有历史记录的 is_proxy 默认为 0
-- 无需额外数据迁移操作

-- --------------------------------------------------------
-- 4. 回滚脚本（如需要回滚，请执行以下SQL）
-- --------------------------------------------------------
/*
-- 4.1 删除新增字段
ALTER TABLE oa_approval_history
    DROP COLUMN approval_type,
    DROP COLUMN is_proxy,
    DROP COLUMN original_approver_id,
    DROP COLUMN proxy_reason;

-- 4.2 删除索引
DROP INDEX idx_approval_type ON oa_approval_history;
DROP INDEX idx_is_proxy ON oa_approval_history;

-- 4.3 恢复原有权限配置（请根据实际备份数据恢复）
-- UPDATE sys_role SET permissions = '原有权限JSON' WHERE name = 'ADMIN';
-- UPDATE sys_role SET permissions = '原有权限JSON' WHERE name = 'MANAGER';
*/

-- --------------------------------------------------------
-- 变更完成
-- --------------------------------------------------------
