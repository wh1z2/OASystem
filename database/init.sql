-- ============================================
-- OA System - Database Initialization Script
-- Phase 2: Database Design and Initialization
-- ============================================

-- 0. Drop and Recreate Database
DROP DATABASE IF EXISTS oa_system;
CREATE DATABASE oa_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE oa_system;

-- Set charset for this session
SET NAMES utf8mb4;

-- 2. Create User Table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key ID',
    username VARCHAR(50) NOT NULL COMMENT 'Username',
    password VARCHAR(100) NOT NULL COMMENT 'Encrypted Password',
    name VARCHAR(50) NOT NULL COMMENT 'Real Name',
    email VARCHAR(100) NOT NULL COMMENT 'Email',
    phone VARCHAR(20) COMMENT 'Phone Number',
    avatar VARCHAR(200) COMMENT 'Avatar URL',
    role_id BIGINT NOT NULL COMMENT 'Role ID (Logical FK to sys_role)',
    dept_id BIGINT COMMENT 'Department ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-Disabled, 1-Enabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    UNIQUE KEY uk_username (username),
    KEY idx_role_id (role_id),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User Table';

-- 3. Create Role Table
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key ID',
    name VARCHAR(50) NOT NULL COMMENT 'Role Identifier',
    label VARCHAR(100) NOT NULL COMMENT 'Role Display Name',
    description VARCHAR(500) COMMENT 'Role Description',
    permissions JSON COMMENT 'Permission List',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role Table';

