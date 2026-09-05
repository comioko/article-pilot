# 文章精修版本历史

use ArticlePilot;

create table if not exists article_revision
(
    id              bigint auto_increment comment 'id' primary key,
    taskId          varchar(64)                        not null comment '文章任务ID',
    userId          bigint                             not null comment '文章所有者ID',
    revisionNumber  int                                not null comment '版本号',
    content         text                               null comment '正文 Markdown',
    fullContent     text                               null comment '完整图文 Markdown',
    revisionNote    varchar(500)                       null comment '版本说明',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    unique key uk_task_revision (taskId, revisionNumber),
    index idx_taskId (taskId)
) comment '文章版本历史' collate = utf8mb4_unicode_ci;
