/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 50729 (5.7.29-log)
 Source Host           : localhost:3306
 Source Schema         : happy-companion-flower-field

 Target Server Type    : MySQL
 Target Server Version : 50729 (5.7.29-log)
 File Encoding         : 65001

 Date: 04/09/2026 10:12:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category` varchar(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL COMMENT '分类名称',
  `sort` int(11) NULL DEFAULT 0 COMMENT '顺序',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '0禁用/1启用',
  `createTime` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = latin1 COLLATE = latin1_swedish_ci COMMENT = '空间分类(单级)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------

-- ----------------------------
-- Table structure for chat_list
-- ----------------------------
DROP TABLE IF EXISTS `chat_list`;
CREATE TABLE `chat_list`  (
  `listId` int(11) NOT NULL AUTO_INCREMENT,
  `linkId` int(11) NULL DEFAULT NULL,
  `userId` bigint(20) NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `createtime` datetime NULL DEFAULT NULL,
  `toId` bigint(20) NULL DEFAULT NULL COMMENT '接收者 id',
  `isRead` tinyint(4) NOT NULL DEFAULT 0 COMMENT '接收者是否已读：0-未读 1-已读',
  PRIMARY KEY (`listId`) USING BTREE,
  INDEX `idx_toId_isRead`(`toId`, `isRead`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of chat_list
-- ----------------------------
INSERT INTO `chat_list` VALUES (1, 1, 1957032172908269570, '你好', '2026-04-27 17:24:27', NULL, 1);
INSERT INTO `chat_list` VALUES (2, 1, 1957032172908269570, '你好，有什么问题吗', '2026-04-27 17:59:56', NULL, 1);
INSERT INTO `chat_list` VALUES (3, 2, 1957032172908269570, '你好', '2026-04-27 18:00:15', NULL, 1);
INSERT INTO `chat_list` VALUES (4, 3, 1957032172908269570, 'hi', '2026-04-27 18:00:23', NULL, 1);
INSERT INTO `chat_list` VALUES (5, 1, 1956985828009132034, 'hi', '2026-04-27 18:02:15', NULL, 1);
INSERT INTO `chat_list` VALUES (6, 4, 1956985828009132034, 'hi', '2026-04-27 18:03:32', NULL, 1);
INSERT INTO `chat_list` VALUES (7, 1, 1956985828009132034, 'hi', '2026-04-27 18:08:25', NULL, 1);
INSERT INTO `chat_list` VALUES (8, 1, 1956985828009132034, 'zm', '2026-04-27 18:09:06', NULL, 1);
INSERT INTO `chat_list` VALUES (9, 1, 1956985828009132034, 'c', '2026-04-27 18:19:56', NULL, 1);
INSERT INTO `chat_list` VALUES (10, 1, 1956985828009132034, '怎么回事', '2026-04-27 18:45:50', NULL, 1);
INSERT INTO `chat_list` VALUES (11, 1, 1956985828009132034, '好了吗', '2026-04-27 18:50:52', NULL, 1);
INSERT INTO `chat_list` VALUES (12, 1, 1956985828009132034, 'H', '2026-04-27 18:53:43', NULL, 1);
INSERT INTO `chat_list` VALUES (13, 1, 1957032172908269570, '好了，可以正常对话了', '2026-04-27 19:03:25', NULL, 1);
INSERT INTO `chat_list` VALUES (14, 1, 1956985828009132034, '哈喽', '2026-04-15 17:48:59', NULL, 1);
INSERT INTO `chat_list` VALUES (15, 1, 1957032172908269570, 'hi', '2026-04-27 10:26:36', NULL, 1);
INSERT INTO `chat_list` VALUES (16, 3, 1957032172908269570, '今天怎么样？', '2026-05-11 15:40:47', NULL, 1);
INSERT INTO `chat_list` VALUES (17, 1, 1957032172908269570, '今天怎么样', '2026-05-27 11:15:28', NULL, 1);
INSERT INTO `chat_list` VALUES (18, 1, 1956985828009132034, '不错', '2026-05-27 11:15:39', NULL, 1);
INSERT INTO `chat_list` VALUES (19, 1, 1957032172908269570, '主页的帖子不错', '2026-05-27 11:25:32', NULL, 1);
INSERT INTO `chat_list` VALUES (20, 1, 1956985828009132034, '谢谢', '2026-05-27 11:25:45', NULL, 1);
INSERT INTO `chat_list` VALUES (21, 1, 1956985828009132034, 'hi', '2026-08-25 12:16:48', 1957032172908269570, 1);
INSERT INTO `chat_list` VALUES (22, 1, 1956985828009132034, '这边天气不错', '2026-08-25 12:17:30', 1957032172908269570, 1);

-- ----------------------------
-- Table structure for field
-- ----------------------------
DROP TABLE IF EXISTS `field`;
CREATE TABLE `field`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '队伍名称',
  `fieldPicUrl` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '队伍封面',
  `description` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `maxNum` int(11) NULL DEFAULT 1 COMMENT '最大人数',
  `userId` bigint(20) NULL DEFAULT NULL COMMENT '用户id',
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '0 - 公开，1 - 私有，2 - 加密',
  `password` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `spaceId` bigint(20) NOT NULL COMMENT '空间(团队图库) id',
  `firstCategory` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '一级分类',
  `secondCategory` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '二级分类',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `postNum` int(11) NULL DEFAULT NULL COMMENT '帖子数量',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2090270615485345795 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '花田' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of field
-- ----------------------------
INSERT INTO `field` VALUES (1981969199210491906, 'Re:从零开始的异世界生活', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_cHDNprGtUPZDJ6de.webp', '即使你忘记了我，我也不会忘记你', 4, 1956985828009132034, 0, '', 1981969199176937474, '动漫宅', '动漫', '2026-04-25 14:20:54', '2026-04-05 18:10:18', 0, NULL);
INSERT INTO `field` VALUES (1982039684778598402, '公主连结', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-10-25_S4kbkuvRf8cq4Csm.webp', '结哥别死', NULL, 1957032172908269570, 0, '', 1982039684560494594, NULL, NULL, '2026-04-25 19:01:00', '2026-04-02 16:22:47', 0, NULL);
INSERT INTO `field` VALUES (1982078321423572993, 'java兴趣花田', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-10-25_SKU6bRTsxPykZHZ7.webp', 'java', NULL, 1957032183779905538, 0, '', 1982078321301938178, '程序员', '后端开发', '2026-04-25 21:34:31', '2026-04-05 18:10:59', 0, NULL);
INSERT INTO `field` VALUES (1986341066856574978, '前端美化技术兴趣花田', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986341066734940161/2025-11-06_Qzjs7Ac7jyX5Htlr.webp', '前端的一百种美化方法', NULL, 1957032183779905538, 0, '', 1986341066734940161, '程序员', '前端开发', '2026-04-06 15:53:09', '2026-05-11 15:16:02', 0, NULL);
INSERT INTO `field` VALUES (1986618482140438530, '前端框架vue', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986618482027192321/2025-11-07_YlrgF23Wymi5UgSi.webp', '主流框架vue', NULL, 1957032172908269570, 0, '', 1986618482027192321, '程序员', '前端开发', '2026-04-07 10:15:30', '2026-04-07 10:16:36', 0, NULL);
INSERT INTO `field` VALUES (1986619202860277762, 'python技术交流', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2025-11-07_CD5BJplfLDz8mHVJ.webp', 'python技术交流', NULL, 1957032183779905538, 0, '', 1986619202856083457, '程序员', '后端开发', '2026-04-07 10:18:22', '2026-04-07 10:19:19', 0, NULL);
INSERT INTO `field` VALUES (1986619913408929793, 'Linux运维交流', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619913400541186/2025-11-07_fbiOCHEDxYynQW4p.webp', 'Linux运维交流', NULL, 1957032183779905538, 0, '', 1986619913400541186, '程序员', '运维', '2026-04-07 10:21:11', '2026-04-07 10:21:36', 0, NULL);
INSERT INTO `field` VALUES (1986620185019473922, '云计算运维交流', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986620184998502401/2025-11-07_RmdJHA9O0E0SjuFO.webp', '云计算运维', NULL, 1957032183779905538, 0, '', 1986620184998502401, '程序员', '运维', '2026-04-07 10:22:16', '2026-04-07 10:23:15', 0, NULL);
INSERT INTO `field` VALUES (1986620964610564098, '路人女主秘密花田', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986620964593786881/2025-11-07_VaTIRxyLo1x8yGmq.webp', '路人女主秘密花田', NULL, 1957032197113597954, 1, '', 1986620964593786881, '动漫宅', '动漫', '2026-04-07 10:25:22', '2026-04-07 12:19:24', 1, NULL);
INSERT INTO `field` VALUES (1986622914475368450, '路人女主加密花田', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986622914441814018/2025-11-07_jhiZBs5a2gOgF2FU.webp', '路人女主加密花田', NULL, 1957032197113597954, 2, '12345678zxc', 1986622914441814018, '动漫宅', '轻小说', '2026-04-07 10:33:07', '2026-04-07 10:33:17', 0, NULL);
INSERT INTO `field` VALUES (1986720492869148674, '路人女主秘密花田', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986720492802039809/2025-11-07_aGTPmcdB6DM9QfBG.webp', '路人女主秘密花田', 10, 1956985828009132034, 1, '', 1986720492802039809, '动漫宅', '动漫', '2026-04-07 17:00:51', '2026-08-20 10:50:04', 1, NULL);
INSERT INTO `field` VALUES (1986721650585133058, '美食殿堂', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986721650559967234/2025-11-07_HtxfSetxXGLIDLLH.webp', '吃遍天下美食', NULL, 1957032197113597954, 0, '', 1986721650559967234, '生活', '美食', '2026-04-07 17:05:27', '2026-04-07 17:07:38', 0, NULL);
INSERT INTO `field` VALUES (1986722832753909762, '黑暗之魂3交流', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986722832732938242/2025-11-07_Nw6w8PDtkBfQkfnW.webp', '黑暗之魂3交流', 100, 1957032197113597954, 0, '', 1986722832732938242, '游戏', '3A单机', '2026-04-07 17:10:09', '2026-04-07 17:10:38', 0, NULL);
INSERT INTO `field` VALUES (1986748448161218562, 'cs1', '', 'cs', 1, 1956985828009132034, 1, '', 1986748448073138177, '娱乐', '网络红人', '2026-04-07 18:51:56', '2026-04-07 18:53:05', 1, NULL);
INSERT INTO `field` VALUES (1986748836641849346, 'cs在', '', 'cs', 123, 1956985828009132034, 0, '', 1986748836625072129, '娱乐', '网络红人', '2026-04-07 18:53:29', '2026-04-07 18:54:13', 1, NULL);
INSERT INTO `field` VALUES (2090270615485345794, '测试创建者退出花田问题', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/2090270615435014146/2026-08-20_crqeZm6U8hhhV4py.webp', '测试用', NULL, 1957032172908269570, 0, '', 2090270615435014146, '程序员', '测试', '2026-08-20 10:52:04', '2026-08-20 10:52:53', 0, NULL);

-- ----------------------------
-- Table structure for file
-- ----------------------------
DROP TABLE IF EXISTS `file`;
CREATE TABLE `file`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片 url',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片名称',
  `introduction` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '简介',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类',
  `tags` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（JSON 数组）',
  `coverUrl` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图片URL',
  `duration` int(11) NULL DEFAULT NULL COMMENT '视频时长（秒）',
  `size` bigint(20) NULL DEFAULT NULL COMMENT '图片体积',
  `picWidth` int(11) NULL DEFAULT NULL COMMENT '图片宽度',
  `picHeight` int(11) NULL DEFAULT NULL COMMENT '图片高度',
  `picScale` double NULL DEFAULT NULL COMMENT '图片宽高比例',
  `format` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '格式',
  `userId` bigint(20) NOT NULL COMMENT '创建用户 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `reviewStatus` int(11) NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
  `reviewMessage` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核信息',
  `reviewerId` bigint(20) NULL DEFAULT NULL COMMENT '审核人 ID',
  `reviewTime` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `thumbnailUrl` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '缩略图 url',
  `spaceId` bigint(20) NULL DEFAULT NULL COMMENT '空间 id（为空表示公共空间）',
  `picColor` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片主色调',
  `fileType` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件类型',
  `summary` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI 生成的摘要',
  `canDownload` int(11) NULL DEFAULT 1 COMMENT '是否允许下载：0-不允许 1-允许（默认允许，发布帖子时可选）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_introduction`(`introduction`) USING BTREE,
  INDEX `idx_category`(`category`) USING BTREE,
  INDEX `idx_tags`(`tags`) USING BTREE,
  INDEX `idx_userId`(`userId`) USING BTREE,
  INDEX `idx_reviewStatus`(`reviewStatus`) USING BTREE,
  INDEX `idx_spaceId`(`spaceId`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2094713125179805699 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file
-- ----------------------------
INSERT INTO `file` VALUES (1981971519432355842, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_cHDNprGtUPZDJ6de.webp', '6305ff255a4b798ebe989b7c002e8e917749421f', NULL, NULL, NULL, NULL, NULL, 1048796, 1920, 1080, 1.78, 'webp', 1956985828009132034, '2025-10-25 14:30:08', '2025-10-25 14:30:08', '2025-10-25 14:30:08', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 14:30:08', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_cHDNprGtUPZDJ6de_thumbnail.jpg', 1981969199176937474, '0xa6c2df', NULL, NULL);
INSERT INTO `file` VALUES (1982003607053283329, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_MTXhFRQqHJ0Vmqji.webp', '艾米莉亚1', NULL, NULL, NULL, NULL, NULL, 109682, 800, 1069, 0.75, 'webp', 1956985828009132034, '2025-10-25 16:37:38', '2025-10-25 16:37:38', '2026-05-10 11:28:10', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 16:37:38', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_MTXhFRQqHJ0Vmqji_thumbnail.png', 1981969199176937474, '0x917791', NULL, NULL);
INSERT INTO `file` VALUES (1982028743227777026, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_LHwdhMM3uMsd1J09.webp', 'e9ee0a594bd81d578393bd0a9f2e26e4fb91519a', NULL, '美图', NULL, NULL, NULL, 69478, 1920, 1080, 1.78, 'webp', 1957032172908269570, '2025-10-25 18:17:31', '2025-10-25 18:17:31', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_LHwdhMM3uMsd1J09_thumbnail.jpg', 1981969199176937474, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1982044771735351297, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-10-25_S4kbkuvRf8cq4Csm.webp', '20200428085453_ttAMQ', NULL, NULL, NULL, NULL, NULL, 68752, 1000, 1000, 1, 'webp', 1957032172908269570, '2025-10-25 19:21:12', '2025-10-25 19:21:12', '2025-10-25 19:21:12', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 19:21:13', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-10-25_S4kbkuvRf8cq4Csm_thumbnail.jpeg', 1982039684560494594, '0x5d7dd6', NULL, NULL);
INSERT INTO `file` VALUES (1982045643999588353, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-10-25_mFBwPiqXPJ6mXeCz.webp', '148952949', NULL, NULL, NULL, NULL, NULL, 63786, 700, 573, 1.22, 'webp', 1957032172908269570, '2025-10-25 19:24:40', '2025-10-25 19:24:40', '2025-10-25 19:24:40', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 19:24:41', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-10-25_mFBwPiqXPJ6mXeCz_thumbnail.jpg', 1982039684560494594, '0x4281e0', NULL, NULL);
INSERT INTO `file` VALUES (1982047866502520834, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2025-10-25_ICA8wZ8YRWE8Wghv.webp', '公主链接-美食殿堂', NULL, NULL, '[]', NULL, NULL, 1592746, 3000, 3000, 1, 'webp', 1957032172908269570, '2025-10-25 19:33:30', '2025-10-25 19:41:37', '2025-10-25 19:41:37', 0, 1, '管理员操作通过', 1956985828009132034, '2025-10-25 19:39:53', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2025-10-25_ICA8wZ8YRWE8Wghv_thumbnail.', NULL, '0xe0e091', NULL, NULL);
INSERT INTO `file` VALUES (1982050247223025666, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_tpyoi7XBi42isuHL.webp', '美食殿堂-露营小歇', NULL, '美图', '[\"动漫\",\"人物\",\"游戏\"]', NULL, NULL, 153702, 1062, 1500, 0.71, 'webp', 1957032172908269570, '2025-10-25 19:42:58', '2025-10-25 19:45:11', '2025-10-25 19:45:11', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 19:42:58', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_tpyoi7XBi42isuHL_thumbnail.', 1982050098652389377, '0x476364', NULL, NULL);
INSERT INTO `file` VALUES (1982051910251978754, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_P0fOz39ChgUIcPOB.webp', '06821140fdc71f831dba6d4a2b393c9df4c894c4', NULL, NULL, NULL, NULL, NULL, 387112, 2560, 1440, 1.78, 'webp', 1957032172908269570, '2025-10-25 19:49:34', '2025-10-25 19:49:38', '2025-10-25 19:49:37', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 19:49:35', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_P0fOz39ChgUIcPOB_thumbnail.', 1982050098652389377, '0x253e76', NULL, NULL);
INSERT INTO `file` VALUES (1982052278327320577, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2025-10-25_qzSkhnUcVwLwbCg0.webp', '06821140fdc71f831dba6d4a2b393c9df4c894c4', NULL, NULL, NULL, NULL, NULL, 387112, 2560, 1440, 1.78, 'webp', 1957032172908269570, '2025-10-25 19:51:02', '2025-10-25 19:51:09', '2025-11-06 21:11:00', 1, 1, '管理员操作通过', 1956985828009132034, '2025-11-06 21:10:30', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2025-10-25_qzSkhnUcVwLwbCg0_thumbnail.', NULL, '0x253e76', NULL, NULL);
INSERT INTO `file` VALUES (1982053382238773250, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2025-10-25_R0Q6cwaQzA5EUUpb.webp', 'e9ee0a594bd81d578393bd0a9f2e26e4fb91519a', NULL, NULL, NULL, NULL, NULL, 69478, 1920, 1080, 1.78, 'webp', 1957032172908269570, '2025-10-25 19:55:25', '2025-10-25 19:55:31', '2025-10-25 19:55:31', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 19:55:26', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2025-10-25_R0Q6cwaQzA5EUUpb_thumbnail.', 1982053156065124354, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1982054012160319490, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_Ev1FMwxXT4BFhZc5.webp', '惠', NULL, NULL, '[]', NULL, NULL, 101450, 1131, 800, 1.41, 'webp', 1957032172908269570, '2025-10-25 19:57:55', '2025-10-25 20:07:37', '2025-10-25 20:07:36', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 19:58:49', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_Ev1FMwxXT4BFhZc5_thumbnail.jpg', 1982050098652389377, '0xe0acc6', NULL, NULL);
INSERT INTO `file` VALUES (1982056502268272641, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_1e0qxClI2Z9lSrS7.webp', 'u=3507289656,1188725908&fm=253&app=138&f=JPEG', NULL, NULL, NULL, NULL, NULL, 83924, 1131, 800, 1.41, 'webp', 1957032172908269570, '2025-10-25 20:07:49', '2025-10-25 20:09:12', '2025-10-25 20:09:11', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 20:07:50', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_1e0qxClI2Z9lSrS7_thumbnail.jpg', 1982050098652389377, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1982057442522177537, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_VbulNkYHPS79bQgJ.webp', '0GSnnzJGf0Q6mM0.thumb.1000_0', NULL, NULL, NULL, NULL, NULL, 43366, 1000, 751, 1.33, 'webp', 1957032172908269570, '2025-10-25 20:11:33', '2025-10-25 20:11:43', '2025-10-25 20:11:42', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 20:11:34', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_VbulNkYHPS79bQgJ_thumbnail.png', 1982050098652389377, '0xe0a855', NULL, NULL);
INSERT INTO `file` VALUES (1982057618527756290, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2025-10-25_KIAeAWXIdby8qqAf.webp', 'u=4112977451,1620392050&fm=253&app=138&f=JPEG', NULL, NULL, NULL, NULL, NULL, 85394, 800, 1200, 0.67, 'webp', 1957032172908269570, '2025-10-25 20:12:15', '2025-10-25 20:12:22', '2025-10-25 20:12:21', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 20:12:16', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2025-10-25_KIAeAWXIdby8qqAf_thumbnail.jpg', 1982053156065124354, '0x3d4659', NULL, NULL);
INSERT INTO `file` VALUES (1982059684079869953, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_gppipqsWVeqgmZfA.webp', 'W3Cy9AAve-Mujica-s-1st-Album-Completeness-Ave-Mujica-mViWBz', NULL, NULL, NULL, NULL, NULL, 298434, 800, 1117, 0.72, 'webp', 1957032172908269570, '2025-10-25 20:20:28', '2025-10-25 20:20:47', '2025-10-25 20:20:47', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 20:20:28', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_gppipqsWVeqgmZfA_thumbnail.jpg', 1982050098652389377, '0x5e7b7b', NULL, NULL);
INSERT INTO `file` VALUES (1982060581765783553, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_CLj03d4RYm6IHiw1.webp', 'u=1982930871,2794068901&fm=253&app=138&f=JPEG', NULL, NULL, NULL, NULL, NULL, 84586, 800, 1200, 0.67, 'webp', 1956985828009132034, '2025-10-25 20:24:02', '2025-10-25 20:24:02', '2025-10-25 20:24:02', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:24:02', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_CLj03d4RYm6IHiw1_thumbnail.jpg', 1982050098652389377, '0xa1715a', NULL, NULL);
INSERT INTO `file` VALUES (1982060859835555841, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_xnFOExGI1ho1ocfz.webp', '公主连接logo', NULL, NULL, NULL, NULL, NULL, 38998, 891, 500, 1.78, 'webp', 1956985828009132034, '2025-10-25 20:25:08', '2025-10-25 20:25:37', '2025-10-25 20:25:36', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:25:09', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_xnFOExGI1ho1ocfz_thumbnail.png', 1982050098652389377, '0xe06b88', NULL, NULL);
INSERT INTO `file` VALUES (1982061376670277634, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_1KEfbooUTS9zEbwe.webp', 'edf80873fa305e6414c5cbe282e50296', NULL, NULL, NULL, NULL, NULL, 139482, 1920, 1080, 1.78, 'webp', 1956985828009132034, '2025-10-25 20:27:11', '2025-10-25 20:27:11', '2025-10-25 20:27:11', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:27:12', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_1KEfbooUTS9zEbwe_thumbnail.', 1982050098652389377, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1982061531091968002, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_Ld1Xe9joaVkzIRcN.webp', 'edf80873fa305e6414c5cbe282e50296', NULL, NULL, NULL, NULL, NULL, 139482, 1920, 1080, 1.78, 'webp', 1956985828009132034, '2025-10-25 20:27:48', '2025-10-25 20:27:48', '2025-10-25 20:27:48', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:27:49', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_Ld1Xe9joaVkzIRcN_thumbnail.', 1982050098652389377, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1982061829776744450, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_ceBM1ll18jJn3dfL.webp', 'edf80873fa305e6414c5cbe282e50296', NULL, NULL, NULL, NULL, NULL, 139482, 1920, 1080, 1.78, 'webp', 1956985828009132034, '2025-10-25 20:28:59', '2025-10-25 20:28:59', '2025-10-25 20:28:59', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:29:00', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_ceBM1ll18jJn3dfL_thumbnail.', 1982050098652389377, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1982062055753261058, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_Vb0Ctn5i8zwdaLPY.webp', '20200622142930_lrqwy', NULL, NULL, NULL, NULL, NULL, 80474, 718, 718, 1, 'webp', 1956985828009132034, '2025-10-25 20:29:53', '2025-10-25 20:29:53', '2025-10-25 20:29:53', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:29:54', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_Vb0Ctn5i8zwdaLPY_thumbnail.', 1982050098652389377, '0x415a5c', NULL, NULL);
INSERT INTO `file` VALUES (1982062508595486722, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_dI5L8nZirNBPKLO5.webp', '20210129211550_0e324', NULL, NULL, NULL, NULL, NULL, 445020, 974, 974, 1, 'webp', 1956985828009132034, '2025-10-25 20:31:41', '2025-10-25 20:31:41', '2025-10-25 20:31:41', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:31:42', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_dI5L8nZirNBPKLO5_thumbnail.', 1982050098652389377, '0xa6896c', NULL, NULL);
INSERT INTO `file` VALUES (1982062804663017473, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_kghrQGFI9KW4kXI3.webp', '%E8%A1%8C%E4%BC%9A%E6%8B%9B%E5%8B%9F', NULL, NULL, NULL, NULL, NULL, 154168, 1200, 800, 1.5, 'webp', 1956985828009132034, '2025-10-25 20:32:52', '2025-10-25 20:32:52', '2025-10-25 20:32:52', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:32:52', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_kghrQGFI9KW4kXI3_thumbnail.', 1982050098652389377, '0xa3c0de', NULL, NULL);
INSERT INTO `file` VALUES (1982063816043929602, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2025-11-06_88KxyV1UJDF2crf2.webp', '%E8%A1%8C%E4%BC%9A%E6%8B%9B%E5%8B%9F', NULL, NULL, NULL, NULL, NULL, 38380, 600, 480, 1.25, 'webp', 1957032172908269570, '2025-10-25 20:36:53', '2025-11-06 16:38:13', '2025-11-06 16:38:13', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-11-06 16:38:13', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2025-11-06_88KxyV1UJDF2crf2_thumbnail.png', 1982053156065124354, '0xa5c2df', NULL, NULL);
INSERT INTO `file` VALUES (1982065860154761218, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_2IuU4luW5XmtoBhS.webp', 'OIP-C', NULL, NULL, NULL, NULL, NULL, 19772, 474, 296, 1.6, 'WEBP', 1957032172908269570, '2025-10-25 20:45:00', '2025-10-25 20:45:24', '2025-10-25 20:45:23', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 20:45:01', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_2IuU4luW5XmtoBhS_thumbnail.', 1982050098652389377, '0x4b6370', NULL, NULL);
INSERT INTO `file` VALUES (1982066990905565186, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_rrAxnV6g1HmGxVxk.webp', 'OIP-C', NULL, NULL, NULL, NULL, NULL, 4154, 244, 195, 1.25, 'webp', 1957032172908269570, '2025-10-25 20:49:30', '2025-10-25 20:49:43', '2025-10-25 20:49:43', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-10-25 20:49:43', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982050098652389377/2025-10-25_rrAxnV6g1HmGxVxk_thumbnail.png', 1982050098652389377, '0x7f5f9e', NULL, NULL);
INSERT INTO `file` VALUES (1982067473967751170, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_ge6h0oYc2NfMYIvw.webp', '二次元1', NULL, NULL, NULL, NULL, NULL, 49672, 474, 1002, 0.47, 'webp', 1956985828009132034, '2025-10-25 20:51:25', '2025-10-25 20:51:25', '2025-11-06 21:10:57', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:25', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_ge6h0oYc2NfMYIvw_thumbnail.', NULL, '0x87a3dc', NULL, NULL);
INSERT INTO `file` VALUES (1982067479751696386, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_JkLvyE8PXiDH5O6Q.webp', '二次元2', NULL, NULL, NULL, NULL, NULL, 51114, 467, 620, 0.75, 'webp', 1956985828009132034, '2025-10-25 20:51:26', '2025-10-25 20:51:26', '2025-11-06 21:11:10', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:27', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_JkLvyE8PXiDH5O6Q_thumbnail.', NULL, '0x6c88a5', NULL, NULL);
INSERT INTO `file` VALUES (1982067483513987074, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_I5lfU1HzltN57IUo.webp', '二次元3', NULL, NULL, NULL, NULL, NULL, 68118, 474, 1027, 0.46, 'webp', 1956985828009132034, '2025-10-25 20:51:27', '2025-10-25 20:51:27', '2025-11-06 21:11:12', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:28', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_I5lfU1HzltN57IUo_thumbnail.', NULL, '0xddc3a9', NULL, NULL);
INSERT INTO `file` VALUES (1982067486387085313, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_J0ZuUVWx9F7DSbiP.webp', '二次元4', NULL, NULL, NULL, NULL, NULL, 39608, 474, 629, 0.75, 'webp', 1956985828009132034, '2025-10-25 20:51:28', '2025-10-25 20:51:28', '2025-11-06 21:11:14', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:28', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_J0ZuUVWx9F7DSbiP_thumbnail.', NULL, '0x51a51', NULL, NULL);
INSERT INTO `file` VALUES (1982067489948049410, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_iEdNoimndJntIZvI.webp', '二次元5', NULL, NULL, NULL, NULL, NULL, 69178, 474, 842, 0.56, 'webp', 1956985828009132034, '2025-10-25 20:51:29', '2025-10-25 20:51:29', '2025-11-06 21:11:15', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:29', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_iEdNoimndJntIZvI_thumbnail.', NULL, '0x223f5e', NULL, NULL);
INSERT INTO `file` VALUES (1982067492972142593, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_kynu4JBlCe9k3dtm.webp', '二次元6', NULL, NULL, NULL, NULL, NULL, 28960, 474, 669, 0.71, 'webp', 1956985828009132034, '2025-10-25 20:51:30', '2025-10-25 20:51:30', '2025-11-06 21:11:16', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:30', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_kynu4JBlCe9k3dtm_thumbnail.', NULL, '0xceb196', NULL, NULL);
INSERT INTO `file` VALUES (1982067495987847169, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_rBfDPohzPHKR9HLO.webp', '二次元7', NULL, NULL, NULL, NULL, NULL, 33742, 474, 710, 0.67, 'webp', 1956985828009132034, '2025-10-25 20:51:30', '2025-10-25 20:51:30', '2025-11-06 21:11:18', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:31', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_rBfDPohzPHKR9HLO_thumbnail.', NULL, '0x3c3973', NULL, NULL);
INSERT INTO `file` VALUES (1982067498735116289, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_ytNwJU78Cx9jYP3U.webp', '二次元8', NULL, NULL, NULL, NULL, NULL, 52238, 474, 1017, 0.47, 'webp', 1956985828009132034, '2025-10-25 20:51:31', '2025-10-25 20:51:31', '2025-11-06 21:11:19', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:31', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_ytNwJU78Cx9jYP3U_thumbnail.', NULL, '0x47161b', NULL, NULL);
INSERT INTO `file` VALUES (1982067501784375298, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_3yCVFXPXJIIoIX6Z.webp', '二次元9', NULL, NULL, NULL, NULL, NULL, 56272, 474, 842, 0.56, 'webp', 1956985828009132034, '2025-10-25 20:51:32', '2025-10-25 20:51:32', '2025-11-06 21:11:21', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:32', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_3yCVFXPXJIIoIX6Z_thumbnail.', NULL, '0x3e3e73', NULL, NULL);
INSERT INTO `file` VALUES (1982067504208683010, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_22Yt9KPsmyPWgLML.webp', '二次元10', NULL, NULL, NULL, NULL, NULL, 61332, 474, 842, 0.56, 'webp', 1956985828009132034, '2025-10-25 20:51:32', '2025-10-25 20:51:32', '2025-11-06 21:11:22', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-10-25 20:51:33', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-10-25_22Yt9KPsmyPWgLML_thumbnail.', NULL, '0x967cb1', NULL, NULL);
INSERT INTO `file` VALUES (1982078528546693121, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-10-25_SKU6bRTsxPykZHZ7.webp', '1775037-20200905103857971-998837168', NULL, NULL, NULL, NULL, NULL, 13800, 440, 288, 1.53, 'webp', 1957032183779905538, '2025-10-25 21:35:21', '2025-10-25 21:35:21', '2025-10-25 21:35:21', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2025-10-25 21:35:21', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-10-25_SKU6bRTsxPykZHZ7_thumbnail.png', 1982078321301938178, '0x437777', NULL, NULL);
INSERT INTO `file` VALUES (1982079149203021826, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-10-25_osUl1owZM5mzUlWD.webp', '0ff41bd5ad6eddc451dab344b583a1fd5266d016954e', NULL, NULL, NULL, NULL, NULL, 43444, 1366, 868, 1.57, 'webp', 1957032183779905538, '2025-10-25 21:37:49', '2025-10-25 21:37:49', '2025-10-25 21:37:49', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2025-10-25 21:37:49', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-10-25_osUl1owZM5mzUlWD_thumbnail.png', 1982078321301938178, '0xc0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1984822482367213570, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-02_69DgISW7kyvpdwb3.webp', '286c80b30f2442a7180855b39743ad4bd1130264', NULL, '美图', NULL, NULL, NULL, 62356, 580, 823, 0.7, 'webp', 1956985828009132034, '2025-11-02 11:18:50', '2025-11-02 11:18:50', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-02 11:18:51', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-02_69DgISW7kyvpdwb3_thumbnail.jpg', 1981969199176937474, '0x336a86', NULL, NULL);
INSERT INTO `file` VALUES (1984824050210631682, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-11-02_fLHRjXMX0KuDnfio.webp', '屏幕截图 2025-11-02 112409', NULL, NULL, NULL, NULL, NULL, 69426, 915, 495, 1.85, 'webp', 1957032172908269570, '2025-11-02 11:25:04', '2025-11-02 11:25:04', '2025-11-02 11:25:04', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-11-02 11:25:04', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-11-02_fLHRjXMX0KuDnfio_thumbnail.png', 1982039684560494594, '0x9fbedd', NULL, NULL);
INSERT INTO `file` VALUES (1984825976201449473, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-02_xFCbIZmWL7WVpbA7.webp', 'f5940d26cffc1e177f833b210c90f603738de9a5', NULL, '美图', NULL, NULL, NULL, 30952, 1080, 591, 1.83, 'webp', 1956985828009132034, '2025-11-02 11:32:43', '2025-11-02 11:32:43', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-02 11:32:44', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-02_xFCbIZmWL7WVpbA7_thumbnail.jpg', 1981969199176937474, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1985256879935488001, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-11-03_miPvStKCrAdcDdB8.webp', '屏幕截图 2025-11-03 160443', NULL, NULL, NULL, NULL, NULL, 98072, 2155, 1347, 1.6, 'webp', 1957032183779905538, '2025-11-03 16:04:59', '2025-11-03 16:04:59', '2025-11-03 16:04:59', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2025-11-03 16:04:59', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-11-03_miPvStKCrAdcDdB8_thumbnail.png', 1982078321301938178, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1985268241956896769, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-11-03_5OYqoYuhvV3wn8Fu.webp', '凯露表情包', NULL, NULL, NULL, NULL, NULL, 53380, 807, 808, 1, 'webp', 1957032172908269570, '2025-11-03 16:50:08', '2025-11-03 16:50:08', '2025-11-03 16:50:08', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-11-03 16:50:08', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-11-03_5OYqoYuhvV3wn8Fu_thumbnail.png', 1982039684560494594, '0x5a3e5a', NULL, NULL);
INSERT INTO `file` VALUES (1985924783077945346, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2025-11-05_XOJbLI2TW54X4Ylm.webp', '984e04d4b31c8701b7fc36552f7f9e2f0608fffd', NULL, NULL, NULL, NULL, NULL, 839988, 800, 1300, 0.62, 'webp', 1957032172908269570, '2025-11-05 12:18:59', '2025-11-05 12:19:02', '2025-11-05 12:19:02', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-11-05 12:19:00', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2025-11-05_XOJbLI2TW54X4Ylm_thumbnail.', 1982053156065124354, '0x000', NULL, NULL);
INSERT INTO `file` VALUES (1985924930180575234, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-08-24_tfs9wbV1mp87UwLY.webp', 'cdf68546f21fbe0996bd0aa563600c338644ad5b', NULL, '美图', NULL, NULL, NULL, 39654, 431, 581, 0.74, 'webp', 1957032172908269570, '2025-11-05 12:19:34', '2026-08-24 11:35:27', '2026-09-03 19:16:31', 0, 1, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-08-24_tfs9wbV1mp87UwLY_thumbnail.png', 1981969199176937474, '0x8f74aa', NULL, NULL);
INSERT INTO `file` VALUES (1985925082282815490, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-05_W0IItuyGjPURDfzo.webp', '2025-11-05_XOJbLI2TW54X4Ylm', NULL, '美图', NULL, NULL, NULL, 839988, 800, 1300, 0.62, 'WEBP', 1957032172908269570, '2025-11-05 12:20:10', '2025-11-05 12:20:10', '2026-09-03 19:16:31', 0, 1, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-05_W0IItuyGjPURDfzo_thumbnail.', 1981969199176937474, '0x000', NULL, NULL);
INSERT INTO `file` VALUES (1986342719647232001, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986341066734940161/2025-11-06_Qzjs7Ac7jyX5Htlr.webp', '屏幕截图 2025-11-06 155934', NULL, NULL, NULL, NULL, NULL, 59906, 1327, 714, 1.86, 'webp', 1957032172908269570, '2025-11-06 15:59:43', '2025-11-06 15:59:43', '2025-11-06 15:59:43', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-11-06 15:59:43', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986341066734940161/2025-11-06_Qzjs7Ac7jyX5Htlr_thumbnail.png', 1986341066734940161, '0xa084bd', NULL, NULL);
INSERT INTO `file` VALUES (1986378036072787970, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2025-11-06_TTDPRbYoXgewT8uq.webp', 'c5240ebcab51f53e43378fbdadd92f81298252208', NULL, NULL, NULL, NULL, NULL, 70940, 832, 1216, 0.68, 'WEBP', 1957032172908269570, '2025-11-06 18:20:03', '2025-11-06 18:20:50', '2025-11-06 21:11:25', 1, 1, '管理员操作通过', 1956985828009132034, '2025-11-06 21:08:04', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2025-11-06_TTDPRbYoXgewT8uq_thumbnail.jpg@1192w', NULL, '0x9ab4d0', NULL, NULL);
INSERT INTO `file` VALUES (1986420491903406082, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_RllduMZTewlH1oHi.webp', '动漫少女乐队1', NULL, NULL, NULL, NULL, NULL, 22406, 474, 345, 1.37, 'webp', 1956985828009132034, '2025-11-06 21:08:45', '2025-11-06 21:08:45', '2025-11-06 21:08:45', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:46', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_RllduMZTewlH1oHi_thumbnail.', NULL, '0x886e62', NULL, NULL);
INSERT INTO `file` VALUES (1986420496378728449, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_7gHYKOa7C8mHpD2z.webp', '动漫少女乐队2', NULL, NULL, NULL, NULL, NULL, 19858, 474, 302, 1.57, 'webp', 1956985828009132034, '2025-11-06 21:08:46', '2025-11-06 21:08:46', '2025-11-06 21:08:46', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:47', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_7gHYKOa7C8mHpD2z_thumbnail.', NULL, '0xa68d7e', NULL, NULL);
INSERT INTO `file` VALUES (1986420499893555202, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_efFhCRvhFVvUrMJC.webp', '动漫少女乐队3', NULL, NULL, NULL, NULL, NULL, 23672, 474, 266, 1.78, 'webp', 1956985828009132034, '2025-11-06 21:08:47', '2025-11-06 21:08:47', '2025-11-06 21:08:47', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:48', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_efFhCRvhFVvUrMJC_thumbnail.', NULL, '0x506265', NULL, NULL);
INSERT INTO `file` VALUES (1986420504222076929, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_Y4s2kFHbrzhO94MT.webp', '动漫少女乐队4', NULL, NULL, NULL, NULL, NULL, 19602, 223, 297, 0.75, 'webp', 1956985828009132034, '2025-11-06 21:08:48', '2025-11-06 21:08:48', '2025-11-06 21:08:48', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:49', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_Y4s2kFHbrzhO94MT_thumbnail.', NULL, '0x522627', NULL, NULL);
INSERT INTO `file` VALUES (1986420507627851777, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_vSGkDchnO7w3GiX5.webp', '动漫少女乐队5', NULL, NULL, NULL, NULL, NULL, 40518, 474, 266, 1.78, 'webp', 1956985828009132034, '2025-11-06 21:08:49', '2025-11-06 21:08:49', '2025-11-06 21:08:49', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:50', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_vSGkDchnO7w3GiX5_thumbnail.', NULL, '0xd44662', NULL, NULL);
INSERT INTO `file` VALUES (1986420511645995010, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_3TQOIWiQFlyw5c8K.webp', '动漫少女乐队6', NULL, NULL, NULL, NULL, NULL, 54340, 474, 512, 0.93, 'webp', 1956985828009132034, '2025-11-06 21:08:50', '2025-11-06 21:08:50', '2025-11-06 21:08:50', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:51', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_3TQOIWiQFlyw5c8K_thumbnail.', NULL, '0x585284', NULL, NULL);
INSERT INTO `file` VALUES (1986420516041625602, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_DHRy5X3ZkVEMWZsO.webp', '动漫少女乐队7', NULL, NULL, NULL, NULL, NULL, 16114, 474, 292, 1.62, 'webp', 1956985828009132034, '2025-11-06 21:08:51', '2025-11-06 21:08:51', '2025-11-06 21:08:51', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:52', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_DHRy5X3ZkVEMWZsO_thumbnail.', NULL, '0x391e62', NULL, NULL);
INSERT INTO `file` VALUES (1986420520592445442, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_XWgyGWGSXEeYrNhE.webp', '动漫少女乐队8', NULL, NULL, NULL, NULL, NULL, 40276, 474, 842, 0.56, 'webp', 1956985828009132034, '2025-11-06 21:08:52', '2025-11-06 21:08:52', '2025-11-06 21:08:52', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:53', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_XWgyGWGSXEeYrNhE_thumbnail.', NULL, '0x41211f', NULL, NULL);
INSERT INTO `file` VALUES (1986420523834642433, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_8i7wXMtzp8tLx9Js.webp', '动漫少女乐队9', NULL, NULL, NULL, NULL, NULL, 19268, 406, 228, 1.78, 'webp', 1956985828009132034, '2025-11-06 21:08:53', '2025-11-06 21:08:53', '2025-11-06 21:08:53', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:53', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_8i7wXMtzp8tLx9Js_thumbnail.', NULL, '0x6f83b6', NULL, NULL);
INSERT INTO `file` VALUES (1986420527232028674, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_turPrA82Huum2oo7.webp', '动漫少女乐队10', NULL, NULL, NULL, NULL, NULL, 13492, 301, 170, 1.77, 'webp', 1956985828009132034, '2025-11-06 21:08:54', '2025-11-06 21:08:54', '2025-11-06 21:08:54', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:08:54', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_turPrA82Huum2oo7_thumbnail.', NULL, '0x533a56', NULL, NULL);
INSERT INTO `file` VALUES (1986421496405659650, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_AZvwBsP0EVj4q1bo.webp', '二次元1', NULL, NULL, NULL, NULL, NULL, 56318, 474, 842, 0.56, 'webp', 1956985828009132034, '2025-11-06 21:12:45', '2025-11-06 21:12:45', '2025-11-06 21:12:45', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:45', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_AZvwBsP0EVj4q1bo_thumbnail.', NULL, '0x95c8cb', NULL, NULL);
INSERT INTO `file` VALUES (1986421501552070657, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_mmhy4x1p0HthDVIo.webp', '二次元2', NULL, NULL, NULL, NULL, NULL, 68118, 474, 1027, 0.46, 'webp', 1956985828009132034, '2025-11-06 21:12:46', '2025-11-06 21:12:46', '2025-11-06 21:12:46', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:47', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_mmhy4x1p0HthDVIo_thumbnail.', NULL, '0xddc3a9', NULL, NULL);
INSERT INTO `file` VALUES (1986421505687654402, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_WvPhy2CusAToXHYq.webp', '二次元3', NULL, NULL, NULL, NULL, NULL, 29442, 474, 770, 0.62, 'webp', 1956985828009132034, '2025-11-06 21:12:47', '2025-11-06 21:12:47', '2025-11-06 21:12:47', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:48', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_WvPhy2CusAToXHYq_thumbnail.', NULL, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1986421508686581762, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_ief6QxnwtU1Q9Phm.webp', '二次元4', NULL, NULL, NULL, NULL, NULL, 61332, 474, 842, 0.56, 'webp', 1956985828009132034, '2025-11-06 21:12:48', '2025-11-06 21:12:48', '2025-11-06 21:12:48', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:48', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_ief6QxnwtU1Q9Phm_thumbnail.', NULL, '0x967cb1', NULL, NULL);
INSERT INTO `file` VALUES (1986421512251740161, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_HvwcxX2WlBKUvWJP.webp', '二次元5', NULL, NULL, NULL, NULL, NULL, 33742, 474, 710, 0.67, 'webp', 1956985828009132034, '2025-11-06 21:12:49', '2025-11-06 21:12:49', '2025-11-06 21:12:49', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:49', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_HvwcxX2WlBKUvWJP_thumbnail.', NULL, '0x3c3973', NULL, NULL);
INSERT INTO `file` VALUES (1986421516618010626, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_NXu7Uq90sfVT4nvd.webp', '二次元6', NULL, NULL, NULL, NULL, NULL, 24526, 474, 426, 1.11, 'webp', 1956985828009132034, '2025-11-06 21:12:50', '2025-11-06 21:12:50', '2025-11-06 21:12:50', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:50', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_NXu7Uq90sfVT4nvd_thumbnail.', NULL, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1986421522863329282, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_bHu8yGWGxKWNHvNr.webp', '二次元7', NULL, NULL, NULL, NULL, NULL, 69178, 474, 842, 0.56, 'webp', 1956985828009132034, '2025-11-06 21:12:51', '2025-11-06 21:12:51', '2025-11-06 21:12:51', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:52', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_bHu8yGWGxKWNHvNr_thumbnail.', NULL, '0x223f5e', NULL, NULL);
INSERT INTO `file` VALUES (1986421527653224449, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_8rAf2qx5q4UOlIsA.webp', '二次元8', NULL, NULL, NULL, NULL, NULL, 49672, 474, 1002, 0.47, 'webp', 1956985828009132034, '2025-11-06 21:12:52', '2025-11-06 21:12:52', '2025-11-06 21:12:52', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:53', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_8rAf2qx5q4UOlIsA_thumbnail.', NULL, '0x87a3dc', NULL, NULL);
INSERT INTO `file` VALUES (1986421531843334146, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_t4vfkAeEQdNJzmso.webp', '二次元9', NULL, NULL, NULL, NULL, NULL, 35532, 474, 701, 0.68, 'webp', 1956985828009132034, '2025-11-06 21:12:53', '2025-11-06 21:12:53', '2025-11-06 21:12:53', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:54', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_t4vfkAeEQdNJzmso_thumbnail.', NULL, '0x8a5153', NULL, NULL);
INSERT INTO `file` VALUES (1986421535802757121, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_YKAIlcw93Y8SDF2Q.webp', '二次元10', NULL, NULL, NULL, NULL, NULL, 49030, 474, 842, 0.56, 'webp', 1956985828009132034, '2025-11-06 21:12:54', '2025-11-06 21:12:54', '2025-11-06 21:12:54', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-06 21:12:55', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2025-11-06_YKAIlcw93Y8SDF2Q_thumbnail.', NULL, '0x4e316b', NULL, NULL);
INSERT INTO `file` VALUES (1986618710436405249, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986618482027192321/2025-11-07_YlrgF23Wymi5UgSi.webp', '46b2c52a986df78b3c2da0439ca0b3ce7349', NULL, NULL, NULL, NULL, NULL, 3934, 883, 504, 1.75, 'webp', 1957032172908269570, '2025-11-07 10:16:24', '2025-11-07 10:16:24', '2025-11-07 10:16:24', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2025-11-07 10:16:25', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986618482027192321/2025-11-07_YlrgF23Wymi5UgSi_thumbnail.png', 1986618482027192321, '0x40a080', NULL, NULL);
INSERT INTO `file` VALUES (1986619376802258945, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2025-11-07_CD5BJplfLDz8mHVJ.webp', '0', NULL, NULL, NULL, NULL, NULL, 15336, 1920, 1079, 1.78, 'webp', 1957032183779905538, '2025-11-07 10:19:03', '2025-11-07 10:19:03', '2025-11-07 10:19:03', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2025-11-07 10:19:04', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2025-11-07_CD5BJplfLDz8mHVJ_thumbnail.png', 1986619202856083457, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1986620009835978754, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619913400541186/2025-11-07_fbiOCHEDxYynQW4p.webp', '9e6f778bd5e93bcae6b129c3e20e5008dd03cc9b', NULL, NULL, NULL, NULL, NULL, 28844, 2000, 1500, 1.33, 'webp', 1957032183779905538, '2025-11-07 10:21:34', '2025-11-07 10:21:34', '2025-11-07 10:21:34', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2025-11-07 10:21:35', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619913400541186/2025-11-07_fbiOCHEDxYynQW4p_thumbnail.jpg', 1986619913400541186, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1986620415655862274, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986620184998502401/2025-11-07_RmdJHA9O0E0SjuFO.webp', 'u=2438408973,4077962379&fm=224&app=112&f=JPEG', NULL, NULL, NULL, NULL, NULL, 17772, 500, 500, 1, 'webp', 1957032183779905538, '2025-11-07 10:23:11', '2025-11-07 10:23:11', '2025-11-07 10:23:11', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2025-11-07 10:23:11', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986620184998502401/2025-11-07_RmdJHA9O0E0SjuFO_thumbnail.jpg', 1986620184998502401, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1986621024391979010, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986620964593786881/2025-11-07_VaTIRxyLo1x8yGmq.webp', 'u=4173480827,1809895968&fm=253&app=138&f=JPEG', NULL, NULL, NULL, NULL, NULL, 101450, 1131, 800, 1.41, 'webp', 1957032197113597954, '2025-11-07 10:25:36', '2025-11-07 10:25:36', '2025-11-07 10:25:36', 0, 1, '管理员或空间创建者自动过审', 1957032197113597954, '2025-11-07 10:25:37', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986620964593786881/2025-11-07_VaTIRxyLo1x8yGmq_thumbnail.jpg', 1986620964593786881, '0xe0acc6', NULL, NULL);
INSERT INTO `file` VALUES (1986622000876265473, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986621964213854210/2025-11-07_dfKILquPjwpc92lW.webp', 'u=3507289656,1188725908&fm=253&app=138&f=JPEG', NULL, NULL, NULL, NULL, NULL, 83924, 1131, 800, 1.41, 'webp', 1957032197113597954, '2025-11-07 10:29:29', '2025-11-07 10:29:29', '2025-11-07 10:29:29', 0, 1, '管理员或空间创建者自动过审', 1957032197113597954, '2025-11-07 10:29:29', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986621964213854210/2025-11-07_dfKILquPjwpc92lW_thumbnail.jpg', 1986621964213854210, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1986622947404849153, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986622914441814018/2025-11-07_jhiZBs5a2gOgF2FU.webp', 'u=3507289656,1188725908&fm=253&app=138&f=JPEG', NULL, NULL, NULL, NULL, NULL, 83924, 1131, 800, 1.41, 'webp', 1957032197113597954, '2025-11-07 10:33:14', '2025-11-07 10:33:14', '2025-11-07 10:33:14', 0, 1, '管理员或空间创建者自动过审', 1957032197113597954, '2025-11-07 10:33:15', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986622914441814018/2025-11-07_jhiZBs5a2gOgF2FU_thumbnail.jpg', 1986622914441814018, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (1986720530232008706, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986720492802039809/2025-11-07_aGTPmcdB6DM9QfBG.webp', 'u=4173480827,1809895968&fm=253&app=138&f=JPEG', NULL, NULL, NULL, NULL, NULL, 101450, 1131, 800, 1.41, 'webp', 1956985828009132034, '2025-11-07 17:01:00', '2025-11-07 17:01:00', '2025-11-07 17:01:00', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2025-11-07 17:01:01', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986720492802039809/2025-11-07_aGTPmcdB6DM9QfBG_thumbnail.jpg', 1986720492802039809, '0xe0acc6', NULL, NULL);
INSERT INTO `file` VALUES (1986722165486280705, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986721650559967234/2025-11-07_HtxfSetxXGLIDLLH.webp', '002diGk7ly8hxs2wiazl5j60sg0sgtfc02', NULL, NULL, NULL, NULL, NULL, 109712, 690, 690, 1, 'webp', 1957032197113597954, '2025-11-07 17:07:30', '2025-11-07 17:07:30', '2025-11-07 17:07:30', 0, 1, '管理员或空间创建者自动过审', 1957032197113597954, '2025-11-07 17:07:30', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986721650559967234/2025-11-07_HtxfSetxXGLIDLLH_thumbnail.jpg', 1986721650559967234, '0x83201e', NULL, NULL);
INSERT INTO `file` VALUES (1986722943831662593, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986722832732938242/2025-11-07_Nw6w8PDtkBfQkfnW.webp', '1810ffe722a34055ab4ce58ba63433da72484f3e', NULL, NULL, NULL, NULL, NULL, 139016, 1920, 1080, 1.78, 'webp', 1957032197113597954, '2025-11-07 17:10:35', '2025-11-07 17:10:35', '2025-11-07 17:10:35', 0, 1, '管理员或空间创建者自动过审', 1957032197113597954, '2025-11-07 17:10:36', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986722832732938242/2025-11-07_Nw6w8PDtkBfQkfnW_thumbnail.jpg', 1986722832732938242, '0x69540', NULL, NULL);
INSERT INTO `file` VALUES (2041550285686706177, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/upload_6435675227617880819.mp4_f91cd8bdf834161468024bd220317e9e.mp4', 'upload_6435675227617880819.mp4_f91cd8bdf834161468024bd220317e9e.mp4', NULL, '美图', NULL, NULL, NULL, 91070577, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-08 00:14:52', '2026-04-08 00:14:52', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-08 00:14:53', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/thumbnails/upload_6435675227617880819_thumbnail_f91cd8bdf834161468024bd220317e9e.jpg', 1981969199176937474, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2041864820247252993, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-08_UUnK0yf8JnWPgNUr.doc', '2026-04-08_UUnK0yf8JnWPgNUr.doc', NULL, NULL, NULL, NULL, NULL, 168448, NULL, NULL, NULL, 'doc', 1956985828009132034, '2026-04-08 21:04:43', '2026-04-08 21:05:05', '2026-08-25 16:01:08', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-08 21:04:44', 'https://ts4.tc.mm.bing.net/th/id/OIP-C.zWkvn5raPozvdlusbxu-UQAAAA?rs=1&pid=ImgDetMain&o=7&rm=3', 1981969199176937474, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042172003023360001, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-09_hzMUw5xS6I3tBSwK.webp', '138614264_p0_master120', NULL, NULL, NULL, NULL, NULL, 158910, NULL, NULL, NULL, 'webp', 1956985828009132034, '2026-04-09 17:25:21', '2026-04-09 17:25:21', '2026-04-09 17:25:21', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-09 17:25:22', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-09_hzMUw5xS6I3tBSwK_thumbnail.jpg', NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042173333049741314, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/upload_12859349528452279419.mp4_f91cd8bdf834161468024bd220317e9e.mp4', 'upload_12859349528452279419.mp4_f91cd8bdf834161468024bd220317e9e.mp4', NULL, NULL, NULL, NULL, NULL, 91070577, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-09 17:30:39', '2026-04-09 17:30:39', '2026-04-09 17:30:39', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-09 17:30:39', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/thumbnails/upload_12859349528452279419_thumbnail_f91cd8bdf834161468024bd220317e9e.jpg', NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042173933808267266, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-09_5IO9PHVPwdr9wX9w.webp', 'OIP-C', NULL, NULL, NULL, NULL, NULL, 3442, NULL, NULL, NULL, 'WEBP', 1956985828009132034, '2026-04-09 17:33:02', '2026-04-09 17:33:07', '2026-04-09 17:33:07', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-09 17:33:02', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-09_5IO9PHVPwdr9wX9w_thumbnail.webp', NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042178591582859265, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-09_sCSlnQ3gmnaFPUQ7.docx', '2026-04-09_sCSlnQ3gmnaFPUQ7.docx', NULL, NULL, NULL, NULL, NULL, 12998, NULL, NULL, NULL, 'docx', 1956985828009132034, '2026-04-09 17:51:32', '2026-04-09 17:51:35', '2026-04-11 18:16:22', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-09 17:51:33', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042883792543485954, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1981969199176937474/upload_11003390638188591004.mp4_c91f18ee05e73dcc21a0a9f629971311.mp4', 'upload_11003390638188591004.mp4_c91f18ee05e73dcc21a0a9f629971311.mp4', NULL, '美图', '[]', NULL, NULL, 4833743, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-11 16:33:45', '2026-08-24 10:36:55', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 16:33:46', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/cover/2042883792543485954_A3EBel6B.jpg', 1981969199176937474, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042895073392529409, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-11_EIvNRzp3HqYv7zd8.png', '2026-04-11_EIvNRzp3HqYv7zd8.png', '', '', '[]', NULL, NULL, 340448, NULL, NULL, NULL, 'png', 1956985828009132034, '2026-04-11 17:18:35', '2026-04-11 17:19:04', '2026-04-11 17:19:03', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 17:18:35', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042895313260580866, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-11_EtK9jjWXaErkSk8B.jpg', '2026-04-11_EtK9jjWXaErkSk8B.jpg', NULL, NULL, NULL, NULL, NULL, 14317, NULL, NULL, NULL, 'jpg', 1956985828009132034, '2026-04-11 17:19:32', '2026-04-11 17:19:32', '2026-04-11 17:19:32', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 17:19:32', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042896386683637762, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-11_HqCfRNcGXpe62pxB.mp4', '2026-04-11_HqCfRNcGXpe62pxB.mp4', '', '', '[]', NULL, NULL, 16952620, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-11 17:23:48', '2026-04-11 17:24:07', '2026-04-11 17:24:06', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 17:23:48', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042896656146698241, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-11_c2mWAv1EDzljEcAU.png', '2026-04-11_c2mWAv1EDzljEcAU.png', NULL, NULL, NULL, NULL, NULL, 1360325, NULL, NULL, NULL, 'png', 1956985828009132034, '2026-04-11 17:24:52', '2026-04-11 17:24:52', '2026-04-11 17:24:52', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 17:24:53', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042897362769481730, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/upload_7500925129853625100.mp4_939f762b951077e352b7dce9d0138a5d.mp4', 'upload_7500925129853625100.mp4_939f762b951077e352b7dce9d0138a5d.mp4', NULL, NULL, NULL, NULL, NULL, 16952620, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-11 17:27:41', '2026-04-11 17:27:50', '2026-04-11 17:27:49', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 17:27:41', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/thumbnails/upload_7500925129853625100_thumbnail_939f762b951077e352b7dce9d0138a5d.jpg', NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042899676460814337, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-11_etJcwX60Yy8Kiu5K.png', '2026-04-11_etJcwX60Yy8Kiu5K.png', NULL, NULL, NULL, NULL, NULL, 915931, NULL, NULL, NULL, 'png', 1956985828009132034, '2026-04-11 17:36:52', '2026-04-11 17:36:52', '2026-04-11 17:36:52', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 17:36:53', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042899742865035265, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-11_rtLbxZ63F59UrLup.mp4', '2026-04-11_rtLbxZ63F59UrLup.mp4', NULL, NULL, NULL, NULL, NULL, 16952620, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-11 17:37:08', '2026-04-11 17:37:08', '2026-04-27 10:41:26', 0, 2, '管理员操作拒绝', 1956985828009132034, '2026-04-27 10:41:26', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042899801576902658, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/2026-04-11_wHXsJuMwFdAvOwVT.mp4', '2026-04-11_wHXsJuMwFdAvOwVT.mp4', NULL, NULL, NULL, NULL, NULL, 16952620, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-11 17:37:22', '2026-04-11 17:37:22', '2026-04-27 10:41:10', 0, 2, '管理员操作拒绝', 1956985828009132034, '2026-04-27 10:41:10', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042902878535094273, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-11_Lkn60ArroJmYcQMN.webp', '图7-1 TensorFlow.js架构', NULL, NULL, NULL, NULL, NULL, 16766, NULL, NULL, NULL, 'webp', 1956985828009132034, '2026-04-11 17:49:36', '2026-04-11 17:49:36', '2026-04-11 17:49:36', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 17:49:36', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-11_Lkn60ArroJmYcQMN_thumbnail.png', NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042906325145686017, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-11_i3PJzl5wQngNxbZk.webp', '图7-4 tfvis绘制散点图', '', '', '[]', NULL, NULL, 14686, NULL, NULL, NULL, 'webp', 1956985828009132034, '2026-04-11 18:03:17', '2026-04-11 18:03:28', '2026-04-11 18:03:28', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 18:03:18', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-11_i3PJzl5wQngNxbZk_thumbnail.png', NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2042906429252505601, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/upload_14027626240046778067.mp4_939f762b951077e352b7dce9d0138a5d.mp4', 'upload_14027626240046778067.mp4_939f762b951077e352b7dce9d0138a5d.mp4', '', '', '[]', NULL, NULL, 16952620, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-11 18:03:42', '2026-04-11 18:03:51', '2026-04-11 18:03:51', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-11 18:03:43', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/thumbnails/upload_14027626240046778067_thumbnail_939f762b951077e352b7dce9d0138a5d.jpg', NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2044332818681868290, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-15_YaRvx04JGGD1bmFP.webp', '未命名绘图.drawio', NULL, NULL, NULL, NULL, NULL, 430, 375, 171, 2.19, 'webp', 1956985828009132034, '2026-04-15 16:31:40', '2026-04-15 16:31:40', '2026-04-15 16:31:40', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-15 16:31:40', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/2026-04-15_YaRvx04JGGD1bmFP_thumbnail.png', NULL, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (2044333022151749634, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1956985828009132034/upload_15974096573698234837.mp4_90475734512666d10310c636ef061b6a.mp4', '保家卫国是军人的天职.mp4', NULL, NULL, '[]', NULL, NULL, 82531187, NULL, NULL, NULL, 'mp4', 1956985828009132034, '2026-04-15 16:32:29', '2026-08-20 16:55:37', '2026-08-20 16:55:36', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-15 16:32:29', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1956985828009132034/thumbnails/upload_15974096573698234837_thumbnail_90475734512666d10310c636ef061b6a.jpg', NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2044339994632699906, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-04-15_PEmJLjXck6Ff3npT.docx', '实验报告：Jupyter Notebook使用', NULL, '美图', NULL, NULL, NULL, 56685, NULL, NULL, NULL, 'docx', 1956985828009132034, '2026-04-15 17:00:11', '2026-04-15 17:00:11', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-04-15 17:00:11', NULL, 1981969199176937474, NULL, NULL, '本实验报告介绍了Jupyter Notebook的使用方法，包括Anaconda的安装与环境变量配置、Notebook的启动与基本操作，以及通过交互式和外部脚本两种方式运行Python程序。实验掌握了conda和jupyter命令验证、代码与Markdown单元格操作，并使用%load和%run命令执行外部文件。总结指出交互式模式便于调试，外部脚本具有复用性，适合结合使用。');
INSERT INTO `file` VALUES (2053438272972349442, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083457/upload_16028027244544097885.exe_2f2ab2472a6aa29f8755c72c58f58f4b.exe', 'python-3.12.8-amd64.exe', 'python-3.12.8-windos安装程序', NULL, NULL, NULL, NULL, 27043760, NULL, NULL, NULL, 'exe', 1957032183779905538, '2026-05-10 19:33:29', '2026-05-10 19:34:21', '2026-05-10 19:34:20', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-10 19:33:30', NULL, 1986619202856083457, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053441331148447745, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083457/upload_1391435695624068797.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', 'upload_1391435695624068797.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', NULL, NULL, NULL, NULL, NULL, 50813912, NULL, NULL, NULL, 'mp4', 1957032183779905538, '2026-05-10 19:45:39', '2026-05-10 19:45:39', '2026-05-11 15:17:27', 1, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-10 19:45:39', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/thumbnails/upload_1391435695624068797_thumbnail_ae0f8b588e9e1e131feb9b2e7f4199fe.jpg', 1986619202856083457, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053441401826664449, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083457/upload_3653024438274919359.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', 'upload_3653024438274919359.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', NULL, NULL, NULL, NULL, NULL, 50813912, NULL, NULL, NULL, 'mp4', 1957032183779905538, '2026-05-10 19:45:55', '2026-05-10 19:45:55', '2026-05-11 15:17:29', 1, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-10 19:45:56', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/thumbnails/upload_3653024438274919359_thumbnail_ae0f8b588e9e1e131feb9b2e7f4199fe.jpg', 1986619202856083457, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053441552892911618, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083457/upload_1031385600363032093.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', 'upload_1031385600363032093.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', NULL, NULL, NULL, NULL, NULL, 50813912, NULL, NULL, NULL, 'mp4', 1957032183779905538, '2026-05-10 19:46:31', '2026-05-10 19:46:31', '2026-05-11 15:17:30', 1, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-10 19:46:32', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/thumbnails/upload_1031385600363032093_thumbnail_ae0f8b588e9e1e131feb9b2e7f4199fe.jpg', 1986619202856083457, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053441618965782530, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083457/upload_13151606621812477111.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', 'upload_13151606621812477111.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', NULL, NULL, NULL, NULL, NULL, 50813912, NULL, NULL, NULL, 'mp4', 1957032183779905538, '2026-05-10 19:46:47', '2026-05-10 19:46:47', '2026-05-10 19:46:47', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-10 19:46:48', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/thumbnails/upload_13151606621812477111_thumbnail_ae0f8b588e9e1e131feb9b2e7f4199fe.jpg', 1986619202856083457, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053667430868504577, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_v2mEPRgYXFvg8xRe.webp', '2b4f050828381f300c1e81bfef014c086e06f006', NULL, NULL, NULL, NULL, NULL, 11186, 705, 269, 2.62, 'webp', 1957032172908269570, '2026-05-11 10:44:05', '2026-05-11 10:44:05', '2026-05-11 10:44:05', 0, 0, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_v2mEPRgYXFvg8xRe_thumbnail.jpg', 1986619202856083457, '0x202020', NULL, NULL);
INSERT INTO `file` VALUES (2053670249738588162, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986721650559967234/2026-05-11_nUIWRbAwCnDGELz2.webp', '屏幕截图 2026-05-11 105507', NULL, NULL, NULL, NULL, NULL, 59746, 1389, 979, 1.42, 'webp', 1957032172908269570, '2026-05-11 10:55:17', '2026-05-11 10:55:17', '2026-05-11 10:55:17', 0, 0, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986721650559967234/2026-05-11_nUIWRbAwCnDGELz2_thumbnail.png', 1986721650559967234, '0x57573a', NULL, NULL);
INSERT INTO `file` VALUES (2053726515299151873, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986341066734940161/2026-05-11_vtI2ji6G6qjC7Rld.webp', '屏幕截图 2026-05-11 143842', NULL, NULL, NULL, NULL, NULL, 90938, 2524, 1281, 1.97, 'webp', 1957032183779905538, '2026-05-11 14:38:52', '2026-05-11 14:38:52', '2026-05-11 14:38:52', 0, 0, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986341066734940161/2026-05-11_vtI2ji6G6qjC7Rld_thumbnail.png', 1986341066734940161, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (2053729897531371521, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619913400541186/2026-05-11_cvDJwKzWG2EoC66N.webp', '4ec4ac0b12f8e77060f95e02e28dd6f6 (1)', NULL, NULL, NULL, NULL, NULL, 37170, 1080, 1410, 0.77, 'webp', 1957032183779905538, '2026-05-11 14:52:18', '2026-05-11 14:52:18', '2026-05-11 14:52:18', 0, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-11 14:52:19', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619913400541186/2026-05-11_cvDJwKzWG2EoC66N_thumbnail.jpeg', 1986619913400541186, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (2053740900272852993, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-05-11_OZMxJRNCgX3pPf6l.webp', 'R-C', NULL, '美图', NULL, NULL, NULL, 37534, 858, 536, 1.6, 'webp', 1956985828009132034, '2026-05-11 15:36:01', '2026-05-11 15:36:01', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-05-11 15:36:02', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-05-11_OZMxJRNCgX3pPf6l_thumbnail.jpg', 1981969199176937474, '0xa2c1df', NULL, NULL);
INSERT INTO `file` VALUES (2053774885745127426, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_KZB6s9ZbW7OXWxiq.webp', 'u=3410925877,2159896568&fm=3074&app=3074&f=PNG?w=2048&h=2048', NULL, NULL, NULL, NULL, NULL, 218582, 2048, 2048, 1, 'webp', 1957032183779905538, '2026-05-11 17:51:04', '2026-05-11 17:51:04', '2026-05-11 17:51:20', 1, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-11 17:51:05', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_KZB6s9ZbW7OXWxiq_thumbnail.', 1986619202856083457, '0x000000', NULL, NULL);
INSERT INTO `file` VALUES (2053775072823668737, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_dOQzqhkcxSVRfFSp.webp', '0GSnnzJGf0Q6mM0.thumb.1000_0', NULL, NULL, NULL, NULL, NULL, 43366, 1000, 751, 1.33, 'webp', 1957032183779905538, '2026-05-11 17:51:49', '2026-05-11 17:51:49', '2026-05-11 17:52:04', 1, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-11 17:51:49', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_dOQzqhkcxSVRfFSp_thumbnail.png', 1986619202856083457, '0xe0a855', NULL, NULL);
INSERT INTO `file` VALUES (2053775706926936066, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2026-05-11_d6yGV7I3gY4kKbvf.webp', '853d1bce0c309d5f36190b57a54f9cdc', '', '', '[]', NULL, NULL, 218582, 2048, 2048, 1, 'webp', 1957032172908269570, '2026-05-11 17:54:20', '2026-05-11 17:54:26', '2026-05-11 17:58:40', 1, 0, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2026-05-11_d6yGV7I3gY4kKbvf_thumbnail.jpeg', NULL, '0x000000', NULL, NULL);
INSERT INTO `file` VALUES (2053775973575618562, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2026-05-11_1fwKDlDCZEZ9C9zw.webp', '黑白山水', '', '', '[]', NULL, NULL, 218582, 2048, 2048, 1, 'webp', 1957032172908269570, '2026-05-11 17:55:24', '2026-05-11 17:55:42', '2026-05-11 17:58:46', 1, 0, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2026-05-11_1fwKDlDCZEZ9C9zw_thumbnail.jpeg', NULL, '0x000000', NULL, NULL);
INSERT INTO `file` VALUES (2053776349825687553, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2026-05-11_PM18XjMstw5zleM5.webp', '黑白山水', '', '', '[]', NULL, NULL, 218582, 2048, 2048, 1, 'webp', 1957032172908269570, '2026-05-11 17:56:53', '2026-05-11 17:57:15', '2026-05-27 12:17:45', 0, 1, '管理员操作通过', 1956985828009132034, '2026-05-27 12:17:45', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2026-05-11_PM18XjMstw5zleM5_thumbnail.jpeg', NULL, '0x000000', NULL, NULL);
INSERT INTO `file` VALUES (2053779489773936641, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_gqeEp6ZgRlkIiwry.webp', '凯露测试', NULL, NULL, NULL, NULL, NULL, 141706, 1128, 1808, 0.62, 'webp', 1957032183779905538, '2026-05-11 18:09:22', '2026-05-11 18:09:22', '2026-05-11 18:09:37', 1, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-11 18:09:22', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_gqeEp6ZgRlkIiwry_thumbnail.png', 1986619202856083457, '0x2f4861', NULL, NULL);
INSERT INTO `file` VALUES (2053780310322405377, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_bh1gLJzvhrWlvmlw.webp', '凯露测试', NULL, NULL, NULL, NULL, NULL, 141706, 1128, 1808, 0.62, 'webp', 1957032172908269570, '2026-05-11 18:12:38', '2026-05-11 18:12:38', '2026-05-11 18:12:38', 0, 0, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_bh1gLJzvhrWlvmlw_thumbnail.png', 1986619202856083457, '0x2f4861', NULL, NULL);
INSERT INTO `file` VALUES (2053780544653975554, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083500/upload_18073617654239886915.mp4_c86d2ec4f5c63e5af6fa305d44427eb3.mp4', 'upload_18073617654239886915.mp4_c86d2ec4f5c63e5af6fa305d44427eb3.mp4', NULL, NULL, NULL, NULL, NULL, 52417424, NULL, NULL, NULL, 'mp4', 1957032172908269570, '2026-05-11 18:13:33', '2026-05-11 18:13:33', '2026-05-11 18:13:33', 0, 0, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083500/thumbnails/upload_18073617654239886915_thumbnail_c86d2ec4f5c63e5af6fa305d44427eb3.jpg', 1986619202856083500, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053780593517617154, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083500/upload_12581540066261186650.mp4_c86d2ec4f5c63e5af6fa305d44427eb3.mp4', 'upload_12581540066261186650.mp4_c86d2ec4f5c63e5af6fa305d44427eb3.mp4', NULL, NULL, NULL, NULL, NULL, 52417424, NULL, NULL, NULL, 'mp4', 1957032172908269570, '2026-05-11 18:13:45', '2026-05-11 18:13:45', '2026-05-11 18:13:45', 0, 0, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083500/thumbnails/upload_12581540066261186650_thumbnail_c86d2ec4f5c63e5af6fa305d44427eb3.jpg', 1986619202856083500, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053781541048639489, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083457/upload_5366390416454861883.mp4_c86d2ec4f5c63e5af6fa305d44427eb3.mp4', 'upload_5366390416454861883.mp4_c86d2ec4f5c63e5af6fa305d44427eb3.mp4', NULL, NULL, NULL, NULL, NULL, 52417424, NULL, NULL, NULL, 'mp4', 1957032183779905538, '2026-05-11 18:17:31', '2026-05-11 18:17:31', '2026-05-11 18:17:54', 1, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-11 18:17:31', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/thumbnails/upload_5366390416454861883_thumbnail_c86d2ec4f5c63e5af6fa305d44427eb3.jpg', 1986619202856083457, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053781587232120834, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083457/upload_7067768443271733545.mp4_c86d2ec4f5c63e5af6fa305d44427eb3.mp4', 'upload_7067768443271733545.mp4_c86d2ec4f5c63e5af6fa305d44427eb3.mp4', NULL, NULL, NULL, NULL, NULL, 52417424, NULL, NULL, NULL, 'mp4', 1957032183779905538, '2026-05-11 18:17:42', '2026-05-11 18:17:42', '2026-05-11 18:17:53', 1, 1, '管理员或空间创建者自动过审', 1957032183779905538, '2026-05-11 18:17:42', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/thumbnails/upload_7067768443271733545_thumbnail_c86d2ec4f5c63e5af6fa305d44427eb3.jpg', 1986619202856083457, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2053782259402891265, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2026-05-11_8o1zdHtBY0Ud4zIi.webp', '凯露表情包', NULL, NULL, NULL, NULL, NULL, 53380, 807, 808, 1, 'webp', 1957032172908269570, '2026-05-11 18:20:22', '2026-05-11 18:20:26', '2026-05-11 18:20:25', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2026-05-11 18:20:23', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2026-05-11_8o1zdHtBY0Ud4zIi_thumbnail.png', 1982053156065124354, '0x5a3e5a', NULL, '图片为动漫风格猫耳少女角色，灰黑色头发中带有白色挑染，绿色眼睛，脸颊泛红，张口露出小虎牙并挂有汗珠，神情显得生气而慌乱，身穿白色立领服饰，背景为纯白色。');
INSERT INTO `file` VALUES (2090270684297097217, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/2090270615435014146/2026-08-20_crqeZm6U8hhhV4py.webp', 'c9fe65397fc140a6c63507181d795a9d06a44f28', NULL, NULL, NULL, NULL, NULL, 84488, 1440, 1080, 1.33, 'webp', 1957032172908269570, '2026-08-20 10:52:21', '2026-08-20 10:52:21', '2026-08-20 10:52:21', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2026-08-20 10:52:21', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/2090270615435014146/2026-08-20_crqeZm6U8hhhV4py_thumbnail.jpg', 2090270615435014146, '0xb99c84', NULL, NULL);
INSERT INTO `file` VALUES (2090747179641675777, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-08-21_ziKHYMy92RwsNRl1.md', '飞鸟集', NULL, '美图', NULL, NULL, NULL, 959, NULL, NULL, NULL, 'md', 1956985828009132034, '2026-08-21 18:25:46', '2026-08-21 18:26:08', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-08-21 18:25:46', NULL, 1981969199176937474, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2090775852205772802, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1981969199176937474/upload_11767503334278792440.flac_97d868397f1fbe24257a71e3f5513da0.flac', 'Stay Alive.flac', 'Stay Alive艾米利亚', '美图', NULL, NULL, NULL, 33908053, NULL, NULL, NULL, 'flac', 1956985828009132034, '2026-08-21 20:19:42', '2026-08-21 20:20:16', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-08-21 20:19:43', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/cover/2090775852205772802_AhoRU0bT.jpg', 1981969199176937474, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2090781141378371585, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/2026-08-21_sw19szZootSQTOXc.m4a', '18岁老师的普通话指点', '', '', '[]', NULL, NULL, 330577, NULL, NULL, NULL, 'm4a', 1957032172908269570, '2026-08-21 20:40:43', '2026-08-21 20:40:47', '2026-08-21 20:41:16', 0, 1, '管理员操作通过', 1956985828009132034, '2026-08-21 20:41:17', NULL, NULL, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2091002221296717826, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//public/1957032172908269570/upload_11494852741945071691.mp4_dbd94473f1228190ec8b3c95e1d5b83d.mp4', 'c团来了个年轻人.mp4', '', '', '[]', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/video/cover/2091002221296717826.jpg', NULL, 44975542, NULL, NULL, NULL, 'mp4', 1957032172908269570, '2026-08-22 11:19:13', '2026-08-22 11:19:56', '2026-08-22 11:20:06', 0, 1, '管理员操作通过', 1956985828009132034, '2026-08-22 11:20:07', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/public/1957032172908269570/thumbnails/upload_11494852741945071691_thumbnail_dbd94473f1228190ec8b3c95e1d5b83d.jpg', NULL, NULL, NULL, '这段转写内容是一场CS:GO Major淘汰赛的残局解说。视频中，队伍已被淘汰，解说员对选手表现进行激烈点评，批评外场选手枪法失误、补枪不及时，导致优势局面被翻盘。残局阶段，sucky等选手多次尝试操作，但未能拿下关键人头。最终，队伍输掉残局，解说感叹“世界级残局倒在最后一步”，并暗指sucky职业生涯遭遇挫败。整体内容以赛事最后时刻的紧张战况和遗憾结局为核心。');
INSERT INTO `file` VALUES (2091162046156574722, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2026-08-22_1CIigpbRJ7Oy68Fa.txt', '8位移动', NULL, NULL, NULL, NULL, NULL, 2969, NULL, NULL, NULL, 'txt', 1957032172908269570, '2026-08-22 21:54:18', '2026-08-22 21:54:21', '2026-08-22 21:54:20', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2026-08-22 21:54:18', NULL, 1982053156065124354, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2091169156445560834, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2026-08-22_TvkcbTvAzcqUF0l6.doc', '7.实习手册(1)', NULL, NULL, NULL, NULL, NULL, 168448, NULL, NULL, NULL, 'doc', 1957032172908269570, '2026-08-22 22:22:33', '2026-08-22 22:22:35', '2026-08-22 22:37:25', 1, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2026-08-22 22:22:34', NULL, 1982053156065124354, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2091172833185030146, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982053156065124354/2026-08-22_mcVu18u4h1Pzmd6n.docx', '开场白', NULL, NULL, NULL, NULL, NULL, 12998, NULL, NULL, NULL, 'docx', 1957032172908269570, '2026-08-22 22:37:10', '2026-08-22 22:37:10', '2026-08-22 22:37:10', 0, 1, '管理员或空间创建者自动过审', 1957032172908269570, '2026-08-22 22:37:10', NULL, 1982053156065124354, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2094710439239798785, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1981969199176937474/upload_13046283220733534993.flac_b421673b3d22e15d32ad46c9d6167841.flac', 'upload_13046283220733534993.flac_b421673b3d22e15d32ad46c9d6167841.flac', NULL, '美图', NULL, NULL, NULL, 55720434, NULL, NULL, NULL, 'flac', 1956985828009132034, '2026-09-01 16:54:21', '2026-09-01 16:54:21', '2026-09-03 19:16:31', 0, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-09-01 16:54:21', NULL, 1981969199176937474, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2094712044538351618, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-09-01_P5CVotXAdnJ4quRI.webp', '109951171350922045', NULL, NULL, NULL, NULL, NULL, 116866, 1408, 1410, 1, 'webp', 1956985828009132034, '2026-09-01 17:00:43', '2026-09-01 17:00:43', '2026-09-01 17:05:31', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-09-01 17:00:44', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-09-01_P5CVotXAdnJ4quRI_thumbnail.jpg', 1981969199176937474, '0xe0e0e0', NULL, NULL);
INSERT INTO `file` VALUES (2094712153468620801, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-09-01_A4KBwX3ClQI1WuMb.zip', 'thinksaas3_75', NULL, NULL, NULL, NULL, NULL, 3671880, NULL, NULL, NULL, 'zip', 1956985828009132034, '2026-09-01 17:01:09', '2026-09-01 17:01:09', '2026-09-01 17:05:22', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-09-01 17:01:10', NULL, 1981969199176937474, NULL, NULL, NULL);
INSERT INTO `file` VALUES (2094713125179805698, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-09-01_4Y6u4f7upF4Q2suG.mp3', 'sms-sound', NULL, NULL, NULL, NULL, NULL, 52497, NULL, NULL, NULL, 'mp3', 1956985828009132034, '2026-09-01 17:05:01', '2026-09-01 17:05:01', '2026-09-01 17:05:21', 1, 1, '管理员或空间创建者自动过审', 1956985828009132034, '2026-09-01 17:05:02', NULL, 1981969199176937474, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for file_comment
-- ----------------------------
DROP TABLE IF EXISTS `file_comment`;
CREATE TABLE `file_comment`  (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `fileId` bigint(20) NOT NULL COMMENT '文件 id',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `userId` bigint(20) NOT NULL COMMENT '评论用户 id',
  `atUserIds` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '被 @ 的用户 id 列表（JSON 数组）',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_fileId`(`fileId`) USING BTREE,
  INDEX `idx_userId`(`userId`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件评论' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_comment
-- ----------------------------
INSERT INTO `file_comment` VALUES (2091011558559776770, 2091002221296717826, '@zzx 这里有cs相关视频', 1957032172908269570, '[1956985828009132034]', '2026-08-22 11:56:19', '2026-08-22 11:56:19', 0);
INSERT INTO `file_comment` VALUES (2091785277842419713, 2042883792543485954, '@zzx1 不错', 1956985828009132034, '[1957032172908269570]', '2026-08-24 15:10:48', '2026-08-24 15:10:48', 0);
INSERT INTO `file_comment` VALUES (2092100918625464322, 2042883792543485954, '@zzx1 就是画质有点拉', 1956985828009132034, '[1957032172908269570]', '2026-08-25 12:05:03', '2026-08-25 12:05:03', 0);
INSERT INTO `file_comment` VALUES (2092101085298716674, 2090747179641675777, '@zzx1 这是什么', 1956985828009132034, '[1957032172908269570]', '2026-08-25 12:05:43', '2026-08-25 12:05:43', 0);

-- ----------------------------
-- Table structure for file_mention
-- ----------------------------
DROP TABLE IF EXISTS `file_mention`;
CREATE TABLE `file_mention`  (
  `id` bigint(20) NOT NULL COMMENT 'id',
  `commentId` bigint(20) NOT NULL COMMENT '评论 id',
  `fileId` bigint(20) NOT NULL COMMENT '文件 id',
  `spaceId` bigint(20) NOT NULL COMMENT '空间 id（仅团队/社区空间）',
  `fromUserId` bigint(20) NOT NULL COMMENT '评论者 id',
  `toUserId` bigint(20) NOT NULL COMMENT '被 @ 的用户 id',
  `isRead` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_toUser_read`(`toUserId`, `isRead`) USING BTREE,
  INDEX `idx_spaceId`(`spaceId`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文件评论@提及记录' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of file_mention
-- ----------------------------
INSERT INTO `file_mention` VALUES (2091785277993414658, 2091785277842419713, 2042883792543485954, 1981969199176937474, 1956985828009132034, 1957032172908269570, 1, '2026-08-24 15:10:48', '2026-08-24 15:11:35');
INSERT INTO `file_mention` VALUES (2092100918789042177, 2092100918625464322, 2042883792543485954, 1981969199176937474, 1956985828009132034, 1957032172908269570, 1, '2026-08-25 12:05:03', '2026-08-25 12:07:14');
INSERT INTO `file_mention` VALUES (2092101085328076801, 2092101085298716674, 2090747179641675777, 1981969199176937474, 1956985828009132034, 1957032172908269570, 1, '2026-08-25 12:05:43', '2026-08-25 12:07:14');

-- ----------------------------
-- Table structure for first_category
-- ----------------------------
DROP TABLE IF EXISTS `first_category`;
CREATE TABLE `first_category`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '分类名称',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '顺序',
  `status` int(11) NULL DEFAULT NULL COMMENT '分类状态 0:禁用，1:启用',
  `createTime` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `createUser` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `updateUser` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_category_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '花田一级分类' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of first_category
-- ----------------------------

-- ----------------------------
-- Table structure for post
-- ----------------------------
DROP TABLE IF EXISTS `post`;
CREATE TABLE `post`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `spaceId` bigint(20) NULL DEFAULT NULL COMMENT '所属社区空间id(即原花田)',
  `fieldId` bigint(20) NOT NULL COMMENT '花田id',
  `userId` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `images` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '帖子照片，最多9张，多张以\",\"隔开',
  `content` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文字描述',
  `liked` int(8) UNSIGNED NULL DEFAULT 0 COMMENT '点赞数量',
  `comments` int(8) UNSIGNED ZEROFILL NULL DEFAULT 00000000 COMMENT '评论数量',
  `createTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2053741058695909378 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of post
-- ----------------------------
INSERT INTO `post` VALUES (1982029592830189569, 1981969199176937474, 1981969199210491906, 1957032172908269570, '无数次踏入地狱，只是为了拯救你', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_LHwdhMM3uMsd1J09.webp', '在从便利商店回家的路上，突然被异世界召唤的少年菜月昴。在无法依靠任何东西的异世界，无力的少年手唯一的力量……那是“死去然后重新开始”的力量。为了守护最重要的人们，为了取回确实存在着又无可替代的时间，少年抗拒着绝望，勇敢地面对着残酷的命运', 2, NULL, '2026-04-25 18:20:53', '2026-09-03 18:53:06');
INSERT INTO `post` VALUES (1982045656003686402, NULL, 1982039684778598402, 1957032172908269570, '公主连结 与你重逢、超异域公主连结☆Re:Dive', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-10-25_mFBwPiqXPJ6mXeCz.webp', '平静的风吹过美丽的大地——阿斯特莱亚大陆。在大陆的某个角落，失去记忆的少年佑树醒了过来。照顾他的小小指引者可可萝。总是饿肚子的美少女剑士佩可莉姆。有一点冷酷的猫耳魔法少女凯露。在命运的指引下，他们创立了名为“美食殿堂”的公会。现在，佑树和她们的冒险揭幕了', 2, 00000002, '2026-04-25 19:24:43', '2026-04-06 23:23:57');
INSERT INTO `post` VALUES (1982079154374598658, NULL, 1982078321423572993, 1957032183779905538, 'Java', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-10-25_osUl1owZM5mzUlWD.webp', 'Java是一门面向对象的编程语言，不仅吸收了C++语言的各种优点，还摒弃了C++里难以理解的多继承、指针等概念，因此Java语言具有功能强大和简单易用两个特征。Java语言作为静态面向对象编程语言的代表，极好地实现了面向对象理论，允许程序员以优雅的思维方式进行复杂的编程。', 2, NULL, '2026-04-25 21:37:50', '2026-05-11 10:47:27');
INSERT INTO `post` VALUES (1984822492550983681, NULL, 1981969199210491906, 1956985828009132034, '从零开始的异世界生活小说1-42卷', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-02_69DgISW7kyvpdwb3.webp', '文库版42卷来啦\n文库版1-42卷+web版+短集篇+外传+番外+同人\nepub和txt格式\n放下了\n提示：1-37卷是台版\n链接网页链接', 1, NULL, '2026-04-02 11:18:53', '2026-08-18 20:53:02');
INSERT INTO `post` VALUES (1984824211603255297, NULL, 1982039684778598402, 1957032172908269570, '【11月抽卡前瞻】这个月最低限度要抽2.5张卡，做好准备。', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-11-02_fLHRjXMX0KuDnfio.webp', '首先说下\n在11.10日到11.14日有5天的免费连，价值7500钻，正好跟2个新卡池有覆盖，只要愿意等到那个时候抽卡，就可以剩下7500钻。\n13号那天就把角色抽出来，因为14号那天是银莲的卡池，不管是给病娇还是银联，其实没有任何区别基本上可以说。都是免费减少支出', 1, NULL, '2026-04-02 11:25:42', '2026-05-11 10:52:01');
INSERT INTO `post` VALUES (1984826050562265090, NULL, 1981969199210491906, 1956985828009132034, '「Re:从零开始的异世界生活」动画第四季 2026年开播', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-02_xFCbIZmWL7WVpbA7.webp', '如题', 1, NULL, '2026-04-02 11:33:01', '2026-08-18 20:53:03');
INSERT INTO `post` VALUES (1985256888668028929, NULL, 1982078321423572993, 1957032183779905538, '前后端分离，千万别再搞错了！', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982078321301938178/2025-11-03_miPvStKCrAdcDdB8.webp', '所谓全栈，就是 全干，整个网站项目的后端和前端都由你一个人负责开发。\n\n当用户打开一个网站，能直观看到、可交互操作的界面，就是前端。\n\n\n而当用户点击操作按钮后，触发的操作验证、数据查询、业务逻辑处理等种种 “看不到” 的操作，都由后端来完成。\n\n你使用的开发技术是比你年龄都大的 JSP，它的特点是把前端 HTML 网页代码和后端 Java 代码混在一起写。\n\n刚开始项目代码不多的时候，你写的很舒服。\n\n但随着项目越做越大，更多同事加入了这个项目的开发，你也渐渐发现了几个问题。\n\n代码混乱难维护：前端后端代码纠缠在一起，不利于阅读，也不利于排查 Bug。\n\n团队协作困难：你在改前端样式，同事在改后端逻辑，经常改着改着就冲突了。\n\n人员要求更高：开发人员必须同时学习前端和后端技术，今天写 CSS 调样式，明天写 SQL 查数据库，根本忙不过来。\n\n部署效率低下：由于前端后端写在一起，哪怕改个按钮颜色都要重新编译部署整个项目。\n\n\n受这些问题的影响，项目开发效率越来越低，Bug 却越来越多，你压力山大、发如雨下。\n\n\n老板看出了你毛儿的不易，给公司请来了一位新的技术导师，号称 “全栈冤种” 的。\n\n\n看了看你的项目，摇了摇头：连前后端分离都不做么？\n\n你疑惑地问：前后端分离？没听说过。\n\n这时，你的同事阿土申插嘴道：我知道啊！我们现在把前端静态文件放在 static 文件夹，JSP 代码放在 jsp 文件夹，这不就是前后端分离吗？\n\n皱眉：那只是文件夹分离，不是真正的前后端分离。\n\n阿土申不服：老狗，那你说说用什么技术才算是前后端分离？\n\n笑道：前后端分离可不是某种特定的技术，而是一种 架构 思想。\n\n看看你们现在的项目，用户访问网站时，是由后端服务器接收请求，然后在后端查询数据并拼接到 HTML 网页模板中，生成完整的网页，最后再返回给用户。\n\n\n\n\n显然，后端承受了太多！\n\n而如果改造为前后端分离：\n\n前端专注于用户界面的呈现和交互逻辑\n\n后端专注于数据处理和业务逻辑，不涉及界面代码和网页生成\n\n用户在前端进行操作时，前端通过 API 接口 向后端发送请求，由后端处理操作并返回数据给前端，由前端完成数据的最终展示。\n\n\n\n\n你不明觉厉：可是这样做有什么好处呢？\n\n指着你之前列出的问题说：前后端分离就是专门解决这些痛点的！\n\n1）首先是 代码分离：将前端代码和后端代码完全分开，最好是 2 个独立的项目\n\n2）代码分离后，就可以 开发分离：前端和后端程序员可以同时干活，互不干扰。\n\n3）还可以 部署分离：前端和后端项目各自独立部署，后端出了问题或者修改了逻辑，不用重新部署前端。\n\n\n4）做到前面 3 个分离后，我们便能实现前后端分离的本质 —— 职责分离，专业的人干专业的事情，各自发挥特长，整体效率自然更高。当然最主要的是，后端出了问题，前端不用背锅。 \n\n\n\n\n听着滔滔不绝，你双眼放光：前后端分离这么厉害，是不是还能提升网站性能呢？\n\n摇摇头：不一定，而且有时前后端分离可能更慢！因为用户访问网站时，浏览器需要先加载前端页面，然后执行 JavaScript 脚本请求后端数据，这比传统的后端直接返回完整页面多了一次网络请求。\n\n\n\n\n但是也别灰心，前后端分离后，前端和后端都可以分别进行优化，更灵活。\n\n\n\n\n你点了点头：原来如此，看来前后端分离的主要目的是 提升开发效率和代码维护性，而不是提升性能。\n\n于是你带领团队开始重构项目，前端用 React 框架重写了整个界面，打包成静态文件部署到 CDN 加速网络上；后端则专门提供 API 接口，返回 JSON 数据，部署在独立的服务器上，互不影响。\n\n\n几个月后，你感受到了前后端分离的优势：\n\n团队成员各司其职\n\n前端技术灵活选择\n\n后端服务多端复用\n\n\n\n\n虽然前端和后端对接的过程中偶尔会斗嘴打架，但你也通过自己总结的一套前后端分离的实战经验完美解决。\n\n\n\n\n你兴奋地对说：前后端分离也太爽了吧！\n\n笑着问：那我问你，前后端分离一定比不分离更好么？\n\n你不假思索：那当然了！你看看这盛世岂不如你所愿？\n\n\n摇摇头：小阿巴你要记住，没有最好的架构，只有相对更合适的架构！是否采用前后端分离，要根据团队项目规模、项目实际需求、团队技术能力等综合决定。\n\n你：我明白了，抛开实际场景谈架构都是耍流氓~\n\n：最后问个问题，用了 React 框架一定就是前后端分离么？\n\n朋友，说说你的看法吧。\n\n', 2, NULL, '2026-04-03 16:05:01', '2026-05-11 10:47:28');
INSERT INTO `post` VALUES (1985266741067153409, NULL, 1981969199210491906, 1957032172908269570, NULL, NULL, '只是随便一发，水', 0, NULL, '2026-04-03 16:44:10', '2026-08-18 20:52:11');
INSERT INTO `post` VALUES (1985268250752352258, NULL, 1982039684778598402, 1957032172908269570, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-11-03_5OYqoYuhvV3wn8Fu.webp', NULL, 1, NULL, '2026-04-03 16:50:10', '2026-05-11 10:52:02');
INSERT INTO `post` VALUES (1985924954931163137, NULL, 1981969199210491906, 1957032172908269570, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-05_dGrRAeX7aZrMBadI.webp', NULL, 0, NULL, '2026-04-05 12:19:40', '2026-04-05 12:19:40');
INSERT INTO `post` VALUES (1985925100326711298, NULL, 1981969199210491906, 1957032172908269570, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-11-05_W0IItuyGjPURDfzo.webp', NULL, 0, NULL, '2026-04-05 12:20:15', '2026-04-05 12:20:15');
INSERT INTO `post` VALUES (2042883838278176769, NULL, 1981969199210491906, 1956985828009132034, 'ed', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1981969199176937474/upload_11003390638188591004.mp4_c91f18ee05e73dcc21a0a9f629971311.mp4', NULL, 0, NULL, '2026-04-11 16:33:56', '2026-04-11 16:33:56');
INSERT INTO `post` VALUES (2053441660967542786, NULL, 1986619202860277762, 1957032183779905538, 'Python所有核心概念', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com//space/1986619202856083457/upload_13151606621812477111.mp4_ae0f8b588e9e1e131feb9b2e7f4199fe.mp4', NULL, 2, 00000001, '2026-05-10 19:46:57', '2026-05-11 10:47:40');
INSERT INTO `post` VALUES (2053667485990047745, NULL, 1986619202860277762, 1957032172908269570, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619202856083457/2026-05-11_v2mEPRgYXFvg8xRe.webp', 'Python变量命名遵循以下规则和约定：\n变量名只能包含字母、数字和下划线（A-z, 0-9, _）\n变量名不能以数字开头\n变量名不能是Python关键字（如if, else, for等）\n命名风格：\n使用小写字母和下划线：student_name, total_score\n常量使用大写字母：MAX_SIZE = 100\n使用有意义的描述性名称：student_name 而不是 sn\nPython的基础类型：\n整数跟浮点数', 2, 00000001, '2026-05-11 10:44:18', '2026-05-27 11:19:56');
INSERT INTO `post` VALUES (2053670260312428545, NULL, 1986721650585133058, 1957032172908269570, '就爱吃家门口这家包子', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986721650559967234/2026-05-11_nUIWRbAwCnDGELz2.webp', '家门口的包子铺，没有招牌，没有店名，不大的店面几张桌子，食客接踵而坐。他家有个特点就是老板会逢年过节，寒暑假都要休息，嘎嘎嘎。也是很特别的一家店。他家开了40多年了，老板从小伙到快退休的年纪了。周围也开过几家包子铺，都没干过他家，疫情时期好几家餐饮小店都撑不下去了。唯有他家，依然很多食客。他家包子味道正宗，都是选的好肉自己绞馅儿。半发面的皮加上香油调的馅儿，倍儿香。他家卖的都是老主顾，虽然没有店名，但四方八邻的都知道他家。我虽然不在附近住了，仍然会想念这种从小吃到大的包子，吃一口包子都是童年的味道。', 0, 00000000, '2026-05-11 10:55:20', '2026-05-11 10:55:20');
INSERT INTO `post` VALUES (2053726524367237122, NULL, 1986341066856574978, 1957032183779905538, 'Ant Design Vue的定制主题', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986341066734940161/2026-05-11_vtI2ji6G6qjC7Rld.webp', '定制主题 \nAnt Design Vue 设计规范和技术上支持灵活的样式定制，以满足业务和品牌上多样化的视觉需求，包括但不限于全局样式（主色、圆角、边框）和指定组件的视觉定制。\n\n在 4.0 版本的 Ant Design Vue 中，我们提供了一套全新的定制主题方案。不同于 3.x 版本的 less 和 CSS 变量，有了 CSS-in-JS 的加持后，动态主题的能力也得到了加强，包括但不限于：\n\n支持动态切换主题；\n支持同时存在多个主题；\n支持针对某个/某些组件修改主题变量；', 0, 00000000, '2026-05-11 14:38:54', '2026-05-11 14:38:54');
INSERT INTO `post` VALUES (2053730696093298690, NULL, 1986619913408929793, 1957032183779905538, 'Linux命令行入门：新手必学核心命令之文件和目录操作', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1986619913400541186/2026-05-11_cvDJwKzWG2EoC66N.webp', 'Linux命令行是核心，新手先掌握以下命令，每个命令均附“语法+示例+效果说明”。\n\n提示：命令行中，「#」后面是注释，无需输入；「$」是命令提示符，无需输入；区分大小写（ls和LS是两个不同命令）。\n\n基础操作命令（5个，必记）\nls：查看当前目录下的文件/文件夹\n\n语法：ls [选项]\n\n示例1：ls （查看当前目录下的文件/文件夹，默认不显示隐藏文件）\n\n示例2：ls -l （查看详细信息，包括权限、大小、修改时间等，简称“ll”，新手常用）\n\n示例3：ls -a （查看所有文件/文件夹，包括隐藏文件（以“.”开头的文件））\n\ncd：切换目录（核心，高频使用）\n\n语法：cd 目录路径\n\n示例1：cd /home （切换到/home目录，“/”是根目录，所有目录都在根目录下）\n\n示例2：cd .. （切换到上一级目录，两个点表示上一级）\n\n示例3：cd ~ （切换到当前用户的家目录，比如用户名为test，家目录就是/home/test）\n\n示例4：cd / （切换到根目录）\n\npwd：查看当前所在目录的完整路径\n\n语法：pwd\n\n示例：pwd （执行后会显示类似“/home/test”的路径，避免迷路）\n\nclear：清空命令行屏幕\n\n语法：clear （快捷键：Ctrl+L，更快捷）\n\nexit：退出命令行/远程连接\n\n语法：exit\n\n文件/文件夹操作命令（7个，高频）\ntouch：创建空文件\n\n语法：touch 文件名\n\n示例：touch test.txt （在当前目录下创建一个名为test.txt的空文件）\n\nmkdir：创建文件夹\n\n语法：mkdir 文件夹名\n\n示例1：mkdir test （创建一个名为test的文件夹）\n\n示例2：mkdir -p test1/test2 （创建多级文件夹，test1里面包含test2，-p表示递归创建）\n\nrm：删除文件/文件夹（谨慎使用，删除后无法恢复）\n\n语法：rm [选项] 文件名/文件夹名\n\n示例1：rm test.txt （删除test.txt文件，会提示确认，输入y确认删除）\n\n示例2：rm -f test.txt （强制删除，不提示确认，新手慎用）\n\n示例3：rm -r test （删除test文件夹及里面所有内容，-r表示递归删除）\n\n示例4：rm -rf test （强制删除文件夹及内容，新手禁用！误删系统文件会导致系统崩溃）\n\ncp：复制文件/文件夹\n\n语法：cp [选项] 源文件/文件夹 目标路径\n\n示例1：cp test.txt /home/test （将当前目录的test.txt复制到/home/test目录下）\n\n示例2：cp -r test /home/test （复制test文件夹及内容到/home/test目录，-r用于复制文件夹）\n\nmv：移动文件/文件夹（或重命名）\n\n语法：mv 源文件/文件夹 目标路径/新名称\n\n示例1：mv test.txt /home/test （将test.txt移动到/home/test目录）\n\n示例2：mv test.txt test1.txt （将test.txt重命名为test1.txt，在当前目录下）\n\ncat：查看文件内容（适合查看小型文件）\n\n语法：cat 文件名\n\n示例：cat test.txt （查看test.txt文件的全部内容，直接显示在命令行）\n\nmore：查看文件内容（适合查看大型文件，分页显示）\n\n语法：more 文件名\n\n操作：按空格键翻页，按q键退出查看\n\n示例：more /etc/passwd （查看系统用户文件，大型文件分页显示）\n\n\n\n新手命令练习建议\n1. 新建一个文件夹，在文件夹中创建多个文件，练习ls、cd、touch、mkdir命令；\n\n2. 练习复制、移动、删除文件/文件夹（注意：删除时不要用rm -rf，避免误删）；\n\n3. 用cat、more命令查看系统文件（比如/etc/passwd、/etc/hosts）；\n\n4. 练习sudo命令，安装vim编辑器，熟悉管理员权限操作。', 0, 00000000, '2026-05-11 14:55:29', '2026-05-11 14:55:29');
INSERT INTO `post` VALUES (2053741058695909377, NULL, 1981969199210491906, 1956985828009132034, '动画第二期介绍', 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2026-05-11_OZMxJRNCgX3pPf6l.webp', '“我一定会拯救你的。”\n打倒了担任“怠惰”的魔女教大罪司教，培提奇乌斯·罗曼尼康帝后，菜月昴和爱蜜莉雅又再度相遇了。在跨越了痛苦的诀别，终于能够和解的两人身边，新的篇章却又开始了。超乎想像，九死一生的危机，以及冷酷无情的现实向他们袭来。少年将再次起身，面对残酷的命运。', 0, 00000002, '2026-05-11 15:36:39', '2026-05-11 15:36:39');

-- ----------------------------
-- Table structure for post_comment
-- ----------------------------
DROP TABLE IF EXISTS `post_comment`;
CREATE TABLE `post_comment`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '评论id',
  `context` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '评论内容',
  `publisher` bigint(20) NOT NULL COMMENT '发布者id',
  `post` bigint(20) NOT NULL COMMENT '帖子id',
  `createTime` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2089697946109026307 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of post_comment
-- ----------------------------
INSERT INTO `post_comment` VALUES (1983379276228243458, '福利非常不错', 1957032172908269570, 1982045656003686402, '2026-04-27 11:44:03', '2026-04-27 11:44:03');
INSERT INTO `post_comment` VALUES (1983450844224626689, '在我手机里待了好久', 1956985828009132034, 1982045656003686402, '2026-04-27 16:28:27', '2026-04-27 16:28:27');
INSERT INTO `post_comment` VALUES (2053668419692449793, '欢迎大家来看', 1957032183779905538, 2053441660967542786, '2026-05-11 10:48:01', '2026-05-11 10:48:01');
INSERT INTO `post_comment` VALUES (2053668479289315329, '不错不错', 1957032183779905538, 2053667485990047745, '2026-05-11 10:48:15', '2026-05-11 10:48:15');
INSERT INTO `post_comment` VALUES (2089697396571316226, '第二季更残酷', 1956985828009132034, 2053741058695909377, '2026-08-18 20:54:19', '2026-08-18 20:54:19');
INSERT INTO `post_comment` VALUES (2089697946109026306, '但是486和emt的感情也前进了一大步', 1956985828009132034, 2053741058695909377, '2026-08-18 20:56:30', '2026-08-18 20:56:30');

-- ----------------------------
-- Table structure for second_category
-- ----------------------------
DROP TABLE IF EXISTS `second_category`;
CREATE TABLE `second_category`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `firstCategoryId` bigint(20) NOT NULL COMMENT '一级分类id',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL COMMENT '分类名称',
  `sort` int(11) NOT NULL DEFAULT 0 COMMENT '顺序',
  `status` int(11) NULL DEFAULT NULL COMMENT '分类状态 0:禁用，1:启用',
  `createTime` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `createUser` bigint(20) NULL DEFAULT NULL COMMENT '创建人',
  `updateUser` bigint(20) NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_category_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '花田二级分类' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of second_category
-- ----------------------------

-- ----------------------------
-- Table structure for space
-- ----------------------------
DROP TABLE IF EXISTS `space`;
CREATE TABLE `space`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `spaceName` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '空间名称',
  `spaceLevel` int(11) NULL DEFAULT 0 COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
  `maxSize` bigint(20) NULL DEFAULT 0 COMMENT '空间图片的最大总大小',
  `maxCount` bigint(20) NULL DEFAULT 0 COMMENT '空间图片的最大数量',
  `totalSize` bigint(20) NULL DEFAULT 0 COMMENT '当前空间下图片的总大小',
  `totalCount` bigint(20) NULL DEFAULT 0 COMMENT '当前空间下的图片数量',
  `userId` bigint(20) NOT NULL COMMENT '创建用户 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `spaceType` int(11) NOT NULL DEFAULT 0 COMMENT '空间类型：0-私有 1-团队 2-花田',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '可见状态 0公开/1私有/2加密(仅社区)',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '社区密码(仅加密社区)',
  `categoryId` bigint(20) NULL DEFAULT NULL COMMENT '分类id(仅社区空间,冗余)',
  `maxNum` int(11) NULL DEFAULT NULL COMMENT '最大人数(团队/社区空间)',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述(社区空间)',
  `picUrl` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '空间封面图',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_userId`(`userId`) USING BTREE,
  INDEX `idx_spaceName`(`spaceName`) USING BTREE,
  INDEX `idx_spaceLevel`(`spaceLevel`) USING BTREE,
  INDEX `idx_spaceType`(`spaceType`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2090270615435014147 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '空间' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of space
-- ----------------------------
INSERT INTO `space` VALUES (1981969199176937474, 'Re:从零开始的异世界生活的空间zzx', NULL, 0, 0, -100023693, -7, 1956985828009132034, '2026-04-25 14:20:54', '2026-04-25 14:20:54', '2026-09-03 18:45:50', 0, 2, 0, NULL, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1981969199176937474/2025-10-25_cHDNprGtUPZDJ6de.webp');
INSERT INTO `space` VALUES (1982039684560494594, '公主连结的空间zzx1', NULL, 0, 0, 0, 0, 1957032172908269570, '2026-04-25 19:00:59', '2026-04-25 19:00:59', '2026-09-03 18:47:21', 0, 2, 0, NULL, NULL, NULL, NULL, 'https://yun-picture-1350469262.cos.ap-beijing.myqcloud.com/space/1982039684560494594/2025-10-25_S4kbkuvRf8cq4Csm.webp');
INSERT INTO `space` VALUES (1982050098652389377, 'zzx1的团队空间', 0, 1048576000, 1000, 3468194, 20, 1957032172908269570, '2026-04-25 19:42:22', '2026-04-25 19:42:22', '2026-04-26 21:23:37', 0, 1, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1982053156065124354, 'zzx1的个人空间', 0, 104857600, 100, 1293953, 9, 1957032172908269570, '2026-04-25 19:54:31', '2026-04-25 19:54:31', '2026-08-22 22:37:25', 0, 0, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1982078321301938178, 'java兴趣花田的空间无名', NULL, 0, 0, 0, 0, 1957032183779905538, '2026-04-25 21:34:31', '2026-04-25 21:34:31', '2026-05-10 19:07:31', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986341066734940161, '前端美化技术兴趣花田的空间无名', 0, 314572800, 0, 0, 0, 1957032183779905538, '2026-04-06 15:53:09', '2026-04-06 15:53:09', '2026-05-11 19:43:48', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986618482027192321, '前端框架vue的空间zzx1', 0, 0, 0, 0, 0, 1957032172908269570, '2026-04-07 10:15:30', '2026-04-07 10:15:30', '2026-05-10 19:07:24', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986619202856083457, 'python技术交流的空间无名', 0, 314572800, 0, -257680238, -8, 1957032183779905538, '2026-04-07 10:18:22', '2026-04-07 10:18:22', '2026-05-11 18:17:54', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986619913400541186, 'Linux运维交流的空间无名', 0, 314572800, 0, 0, 0, 1957032183779905538, '2026-04-07 10:21:11', '2026-04-07 10:21:11', '2026-05-11 14:52:17', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986620184998502401, '云计算运维交流的空间无名', 0, 0, 0, 0, 0, 1957032183779905538, '2026-04-07 10:22:16', '2026-04-07 10:22:16', '2026-05-10 19:07:14', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986620964593786881, '路人女主秘密花田的空间无名', 0, 0, 0, 0, 0, 1957032197113597954, '2026-04-07 10:25:22', '2026-04-07 10:25:22', '2026-05-10 19:07:10', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986622914441814018, '路人女主加密花田的空间无名', 0, 1048576000, 1000, 83924, 1, 1957032197113597954, '2026-04-07 10:33:07', '2026-04-07 10:33:07', '2026-05-11 15:49:53', 0, 1, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986720492802039809, '路人女主秘密花田的空间zzx', 0, 1048576000, 1000, 101450, 1, 1956985828009132034, '2026-04-07 17:00:51', '2026-04-07 17:00:51', '2026-05-11 15:51:01', 0, 1, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986721650559967234, '美食殿堂的空间无名', 0, 314572800, 0, 0, 0, 1957032197113597954, '2026-04-07 17:05:27', '2026-04-07 17:05:27', '2026-05-11 10:55:15', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (1986722832732938242, '黑暗之魂3交流的空间无名', 0, 0, 0, 0, 0, 1957032197113597954, '2026-04-07 17:10:09', '2026-04-07 17:10:09', '2026-05-10 19:06:53', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (2053424685683924994, '无名的团队空间', 0, 1048576000, 1000, 0, 0, 1957032183779905538, '2026-05-10 18:39:30', '2026-05-10 18:39:30', '2026-05-10 18:39:30', 0, 1, 0, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `space` VALUES (2090270615435014146, '测试创建者退出花田问题的空间zzx1', 0, 314572800, 0, 0, 0, 1957032172908269570, '2026-08-20 10:52:04', '2026-08-20 10:52:04', '2026-08-20 10:52:19', 0, 2, 0, NULL, NULL, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for space_user
-- ----------------------------
DROP TABLE IF EXISTS `space_user`;
CREATE TABLE `space_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `spaceId` bigint(20) NOT NULL COMMENT '空间 id',
  `userId` bigint(20) NOT NULL COMMENT '用户 id',
  `spaceRole` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'viewer' COMMENT '空间角色：viewer/editor/admin',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_spaceId_userId`(`spaceId`, `userId`) USING BTREE,
  INDEX `idx_spaceId`(`spaceId`) USING BTREE,
  INDEX `idx_userId`(`userId`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 39 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '空间用户关联' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of space_user
-- ----------------------------
INSERT INTO `space_user` VALUES (10, 1981969199176937474, 1956985828009132034, 'admin', '2026-04-25 14:05:40', '2026-04-22 15:22:23');
INSERT INTO `space_user` VALUES (12, 1982039684560494594, 1957032172908269570, 'admin', '2026-04-25 19:01:00', '2026-04-25 19:01:00');
INSERT INTO `space_user` VALUES (13, 1982050098652389377, 1957032172908269570, 'admin', '2026-04-25 19:42:22', '2026-04-25 19:42:22');
INSERT INTO `space_user` VALUES (15, 1982078321301938178, 1957032183779905538, 'admin', '2026-04-25 21:34:31', '2026-04-25 21:34:31');
INSERT INTO `space_user` VALUES (17, 1982050098652389377, 1956985828009132034, 'viewer', '2026-04-27 21:36:51', '2026-04-27 21:36:51');
INSERT INTO `space_user` VALUES (19, 1986618482027192321, 1957032172908269570, 'admin', '2026-04-07 10:15:30', '2026-04-07 10:15:30');
INSERT INTO `space_user` VALUES (20, 1986619202856083457, 1957032183779905538, 'admin', '2026-04-07 10:18:22', '2026-04-07 10:18:22');
INSERT INTO `space_user` VALUES (21, 1986619913400541186, 1957032183779905538, 'admin', '2026-04-07 10:21:11', '2026-04-07 10:21:11');
INSERT INTO `space_user` VALUES (22, 1986620184998502401, 1957032183779905538, 'admin', '2026-04-07 10:22:16', '2026-04-07 10:22:16');
INSERT INTO `space_user` VALUES (23, 1986620964593786881, 1957032197113597954, 'admin', '2026-04-07 10:25:22', '2026-04-07 10:25:22');
INSERT INTO `space_user` VALUES (27, 1986721650559967234, 1957032197113597954, 'admin', '2026-04-07 17:05:27', '2026-04-07 17:05:27');
INSERT INTO `space_user` VALUES (28, 1986722832732938242, 1957032197113597954, 'admin', '2026-04-07 17:10:09', '2026-04-07 17:10:09');
INSERT INTO `space_user` VALUES (29, 1982039684560494594, 1957032197113597954, 'viewer', '2026-05-10 11:30:27', '2026-05-10 11:30:27');
INSERT INTO `space_user` VALUES (30, 2053424685683924994, 1957032183779905538, 'admin', '2026-05-10 18:39:30', '2026-05-10 18:39:30');
INSERT INTO `space_user` VALUES (31, 1986619202856083457, 1957032172908269570, 'viewer', '2026-05-11 10:43:30', '2026-05-11 10:43:30');
INSERT INTO `space_user` VALUES (32, 1986721650559967234, 1957032172908269570, 'viewer', '2026-05-11 10:53:17', '2026-05-11 10:53:17');
INSERT INTO `space_user` VALUES (34, 1986341066734940161, 1957032183779905538, 'admin', '2026-05-11 14:35:28', '2026-05-11 19:44:12');
INSERT INTO `space_user` VALUES (35, 2090270615435014146, 1957032172908269570, 'admin', '2026-08-20 10:52:04', '2026-08-20 10:52:04');
INSERT INTO `space_user` VALUES (36, 2090270615435014146, 1956985828009132034, 'viewer', '2026-08-20 10:53:28', '2026-08-20 10:53:28');
INSERT INTO `space_user` VALUES (37, 1981969199176937474, 1957032172908269570, 'editor', '2026-08-21 18:45:40', '2026-08-21 18:46:02');

-- ----------------------------
-- Table structure for tb_follow
-- ----------------------------
DROP TABLE IF EXISTS `tb_follow`;
CREATE TABLE `tb_follow`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) UNSIGNED NOT NULL COMMENT '用户id',
  `follow_user_id` bigint(20) UNSIGNED NOT NULL COMMENT '关联的用户id',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 27 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = COMPACT;

-- ----------------------------
-- Records of tb_follow
-- ----------------------------
INSERT INTO `tb_follow` VALUES (7, 1957032172908269570, 1956985828009132034, '2026-04-02 09:40:57');
INSERT INTO `tb_follow` VALUES (9, 1957032183779905538, 1957032172908269570, '2026-04-02 20:27:10');
INSERT INTO `tb_follow` VALUES (18, 1957032172908269570, 1957032183779905538, '2026-04-05 16:43:20');
INSERT INTO `tb_follow` VALUES (19, 1956985828009132034, 1957032183779905538, '2026-08-30 15:56:17');
INSERT INTO `tb_follow` VALUES (26, 1956985828009132034, 1957032172908269570, '2026-08-30 17:52:22');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `tags` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签 json 列表',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `fansNum` int(11) NULL DEFAULT 0 COMMENT '粉丝数',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_userAccount`(`userAccount`) USING BTREE,
  INDEX `idx_userName`(`userName`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1985986839558017027 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1956985828009132034, 'zzxcs4', '5504df6ea1e86d05aa176fc234221b4b', 'zzx', 'https://ts2.tc.mm.bing.net/th/id/OIP-C.xOMhNmAJqSVNWJz90lV19wHaHa?w=250&h=250&c=8&rs=1&qlt=90&o=6&dpr=1.5&pid=3.1&rm=2', '这个网站的老大', 'admin', '[\"女\",\"动漫\"]', '2026-03-17 15:45:55', '2026-03-17 15:45:55', '2026-04-07 17:50:26', 0, 1);
INSERT INTO `user` VALUES (1957032172908269570, 'zzxcs1', '5504df6ea1e86d05aa176fc234221b4b', 'zzx1', 'https://n.sinaimg.cn/sinakd10105/115/w1200h1315/20200413/a235-isehnni6278446.jpg', '万水千山走过，内心柔软如初', 'user', '[\"前端开发\",\"男\",\"动漫\",\"轻小说\"]', '2026-03-17 18:50:04', '2026-03-17 18:50:04', '2026-08-30 17:52:16', 0, 2);
INSERT INTO `user` VALUES (1957032183779905538, 'zzxcs2', '5504df6ea1e86d05aa176fc234221b4b', '无名', 'https://ts3.tc.mm.bing.net/th/id/OIP-C.sKekWmKFXE9r72iVsI1S2wHaHa?w=250&h=250&c=8&rs=1&qlt=90&o=6&dpr=1.5&pid=3.1&rm=2', NULL, 'user', '[\"后端开发\",\"运维\",\"男\",\"动漫\",\"游戏\"]', '2026-03-17 18:50:07', '2026-03-17 18:50:07', '2026-04-07 17:45:50', 0, 2);
INSERT INTO `user` VALUES (1957032197113597954, 'zzxcs3', '5504df6ea1e86d05aa176fc234221b4b', '无名', 'https://ts1.tc.mm.bing.net/th/id/OIP-C.JQKwY4z7yVJpWFR7WKOGAAHaHa?w=250&h=250&c=8&rs=1&qlt=90&o=6&dpr=1.5&pid=3.1&rm=2', NULL, 'user', '[\"前端开发\",\"女\",\"动漫\",\"游戏\"]', '2026-03-17 18:50:10', '2026-03-17 18:50:10', '2026-04-07 17:44:25', 0, 0);
INSERT INTO `user` VALUES (1957774606432751617, 'zzxcs7', '5504df6ea1e86d05aa176fc234221b4b', '无名', NULL, NULL, 'user', '[\"后端开发\",\"男\",\"前端开发\"]', '2026-03-19 20:00:14', '2026-03-19 20:00:14', '2026-04-07 17:43:24', 1, 0);
INSERT INTO `user` VALUES (1985934115839725570, 'zzxcs8', '5504df6ea1e86d05aa176fc234221b4b', '无名', NULL, NULL, 'user', NULL, '2026-04-05 12:56:04', '2026-04-05 12:56:04', '2026-04-05 16:50:29', 0, 0);
INSERT INTO `user` VALUES (1985984422691000321, 'zzxcs9', '5504df6ea1e86d05aa176fc234221b4b', '无名', NULL, NULL, 'user', NULL, '2026-04-05 16:15:58', '2026-04-05 16:15:58', '2026-04-05 16:50:30', 0, 0);
INSERT INTO `user` VALUES (1985986277257039874, 'zzxcs10', '5504df6ea1e86d05aa176fc234221b4b', '无名', NULL, NULL, 'user', NULL, '2026-04-05 16:23:20', '2026-04-05 16:23:20', '2026-04-05 16:50:31', 0, 0);
INSERT INTO `user` VALUES (1985986839558017026, 'zzxcs11', '5504df6ea1e86d05aa176fc234221b4b', '无名', NULL, '喜欢结交朋友', 'user', NULL, '2026-04-05 16:25:35', '2026-04-05 16:25:35', '2026-04-05 16:50:36', 0, 0);

-- ----------------------------
-- Table structure for user_field
-- ----------------------------
DROP TABLE IF EXISTS `user_field`;
CREATE TABLE `user_field`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userId` bigint(20) NULL DEFAULT NULL COMMENT '用户id',
  `fieldId` bigint(20) NULL DEFAULT NULL COMMENT '花田id',
  `joinTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `createTime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `isDelete` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户花田关系' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_field
-- ----------------------------
INSERT INTO `user_field` VALUES (28, 1956985828009132034, 1981969199210491906, '2026-04-25 14:20:54', '2026-04-25 14:20:54', '2026-04-25 14:20:54', 0);
INSERT INTO `user_field` VALUES (30, 1957032172908269570, 1981969199210491906, '2026-04-25 17:29:32', '2026-04-25 17:29:31', '2026-04-25 17:29:31', 0);
INSERT INTO `user_field` VALUES (31, 1957032172908269570, 1982039684778598402, '2026-04-25 19:01:00', '2026-04-25 19:01:00', '2026-04-25 19:01:00', 0);
INSERT INTO `user_field` VALUES (32, 1957032183779905538, 1982078321423572993, '2026-04-25 21:34:31', '2026-04-25 21:34:31', '2026-04-25 21:34:31', 0);
INSERT INTO `user_field` VALUES (33, 1957032172908269570, 1982078321423572993, '2026-04-27 20:04:36', '2026-04-27 20:04:36', '2026-04-27 20:04:37', 1);
INSERT INTO `user_field` VALUES (34, 1957032172908269570, 1982078321423572993, '2026-04-27 20:04:39', '2026-04-27 20:04:38', '2026-04-27 20:04:39', 1);
INSERT INTO `user_field` VALUES (35, 1957032172908269570, 1982078321423572993, '2026-04-27 20:04:40', '2026-04-27 20:04:39', '2026-04-27 20:04:40', 1);
INSERT INTO `user_field` VALUES (36, 1957032172908269570, 1982078321423572993, '2026-04-27 20:04:46', '2026-04-27 20:04:45', '2026-04-27 20:04:46', 1);
INSERT INTO `user_field` VALUES (37, 1957032172908269570, 1982078321423572993, '2026-04-27 20:04:52', '2026-04-27 20:04:52', '2026-04-27 20:04:52', 1);
INSERT INTO `user_field` VALUES (38, 1957032172908269570, 1982078321423572993, '2026-04-27 20:04:53', '2026-04-27 20:04:52', '2026-04-27 20:04:53', 1);
INSERT INTO `user_field` VALUES (39, 1957032172908269570, 1982078321423572993, '2026-04-27 20:04:53', '2026-04-27 20:04:53', '2026-04-27 20:04:54', 1);
INSERT INTO `user_field` VALUES (40, 1956985828009132034, 1982039684778598402, '2026-04-27 16:27:51', '2026-04-27 16:27:50', '2026-04-27 21:35:57', 1);
INSERT INTO `user_field` VALUES (41, 1957032172908269570, 1986341066856574978, '2026-04-06 15:53:09', '2026-04-06 15:53:09', '2026-05-11 15:16:02', 1);
INSERT INTO `user_field` VALUES (42, 1957032172908269570, 1986618482140438530, '2026-04-07 10:15:30', '2026-04-07 10:15:30', '2026-04-07 10:15:30', 0);
INSERT INTO `user_field` VALUES (43, 1957032183779905538, 1986619202860277762, '2026-04-07 10:18:22', '2026-04-07 10:18:22', '2026-04-07 10:18:22', 0);
INSERT INTO `user_field` VALUES (44, 1957032183779905538, 1986619913408929793, '2026-04-07 10:21:11', '2026-04-07 10:21:11', '2026-04-07 10:21:11', 0);
INSERT INTO `user_field` VALUES (45, 1957032183779905538, 1986620185019473922, '2026-04-07 10:22:16', '2026-04-07 10:22:16', '2026-04-07 10:22:16', 0);
INSERT INTO `user_field` VALUES (46, 1957032197113597954, 1986620964610564098, '2026-04-07 10:25:22', '2026-04-07 10:25:22', '2026-04-07 12:19:24', 1);
INSERT INTO `user_field` VALUES (47, 1957032197113597954, 1986621964276768770, '2026-04-07 10:29:20', '2026-04-07 10:29:20', '2026-04-07 12:19:46', 1);
INSERT INTO `user_field` VALUES (48, 1957032197113597954, 1986622914475368450, '2026-04-07 10:33:07', '2026-04-07 10:33:07', '2026-04-07 10:33:07', 0);
INSERT INTO `user_field` VALUES (49, 1956985828009132034, 1986622914475368450, '2026-04-07 12:25:28', '2026-04-07 12:25:28', '2026-04-07 16:57:34', 1);
INSERT INTO `user_field` VALUES (50, 1957032172908269570, 1986622914475368450, '2026-04-07 16:35:07', '2026-04-07 16:35:07', '2026-04-07 16:52:11', 1);
INSERT INTO `user_field` VALUES (51, 1956985828009132034, 1986622914475368450, '2026-04-07 16:57:55', '2026-04-07 16:57:55', '2026-04-07 16:57:55', 0);
INSERT INTO `user_field` VALUES (52, 1956985828009132034, 1986720492869148674, '2026-04-07 17:00:51', '2026-04-07 17:00:51', '2026-08-20 10:50:04', 1);
INSERT INTO `user_field` VALUES (53, 1957032197113597954, 1986721650585133058, '2026-04-07 17:05:27', '2026-04-07 17:05:27', '2026-04-07 17:05:27', 0);
INSERT INTO `user_field` VALUES (54, 1957032197113597954, 1986722832753909762, '2026-04-07 17:10:09', '2026-04-07 17:10:09', '2026-04-07 17:10:09', 0);
INSERT INTO `user_field` VALUES (55, 1957032172908269570, 1986622914475368450, '2026-04-07 17:17:58', '2026-04-07 17:17:57', '2026-04-07 17:17:59', 1);
INSERT INTO `user_field` VALUES (56, 1957032172908269570, 1986720492869148674, '2026-04-07 18:22:51', '2026-04-07 18:22:51', '2026-04-07 18:23:39', 1);
INSERT INTO `user_field` VALUES (57, 1957032172908269570, 1986720492869148674, '2026-04-07 18:23:43', '2026-04-07 18:23:43', '2026-04-07 18:25:27', 1);
INSERT INTO `user_field` VALUES (58, 1957032172908269570, 1986720492869148674, '2026-04-07 18:25:28', '2026-04-07 18:25:28', '2026-04-07 18:25:53', 1);
INSERT INTO `user_field` VALUES (59, 1957032172908269570, 1986720492869148674, '2026-04-07 18:25:55', '2026-04-07 18:25:54', '2026-04-07 18:25:56', 1);
INSERT INTO `user_field` VALUES (60, 1956985828009132034, 1986748448161218562, '2026-04-07 18:51:56', '2026-04-07 18:51:56', '2026-04-07 18:53:04', 1);
INSERT INTO `user_field` VALUES (61, 1956985828009132034, 1986748836641849346, '2026-04-07 18:53:29', '2026-04-07 18:53:29', '2026-04-07 18:54:13', 1);
INSERT INTO `user_field` VALUES (62, 1957032197113597954, 1982039684778598402, '2026-05-10 11:30:28', '2026-05-10 11:30:27', '2026-05-10 11:30:27', 0);
INSERT INTO `user_field` VALUES (63, 1957032172908269570, 1986619202860277762, '2026-05-11 10:43:30', '2026-05-11 10:43:30', '2026-05-11 10:43:30', 0);
INSERT INTO `user_field` VALUES (64, 1957032172908269570, 1986721650585133058, '2026-05-11 10:53:18', '2026-05-11 10:53:17', '2026-05-11 10:53:17', 0);
INSERT INTO `user_field` VALUES (65, 1957032183779905538, 1981969199210491906, '2026-05-11 11:37:45', '2026-05-11 11:37:44', '2026-05-11 11:37:52', 1);
INSERT INTO `user_field` VALUES (66, 1957032183779905538, 1986341066856574978, '2026-05-11 14:35:29', '2026-05-11 14:35:28', '2026-05-11 14:35:28', 0);
INSERT INTO `user_field` VALUES (67, 1957032172908269570, 1986341066856574978, '2026-05-11 19:41:14', '2026-05-11 19:41:13', '2026-05-11 19:41:26', 1);
INSERT INTO `user_field` VALUES (68, 1957032172908269570, 2090270615485345794, '2026-08-20 10:52:04', '2026-08-20 10:52:04', '2026-08-20 10:52:04', 0);
INSERT INTO `user_field` VALUES (69, 1956985828009132034, 2090270615485345794, '2026-08-20 10:53:28', '2026-08-20 10:53:28', '2026-08-20 10:53:28', 0);
INSERT INTO `user_field` VALUES (70, 1985986839558017026, 1981969199210491906, '2026-08-31 13:08:54', '2026-08-31 13:08:53', '2026-08-31 13:09:04', 1);

-- ----------------------------
-- Table structure for user_field_score
-- ----------------------------
DROP TABLE IF EXISTS `user_field_score`;
CREATE TABLE `user_field_score`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户id',
  `space_id` bigint(20) NOT NULL COMMENT '花田id',
  `score` double NOT NULL DEFAULT 0 COMMENT '评分（0~10）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_field`(`user_id`, `space_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户-花田评分表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_field_score
-- ----------------------------
INSERT INTO `user_field_score` VALUES (1, 1956985828009132034, 1981969199210491906, 1.64, '2026-04-14 13:42:01', '2026-09-03 00:13:12');
INSERT INTO `user_field_score` VALUES (2, 1957032197113597954, 1982039684778598402, 0, '2026-05-10 11:30:28', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (3, 1957032183779905538, 1986619202860277762, 6.1, '2026-05-10 19:45:49', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (4, 1957032172908269570, 1982078321423572993, 8.4, '2026-05-11 10:38:03', '2026-05-11 10:46:56');
INSERT INTO `user_field_score` VALUES (5, 1957032172908269570, 1986619202860277762, 8.14, '2026-05-11 10:43:30', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (6, 1957032183779905538, 1982078321423572993, 5.4, '2026-05-11 10:47:28', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (7, 1957032172908269570, 1982039684778598402, 8.4, '2026-05-11 10:52:01', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (8, 1957032172908269570, 1986721650585133058, 8.34, '2026-05-11 10:53:18', '2026-08-19 10:32:37');
INSERT INTO `user_field_score` VALUES (9, 1957032183779905538, 1981969199210491906, 1, '2026-05-11 11:37:45', '2026-05-11 11:37:45');
INSERT INTO `user_field_score` VALUES (10, 1957032183779905538, 1986341066856574978, 5.34, '2026-05-11 14:35:29', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (11, 1957032183779905538, 1986619913408929793, 6.34, '2026-05-11 14:55:29', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (12, 1957032197113597954, 1986622914475368450, 0, '2026-08-19 10:32:36', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (13, 1957032197113597954, 1986721650585133058, 0, '2026-08-19 10:32:36', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (14, 1957032197113597954, 1986722832753909762, 0, '2026-08-19 10:32:36', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (15, 1957032183779905538, 1986620185019473922, 0, '2026-08-19 10:32:36', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (16, 1957032172908269570, 1981969199210491906, 0, '2026-08-19 10:32:36', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (17, 1957032172908269570, 1986618482140438530, 0, '2026-08-19 10:32:36', '2026-08-19 10:32:36');
INSERT INTO `user_field_score` VALUES (18, 1956985828009132034, 1981969199210492000, 0.6, '2026-08-20 10:46:06', '2026-08-20 10:46:06');
INSERT INTO `user_field_score` VALUES (19, 1956985828009132034, 1986720492869148674, 0, '2026-08-20 10:50:05', '2026-08-20 10:50:05');
INSERT INTO `user_field_score` VALUES (20, 1956985828009132034, 2090270615485345794, 1, '2026-08-20 10:53:28', '2026-08-20 10:53:28');
INSERT INTO `user_field_score` VALUES (21, 1985986839558017026, 1981969199210491906, 0, '2026-08-31 13:08:54', '2026-08-31 13:09:05');
INSERT INTO `user_field_score` VALUES (22, 1956985828009132034, 1986622914475368450, 0, '2026-09-03 01:59:56', '2026-09-03 01:59:56');

-- ----------------------------
-- Table structure for user_link
-- ----------------------------
DROP TABLE IF EXISTS `user_link`;
CREATE TABLE `user_link`  (
  `linkId` int(11) NOT NULL AUTO_INCREMENT,
  `from` bigint(20) NOT NULL,
  `to` bigint(20) NOT NULL,
  PRIMARY KEY (`linkId`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_link
-- ----------------------------
INSERT INTO `user_link` VALUES (1, 1956985828009132034, 1957032172908269570);
INSERT INTO `user_link` VALUES (2, 1957032172908269570, 1957032172908269570);
INSERT INTO `user_link` VALUES (3, 1957032172908269570, 1957032183779905538);
INSERT INTO `user_link` VALUES (4, 1956985828009132034, 1956985828009132034);

-- ----------------------------
-- Table structure for videos
-- ----------------------------
DROP TABLE IF EXISTS `videos`;
CREATE TABLE `videos`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频 url',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '视频名称',
  `introduction` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '视频简介',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类',
  `tags` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（JSON 数组）',
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图片URL',
  `duration` int(11) NULL DEFAULT NULL COMMENT '视频时长（秒）',
  `fileSize` bigint(20) NULL DEFAULT NULL COMMENT '文件体积',
  `fileFormat` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件格式',
  `userId` bigint(20) NOT NULL COMMENT '创建用户 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint(4) NOT NULL DEFAULT 0 COMMENT '是否删除',
  `reviewStatus` int(11) NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
  `reviewMessage` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核信息',
  `reviewerId` bigint(20) NULL DEFAULT NULL COMMENT '审核人 ID',
  `reviewTime` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `spaceId` bigint(20) NULL DEFAULT NULL COMMENT '空间 id（为空表示公共空间）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_introduction`(`introduction`) USING BTREE,
  INDEX `idx_category`(`category`) USING BTREE,
  INDEX `idx_tags`(`tags`) USING BTREE,
  INDEX `idx_userId`(`userId`) USING BTREE,
  INDEX `idx_reviewStatus`(`reviewStatus`) USING BTREE,
  INDEX `idx_spaceId`(`spaceId`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '视频' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of videos
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
