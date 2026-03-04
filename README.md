# VoiceText Weaver 多租户音频处理平台

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

## 二、租户资源池 & 共享 (tenant-resource)

| 功能 | 描述 | 权限/备注 |
|------|------|-----------|
| 音频/文本资源上传 | 分片上传、断点续传、格式检测 | tenant:resource:upload |
| 资源列表/查询 | 按类型/权限/日期过滤，波形预览 | tenant:resource:list |
| 资源权限控制 | 私有/租户共享/跨租户授权 | permission_type字段 |
| 跨租户共享 | 按租户或用户授权，有效期/权限码(view/edit/download/merge) | resource_share表 |
| 资源回收站 | 软删除、批量恢复、自动清理 | resource_status=0 |
| 配额统计 | 存储用量、处理次数、TTS字符数 | sys_tenant扩展 |
| 资源波形/预览 | 实时波形、频谱图、VAD结果 | Wavesurfer.js |

## 三、音频处理核心 (common-audio)

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

## 七扩展AI能力

- AI助手(RAG)：基于知识库问答，结合音频转写内容 (ai-assistant)
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


### 一、项目整体架构设计

#### 1.1 技术栈整合方案

**后端技术栈**

| 技术领域 | 核心框架/工具 | 版本 | 说明 |
|---------|--------------|------|------|
| 基础框架 | Spring Boot | 3.5.10 | 原框架已集成 |
| 多租户核心 | Mybatis-Plus 多租户插件 | 3.5.16 | 原框架已深度集成 |
| 权限认证 | Sa-Token | 1.44.0 | 原框架已集成，扩展租户级权限 |
| 音频处理 | TarsosDSP + FFmpeg | 2.5 | 新增音频分析核心 |
| 语音识别 | Whisper(Java封装) | 20231106 | 通过JNI调用或HTTP服务 |
| 语音合成 | Coqui TTS(HTTP服务) | 0.13.3 | 独立服务部署 |
| 声音克隆 | So-VITS-SVC / OpenVoice | - | 新增声音训练与克隆 |
| 分布式任务 | SnailJob | 1.9.0 | 原框架已集成，处理TTS长任务 |
| 分布式锁 | Lock4j + Redisson | 2.2.7/3.52.0 | 原框架已集成 |
| 对象存储 | Minio/AWS S3 | 2.28.22 | 原框架已集成，存储音频文件 |
| 缓存 | Redis(Redisson) | 3.52.0 | 原框架已集成 |
| 消息队列 | Redis Stream/队列 | - | 利用Redisson分布式队列 |
| 图数据库 | Neo4j | 5.26.0 | 原框架已支持，用于知识图谱 |
| AI能力 | Spring AI + LangChain4j | 1.0.2/1.3.0 | 原框架已集成，扩展RAG能力 |

**前端技术栈**

| 技术领域 | 框架/工具 | 版本 | 说明 |
|---------|----------|------|------|
| 基础框架 | Vue 3 + TypeScript | - | 原框架已采用 |
| UI组件 | Element Plus | - | 原框架已采用 |
| 音频可视化 | Wavesurfer.js | 7.8.0 | 新增音频波形显示 |
| 音频录制 | RecordRTC | 5.6.2 | 新增浏览器录音功能 |
| 音频剪辑 | WaveSurfer Regions | - | 音频片段选择与编辑 |

#### 1.2 模块架构设计

