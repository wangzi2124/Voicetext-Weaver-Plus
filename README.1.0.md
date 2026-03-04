# VoiceText Weaver 多租户音频处理平台 - 全功能列表

**版本**: 2.0.0 · 最后更新: 2026-03-04


## 一、核心功能

| 功能模块 | 核心特性 | 说明 |
|---------|----------|------|
| 多租户管理 | 租户隔离、套餐配额、全局数据源隔离 | Mybatis-Plus多租户插件 |
| 用户/部门/岗位 | 用户管理、部门树、岗位字典 | 租户自管理 |
| 菜单与权限 | 动态路由、按钮级权限、Sa-Token认证 | RBAC模型 |
| 角色管理 | 租户内角色、数据权限 | 可扩展租户管理员 |
| 字典管理 | 全局/租户级字典 | 音频格式、任务类型等 |
| 操作日志 | 所有资源变更、登录日志 | 注解@Log |
| OSS存储 | MinIO/阿里云/七牛云适配 | 统一存储音频/模型/图片 |
| 任务调度 | SnailJob分布式调度、失败重试 | 处理长任务 |
| 缓存与锁 | Redis(Redisson) + Lock4j | 分布式锁/队列 |

## 二、租户资源池 & 共享 (ruoyi-tenant-resource)

| 功能 | 描述 | 权限/备注 |
|------|------|-----------|
| 音频/文本资源上传 | 分片上传、断点续传、格式检测 | tenant:resource:upload |
| 资源列表/查询 | 按类型/权限/日期过滤，波形预览 | tenant:resource:list |
| 资源权限控制 | 私有/租户共享/跨租户授权 | permission_type字段 |
| 跨租户共享 | 按租户或用户授权，有效期/权限码(view/edit/download/merge) | resource_share表 |
| 资源回收站 | 软删除、批量恢复、自动清理 | resource_status=0 |
| 配额统计 | 存储用量、处理次数、TTS字符数 | sys_tenant扩展 |
| 资源波形/预览 | 实时波形、频谱图、VAD结果 | Wavesurfer.js |

## 三、音频处理核心 (ruoyi-common-audio)

| 功能 | 技术实现 | 任务类型 |
|------|----------|----------|
| 格式转换 | FFmpeg封装 (mp3/wav/flac) | async, audio:convert |
| 音频裁剪/拼接 | FFmpeg + TarsosDSP | async, audio:edit |
| 音量调整/归一化 | TarsosDSP | async |
| 语音活动检测(VAD) | 能量/谱熵，返回语音段落 | 实时/异步 |
| 基频/音高分析 | TarsosDSP PitchDetector | audio:analyze |
| 语音转文字(Whisper) | HTTP调用独立服务 | transcription |
| 文字转语音(TTS) | Coqui TTS / 本地模型池 | tts |
| 声纹/情绪识别(预留) | 扩展接口 | -- |
| 音频指纹/特征提取 | MFCC、频谱质心、过零率 | 用于检索 |

## 四、声音训练 · 克隆 · 模型管理 (VoiceClone)

| 功能 | 详细描述 | 关键点 |
|------|----------|--------|
| 声音训练任务 | 上传10~30分钟音频，训练个性化声音模型(So-VITS-SVC/OpenVoice) | voice:train:create |
| 训练进度监控 | 实时轮次、loss曲线、中间示例生成 | WebSocket轮询 |
| 模型版本管理 | 每个租户最多5个模型，支持设为默认 | voice_model表 |
| 声音克隆合成 | 输入文本+选择模型，生成克隆语音 | voice:clone:use |
| 模型分享/复用 | 跨租户共享声音模型(需授权) | 预留字段 |
| 音频质量检测 | 训练前自动检测底噪、时长、采样率 | 优化建议 |
| 示例音频生成 | 训练过程中每N轮合成示例 | 可播放对比 |
| 模型下载/导出 | 训练完成的模型文件打包 | 租户管理员权限 |

## 五、项目管理 & 音频合并工坊 (Project)

