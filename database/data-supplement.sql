-- ============================================
-- OA System - Data Supplement Script
-- 补充缺失的用户和审批规则数据
-- ============================================

USE oa_system;
SET NAMES utf8mb4;

-- ============================================
-- 1. 补充部门经理和用户数据
-- ============================================

-- 所有新用户使用统一密码: manager123 (BCrypt)
-- 密码哈希: $2a$10$MRzV6NL76jfkfNFktAxqA.uv.XhPTr8lowCmeWSDDJzLQixoUYukW

INSERT INTO sys_user (id, username, password, name, email, phone, role_id, dept_id, status) VALUES
-- 财务部经理
(6, 'caiwujl', '$2a$10$MRzV6NL76jfkfNFktAxqA.uv.XhPTr8lowCmeWSDDJzLQixoUYukW', '刘经理', 'liujl@oasystem.com', '13800000006', 2, 2, 1),
-- 人事部经理
(7, 'renshijl', '$2a$10$MRzV6NL76jfkfNFktAxqA.uv.XhPTr8lowCmeWSDDJzLQixoUYukW', '陈经理', 'chenjl@oasystem.com', '13800000007', 2, 3, 1),
-- 系统管理部经理
(8, 'sysjl', '$2a$10$MRzV6NL76jfkfNFktAxqA.uv.XhPTr8lowCmeWSDDJzLQixoUYukW', '赵经理', 'zhaojl@oasystem.com', '13800000008', 2, 4, 1),
-- 系统管理部员工
(9, 'sunwei', '$2a$10$MRzV6NL76jfkfNFktAxqA.uv.XhPTr8lowCmeWSDDJzLQixoUYukW', '孙运维', 'sunwei@oasystem.com', '13800000009', 3, 4, 1),
-- 技术部新员工
(10, 'zhougong', '$2a$10$MRzV6NL76jfkfNFktAxqA.uv.XhPTr8lowCmeWSDDJzLQixoUYukW', '周工', 'zhougong@oasystem.com', '13800000010', 3, 1, 1),
-- 人事部新员工
(11, 'lili', '$2a$10$MRzV6NL76jfkfNFktAxqA.uv.XhPTr8lowCmeWSDDJzLQixoUYukW', '李莉', 'lili@oasystem.com', '13800000011', 3, 3, 1),
-- 财务部新员工
(12, 'wangcai', '$2a$10$MRzV6NL76jfkfNFktAxqA.uv.XhPTr8lowCmeWSDDJzLQixoUYukW', '王财', 'wangcai@oasystem.com', '13800000012', 3, 2, 1)
ON DUPLICATE KEY UPDATE status = 1;

-- ============================================
-- 2. 补充各部门审批规则
-- ============================================

-- 规则3: 财务部日常审批规则（匹配财务部 + 请假/加班/出差，由部门经理审批）
INSERT INTO oa_approver_rule (id, name, strategy_type, match_conditions, approver_type, approver_value, priority, status, description, scope_type) VALUES
(3, '财务部日常审批规则', 1, '{"deptIds":[2], "types":[1,4,5]}', 2, '[6]', 10, 1, '财务部员工请假、加班或出差，由财务部经理审批', 2)
ON DUPLICATE KEY UPDATE status = 1, name = VALUES(name);

-- 规则4: 人事部日常审批规则（匹配人事部 + 请假/加班/出差，由部门经理审批）
INSERT INTO oa_approver_rule (id, name, strategy_type, match_conditions, approver_type, approver_value, priority, status, description, scope_type) VALUES
(4, '人事部日常审批规则', 1, '{"deptIds":[3], "types":[1,4,5]}', 2, '[7]', 10, 1, '人事部员工请假、加班或出差，由人事部经理审批', 2)
ON DUPLICATE KEY UPDATE status = 1, name = VALUES(name);

-- 规则5: 系统管理部日常审批规则（匹配系统管理部 + 请假/加班/出差，由部门经理审批）
INSERT INTO oa_approver_rule (id, name, strategy_type, match_conditions, approver_type, approver_value, priority, status, description, scope_type) VALUES
(5, '系统管理部日常审批规则', 1, '{"deptIds":[4], "types":[1,4,5]}', 2, '[8]', 10, 1, '系统管理部员工请假、加班或出差，由系统管理部经理审批', 2)
ON DUPLICATE KEY UPDATE status = 1, name = VALUES(name);

-- 规则6: 通用采购审批规则（匹配所有部门 + 采购，由技术部经理审批）
INSERT INTO oa_approver_rule (id, name, strategy_type, match_conditions, approver_type, approver_value, priority, status, description, scope_type) VALUES
(6, '通用采购审批规则', 3, '{"types":[3]}', 1, '[2]', 20, 1, '所有部门的采购申请，由技术部经理统一审批', 1)
ON DUPLICATE KEY UPDATE status = 1, name = VALUES(name);

-- ============================================
-- 3. 调整自增起始值（避免冲突）
-- ============================================

ALTER TABLE sys_user AUTO_INCREMENT = 20;
ALTER TABLE oa_approver_rule AUTO_INCREMENT = 20;

-- ============================================
-- 4. 数据验证查询
-- ============================================

SELECT '=== 部门人员统计 ===' AS info;
SELECT d.name AS 部门, COUNT(u.id) AS 人数, SUM(CASE WHEN u.role_id = 2 THEN 1 ELSE 0 END) AS 经理数
FROM sys_dept d
LEFT JOIN sys_user u ON d.id = u.dept_id AND u.status = 1
GROUP BY d.id, d.name
ORDER BY d.id;

SELECT '=== 审批规则列表 ===' AS info;
SELECT id, name, strategy_type, approver_type, approver_value, priority, status
FROM oa_approver_rule
WHERE status = 1
ORDER BY priority, id;

SELECT 'Data supplement completed!' AS result;