```
voicetext-weaver-plus/
├── voicetext-admin/                    # 系统启动模块
├── voicetext-common/                    # 通用工具模块
│   ├── voicetext-common-core/           # 核心工具
│   ├── voicetext-common-redis/          # Redis缓存
│   ├── voicetext-common-oss/            # 对象存储
│   ├── voicetext-common-satoken/        # 权限认证
│   └── voicetext-common-audio/          # 【新增】音频处理核心
│       ├── src/main/java/org/dromara/audio/
│       │   ├── core/                 # 音频核心处理
│       │   │   ├── AudioProcessor.java
│       │   │   ├── TarsosAudioProcessor.java
│       │   │   ├── FFmpegAudioProcessor.java
│       │   │   └── AudioFeatureExtractor.java
│       │   ├── whisper/              # Whisper语音识别
│       │   │   ├── WhisperService.java
│       │   │   └── WhisperModelManager.java
│       │   ├── tts/                  # TTS语音合成
│       │   │   ├── TtsService.java
│       │   │   └── TtsTaskConsumer.java
│       │   ├── voiceclone/           # 声音克隆模块
│       │   │   ├── VoiceTrainService.java
│       │   │   ├── VoiceCloneService.java
│       │   │   ├── VoiceModelManager.java
│       │   │   └── dto/
│       │   ├── project/              # 项目管理模块
│       │   │   ├── AudioProjectService.java
│       │   │   ├── AudioMergeService.java
│       │   │   ├── SequenceManager.java
│       │   │   └── dto/
│       │   └── dsp/                  # 数字信号处理
│       │       ├── AudioAnalyzer.java
│       │       └── VoiceActivityDetector.java
│       └── pom.xml
├── voicetext-modules/                    # 业务模块
│   ├── voicetext-system/                 # 系统管理
│   ├── voicetext-workflow/               # 工作流
│   ├── voicetext-tenant-resource/        # 【新增】租户资源池管理
│   │   ├── src/main/java/org/dromara/tenantresource/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── mapper/
│   │   │   └── domain/
│   │   └── pom.xml
│   ├── voicetext-ai-gateway/             # 【新增】AI服务网关
│   │   ├── src/main/java/org/dromara/aigateway/
│   │   │   ├── api/                   # 统一AI接口
│   │   │   ├── adapter/               # 服务适配器
│   │   │   │   ├── WhisperAdapter.java
│   │   │   │   ├── CoquiTtsAdapter.java
│   │   │   │   └── VoiceCloneAdapter.java
│   │   │   ├── router/                # 服务路由
│   │   │   └── fallback/              # 降级处理
│   │   └── pom.xml
│   ├── voicetext-ai-assistant/           # AI助手模块
│   └── voicetext-knowledge-graph/        # 知识图谱
└── voicetext-extend/                     # 扩展组件
    └── voicetext-xxl-job/                # 定时任务
```

#### 1.3 数据库设计

**1.3.1 租户资源表 (tenant_resource)**
```sql
CREATE TABLE tenant_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资源ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    creator_id BIGINT NOT NULL COMMENT '创建者用户ID',
    resource_name VARCHAR(200) NOT NULL COMMENT '资源名称',
    resource_type VARCHAR(20) NOT NULL COMMENT '资源类型: audio/text',
    file_format VARCHAR(20) COMMENT '文件格式: mp3/wav/txt/md',
    file_size BIGINT COMMENT '文件大小(字节)',
    oss_url VARCHAR(500) COMMENT 'OSS存储路径',
    duration INT COMMENT '音频时长(秒)',
    sample_rate INT COMMENT '采样率(Hz)',
    channels INT COMMENT '声道数',
    transcription TEXT COMMENT '语音转文本内容',
    permission_type VARCHAR(20) NOT NULL DEFAULT 'private' COMMENT '权限类型: private/tenant_shared/cross_tenant',
    resource_status TINYINT DEFAULT 1 COMMENT '状态: 0删除 1正常 2处理中',
    ref_count INT DEFAULT 0 COMMENT '引用计数(项目引用次数)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_creator_id (creator_id),
    INDEX idx_resource_type (resource_type),
    FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id),
    FOREIGN KEY (creator_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户资源表';
```

**1.3.2 资源共享表 (resource_share)**
```sql
CREATE TABLE resource_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '共享ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    source_tenant_id BIGINT NOT NULL COMMENT '来源租户ID',
    target_tenant_id BIGINT COMMENT '目标租户ID',
    target_user_id BIGINT COMMENT '目标用户ID',
    permission_code VARCHAR(50) NOT NULL COMMENT '权限编码: view/edit/download/delete/merge',
    expire_time DATETIME COMMENT '过期时间',
    share_status TINYINT DEFAULT 1 COMMENT '状态: 0失效 1生效',
    allow_merge TINYINT DEFAULT 0 COMMENT '允许合并: 0否 1是',
    merge_limit INT DEFAULT 0 COMMENT '合并使用次数限制(0无限制)',
    merge_used INT DEFAULT 0 COMMENT '已使用合并次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT COMMENT '创建者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resource_id (resource_id),
    INDEX idx_target_tenant (target_tenant_id),
    FOREIGN KEY (resource_id) REFERENCES tenant_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源共享表';
```

