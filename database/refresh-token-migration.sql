-- Refresh Token 表（用于双Token刷新机制）
-- 创建日期: 2026-04-24

CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    token VARCHAR(512) NOT NULL COMMENT 'Refresh Token字符串',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    revoked TINYINT(1) DEFAULT 0 COMMENT '是否已撤销：0-有效，1-已撤销',
    INDEX idx_user_id (user_id),
    UNIQUE INDEX idx_token (token)
) COMMENT='Refresh Token表，支持单点登出与Token轮换';