| 功能 | 说明 | 操作/规则 |
|------|------|-----------|
| 项目创建/删除/编辑 | 基础CRUD，支持项目名称、描述、输出格式 | project:create |
| 音频资源添加 | 从自有或共享资源选取音频(跨租户可见) | 需merge权限 |
| 序号管理 | 每个音频分配sequence_no决定合并顺序 | 拖拽重排 |
| 片段截取 | 对音频设置start/end，仅合并选定区间 | 精度0.1秒 |
| 音量/淡入淡出 | 独立调整每个片段的音量、淡入/淡出时长 | 实时预览 |
| 合并执行(异步) | 按照序号及参数生成最终音频，存入资源库 | SnailJob任务 |
| 项目状态流转 | 正常→已完成→已归档→回收站→恢复/永久删除 | project_status字段 |
| 回收站与恢复 | 软删除项目，支持批量恢复，30天后自动清理 | project:recover |
| 项目预览 | 合并前试听当前序列(模拟混合) | 波形叠加预览 |
| 跨租户资源协作 | 租户B/C的资源若共享了merge权限，可被租户A项目引用 | allow_merge字段 |
| 合并模板 | 保存常用合并配置(播客模板、有声书模板) | 后续迭代 |

## 六、权限 & 租户配额 (扩展)

| 权限点/配额 | 说明 | 默认值 |
|-------------|------|--------|
| voice:train:create | 发起声音训练 | 租户管理员/普通用户 |
| voice:model:view | 查看声音模型列表 | 全租户内 |
| project:merge | 执行合并操作(可能消耗配额) | 按次计费 |
| resource:share:merge | 共享资源时允许被用于合并 | 资源创建者可配置 |
| storage_quota (租户) | 存储配额(字节) | 默认10GB |
| voice_model_quota | 最大声音模型数 | 5个 |
| project_quota | 最大项目数(含回收站) | 50个 |
| clone_quota (月) | 每月克隆次数 | 1000次 |
| merge_quota (月) | 每月合并任务次数 | 500次 |


## 七、租户资源池 & 共享 (含表结构)

### 新增表：tenant_resource
```sql
CREATE TABLE `tenant_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) NOT NULL,
  `resource_name` varchar(200) NOT NULL,
  `resource_type` varchar(20) NOT NULL,
  `file_format` varchar(20) DEFAULT NULL,
  `file_size` bigint(20) DEFAULT NULL,
  `oss_url` varchar(500) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `sample_rate` int(11) DEFAULT NULL,
  `channels` int(11) DEFAULT NULL,
  `transcription` text,
  `permission_type` varchar(20) NOT NULL DEFAULT 'private',
  `resource_status` tinyint(4) DEFAULT '1',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_resource_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 新增表：resource_share (增加合并字段)
```sql
CREATE TABLE `resource_share` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `source_tenant_id` bigint(20) NOT NULL,
  `target_tenant_id` bigint(20) DEFAULT NULL,
  `target_user_id` bigint(20) DEFAULT NULL,
  `permission_code` varchar(50) NOT NULL,
  `expire_time` datetime DEFAULT NULL,
  `share_status` tinyint(4) DEFAULT '1',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `allow_merge` tinyint(4) DEFAULT '0',
  `merge_limit` int(11) DEFAULT '0',
  `merge_used` int(11) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_resource_id` (`resource_id`),
  KEY `idx_target_tenant` (`target_tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### sys_tenant 扩展字段
```sql
ALTER TABLE sys_tenant 
ADD COLUMN storage_quota bigint DEFAULT 10737418240,
ADD COLUMN storage_used bigint DEFAULT 0,
ADD COLUMN audio_process_quota int DEFAULT 1000,
ADD COLUMN audio_process_used int DEFAULT 0,
ADD COLUMN tts_quota int DEFAULT 500,
ADD COLUMN tts_used int DEFAULT 0,
ADD COLUMN voice_model_quota int DEFAULT 5,
ADD COLUMN voice_model_used int DEFAULT 0,
ADD COLUMN project_quota int DEFAULT 50,
ADD COLUMN project_used int DEFAULT 0,
ADD COLUMN clone_quota int DEFAULT 1000,
ADD COLUMN clone_used int DEFAULT 0,
ADD COLUMN merge_quota int DEFAULT 500,
ADD COLUMN merge_used int DEFAULT 0;
```

---

## 音频处理任务表 (audio_process_task)

```sql
CREATE TABLE `audio_process_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) NOT NULL,
  `resource_id` bigint(20) DEFAULT NULL,
  `tenant_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) NOT NULL,
  `task_type` varchar(30) NOT NULL,
  `task_status` varchar(20) DEFAULT 'pending',
  `task_config` json,
  `task_result` json,
  `error_msg` text,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_task_no` (`task_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_task_type` (`task_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 声音模型表 (voice_model)