**1.3.3 音频处理任务表 (audio_process_task)**
```sql
CREATE TABLE audio_process_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_no VARCHAR(64) NOT NULL UNIQUE COMMENT '任务编号',
    resource_id BIGINT COMMENT '关联资源ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    creator_id BIGINT NOT NULL COMMENT '创建者',
    task_type VARCHAR(30) NOT NULL COMMENT '任务类型: transcription/tts/analysis/voice_train/voice_clone/merge',
    task_status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
    task_config JSON COMMENT '任务配置参数',
    task_result JSON COMMENT '任务结果',
    error_msg TEXT COMMENT '错误信息',
    priority INT DEFAULT 2 COMMENT '优先级: 0实时 1高 2普通 3低',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration INT COMMENT '耗时(秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_task_status (task_status),
    INDEX idx_task_type (task_type),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音频处理任务表';
```

**1.3.4 声音模型表 (voice_model)**
```sql
CREATE TABLE voice_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '模型ID',
    model_no VARCHAR(64) NOT NULL UNIQUE COMMENT '模型编号',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    creator_id BIGINT NOT NULL COMMENT '创建者',
    source_resource_id BIGINT COMMENT '来源音频资源ID',
    model_type VARCHAR(20) NOT NULL COMMENT '模型类型: so-vits-svc/openvoice',
    model_path VARCHAR(500) COMMENT '模型存储路径',
    model_config JSON COMMENT '模型配置参数',
    sample_audio_url VARCHAR(500) COMMENT '示例音频URL',
    train_duration INT COMMENT '训练时长(分钟)',
    train_status VARCHAR(20) DEFAULT 'pending' COMMENT '训练状态: pending/training/success/failed',
    train_progress INT DEFAULT 0 COMMENT '训练进度(0-100)',
    train_task_id BIGINT COMMENT '训练任务ID',
    quality_score DECIMAL(3,2) COMMENT '质量评分(MOS分1-5)',
    similarity_score DECIMAL(3,2) COMMENT '相似度评分',
    use_scene VARCHAR(50) COMMENT '推荐使用场景',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认模型',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_creator_id (creator_id),
    INDEX idx_train_status (train_status),
    FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id),
    FOREIGN KEY (source_resource_id) REFERENCES tenant_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='声音模型表';
```

**1.3.5 音频项目表 (audio_project)**
```sql
CREATE TABLE audio_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
    project_no VARCHAR(64) NOT NULL UNIQUE COMMENT '项目编号',
    project_name VARCHAR(200) NOT NULL COMMENT '项目名称',
    project_desc TEXT COMMENT '项目描述',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    creator_id BIGINT NOT NULL COMMENT '创建者',
    project_status TINYINT DEFAULT 1 COMMENT '状态: 0删除 1正常 2已完成 3已归档',
    output_resource_id BIGINT COMMENT '输出资源ID',
    output_duration INT COMMENT '输出音频时长(秒)',
    output_format VARCHAR(10) DEFAULT 'mp3' COMMENT '输出格式',
    merge_config JSON COMMENT '合并配置',
    current_version INT DEFAULT 1 COMMENT '当前版本号',
    is_recovered TINYINT DEFAULT 0 COMMENT '是否已恢复: 0否 1是',
    recover_from_id BIGINT COMMENT '恢复自哪个项目',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_creator_id (creator_id),
    INDEX idx_project_status (project_status),
    FOREIGN KEY (tenant_id) REFERENCES sys_tenant(id),
    FOREIGN KEY (output_resource_id) REFERENCES tenant_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音频项目表';
```

