use yubi;
-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',

    usageCount   int          default 0                 not null comment '使用次数',
    points       int          default 0                 not null comment '积分',
    lastCheckIn  datetime     default CURRENT_TIMESTAMP not null comment '最后签到时间',

    index idx_userAccount (userAccount)
) comment '用户' collate = utf8mb4_unicode_ci;


-- 图表信息表
create table if not exists chart
(
    id           bigint auto_increment comment 'id' primary key,
    goal				 text null comment'分析目标',
    `name` varchar(128) null comment '图表名称',
    chartData		 text null comment'图表数据',
    chartType		 varchar(128) null comment'图表类型',
    genChart		 text null comment'生成的图表数据',
    genResult		 text null comment'生成的分析结论',
    -- 任务状态字段(排队中wait、执行中running、已完成succeed、失败failed)
    status       varchar(128) not null default 'wait' comment 'wait,running,succeed,failed',
-- 任务执行信息字段
    execMessage  text   null comment '执行信息',

    userId			 bigint null comment'创建用户 id',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除'
) comment '图表信息表' collate = utf8mb4_unicode_ci;
ALTER TABLE `user`
    ADD COLUMN `usageCount` INT NOT NULL DEFAULT 0 COMMENT '使用次数' AFTER `isDelete`,
    ADD COLUMN `points` INT NOT NULL DEFAULT 0 COMMENT '积分' AFTER `usageCount`,
    ADD COLUMN `lastCheckIn` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后签到时间' AFTER `points`;
use yubi;
create table chart_1717550682513911809
(
    日期  int null,
    用户数 int null
);
use yubi;
select * from chart_1717550682513911809;