-- =========================================
-- AI编制系统手机验证码登录功能 - 短信验证码表
-- 用于存储手机验证码，支持验证码登录
-- =========================================

USE `ele_ai_tender`;

-- =========================================
-- 短信验证码表
-- =========================================
CREATE TABLE IF NOT EXISTS sup_sms_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    code VARCHAR(10) NOT NULL COMMENT '验证码(6位数字)',
    scene VARCHAR(50) NOT NULL COMMENT '使用场景:LOGIN(登录)/REGISTER(注册)/RESET_PWD(重置密码)/BIND_PHONE(绑定手机)',
    status VARCHAR(20) DEFAULT 'UNUSED' COMMENT '状态:UNUSED(未使用)/USED(已使用)/EXPIRED(已过期)',
    expire_time DATETIME NOT NULL COMMENT '过期时间(一般5-10分钟)',
    ip_address VARCHAR(50) COMMENT '发送IP地址(防刷)',
    used_time DATETIME COMMENT '使用时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_phone_status (phone, status),
    INDEX idx_expire_time (expire_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信验证码表';

-- =========================================
-- 补充sup_user表phone字段索引(如果不存在)
-- =========================================
-- 注意:如果sup_user表已存在phone字段和索引，此语句会忽略
ALTER TABLE `sup_user` 
ADD COLUMN IF NOT EXISTS `phone` VARCHAR(20) COMMENT '手机号' AFTER `real_name`,
ADD COLUMN IF NOT EXISTS `email` VARCHAR(100) COMMENT '邮箱' AFTER `phone`;

-- 添加手机号唯一索引(如果不存在)
ALTER TABLE `sup_user` 
ADD UNIQUE INDEX IF NOT EXISTS `idx_phone` (`phone`);

-- =========================================
-- 插入测试数据(可选)
-- =========================================
-- 为admin用户设置手机号(如果不存在)
UPDATE `sup_user` 
SET phone = '13900139000' 
WHERE username = 'admin' AND (phone IS NULL OR phone = '');

-- =========================================
-- 验证
-- =========================================
SELECT '========== 短信验证码表结构 ==========' AS info;
DESCRIBE sup_sms_code;

SELECT '========== 用户表手机号字段 ==========' AS info;
SELECT id, username, real_name, phone, email FROM sup_user WHERE phone IS NOT NULL;

SELECT '========== 阶段二手机验证码功能SQL执行完成 ==========' AS message;
