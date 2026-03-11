# VoiceText Weaver 项目详细设计

## 一、功能描述

### 1.1 核心业务概述
将图片中的语音处理工作台整合为项目的核心功能模块，提供一站式的语音合成、克隆、识别与设计服务。工作主界面作为租户统一的操作入口，集成四大核心功能。

### 1.2 四大核心功能

| 功能模块 | 业务描述 | 核心操作 |
|---------|---------|---------|
| **Clone（声音克隆）** | 基于3秒参考音频克隆任何人的声音，支持声音模型训练与管理 | 上传参考音频→转录校对→保存声模→输入文本生成语音 |
| **Design（声音设计）** | 通过自然语言描述创建理想声音，支持年龄、性别、情绪等特征自定义 | 输入声音描述→选择基础风格→生成试听→调整参数 |
| **TTS（文本转语音）** | 使用预设或克隆的声音模型将文本转换为自然语音 | 选择声音→输入文本→调整语速/音调→生成语音 |
| **STT（语音转文本）** | 高精度语音识别，支持多语言自动检测与字幕导出 | 上传音频→自动转录→校对编辑→导出SRT/JSON |

---

## 二、架构设计

### 2.1 整体架构分层

```
┌─────────────────────────────────────────────────────────────┐
│                    展现层（Vue3 + Element Plus）             │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐    │
│  │工作主界面│ │Clone界面│ │Design界面│ │TTS界面 │ │STT界面 │    │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘    │
├─────────────────────────────────────────────────────────────┤
│                    API网关层（Sa-Token认证）                  │
│             租户隔离 · 权限校验 · 流量控制 · 服务路由             │
├─────────────────────────────────────────────────────────────┤
│                     业务逻辑层                                │
│  ┌────────────┐ ┌────────────┐ ┌────────────────────┐       │
│  │项目管理服务 │ │音频处理服务 │ │租户资源池管理服务 │       │
│  ├────────────┤ ├────────────┤ ├────────────────────┤       │
│  │序列管理    │ │TTS合成服务 │ │声模配额管理        │       │
│  │音频合并    │ │STT识别服务 │ │存储空间管理        │       │
│  │版本控制    │ │声音克隆服务 │ │并发任务控制        │       │
│  └────────────┘ └────────────┘ └────────────────────┘       │
├─────────────────────────────────────────────────────────────┤
│                    AI服务网关层                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │Whisper   │ │Coqui TTS │ │OpenVoice │ │So-VITS   │       │
│  │适配器    │ │适配器    │ │适配器    │ │适配器    │       │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
├─────────────────────────────────────────────────────────────┤
│                     基础设施层                                │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌────────────┐       │
│  │MySQL │ │Redis │ │MinIO │ │Neo4j │ │SnailJob    │       │
│  │(多租)│ │(锁/队列)│ │(音频存储)│ │(声纹图谱)│ │(分布式任务)│       │
│  └──────┘ └──────┘ └──────┘ └──────┘ └────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心业务流程

#### 声音克隆流程
```
上传参考音频 → 自动转录校对 → 特征提取 → 模型训练(异步) → 保存至租户声模库
```

#### 文本转语音流程
```
选择声模 → 输入文本 → 任务调度 → 调用TTS服务 → 音频后处理 → 存储至OSS → 返回结果
```

#### 语音转文本流程
```
上传音频 → 格式转换 → 调用Whisper → 后处理校对 → 生成字幕文件 → 返回结果
```

---

## 三、模块设计

### 3.1 模块结构

```
voicetext-weaver-plus/
├── voicetext-admin/                           # 系统启动模块
├── voicetext-common/                           # 通用工具模块
│   ├── voicetext-common-core/                  # 核心工具
│   ├── voicetext-common-redis/                  # Redis缓存
│   ├── voicetext-common-oss/                    # 对象存储
│   ├── voicetext-common-satoken/                 # 权限认证
│   └── voicetext-common-audio/                   # 【新增】音频处理核心
│       ├── src/main/java/org/dromara/audio/
│       │   ├── core/                          # 音频核心处理
│       │   │   ├── AudioProcessor.java        # 音频格式转换
│       │   │   ├── TarsosAudioProcessor.java  # 音频特征分析
│       │   │   ├── FFmpegAudioProcessor.java  # FFmpeg封装
│       │   │   └── AudioFeatureExtractor.java # 声纹特征提取
│       │   ├── whisper/                        # Whisper语音识别
│       │   │   ├── WhisperService.java        # 识别服务接口
│       │   │   └── WhisperModelManager.java   # 模型管理
│       │   ├── tts/                           # TTS语音合成
│       │   │   ├── TtsService.java            # 合成服务接口
│       │   │   └── TtsTaskConsumer.java       # 任务消费者
│       │   ├── voiceclone/                     # 声音克隆模块
│       │   │   ├── VoiceTrainService.java     # 训练服务
│       │   │   ├── VoiceCloneService.java     # 克隆服务
│       │   │   ├── VoiceModelManager.java     # 模型管理
│       │   │   └── dto/                        # 数据传输对象
│       │   └── project/                        # 项目管理模块
│       │       ├── AudioProjectService.java   # 项目服务
│       │       ├── AudioMergeService.java     # 音频合并
│       │       └── SequenceManager.java       # 序列管理
│       └── pom.xml
├── voicetext-modules/                           # 业务模块
│   ├── voicetext-system/                        # 系统管理
│   │   ├── user/                               # 用户管理
│   │   ├── dept/                               # 部门管理
│   │   ├── post/                               # 岗位管理
│   │   ├── menu/                               # 菜单权限
│   │   └── role/                               # 角色管理
│   ├── voicetext-workflow/                      # 工作流
│   ├── voicetext-tenant-resource/               # 【新增】租户资源池管理
│   │   ├── src/main/java/org/dromara/tenantresource/
│   │   │   ├── controller/
│   │   │   │   └── TenantResourceController.java   # 资源配额接口
│   │   │   ├── service/
│   │   │   │   ├── ITenantQuotaService.java       # 配额服务
│   │   │   │   └── ITenantModelService.java       # 声模管理
│   │   │   ├── mapper/
│   │   │   │   ├── TenantQuotaMapper.java         # 配额表
│   │   │   │   └── VoiceModelMapper.java          # 声模表
│   │   │   └── domain/
│   │   │       ├── TenantQuota.java               # 租户配额实体
│   │   │       └── VoiceModel.java                 # 声音模型实体
│   │   └── pom.xml
│   ├── voicetext-ai-gateway/                      # 【新增】AI服务网关
│   │   ├── src/main/java/org/dromara/aigateway/
│   │   │   ├── api/                            # 统一AI接口
│   │   │   │   ├── TtsApi.java                 # TTS接口定义
│   │   │   │   ├── SttApi.java                 # STT接口定义
│   │   │   │   └── CloneApi.java                # 克隆接口定义
│   │   │   ├── adapter/                         # 服务适配器
│   │   │   │   ├── WhisperAdapter.java          # Whisper适配
│   │   │   │   ├── CoquiTtsAdapter.java         # Coqui TTS适配
│   │   │   │   └── VoiceCloneAdapter.java       # 克隆服务适配
│   │   │   ├── router/                          # 服务路由
│   │   │   │   └── AiServiceRouter.java         # 路由策略
│   │   │   └── fallback/                         # 降级处理
│   │   │       └── AiServiceFallback.java       # 服务降级
│   │   └── pom.xml
│   ├── voicetext-ai-assistant/                   # AI助手模块
│   └── voicetext-knowledge-graph/                # 知识图谱
└── voicetext-extend/                             # 扩展组件
    └── voicetext-xxl-job/                        # 定时任务
