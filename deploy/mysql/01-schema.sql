SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS ArticlePilot
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ArticlePilot;

CREATE TABLE IF NOT EXISTS user
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    userAccount  VARCHAR(256)                           NOT NULL COMMENT '账号',
    userPassword VARCHAR(512)                           NOT NULL COMMENT '密码',
    userName     VARCHAR(256)                           NULL COMMENT '用户昵称',
    userAvatar   VARCHAR(1024)                          NULL COMMENT '用户头像',
    userProfile  VARCHAR(512)                           NULL COMMENT '用户简介',
    userRole     VARCHAR(256) DEFAULT 'user'            NOT NULL COMMENT '用户角色：user/admin/vip',
    quota        INT          DEFAULT 5                 NOT NULL COMMENT '剩余配额',
    vipTime      DATETIME                               NULL COMMENT '成为会员时间',
    editTime     DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    createTime   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT      DEFAULT 0                 NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) COMMENT '用户' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS article
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    taskId              VARCHAR(64)                         NOT NULL COMMENT '任务ID（UUID）',
    userId              BIGINT                              NOT NULL COMMENT '用户ID',
    topic               VARCHAR(500)                        NOT NULL COMMENT '选题',
    userDescription     TEXT                                NULL COMMENT '用户补充描述',
    enabledImageMethods JSON                                NULL COMMENT '允许的配图方式列表',
    style               VARCHAR(20)                         NULL COMMENT '文章风格',
    mainTitle           VARCHAR(200)                        NULL COMMENT '主标题',
    subTitle            VARCHAR(300)                        NULL COMMENT '副标题',
    titleOptions        JSON                                NULL COMMENT '标题方案列表',
    outline             JSON                                NULL COMMENT '大纲',
    content             TEXT                                NULL COMMENT '正文',
    fullContent         TEXT                                NULL COMMENT '完整图文',
    coverImage          VARCHAR(512)                        NULL COMMENT '封面图 URL',
    images              JSON                                NULL COMMENT '配图列表',
    status              VARCHAR(20) DEFAULT 'PENDING'       NOT NULL COMMENT '处理状态',
    phase               VARCHAR(50) DEFAULT 'PENDING'       NULL COMMENT '当前阶段',
    errorMessage        TEXT                                NULL COMMENT '错误信息',
    createTime          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    completedTime       DATETIME                            NULL COMMENT '完成时间',
    updateTime          DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete            TINYINT     DEFAULT 0               NOT NULL COMMENT '是否删除',
    UNIQUE KEY uk_taskId (taskId),
    INDEX idx_userId (userId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime),
    INDEX idx_userId_status (userId, status)
) COMMENT '文章表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_log
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    taskId       VARCHAR(64)                         NOT NULL COMMENT '任务ID',
    agentName    VARCHAR(50)                         NOT NULL COMMENT '智能体名称',
    startTime    DATETIME                            NOT NULL COMMENT '开始时间',
    endTime      DATETIME                            NULL COMMENT '结束时间',
    durationMs   INT                                 NULL COMMENT '耗时（毫秒）',
    status       VARCHAR(20)                         NOT NULL COMMENT '状态',
    errorMessage TEXT                                NULL COMMENT '错误信息',
    prompt       TEXT                                NULL COMMENT 'Prompt',
    inputData    JSON                                NULL COMMENT '输入数据',
    outputData   JSON                                NULL COMMENT '输出数据',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL COMMENT '创建时间',
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP  NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT  DEFAULT 0                  NOT NULL COMMENT '是否删除',
    INDEX idx_taskId (taskId),
    INDEX idx_agentName (agentName),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) COMMENT '智能体执行日志表' COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payment_record
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    userId                BIGINT                              NOT NULL COMMENT '用户ID',
    stripeSessionId       VARCHAR(128)                        NULL COMMENT 'Stripe Checkout Session ID',
    stripePaymentIntentId VARCHAR(128)                        NULL COMMENT 'Stripe 支付意向ID',
    amount                DECIMAL(10, 2)                      NOT NULL COMMENT '金额（美元）',
    currency              VARCHAR(8)  DEFAULT 'usd'           NULL COMMENT '货币',
    status                VARCHAR(32)                         NOT NULL COMMENT '状态',
    productType           VARCHAR(32)                         NOT NULL COMMENT '产品类型',
    description           VARCHAR(256)                        NULL COMMENT '描述',
    refundTime            DATETIME                            NULL COMMENT '退款时间',
    refundReason          VARCHAR(512)                        NULL COMMENT '退款原因',
    createTime            DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime            DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_userId (userId),
    INDEX idx_stripeSessionId (stripeSessionId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) COMMENT '支付记录表' COLLATE = utf8mb4_unicode_ci;
