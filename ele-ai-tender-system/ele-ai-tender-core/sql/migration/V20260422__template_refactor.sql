-- =========================================
-- 迁移脚本: 模板重构 - 新增项目模板快照表
-- 版本: V20260422
-- 说明:
--   1. sup_template 新增 file_id 字段(模板文件关联)
--   2. sup_template.content 语义变更(Markdown内容 → 用途说明)，改为可空
--   3. 新建 tb_project_template 项目模板快照表
--   4. tb_project.template_id 语义变更(关联sup_template → 关联tb_project_template)
--   5. 迁移已有数据
-- =========================================

-- 1. sup_template 新增 file_id 字段
ALTER TABLE sup_template ADD COLUMN file_id BIGINT DEFAULT NULL COMMENT '模板文件ID(关联file_info)' AFTER project_type;
ALTER TABLE sup_template ADD INDEX idx_file_id (file_id);

-- 2. content 改为可空（语义从"模板内容(Markdown)"改为"模板用途说明"）
ALTER TABLE sup_template MODIFY COLUMN content LONGTEXT DEFAULT NULL COMMENT '模板用途说明';

-- 3. 新建项目模板快照表
CREATE TABLE IF NOT EXISTS `tb_project_template` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `project_id`            BIGINT       NOT NULL COMMENT '项目ID',
    `template_id`           BIGINT       NOT NULL COMMENT '源模板ID(sup_template.id)',
    `template_code`         VARCHAR(50)  NOT NULL COMMENT '模板编码(快照)',
    `template_name`         VARCHAR(100) NOT NULL COMMENT '模板名称(快照)',
    `project_category`      VARCHAR(30)  NOT NULL COMMENT '适用项目类别(快照)',
    `project_type`          VARCHAR(30)  NOT NULL COMMENT '适用项目类型(快照)',
    `file_id`               BIGINT       DEFAULT NULL COMMENT '模板文件ID(快照，引用sup_template.file_id)',
    `content`               TEXT         DEFAULT NULL COMMENT '模板用途说明(快照)',
    `structure_definition`  JSON         DEFAULT NULL COMMENT 'Word章节结构JSON(快照)',
    `version_no`            INT          NOT NULL DEFAULT 1 COMMENT '模板版本号(快照)',
    `create_time`           DATETIME     NOT NULL COMMENT '创建时间',
    `create_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '创建人ID',
    `create_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '创建人名称',
    `modify_time`           DATETIME     NOT NULL COMMENT '修改时间',
    `modify_id`             BIGINT       NOT NULL DEFAULT 0 COMMENT '修改人ID',
    `modify_name`           VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '修改人名称',
    `ver`                   INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_delete`             TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_project_id` (`project_id`),
    INDEX `idx_template_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目模板表(只读快照)';

-- 4. tb_project.template_id 语义变更
ALTER TABLE tb_project MODIFY COLUMN template_id BIGINT DEFAULT NULL COMMENT '项目模板ID(关联tb_project_template)';

-- 5. 迁移已有数据：将 tb_project.template_id 引用的 sup_template 记录复制到 tb_project_template
INSERT INTO tb_project_template
    (project_id, template_id, template_code, template_name, project_category, project_type,
     file_id, content, structure_definition, version_no,
     create_time, create_id, create_name, modify_time, modify_id, modify_name, ver, is_delete)
SELECT
    p.id, st.id, st.template_code, st.template_name, st.project_category, st.project_type,
    st.file_id, st.content, st.structure_definition, st.version_no,
    NOW(), p.create_id, p.create_name, NOW(), p.modify_id, p.modify_name, 1, 0
FROM tb_project p
INNER JOIN sup_template st ON p.template_id = st.id
WHERE p.template_id IS NOT NULL AND p.is_delete = 0;

-- 6. 更新 tb_project.template_id 指向 tb_project_template.id
UPDATE tb_project p
INNER JOIN tb_project_template pt ON p.id = pt.project_id
SET p.template_id = pt.id
WHERE p.is_delete = 0;