```

---

## 四、数据库设计

### 4.1 核心表结构

```sql
-- 声音模型表
CREATE TABLE voice_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type VARCHAR(20) NOT NULL COMMENT '类型：clone/design/system',
    ref_audio_url VARCHAR(500) COMMENT '参考音频地址',
    ref_text TEXT COMMENT '参考文本',
    model_path VARCHAR(500) COMMENT '模型存储路径',
    preview_audio_url VARCHAR(500) COMMENT '预览音频',
    voice_params JSON COMMENT '声音参数(年龄/性别/情绪等)',
    status TINYINT DEFAULT 0 COMMENT '状态：0训练中 1可用 2失败',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_status (tenant_id, status)
);

-- 音频项目表
CREATE TABLE audio_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    project_type VARCHAR(20) NOT NULL COMMENT 'clone/design/tts/stt',
    source_audio_url VARCHAR(500) COMMENT '源音频',
    source_text TEXT COMMENT '源文本',
    target_audio_url VARCHAR(500) COMMENT '生成音频',
    target_text TEXT COMMENT '识别文本',
    voice_model_id BIGINT COMMENT '使用的声模ID',
    task_id VARCHAR(64) COMMENT '任务ID',
    duration FLOAT COMMENT '音频时长(秒)',
    file_size INT COMMENT '文件大小(KB)',
    render_time FLOAT COMMENT '渲染时间(秒)',
    status TINYINT DEFAULT 0 COMMENT '0处理中 1成功 2失败',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_time (tenant_id, create_time)
);