**1.3.6 项目音频明细表 (project_audio_detail)**
```sql
CREATE TABLE project_audio_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '明细ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    source_tenant_id BIGINT NOT NULL COMMENT '来源租户ID',
    audio_name VARCHAR(200) COMMENT '音频名称',
    audio_duration INT COMMENT '音频时长(秒)',
    sequence_no INT NOT NULL COMMENT '序号',
    start_time DECIMAL(10,2) COMMENT '截取开始时间(秒)',
    end_time DECIMAL(10,2) COMMENT '截取结束时间(秒)',
    volume_adjust DECIMAL(4,2) DEFAULT 1.0 COMMENT '音量调整系数',
    fade_in DECIMAL(5,2) DEFAULT 0 COMMENT '淡入时长(秒)',
    fade_out DECIMAL(5,2) DEFAULT 0 COMMENT '淡出时长(秒)',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_resource_id (resource_id),
    FOREIGN KEY (project_id) REFERENCES audio_project(id),
    FOREIGN KEY (resource_id) REFERENCES tenant_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目音频明细表';
```

**1.3.7 项目版本表 (project_version) 【新增】**
```sql
CREATE TABLE project_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '版本ID',
    project_id BIGINT NOT NULL COMMENT '项目ID',
    version_no INT NOT NULL COMMENT '版本号',
    snapshot JSON NOT NULL COMMENT '项目快照',
    operator_id BIGINT COMMENT '操作人',
    operation_type VARCHAR(20) COMMENT '操作类型: edit/merge/recover',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_version (project_id, version_no),
    FOREIGN KEY (project_id) REFERENCES audio_project(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目版本表';
```

**1.3.8 资源访问日志表 (resource_access_log) 【新增】**
```sql
CREATE TABLE resource_access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    operator_tenant_id BIGINT NOT NULL COMMENT '操作租户ID',
    operator_user_id BIGINT NOT NULL COMMENT '操作用户ID',
    access_type VARCHAR(20) NOT NULL COMMENT '访问类型: view/download/merge',
    access_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    result TINYINT DEFAULT 1 COMMENT '结果: 1成功 0失败',
    fail_reason VARCHAR(200) COMMENT '失败原因',
    INDEX idx_resource_id (resource_id),
    INDEX idx_operator_tenant (operator_tenant_id),
    INDEX idx_access_time (access_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源访问日志表';
```

#### 1.4 扩展POM.xml依赖

```xml
<!-- 在 properties 中添加版本定义 -->
<properties>
    <tarsos.dsp.version>2.5</tarsos.dsp.version>
    <ffmpeg.version>6.1.0-1.5.9</ffmpeg.version>
    <javacpp.version>1.5.9</javacpp.version>
    <whisper.cpp.version>1.5.5</whisper.cpp.version>
    <coqui-tts.version>0.13.3</coqui-tts.version>
    <openvoice.version>1.0.0</openvoice.version>
    <so-vits-svc.version>4.0</so-vits-svc.version>
    <jlayer.version>1.0.1</jlayer.version>
    <javazoom.version>1.1.2</javazoom.version>
</properties>

<!-- 在 dependencyManagement 中添加依赖管理 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>be.tarsos.dsp</groupId>
            <artifactId>core</artifactId>
            <version>${tarsos.dsp.version}</version>
        </dependency>
        <dependency>
            <groupId>org.bytedeco</groupId>
            <artifactId>ffmpeg-platform</artifactId>
            <version>${ffmpeg.version}</version>
        </dependency>
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>4.12.0</version>
        </dependency>
        <dependency>
            <groupId>com.googlecode.soundlibs</groupId>
            <artifactId>mp3spi</artifactId>
            <version>1.9.5.4</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 二、新增功能详细设计

#### 2.1 声音训练与克隆功能

**2.1.1 功能概述**
租户通过上传音频文件训练个性化声音模型，并使用模型将文本合成为自己的声音。

**2.1.2 声音训练流程**
```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  用户   │     │  前端   │     │  后端   │     │AI网关   │     │训练服务 │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │               │
     │ 1.选择训练音频 │               │               │               │
     ├──────────────►│               │               │               │
     │               │ 2.创建训练任务 │               │               │
     │               ├──────────────►│               │               │
     │               │               │ 3.保存模型记录│               │
     │               │               │ (voice_model) │               │
     │               │               │ 4.调用训练API │               │
     │               │               ├──────────────►│               │
     │               │               │               │ 5.路由训练服务│
     │               │               │               ├──────────────►│
     │               │               │               │               │ 6.训练
     │               │               │               │               │───┐
     │               │               │               │               │   │
     │               │               │               │               │◄──┘
     │               │               │               │ 7.返回结果   │
     │               │               │               │◄──────────────┤
     │               │               │ 8.更新状态+质量评分│           │
     │               │               │◄──────────────┤               │
     │ 9.查询训练状态 │               │               │               │
     ├──────────────►│ 10.查询       │               │               │
     │               ├──────────────►│               │               │
     │               │               │ 11.返回状态   │               │
     │               │◄──────────────┤               │               │
     │ 12.显示完成   │               │               │               │
     │◄──────────────┤               │               │               │