-- 4. Create Approval Ticket Table
-- Status Enum: 0=DRAFT, 1=PROCESSING, 2=APPROVED, 3=RETURNED, 4=REVOKED
-- Type Enum: 1=LEAVE, 2=EXPENSE, 3=PURCHASE, 4=OVERTIME, 5=TRAVEL
CREATE TABLE IF NOT EXISTS oa_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key ID',
    title VARCHAR(200) NOT NULL COMMENT 'Approval Title',
    type TINYINT NOT NULL COMMENT 'Approval Type: 1=LEAVE, 2=EXPENSE, 3=PURCHASE, 4=OVERTIME, 5=TRAVEL',
    applicant_id BIGINT NOT NULL COMMENT 'Applicant ID (Logical FK to sys_user)',
    current_approver_id BIGINT COMMENT 'Current Approver ID (Logical FK to sys_user)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT 'Status: 0=DRAFT, 1=PROCESSING, 2=APPROVED, 3=RETURNED, 4=REVOKED',
    priority TINYINT NOT NULL DEFAULT 1 COMMENT 'Priority: 0=LOW, 1=NORMAL, 2=HIGH',
    content TEXT COMMENT 'Application Content',
    form_data JSON COMMENT 'Form Data',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update Time',
    KEY idx_applicant_id (applicant_id),
    KEY idx_current_approver_id (current_approver_id),
    KEY idx_status (status),
    KEY idx_type (type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Approval Ticket Table';

-- 5. Create Approval History Table
-- Action Enum: 0=SUBMIT, 1=APPROVE, 2=REJECT, 3=REEDIT, 4=REVOKE
CREATE TABLE IF NOT EXISTS oa_approval_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key ID',
    approval_id BIGINT NOT NULL COMMENT 'Ticket ID (Logical FK to oa_approval)',
    approver_id BIGINT NOT NULL COMMENT 'Approver ID (Logical FK to sys_user)',
    action TINYINT NOT NULL COMMENT 'Action Type: 0=SUBMIT, 1=APPROVE, 2=REJECT, 3=REEDIT, 4=REVOKE',
    comment VARCHAR(500) COMMENT 'Approval Comment',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation Time',
    KEY idx_approval_id (approval_id),
    KEY idx_approver_id (approver_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Approval History Table';

-- 6. Create Form Template Table
CREATE TABLE IF NOT EXISTS oa_form_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Primary Key ID',
    name VARCHAR(100) NOT NULL COMMENT 'Form Name',
    code VARCHAR(50) NOT NULL COMMENT 'Form Code',
    description VARCHAR(500) COMMENT 'Form Description',
    fields_config JSON NOT NULL COMMENT 'Field Configuration',
    flow_config VARCHAR(50) COMMENT 'Flow Configuration',
    status TINYINT NOT NULL DEFAULT 1 COMMENT 'Status: 0-Disabled, 1-Enabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create Time',
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Form Template Table';

-- ============================================
-- Insert Base Data
-- ============================================

-- 1. Insert Predefined Roles
INSERT INTO sys_role (id, name, label, description, permissions) VALUES
(1, 'admin', '系统管理员', '拥有系统所有权限', '["all", "approval", "apply", "user_view", "user_manage", "role_manage", "report", "personal"]'),
(2, 'manager', '部门经理', '审批权限、查看用户、查看报表', '["approval", "user_view", "report", "apply", "personal"]'),
(3, 'employee', '普通员工', '提交申请、个人信息管理', '["apply", "personal"]');

-- 2. Insert Test Users (Passwords are BCrypt encrypted. Original passwords: admin123, manager123, user123)
INSERT INTO sys_user (id, username, password, name, email, phone, role_id, dept_id, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '系统管理员', 'admin@oasystem.com', '13800000001', 1, 1, 1),
(2, 'manager', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '张经理', 'manager@oasystem.com', '13800000002', 2, 1, 1),
(3, 'user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '李四', 'user@oasystem.com', '13800000003', 3, 2, 1),
(4, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '张三', 'zhangsan@oasystem.com', '13800000004', 3, 2, 1),
(5, 'wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '王五', 'wangwu@oasystem.com', '13800000005', 3, 3, 1);

-- 3. Insert Sample Form Templates
INSERT INTO oa_form_template (id, name, code, description, fields_config, flow_config, status) VALUES
(1, '请假申请单', 'LEAVE_FORM', '员工请假申请表单', '[{"id":"field_001","type":"text","label":"请假事由","name":"reason","placeholder":"请输入请假原因","required":true},{"id":"field_002","type":"select","label":"请假类型","name":"type","options":[{"value":"sick","label":"病假"},{"value":"personal","label":"事假"},{"value":"annual","label":"年假"}],"required":true},{"id":"field_003","type":"date","label":"开始日期","name":"startDate","required":true},{"id":"field_004","type":"date","label":"结束日期","name":"endDate","required":true},{"id":"field_005","type":"textarea","label":"备注","name":"remarks","placeholder":"其他说明","required":false}]', 'simple', 1),
(2, '费用报销单', 'EXPENSE_FORM', '费用报销申请表单', '[{"id":"field_001","type":"text","label":"费用项目","name":"item","placeholder":"请输入费用项目","required":true},{"id":"field_002","type":"number","label":"金额","name":"amount","placeholder":"请输入金额","required":true},{"id":"field_003","type":"date","label":"日期","name":"date","required":true},{"id":"field_004","type":"textarea","label":"费用详情","name":"details","placeholder":"费用详细说明","required":true}]', 'simple', 1),
(3, '采购申请单', 'PURCHASE_FORM', '办公用品采购申请表单', '[{"id":"field_001","type":"text","label":"物品名称","name":"itemName","placeholder":"请输入物品名称","required":true},{"id":"field_002","type":"number","label":"数量","name":"quantity","placeholder":"请输入数量","required":true},{"id":"field_003","type":"number","label":"预估单价","name":"price","placeholder":"请输入预估价格","required":true},{"id":"field_004","type":"textarea","label":"采购理由","name":"reason","placeholder":"采购原因说明","required":true}]', 'simple', 1),
(4, '加班申请单', 'OVERTIME_FORM', '员工加班申请表单', '[{"id":"field_001","type":"date","label":"加班日期","name":"date","required":true},{"id":"field_002","type":"text","label":"开始时间","name":"startTime","placeholder":"例如：18:00","required":true},{"id":"field_003","type":"text","label":"结束时间","name":"endTime","placeholder":"例如：21:00","required":true},{"id":"field_004","type":"textarea","label":"加班原因","name":"reason","placeholder":"加班原因说明","required":true}]', 'simple', 1),
(5, '出差申请单', 'TRAVEL_FORM', '员工出差申请表单', '[{"id":"field_001","type":"text","label":"出差地点","name":"destination","placeholder":"请输入出差地点","required":true},{"id":"field_002","type":"date","label":"出发日期","name":"departureDate","required":true},{"id":"field_003","type":"date","label":"返回日期","name":"returnDate","required":true},{"id":"field_004","type":"textarea","label":"出差事由","name":"reason","placeholder":"出差原因说明","required":true}]', 'simple', 1);

-- 4. Insert Sample Approval Tickets
-- Status: 0=DRAFT, 1=PROCESSING, 2=APPROVED, 3=RETURNED, 4=REVOKED
-- Type: 1=LEAVE, 2=EXPENSE, 3=PURCHASE, 4=OVERTIME, 5=TRAVEL
-- Priority: 0=LOW, 1=NORMAL, 2=HIGH
INSERT INTO oa_approval (id, title, type, applicant_id, current_approver_id, status, priority, content, form_data) VALUES
(1, '李四-病假申请', 1, 3, 2, 1, 1, '申请2天病假', '{"reason":"感冒发烧","type":"sick","startDate":"2026-03-27","endDate":"2026-03-28","remarks":"已就诊并开具病假条"}'),
(2, '张三-差旅费报销', 2, 4, 2, 1, 2, '上海出差差旅费用报销', '{"item":"上海出差","amount":1500.00,"date":"2026-03-20","details":"包含往返机票、住宿费、餐费"}'),
(3, '王五-办公椅采购', 3, 5, NULL, 2, 1, '采购办公椅2把', '{"itemName":"人体工学办公椅","quantity":2,"price":800,"reason":"现有椅子损坏，需要更换"}'),
(4, '李四-年假申请', 1, 3, NULL, 3, 1, '申请3天年假', '{"reason":"家庭旅游","type":"annual","startDate":"2026-04-01","endDate":"2026-04-03","remarks":"提前两周已安排好工作"}'),
(5, '张三-项目上线加班', 4, 4, NULL, 0, 2, '项目上线支持加班申请', '{"date":"2026-03-25","startTime":"18:00","endTime":"22:00","reason":"系统上线需要技术支持"}');

-- 5. Insert Sample Approval History
-- Action: 0=SUBMIT, 1=APPROVE, 2=REJECT, 3=REEDIT, 4=REVOKE
INSERT INTO oa_approval_history (approval_id, approver_id, action, comment) VALUES
(3, 2, 1, '同意采购，请尽快下单'),
(4, 2, 2, '近期项目繁忙，建议推迟一周');

-- Reset Auto Increment IDs
ALTER TABLE sys_role AUTO_INCREMENT = 10;
ALTER TABLE sys_user AUTO_INCREMENT = 10;
ALTER TABLE oa_form_template AUTO_INCREMENT = 10;
ALTER TABLE oa_approval AUTO_INCREMENT = 10;

SELECT 'Database initialization completed!' AS result;