```sql
CREATE TABLE `voice_model` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `model_no` varchar(64) NOT NULL,
  `model_name` varchar(100) NOT NULL,
  `tenant_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) NOT NULL,
  `source_resource_id` bigint(20) DEFAULT NULL,
  `model_type` varchar(20) NOT NULL,
  `model_path` varchar(500) DEFAULT NULL,
  `model_config` json,
  `sample_audio_url` varchar(500) DEFAULT NULL,
  `train_duration` int(11) DEFAULT NULL,
  `train_status` varchar(20) DEFAULT 'pending',
  `train_progress` int(11) DEFAULT '0',
  `train_task_id` bigint(20) DEFAULT NULL,
  `is_default` tinyint(4) DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_model_no` (`model_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_train_status` (`train_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 项目管理 (audio_project & project_audio_detail)

```sql
CREATE TABLE `audio_project` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_no` varchar(64) NOT NULL,
  `project_name` varchar(200) NOT NULL,
  `project_desc` text,
  `tenant_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) NOT NULL,
  `project_status` tinyint(4) DEFAULT '1',
  `output_resource_id` bigint(20) DEFAULT NULL,
  `output_duration` int(11) DEFAULT NULL,
  `output_format` varchar(10) DEFAULT 'mp3',
  `merge_config` json,
  `is_recovered` tinyint(4) DEFAULT '0',
  `recover_from_id` bigint(20) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_project_no` (`project_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project_status` (`project_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `project_audio_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) NOT NULL,
  `resource_id` bigint(20) NOT NULL,
  `source_tenant_id` bigint(20) NOT NULL,
  `audio_name` varchar(200) DEFAULT NULL,
  `audio_duration` int(11) DEFAULT NULL,
  `sequence_no` int(11) NOT NULL,
  `start_time` decimal(10,2) DEFAULT NULL,
  `end_time` decimal(10,2) DEFAULT NULL,
  `volume_adjust` decimal(4,2) DEFAULT '1.00',
  `fade_in` decimal(5,2) DEFAULT '0.00',
  `fade_out` decimal(5,2) DEFAULT '0.00',
  `remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_id` (`project_id`),
  KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```


## 七扩展AI能力

- AI助手(RAG)：基于知识库问答，结合音频转写内容 (ruoyi-ai-assistant)
- 音频知识图谱：存储音频实体、说话人、主题标签，Neo4j图查询
- 智能摘要：对长音频转文字后生成摘要/关键词 (需大模型)

## 八、前端UI/UX (Vue3 + Element Plus)

| 视图 | 组件/能力 |
|------|-----------|
| 资源池主界面 | 列表/卡片切换、权限标签、租户存储用量条 |
| 音频详情页 | 波形预览(Wavesurfer)、播放器、转写文本对照 |
| 声音训练向导 | 多步表单、音频质检、训练进度条、中间结果试听 |
| 项目编辑器 | 拖拽排序轨道、片段截取滑块、音量旋钮、淡入淡出滑块 |
| 资源共享弹窗 | 按租户/用户搜索、权限组合、过期时间 |
| 回收站视图 | 删除时间、剩余天数、恢复按钮 |
| 仪表盘统计 | 资源用量、任务趋势、热门模型 |

## 九、运维 & 监控

- Docker编排：MySQL、Redis、MinIO、Whisper、TTS、voice-clone、model-train (GPU分配)
- Prometheus监控：音频任务队列、GPU利用率、模型训练时长、克隆QPS
- 日志采集：ELK收集操作日志、训练日志、识别错误
- 备份策略：每日备份MySQL、模型文件、OSS增量同步

---

**核心数据实体**: sys_tenant(扩展配额) / tenant_resource / resource_share / voice_model / audio_project / project_audio_detail / audio_process_task

**后续规划**: AI智能剪辑 · 批量项目模板 · 声纹识别分离 · 云端实时协作 · 情感迁移合成

--- 
⚡ 文档版本 2.0.0 · 基于 RuoYi-Vue-Plus 扩展 · 2026-03-04