```

**2.1.3 声音克隆流程**
```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  用户   │     │  前端   │     │  后端   │     │AI网关   │     │克隆服务 │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │               │
     │ 1.选择文本+   │               │               │               │
     │   选择模型    │               │               │               │
     ├──────────────►│               │               │               │
     │               │ 2.创建克隆任务│               │               │
     │               ├──────────────►│               │               │
     │               │               │ 3.校验配额    │               │
     │               │               │ 4.调用克隆API │               │
     │               │               ├──────────────►│               │
     │               │               │               │ 5.路由克隆服务│
     │               │               │               ├──────────────►│
     │               │               │               │               │ 6.推理合成
     │               │               │               │               │───┐
     │               │               │               │               │   │
     │               │               │               │               │◄──┘
     │               │               │               │ 7.返回音频   │
     │               │               │               │◄──────────────┤
     │               │               │ 8.保存资源    │               │
     │               │               │ 9.更新配额    │               │
     │               │               │◄──────────────┤               │
     │ 10.返回结果   │               │               │               │
     │◄──────────────┤               │               │               │
```

**2.1.4 训练要求与参数**

| 参数 | 说明 | 推荐值 | 备注 |
|------|------|--------|------|
| 音频时长 | 训练所需音频总时长 | 10-30分钟 | 越长效果越好 |
| 音频质量 | 采样率/位深度 | 44.1kHz/16bit | 清晰无噪音 |
| 音频格式 | 支持格式 | WAV/MP3 | 自动转换 |
| 语音内容 | 内容多样性 | 覆盖各类发音 | 包含不同情感更好 |
| 训练轮数 | 迭代次数 | 5000-20000 | 根据数据量调整 |
| 训练耗时 | 预计时间 | 30-120分钟 | 依赖GPU性能 |

#### 2.2 项目管理与音频合并功能

**2.2.1 功能概述**
租户创建音频项目，将多个音频文件按序号合并，支持片段截取、音量调整、淡入淡出等编辑功能。

**2.2.2 操作撤销/重做实现**
```java
public class EditOperationStack {
    private Stack<EditAction> undoStack = new Stack<>();
    private Stack<EditAction> redoStack = new Stack<>();
    private final ProjectService projectService;
    
    public void execute(EditAction action) {
        action.execute();
        undoStack.push(action);
        redoStack.clear();
        
        // 保存版本快照
        projectService.saveVersion(action.getProjectId());
    }
    
    public void undo() {
        if (!undoStack.isEmpty()) {
            EditAction action = undoStack.pop();
            action.undo();
            redoStack.push(action);
        }
    }
    