-- 租户资源配额表
CREATE TABLE tenant_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT UNIQUE NOT NULL,
    max_model_count INT DEFAULT 50 COMMENT '最大声模数量',
    max_storage_mb INT DEFAULT 10240 COMMENT '最大存储空间(MB)',
    concurrent_tasks INT DEFAULT 5 COMMENT '并发任务数',
    used_model_count INT DEFAULT 0,
    used_storage_mb INT DEFAULT 0,
    current_tasks INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 任务记录表
CREATE TABLE task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    task_type VARCHAR(20) NOT NULL COMMENT 'clone/tts/stt',
    task_status VARCHAR(20) NOT NULL COMMENT 'pending/running/success/failed',
    task_params JSON COMMENT '任务参数',
    result_url VARCHAR(500) COMMENT '结果地址',
    error_msg TEXT COMMENT '错误信息',
    start_time DATETIME,
    end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_status (tenant_id, task_status)
);
```

---

## 五、UI设计

### 5.1 工作主界面布局

```
┌─────────────────────────────────────────────────────────────┐
│ [Logo] VoiceText Weaver                           [用户头像] │
├─────────────────────────────────────────────────────────────┤
│ ┌───────────┐ ┌─────────────────────────────────────────┐ │
│ │  功能区   │ │             工作区（动态切换）             │ │
│ │ ┌───────┐ │ │ ┌─────────────────────────────────────┐ │ │
│ │ │Clone  │ │ │ │ 当前功能：TTS / Clone / Design / STT │ │ │
│ │ ├───────┤ │ │ ├─────────────────────────────────────┤ │ │
│ │ │Design │ │ │ │ 声音选择器 / 文本输入框 / 参数调节    │ │ │
│ │ ├───────┤ │ │ │                                      │ │ │
│ │ │TTS    │ │ │ │ 音频预览波形（Wavesurfer）           │ │ │
│ │ ├───────┤ │ │ │ ┌─────────────────────────────────┐ │ │ │
│ │ │STT    │ │ │ │ │ ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲             │ │ │ │
│ │ └───────┘ │ │ │ └─────────────────────────────────┘ │ │ │
│ │           │ │ │                                      │ │ │
│ │ 系统信息  │ │ │ [生成按钮]     [播放]  [下载] [分享]  │ │ │
│ │ GPU: RTX  │ │ │                                      │ │ │
│ │ CUDA: 启用│ │ │ 渲染时间: 20.39s  文件大小: 389.0KB   │ │ │
│ └───────────┘ └─────────────────────────────────────────┘ │ │
├─────────────────────────────────────────────────────────────┤
│                          历史记录                            │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ [Clone] 特朗普声音克隆  just now   8.2s   385.2KB [⏵] │ │
│ │ [Design] 活力男声设计    1h ago    9.3s   434.0KB [⏵] │ │
│ │ [TTS]   项目介绍生成     4m ago    10.5s  494.0KB [⏵] │ │
│ │ [STT]   会议录音转录     5m ago    8.0s   374.0KB [⏵] │ │
│ │                                    [Clear All]         │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 各功能界面详细设计

#### Clone界面
```
┌─────────────────────────────────────────────────┐
│ 声音克隆                                         │
├─────────────────────────────────────────────────┤
│ 参考音频上传区                                    │
│ ┌─────────────────────────────────────────────┐ │
│ │ [📁] 拖拽或点击上传音频文件                    │ │
│ │ trump_sample.mp3  (0:04 / 0:04)   [播放]    │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ 转录文本校对                                      │
│ ┌─────────────────────────────────────────────┐ │
│ │ Thank you, class, very much. It's a ...     │ │
│ │ [编辑] [确认]                                 │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ 模型名称：特朗普声音克隆            [保存声模]     │
│                                                  │
│ 文本生成区                                        │
│ ┌─────────────────────────────────────────────┐ │
│ │ Let me tell you about Voice Creator Pro...  │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ [生成语音]                       语言: English   │
│                                 质量: [⚫─────]  │
│                                                  │
│ 输出: 0:09 / 0:09  [⏵]  [⬇]                      │
│ 渲染时间: 29.26s  文件大小: 434.3KB                │
└─────────────────────────────────────────────────┘
```

