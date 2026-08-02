alter table ai_task
    modify biz_id varchar(64) null comment '关联业务ID(需求ID/项目ID/检测记录ID)';

alter table ai_task
    add system_id bigint null comment '调用系统ID' after task_type;

alter table sup_access_system
    modify system_name varchar(50) not null comment '系统名称';

alter table sup_access_system
    modify app_key varchar(50) not null comment '应用Key';
