# 用户品牌知识库
use ArticlePilot;

create table if not exists brand_profile
(
    id              bigint auto_increment comment 'id' primary key,
    userId          bigint                              not null comment '用户ID',
    brandName       varchar(100)                        null comment '品牌或账号名称',
    tone            varchar(200)                        null comment '写作语气',
    targetAudience  varchar(300)                        null comment '目标读者',
    preferredTerms  text                                null comment '优先术语',
    bannedTerms     text                                null comment '禁用词',
    referenceNotes  text                                null comment '参考说明',
    createTime      datetime default CURRENT_TIMESTAMP  not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP  not null on update CURRENT_TIMESTAMP comment '更新时间',
    unique key uk_userId (userId)
) comment '用户品牌知识库' collate = utf8mb4_unicode_ci;