#### Design界面
```
┌─────────────────────────────────────────────────┐
│ 声音设计                                         │
├─────────────────────────────────────────────────┤
│ 声音描述输入                                      │
│ ┌─────────────────────────────────────────────┐ │
│ │ An energetic, naturally expressive male...  │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ 快速预设                                         │
│ [活力男声] [温柔女声] [低沉旁白] [动画配音]       │
│                                                  │
│ 精细调节                                         │
│ 年龄: 20 ━╋━━━━━━━━━┫ 35                        │
│ 音调: 低 ━╋━━━━━━━━━┫ 高                        │
│ 语速: 慢 ━╋━━━━━━━━━┫ 快                        │
│ 音量: 小 ━╋━━━━━━━━━┫ 大                        │
│                                                  │
│ 文本测试区                                        │
│ ┌─────────────────────────────────────────────┐ │
│ │ Watch this I'm gonna type this sentence...  │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ [生成语音]   [试听]  [保存为声模]                  │
│                                                  │
│ 输出: 0:08 / 0:08  [⏵]                           │
│ 渲染时间: 20.39s  文件大小: 389.0KB                │
└─────────────────────────────────────────────────┘
```

#### TTS界面
```
┌─────────────────────────────────────────────────┐
│ 文本转语音                                       │
├─────────────────────────────────────────────────┤
│ 选择声音                                         │
│ ┌─────────────────────────────────────────────┐ │
│ │ ▼ Aiden - Sunny American male voice         │ │
│ │   Sohee - Korean female voice               │ │
│ │   特朗普克隆 - 定制声音                        │ │
│ │   活力男声设计 - 我的设计                       │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ 文本输入                                          │
│ ┌─────────────────────────────────────────────┐ │
│ │ The most human-sounding voice AI. Clone    │ │
│ │ any voice with 3 seconds of audio, or      │ │
│ │ describe your perfect voice and bring...   │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ 语音指令（可选）                                   │
│ ┌─────────────────────────────────────────────┐ │
│ │ Speak with excitement and energy            │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ [生成语音]                       语言: English   │
│                                 质量: [⚫─────]  │
│                                                  │
│ 输出: 0:08 / 0:08  [⏵]  [⬇] [分享]               │
│ 渲染时间: 24.42s  文件大小: 385.2KB                │
└─────────────────────────────────────────────────┘
```

#### STT界面
```
┌─────────────────────────────────────────────────┐
│ 语音转文本                                       │
├─────────────────────────────────────────────────┤
│ 音频源                                           │
│ ┌─────────────────────────────────────────────┐ │
│ │ [📁] 上传音频  [🎤] 录制                       │ │
│ │                                              │ │
│ │ audio_sample.mp3  (0:09 / 0:09)  [⏵] [波形] │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ 语言自动检测: English                             │
│                                                  │
│ 转录结果                                          │
│ ┌─────────────────────────────────────────────┐ │
│ │ The most human-sounding voice AI. Clone     │ │
│ │ any voice with three seconds of audio, or   │ │
│ │ describe your perfect voice and bring it    │ │
│ │ to life.                                     │ │
│ │                                              │ │
│ │ [复制文本]  [下载SRT]  [下载JSON]              │ │
│ └─────────────────────────────────────────────┘ │
│                                                  │
│ 处理时间: 8.2s                                    │
└─────────────────────────────────────────────────┘
```

### 5.3 历史记录组件

每条历史记录包含：
- **标识图标**：Clone/Design/TTS/STT 功能标识
- **内容预览**：文本摘要或文件名
- **时间戳**：just now / 1h ago / 36m ago
- **音频信息**：时长 / 文件大小
- **操作按钮**：播放/下载/重新生成
- **底部**：Clear All 清空按钮

---

## 六、技术栈

### 6.1 后端技术栈