    public void redo() {
        if (!redoStack.isEmpty()) {
            EditAction action = redoStack.pop();
            action.execute();
            undoStack.push(action);
        }
    }
}
```

**2.2.3 音频合并规则**

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| 顺序合并 | 按序号从小到大顺序拼接 | 有声书、播客 |
| 交叉混合 | 音频片段重叠混合 | 背景音乐+人声 |
| 条件合并 | 根据音频特征智能拼接 | 对话场景 |

**编辑参数示例**
```json
{
  "mergeConfig": {
    "global": {
      "outputFormat": "mp3",
      "bitrate": "192k",
      "sampleRate": 44100,
      "crossFade": 0.5
    },
    "tracks": [
      {
        "resourceId": 1001,
        "sequence": 1,
        "startTime": 10.5,
        "endTime": 25.3,
        "volume": 1.2,
        "fadeIn": 0.3,
        "fadeOut": 0.5,
        "speed": 1.0
      }
    ]
  }
}
```

**2.2.4 项目状态流转**

| 状态码 | 状态名称 | 说明 | 可操作 |
|--------|----------|------|--------|
| 1 | 正常 | 项目可编辑 | 编辑、合并、删除 |
| 2 | 已完成 | 已生成输出文件 | 预览、下载、编辑 |
| 3 | 已归档 | 只读状态 | 预览、下载、恢复 |
| 0 | 已删除 | 软删除 | 恢复、永久删除 |

### 三、接口设计

#### 3.1 声音训练与克隆接口

**3.1.1 创建训练任务**
```http
POST /api/voice/train/create

请求体：
{
    "modelName": "我的声音模型",
    "sourceResourceIds": [1001, 1002, 1003],
    "modelType": "so-vits-svc",
    "trainConfig": {
        "epochs": 10000,
        "batchSize": 8,
        "learningRate": 0.0001
    }
}

响应：
{
    "code": 200,
    "data": {
        "modelId": 5001,
        "modelNo": "VM20260304120001",
        "modelName": "我的声音模型",
        "trainStatus": "pending",
        "estimatedTime": 3600
    }
}
```

**3.1.2 查询训练状态**
```http
GET /api/voice/train/status/{modelId}

响应：
{
    "code": 200,
    "data": {
        "modelId": 5001,
        "trainStatus": "training",
        "progress": 45,
        "currentEpoch": 4500,
        "totalEpochs": 10000,
        "loss": 0.0234,
        "eta": 1800,
        "qualityScore": 4.2,
        "similarityScore": 0.85,
        "useScene": "有声书",
        "sampleAudioUrl": "https://oss.xxx.com/sample/5001_preview.wav"
    }
}
```

**3.1.3 声音克隆合成**
```http
POST /api/voice/clone/synthesize

请求体：
{
    "modelId": 5001,
    "text": "欢迎使用VoiceText Weaver平台",
    "config": {
        "speed": 1.0,
        "pitch": 1.0,
        "format": "mp3"
    }
}

响应：
{
    "code": 200,
    "data": {
        "taskId": 6001,
        "taskStatus": "pending",
        "estimatedTime": 5
    }
}

GET /api/voice/clone/result/{taskId}

响应：
{
    "code": 200,
    "data": {
        "taskId": 6001,
        "taskStatus": "success",
        "result": {
            "resourceId": 2001,
            "resourceName": "克隆语音_20260304.mp3",
            "duration": 8.5,
            "ossUrl": "https://oss.xxx.com/tenant_1/clone/2001.mp3"
        }
    }
}
```

#### 3.2 项目管理接口

**3.2.1 创建项目**
```http
POST /api/project/create

请求体：
{
    "projectName": "有声书第一章",
    "projectDesc": "制作我的第一本有声书",
    "outputFormat": "mp3"
}

响应：
{
    "code": 200,
    "data": {
        "projectId": 1001,
        "projectNo": "PJ202603041210001",
        "projectName": "有声书第一章",
        "projectStatus": 1
    }
}
```

**3.2.2 添加音频到项目**
```http
POST /api/project/{projectId}/add-audio

请求体：
{
    "items": [
        {
            "resourceId": 1001,
            "sourceTenantId": 1,
            "sequenceNo": 1,
            "startTime": 0,
            "endTime": 120.5,
            "volumeAdjust": 1.2,
            "fadeIn": 0.3,
            "fadeOut": 0.5
        }
    ]
}

