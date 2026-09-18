-- ============================================================
-- 花田(社区)概念融入空间 —— 数据库迁移脚本
-- 说明：本脚本只做结构变更，不执行；请确认备份后自行执行。
-- 列名使用驼峰命名，与实体类字段一致（mybatis-plus map-underscore-to-camel-case=false）
-- ============================================================

-- 1) space 表新增字段
--    - picUrl：封面图（个人/团队/社区空间均可用）
--    - status：0公开/1私有/2加密（仅社区空间）
--    - password：社区密码（仅加密社区空间）
--    - categoryId：分类id（仅社区空间，冗余）
--    - maxNum：最大人数（团队/社区空间）
--    - description：描述（社区空间）
ALTER TABLE space
    ADD COLUMN picUrl      VARCHAR(512) NULL COMMENT '空间封面图' AFTER spaceType,
    ADD COLUMN status      TINYINT      NULL DEFAULT 0 COMMENT '可见状态 0公开/1私有/2加密(仅社区)' AFTER spaceType,
    ADD COLUMN password    VARCHAR(64)  NULL COMMENT '社区密码(仅加密社区)' AFTER status,
    ADD COLUMN categoryId  BIGINT       NULL COMMENT '分类id(仅社区空间,冗余)' AFTER password,
    ADD COLUMN maxNum      INT          NULL COMMENT '最大人数(团队/社区空间)' AFTER categoryId,
    ADD COLUMN description VARCHAR(255) NULL COMMENT '描述(社区空间)' AFTER maxNum;

-- 2) 新增单级分类表 category（替代原 first_category / second_category）
CREATE TABLE IF NOT EXISTS category (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    category   VARCHAR(64)  NOT NULL COMMENT '分类名称',
    sort       INT          NULL DEFAULT 0 COMMENT '顺序',
    status     TINYINT      NULL DEFAULT 1 COMMENT '0禁用/1启用',
    createTime DATETIME     NULL COMMENT '创建时间',
    updateTime DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT '空间分类(单级)';

-- 3) post 表新增 spaceId（社区空间归属，替代 fieldId）
ALTER TABLE post
    ADD COLUMN spaceId BIGINT NULL COMMENT '所属社区空间id(即原花田)' AFTER id;

-- 4) 数据迁移：将 post 原有的 fieldId 回填为对应 field 的 spaceId
--    若旧数据存于 post.fieldId（指向 field.id），需关联 field.spaceId 得到社区空间id
--    注意：post 表在旧结构下字段为 fieldId；若已把列改名为 spaceId 且直接存社区空间id，可跳过本步。
-- UPDATE post p
--   LEFT JOIN field f ON f.id = p.fieldId
--   SET p.spaceId = f.spaceId
--   WHERE p.fieldId IS NOT NULL;

-- 5) user_field_score 表：字段 field_id 语义改为社区空间id；将列重命名为 space_id
--    （保留该表结构，仅把列语义改为 spaceId；新代码 UserFieldScore.spaceId 映射 space_id）
-- ALTER TABLE user_field_score
--     CHANGE COLUMN field_id space_id BIGINT NULL COMMENT '社区空间id(原花田id)';

-- 6) 可选：分类数据初始化（按需插入）
-- INSERT INTO category (category, sort, status) VALUES
--    ('程序员', 1, 1), ('动漫', 2, 1), ('游戏', 3, 1), ('生活', 4, 1), ('娱乐', 5, 1);