| 技术领域 | 核心框架/工具 | 版本 | 说明 |
|---------|--------------|------|------|
| 基础框架 | Spring Boot | 3.5.10 | 原框架已集成 |
| 多租户核心 | Mybatis-Plus 多租户插件 | 3.5.16 | 租户数据隔离 |
| 权限认证 | Sa-Token | 1.44.0 | 租户级权限控制 |
| 音频处理 | TarsosDSP + FFmpeg | 2.5 | 音频分析核心 |
| 语音识别 | Whisper(Java封装) | 20231106 | JNI/HTTP调用 |
| 语音合成 | Coqui TTS | 0.13.3 | 独立服务部署 |
| 声音克隆 | OpenVoice/So-VITS | - | 模型训练服务 |
| 分布式任务 | SnailJob | 1.9.0 | TTS长任务处理 |
| 分布式锁 | Lock4j + Redisson | 2.2.7/3.52.0 | 并发控制 |
| 对象存储 | Minio/AWS S3 | 2.28.22 | 音频文件存储 |
| 缓存 | Redis(Redisson) | 3.52.0 | 会话/队列 |
| 消息队列 | Redis Stream | - | 任务队列 |
| 图数据库 | Neo4j | 5.26.0 | 声纹知识图谱 |

### 6.2 前端技术栈

| 技术领域 | 框架/工具 | 版本 | 说明 |
|---------|----------|------|------|
| 基础框架 | Vue 3 + TypeScript | - | 原框架已采用 |
| UI组件 | Element Plus | - | 原框架已采用 |
| 音频可视化 | Wavesurfer.js | 7.8.0 | 波形显示 |
| 音频录制 | RecordRTC | 5.6.2 | 浏览器录音 |
| 音频剪辑 | WaveSurfer Regions | - | 片段选择编辑 |
| 状态管理 | Pinia | - | 全局状态 |
| 路由 | Vue Router | 4.x | 页面路由 |

---

## 七、业务流程图

### 7.1 完整业务流

```
┌─────────────┐
│  进入工作主界面 │
└──────┬──────┘
       ↓
┌─────────────┐
│ 选择功能区Tab │
└──────┬──────┘
       ↓
┌─────────────────────────────────────────────────────────┐
│                        分支判断                          │
├──────────┬──────────┬──────────┬──────────┬────────────┤
│    ↓     │    ↓     │    ↓     │    ↓     │     ↓       │
│ Clone流程 │Design流程│ TTS流程  │ STT流程  │ 历史记录    │
├──────────┼──────────┼──────────┼──────────┼────────────┤
│1.上传音频 │1.输入描述 │1.选择声模 │1.上传音频 │点击历史条目 │
│2.转录校对 │2.调节参数 │2.输入文本 │2.自动识别 │→加载至工作区│
│3.保存声模 │3.测试生成 │3.指令优化 │3.校对结果 │重新生成/下载│
│4.输入文本 │4.保存设计 │4.生成语音 │4.导出字幕 │            │
│5.生成语音 │         │         │         │            │
└──────────┴──────────┴──────────┴──────────┴────────────┘
       ↓                        ↓                        ↓
┌─────────────────────────────────────────────────────────┐
│                   生成结果处理                           │
├─────────────────────────────────────────────────────────┤
│ 1. 波形预览显示                                           │
│ 2. 渲染时间/文件大小统计                                   │
│ 3. 自动保存至历史记录                                      │
│ 4. 播放/下载/分享操作                                      │
└─────────────────────────────────────────────────────────┘
```

### 7.2 数据流转

```
租户操作 → 权限校验 → 配额检查 → 任务创建 → 服务调用 → 结果存储 → 历史记录
    ↑          ↑          ↑          ↑          ↑          ↑          ↑
  用户      Sa-Token  资源模块   SnailJob  AI网关    MinIO     MySQL
```

---

## 八、总结

VoiceText Weaver 项目通过将图片中的语音处理工作台系统化、模块化，实现了以下目标：

1. **功能完整还原**：完全复现图片中的 Clone、Design、TTS、STT 四大核心功能
2. **UI精准匹配**：界面布局、操作流程、视觉元素与图片保持一致
3. **多租户集成**：利用原框架的多租户能力，实现租户资源隔离与配额管理
4. **异步任务处理**：通过 SnailJob 处理长耗时任务，保证用户体验
5. **AI服务统一网关**：统一管理各类AI服务，支持服务降级与路由
6. **历史记录追溯**：完整记录所有操作，支持快速复用