响应：
{
    "code": 200,
    "data": {
        "addedCount": 1,
        "totalDuration": 120.5,
        "items": [{
            "detailId": 5001,
            "audioName": "开场白录音.mp3",
            "sequenceNo": 1,
            "duration": 120.5
        }]
    }
}
```

**3.2.3 执行合并**
```http
POST /api/project/{projectId}/merge

请求体：
{
    "outputName": "有声书第一章_最终版.mp3",
    "mergeConfig": {
        "crossFade": 0.8,
        "normalize": true
    }
}

响应：
{
    "code": 200,
    "data": {
        "taskId": 7001,
        "taskStatus": "pending",
        "estimatedTime": 30
    }
}

GET /api/project/merge/result/{taskId}

响应：
{
    "code": 200,
    "data": {
        "taskId": 7001,
        "taskStatus": "success",
        "result": {
            "resourceId": 3001,
            "resourceName": "有声书第一章_最终版.mp3",
            "duration": 185.3,
            "ossUrl": "https://oss.xxx.com/tenant_1/project/3001.mp3"
        }
    }
}
```

**3.2.4 操作撤销**
```http
POST /api/project/{projectId}/undo

响应：
{
    "code": 200,
    "msg": "撤销成功",
    "data": {
        "canUndo": true,
        "canRedo": true
    }
}
```

**3.2.5 项目版本列表**
```http
GET /api/project/{projectId}/versions

响应：
{
    "code": 200,
    "data": {
        "versions": [
            {
                "versionNo": 3,
                "operationType": "merge",
                "operatorName": "张三",
                "createTime": "2026-03-04 12:20:00"
            },
            {
                "versionNo": 2,
                "operationType": "edit",
                "operatorName": "张三",
                "createTime": "2026-03-04 12:15:00"
            }
        ]
    }
}
```

### 四、权限与配额扩展

#### 4.1 新增权限点

| 权限标识 | 说明 | 适用角色 |
|---------|------|----------|
| voice:train:create | 创建声音训练任务 | 租户管理员/普通用户 |
| voice:model:view | 查看声音模型 | 租户管理员/普通用户 |
| voice:clone:use | 使用声音克隆 | 租户管理员/普通用户 |
| project:create | 创建项目 | 租户管理员/普通用户 |
| project:merge | 执行合并操作 | 租户管理员/普通用户 |
| project:recover | 恢复已删除项目 | 租户管理员 |
| resource:share:merge | 共享资源允许合并 | 资源创建者 |

#### 4.2 租户配额扩展

```sql
ALTER TABLE sys_tenant ADD COLUMN voice_model_quota INT DEFAULT 5 COMMENT '声音模型数量配额';
ALTER TABLE sys_tenant ADD COLUMN voice_model_used INT DEFAULT 0 COMMENT '已用模型数量';
ALTER TABLE sys_tenant ADD COLUMN project_quota INT DEFAULT 50 COMMENT '项目数量配额';
ALTER TABLE sys_tenant ADD COLUMN project_used INT DEFAULT 0 COMMENT '已用项目数量';
ALTER TABLE sys_tenant ADD COLUMN clone_quota INT DEFAULT 1000 COMMENT '月克隆次数配额';
ALTER TABLE sys_tenant ADD COLUMN clone_used INT DEFAULT 0 COMMENT '本月已用克隆次数';
ALTER TABLE sys_tenant ADD COLUMN merge_quota INT DEFAULT 500 COMMENT '月合并次数配额';
ALTER TABLE sys_tenant ADD COLUMN merge_used INT DEFAULT 0 COMMENT '本月已用合并次数';
```

### 五、部署与运维

#### 5.1 新增服务部署

```yaml
services:
  ai-gateway:
    image: ai-gateway:latest
    container_name: ai-gateway
    ports:
      - "9001:9001"
    environment:
      - WHISPER_SERVICE_URL=http://whisper:9002
      - COQUI_SERVICE_URL=http://coqui:9003
      - VOICE_CLONE_URL=http://voice-clone:9004
    restart: always
    networks:
      - voicetext-network

  voice-clone-service:
    image: voice-clone-service:latest
    container_name: voice-clone
    ports:
      - "9004:9004"
    volumes:
      - ./data/voice-clone/models:/app/models
    environment:
      - CUDA_VISIBLE_DEVICES=0
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    restart: always
    networks:
      - voicetext-network
```

#### 5.2 新增监控指标

| 监控指标 | 说明 | 告警阈值 |
|---------|------|----------|
| voice.train.pending.count | 待训练任务数 | >5 |
| voice.train.gpu.utilization | GPU使用率 | >90% 持续10分钟 |
| voice.clone.qps | 克隆服务QPS | >20 |
| project.merge.duration | 合并任务平均耗时 | >60秒 |
| resource.ref.count.high | 高引用资源数 | >100 |

### 六、实施计划

#### 6.1 分阶段实施

```
阶段一：基础架构 (3周)
├── 创建声音模型表、项目表、版本表
├── 基础项目管理CRUD
├── 资源引用计数实现
├── 基础权限校验
└── 音频合并核心功能

阶段二：声音训练 (3周)
├── AI网关设计与实现
├── 集成声音克隆服务
├── 训练任务管理
├── 模型质量评估(MOS分)
└── 训练进度实时反馈

阶段三：项目增强 (3周)
├── 音频截取编辑功能
├── 跨租户资源引用
├── 预览波形生成(缓存策略)
├── 操作撤销/重做功能
├── 项目版本管理
└── 项目回收站与恢复

阶段四：前端完善 (2周)
├── 项目编辑界面
├── 声音训练界面
├── 音频波形可视化
├── 拖拽排序功能
└── 实时进度展示

阶段五：性能优化 (2周)
├── 流式处理改造
├── 并发训练队列
├── 优先级任务调度
├── 资源配额校验
└── 压力测试

阶段六：上线运维 (2周)
├── 服务编排部署
├── GPU资源监控
├── 灰度发布策略
├── 备份恢复策略
└── 渗透测试
```

#### 6.2 风险评估与应对

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 声音克隆质量不佳 | 中 | 提供音频质量检测、训练指南、质量评分 |
| GPU资源竞争 | 高 | 任务队列、优先级调度、弹性扩容 |
| 合并音频时长过长 | 中 | 流式处理、进度提示、异步合并 |
| 跨租户权限漏洞 | 高 | 全链路校验、操作审计、访问日志 |
| 模型文件泄露 | 高 | 模型加密存储、访问控制、水印技术 |

### 七、总结

#### 7.1 优化点汇总

| 优化项 | 原方案 | 优化后 |
|--------|--------|--------|
| AI服务集成 | 直接集成 | AI网关解耦 |
| 资源引用 | 无引用计数 | 有引用计数保护 |
| 操作体验 | 无撤销功能 | 支持撤销/重做 |
| 版本管理 | 无版本 | 支持版本回溯 |
| 质量评估 | 无评分 | MOS分+相似度评分 |
| 访问审计 | 无日志 | 完整访问日志 |
| 任务调度 | 无优先级 | 优先级队列 |
| 波形生成 | 实时生成 | 多级缓存 |

#### 7.2 核心优势

- **完整闭环**：从声音训练到项目合成，形成完整音频生产链路
- **灵活协作**：支持自有资源和共享资源混编，打破租户壁垒
- **安全可控**：细粒度权限 + 配额管理 + 操作审计 + 引用保护
- **用户体验**：可视化编辑 + 撤销重做 + 实时反馈 + 版本管理

#### 7.3 后续规划

- **AI智能剪辑**：根据内容自动推荐剪辑点
- **批量项目处理**：模板化项目批量生成
- **声纹识别集成**：自动识别说话人并分离音轨
- **云端协作**：多人实时协作编辑项目
- **效果增强**：AI降噪、音质修复、情感迁移

---
**核心数据实体**: sys_tenant(扩展配额) / tenant_resource / resource_share / voice_model / audio_project / project_audio_detail / audio_process_task

**后续规划**: AI智能剪辑 · 批量项目模板 · 声纹识别分离 · 云端实时协作 · 情感迁移合成
**文档版本**: 2.1.0  
**最后更新**: 2026-03-04  
**文档状态**: 已评审待实施