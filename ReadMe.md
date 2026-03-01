# VoiceText Weaver 多租户音频处理平台
---

# 一、项目整体架构设计

## 1.1 技术栈整合方案

### 后端技术栈

| 技术领域      | 核心框架/工具                 | 版本           | 说明              |
| --------- | ----------------------- | ------------ | --------------- |
| **基础框架**  | Spring Boot             | 3.5.10       | 原框架已集成          |
| **多租户核心** | Mybatis-Plus 多租户插件      | 3.5.16       | 原框架已深度集成        |
| **权限认证**  | Sa-Token                | 1.44.0       | 原框架已集成，扩展租户级权限  |
| **音频处理**  | TarsosDSP + FFmpeg      | 2.5          | 新增音频分析核心        |
| **语音识别**  | Whisper(Java封装)         | 20231106     | 通过JNI调用或HTTP服务  |
| **语音合成**  | Coqui TTS(HTTP服务)       | 0.13.3       | 独立服务部署          |
| **分布式任务** | SnailJob                | 1.9.0        | 原框架已集成，处理TTS长任务 |
| **分布式锁**  | Lock4j + Redisson       | 2.2.7/3.52.0 | 原框架已集成          |
| **对象存储**  | Minio/AWS S3            | 2.28.22      | 原框架已集成，存储音频文件   |
| **缓存**    | Redis(Redisson)         | 3.52.0       | 原框架已集成          |
| **消息队列**  | Redis Stream/队列         | -            | 利用Redisson分布式队列 |
| **图数据库**  | Neo4j                   | 5.26.0       | 原框架已支持，用于知识图谱   |
| **AI能力**  | Spring AI + LangChain4j | 1.0.2/1.3.0  | 原框架已集成，扩展RAG能力  |

### 前端技术栈

| 技术领域      | 框架/工具              | 版本    | 说明        |
| --------- | ------------------ | ----- | --------- |
| **基础框架**  | Vue 3 + TypeScript | -     | 原框架已采用    |
| **UI组件**  | Element Plus       | -     | 原框架已采用    |
| **音频可视化** | Wavesurfer.js      | 7.8.0 | 新增音频波形显示  |
| **音频录制**  | RecordRTC          | 5.6.2 | 新增浏览器录音功能 |

---

## 1.2 模块架构设计

```
ruoyi-vue-plus/
├── ruoyi-admin/                    # 系统启动模块
├── ruoyi-common/                   # 通用工具模块
│   ├── ruoyi-common-core/          # 核心工具
│   ├── ruoyi-common-redis/         # Redis缓存
│   ├── ruoyi-common-oss/           # 对象存储
│   ├── ruoyi-common-satoken/       # 权限认证
│   └── ruoyi-common-audio/         # 【新增】音频处理核心
│       ├── src/main/java/org/dromara/audio/
│       │   ├── core/                # 音频核心处理
│       │   │   ├── AudioProcessor.java        # 音频处理接口
│       │   │   ├── TarsosAudioProcessor.java  # TarsosDSP实现
│       │   │   ├── FFmpegAudioProcessor.java  # FFmpeg实现
│       │   │   └── AudioFeatureExtractor.java # 音频特征提取
│       │   ├── whisper/             # Whisper语音识别
│       │   │   ├── WhisperService.java        # 识别服务
│       │   │   └── WhisperModelManager.java   # 模型管理
│       │   ├── tts/                  # TTS语音合成
│       │   │   ├── TtsService.java            # 合成服务
│       │   │   └── TtsTaskConsumer.java       # 异步任务消费
│       │   └── dsp/                   # 数字信号处理
│       │       ├── AudioAnalyzer.java         # 音频分析
│       │       └── VoiceActivityDetector.java # 语音活动检测
│       └── pom.xml                    # 新增模块依赖
├── ruoyi-modules/                   # 业务模块
│   ├── ruoyi-system/                # 系统管理(扩展租户资源)
│   ├── ruoyi-workflow/              # 工作流(音频审核流程)
│   ├── ruoyi-tenant-resource/       # 【新增】租户资源池管理
│   │   ├── src/main/java/org/dromara/tenantresource/
│   │   │   ├── controller/           # 资源控制器
│   │   │   │   ├── AudioResourceController.java  # 音频资源API
│   │   │   │   ├── TextResourceController.java   # 文本资源API
│   │   │   │   └── ResourceShareController.java  # 资源共享API
│   │   │   ├── service/               # 业务服务
│   │   │   │   ├── IResourcePoolService.java    # 资源池服务
│   │   │   │   ├── IResourcePermissionService.java # 权限服务
│   │   │   │   └── impl/               # 实现类
│   │   │   ├── mapper/                 # 数据访问
│   │   │   │   ├── TenantResourceMapper.java    # 资源表Mapper
│   │   │   │   └── ResourceShareMapper.java     # 共享表Mapper
│   │   │   ├── domain/                  # 领域模型
│   │   │   │   ├── TenantResource.java         # 资源实体
│   │   │   │   ├── ResourceShare.java          # 共享实体
│   │   │   │   └── vo/                   # 视图对象
│   │   │   └── config/                   # 配置类
│   │   └── pom.xml
│   ├── ruoyi-ai-assistant/          # 【新增】AI助手模块(整合Spring AI)
│   └── ruoyi-knowledge-graph/       # 【新增】知识图谱(Neo4j)
└── ruoyi-extend/                    # 扩展组件
    └── ruoyi-xxl-job/               # 保留原定时任务
```

---

## 1.3 数据库设计（扩展RuoYi-Vue-Plus）

### 1.3.1 租户资源表 (tenant_resource)

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

### 1.3.2 资源共享表 (resource_share)

```sql
CREATE TABLE resource_share (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '共享ID',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    source_tenant_id BIGINT NOT NULL COMMENT '来源租户ID',
    target_tenant_id BIGINT COMMENT '目标租户ID',
    target_user_id BIGINT COMMENT '目标用户ID',
    permission_code VARCHAR(50) NOT NULL COMMENT '权限编码: view/edit/download/delete',
    expire_time DATETIME COMMENT '过期时间',
    share_status TINYINT DEFAULT 1 COMMENT '状态: 0失效 1生效',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT COMMENT '创建者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_resource_id (resource_id),
    INDEX idx_target_tenant (target_tenant_id),
    FOREIGN KEY (resource_id) REFERENCES tenant_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源共享表';
```

### 1.3.3 音频处理任务表 (audio_process_task)

```sql
CREATE TABLE audio_process_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_no VARCHAR(64) NOT NULL UNIQUE COMMENT '任务编号',
    resource_id BIGINT COMMENT '关联资源ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    creator_id BIGINT NOT NULL COMMENT '创建者',
    task_type VARCHAR(30) NOT NULL COMMENT '任务类型: transcription/tts/analysis',
    task_status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/running/success/failed',
    task_config JSON COMMENT '任务配置参数',
    task_result JSON COMMENT '任务结果',
    error_msg TEXT COMMENT '错误信息',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration INT COMMENT '耗时(秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_task_status (task_status),
    INDEX idx_task_type (task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='音频处理任务表';
```

---

## 1.4 扩展POM.xml依赖（新增内容）

在原有 `pom.xml` 中添加以下依赖声明：

```xml
<!-- 在 properties 中添加版本定义 -->
<properties>
    <!-- 原有属性保持不变，新增以下 -->
    
    <!-- 音频处理库 -->
    <tarsos.dsp.version>2.5</tarsos.dsp.version>
    <ffmpeg.version>6.1.0-1.5.9</ffmpeg.version>
    <javacpp.version>1.5.9</javacpp.version>
    
    <!-- 语音识别与合成 -->
    <whisper.cpp.version>1.5.5</whisper.cpp.version>
    <coqui-tts.version>0.13.3</coqui-tts.version>
    
    <!-- 音频可视化 -->
    <jlayer.version>1.0.1</jlayer.version>
    <javazoom.version>1.1.2</javazoom.version>
</properties>

<!-- 在 dependencyManagement 中添加依赖管理 -->
<dependencyManagement>
    <dependencies>
        <!-- 原有依赖保持不变，新增以下 -->
        
        <!-- TarsosDSP 音频分析核心 -->
        <dependency>
            <groupId>be.tarsos.dsp</groupId>
            <artifactId>core</artifactId>
            <version>${tarsos.dsp.version}</version>
        </dependency>
        <dependency>
            <groupId>be.tarsos.dsp</groupId>
            <artifactId>jvm</artifactId>
            <version>${tarsos.dsp.version}</version>
        </dependency>
        
        <!-- JavaCPP FFmpeg 封装 -->
        <dependency>
            <groupId>org.bytedeco</groupId>
            <artifactId>ffmpeg-platform</artifactId>
            <version>${ffmpeg.version}</version>
        </dependency>
        <dependency>
            <groupId>org.bytedeco</groupId>
            <artifactId>javacpp</artifactId>
            <version>${javacpp.version}</version>
        </dependency>
        
        <!-- Java 音频处理 -->
        <dependency>
            <groupId>javazoom</groupId>
            <artifactId>jlayer</artifactId>
            <version>${jlayer.version}</version>
        </dependency>
        <dependency>
            <groupId>com.googlecode.soundlibs</groupId>
            <artifactId>mp3spi</artifactId>
            <version>1.9.5.4</version>
        </dependency>
        
        <!-- 音频特征提取 -->
        <dependency>
            <groupId>com.github.fracpete</groupId>
            <artifactId>audiveris-fftw</artifactId>
            <version>1.3</version>
        </dependency>
        
        <!-- 语音识别HTTP客户端 -->
        <dependency>
            <groupId>com.squareup.okhttp3</groupId>
            <artifactId>okhttp</artifactId>
            <version>4.12.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 新增公共音频模块定义 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-common-audio</artifactId>
    <version>${revision}</version>
</dependency>

<!-- 新增租户资源模块定义 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-tenant-resource</artifactId>
    <version>${revision}</version>
</dependency>

<!-- 新增AI助手模块定义 -->
<dependency>
    <groupId>org.dromara</groupId>
    <artifactId>ruoyi-ai-assistant</artifactId>
    <version>${revision}</version>
</dependency>
```

---

# 二、业务功能设计

## 2.1 多租户资源池业务模型

### 2.1.1 租户-用户-资源关系图

```
┌─────────────────────────────────────────────────────────────┐
│                        平台层                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 租户管理(套餐/配额)                   │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   租户A(科技)    │   │   租户B(教育)    │   │   租户C(媒体)    │
│  ┌───────────┐  │   │  ┌───────────┐  │   │  ┌───────────┐  │
│  │ 配额:100GB│  │   │  │ 配额:500GB│  │   │  │ 配额:1TB  │  │
│  │ 已用:35GB │  │   │  │ 已用:210GB│  │   │  │ 已用:850GB│  │
│  └───────────┘  │   │  └───────────┘  │   │  └───────────┘  │
└─────────────────┘   └─────────────────┘   └─────────────────┘
        │                      │                      │
        ▼                      ▼                      ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   用户管理       │   │   用户管理       │   │   用户管理       │
│  ┌───────────┐  │   │  ┌───────────┐  │   │  ┌───────────┐  │
│  │admin(管理)│  │   │  │admin(管理)│  │   │  │admin(管理)│  │
│  │userA(编辑)│  │   │  │userB(编辑)│  │   │  │userC(只读)│  │
│  │userB(只读)│  │   │  │userC(只读)│  │   │  │userD(编辑)│  │
│  └───────────┘  │   │  └───────────┘  │   │  └───────────┘  │
└─────────────────┘   └─────────────────┘   └─────────────────┘
        │                      │                      │
        └──────────┬───────────┴───────────┬──────────┘
                   ▼                       ▼
        ┌─────────────────┐       ┌─────────────────┐
        │   资源池层       │       │   跨租户共享     │
        │  ┌───────────┐  │       │  ┌───────────┐  │
        │  │音频/文本资源│  │◄─────►│  │租户B→租户A│  │
        │  │权限控制    │  │       │  │租户A→租户C│  │
        │  │生命周期管理│  │       │  │有效期/权限│  │
        │  └───────────┘  │       │  └───────────┘  │
        └─────────────────┘       └─────────────────┘
```

### 2.1.2 权限控制矩阵

| 操作类型   | 资源创建者 | 同租户管理员  | 同租户普通用户  | 跨租户授权用户  | 跨租户未授权用户 |
| ------ | ----- | ------- | -------- | -------- | -------- |
| 查看资源列表 | ✅     | ✅       | ✅(可见共享)  | ✅(可见授权)  | ❌        |
| 预览/播放  | ✅     | ✅       | ✅(共享资源)  | ✅(授权资源)  | ❌        |
| 下载     | ✅     | ✅       | ⚠️(配置决定) | ⚠️(授权决定) | ❌        |
| 编辑/修改  | ✅     | ✅       | ❌        | ❌        | ❌        |
| 删除     | ✅     | ✅       | ❌        | ❌        | ❌        |
| 权限配置   | ✅     | ⚠️(租户内) | ❌        | ❌        | ❌        |
| 共享给他人  | ✅     | ✅       | ❌        | ❌        | ❌        |

## 2.2 核心业务流程

### 2.2.1 音频上传与处理流程

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│  用户   │     │  前端   │     │  后端   │     │  OSS    │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ 1.选择音频文件 │               │               │
     ├──────────────►│               │               │
     │               │ 2.分片上传     │               │
     │               ├──────────────►│               │
     │               │               │ 3.存储至OSS   │
     │               │               ├──────────────►│
     │               │               │               │
     │               │               │ 4.异步处理任务 │
     │               │               │───┐           │
     │               │               │   │ 创建任务   │
     │               │               │◄──┘           │
     │               │               │               │
     │               │               │ 5.返回资源ID  │
     │               │◄──────────────┤               │
     │ 6.上传完成    │               │               │
     │◄──────────────┤               │               │
     │               │               │               │
     │               │               │ 7.SnailJob调度│
     │               │               │───┐           │
     │               │               │   │ 音频处理   │
     │               │               │◄──┘           │
     │               │               │ 8.更新资源信息 │
     │               │               │ (时长/转写)    │
     │               │               │               │
     │ 9.查看处理结果 │               │               │
     ├──────────────►│ 10.查询资源   │               │
     │               ├──────────────►│               │
     │               │               │ 11.返回处理状态│
     │               │◄──────────────┤               │
     │ 12.显示波形/  │               │               │
     │    转写文本   │               │               │
     │◄──────────────┤               │               │
     │               │               │               │
```

### 2.2.2 跨租户资源共享流程

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│ 租户A用户│     │ 租户A   │     │ 租户B   │     │ 租户B用户│
│ (创建者)│     │ 后端    │     │ 后端    │     │ (接收者)│
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ 1.选择资源共享 │               │               │
     ├──────────────►│               │               │
     │               │ 2.校验权限    │               │
     │               │───┐           │               │
     │               │   │ 是创建者? │               │
     │               │◄──┘           │               │
     │               │               │               │
     │               │ 3.填写共享信息│               │
     │               │ (租户B/用户)   │               │
     │               │               │               │
     │               │ 4.创建共享记录│               │
     │               │ (resource_share)             │
     │               │               │               │
     │ 5.共享成功    │               │               │
     │◄──────────────┤               │               │
     │               │               │ 6.推送通知    │
     │               │               ├──────────────►│
     │               │               │               │ 7.登录系统
     │               │               │               │◄──┐
     │               │               │               │   │
     │               │               │               │ 8.查看共享
     │               │               │               ├──►│
     │               │               │               │   │
     │               │ 9.请求资源预览│               │   │
     │               │◄──────────────┤               │   │
     │               │               │               │   │
     │               │ 10.权限校验   │               │   │
     │               │───┐           │               │   │
     │               │   │ 有效期?   │               │   │
     │               │   │ 权限匹配? │               │   │
     │               │◄──┘           │               │   │
     │               │               │               │   │
     │               │ 11.返回资源   │               │   │
     │               ├──────────────►│               │   │
     │               │               │ 12.转发预览   │   │
     │               │               ├──────────────►│   │
     │               │               │               │   │
     │               │               │               │   │
     │               │               │               │   │
```

## 2.3 音频处理功能设计

### 2.3.1 音频处理能力矩阵

| 功能分类     | 具体功能           | 技术实现      | 是否异步 | 性能预估        |
| -------- | -------------- | --------- | ---- | ----------- |
| **基础处理** | 格式转换(MP3/WAV)  | FFmpeg    | ✅    | 10MB文件 < 2秒 |
|          | 音频裁剪/拼接        | FFmpeg    | ✅    | < 1秒        |
|          | 音量调整           | TarsosDSP | ✅    | < 1秒        |
|          | 采样率转换          | TarsosDSP | ✅    | 依赖音频长度      |
| **特征提取** | 波形图生成          | TarsosDSP | ❌    | 实时          |
|          | 频谱分析           | TarsosDSP | ✅    | 1-3秒        |
|          | 语音活动检测         | TarsosDSP | ✅    | 实时          |
|          | 基频提取           | TarsosDSP | ✅    | 实时          |
| **智能处理** | 语音转文字(Whisper) | HTTP调用    | ✅    | 1分钟音频 < 10秒 |
|          | 文字转语音(TTS)     | HTTP调用    | ✅    | 100字 < 3秒   |
|          | 声纹识别           | 扩展预留      | ✅    | 需模型支持       |
|          | 情绪识别           | 扩展预留      | ✅    | 需模型支持       |

### 2.3.2 音频处理任务状态机

```
                   ┌─────────────┐
                   │  PENDING    │
                   │  (待处理)    │
                   └──────┬──────┘
                          │ 任务调度
                          ▼
                   ┌─────────────┐
              ┌───▶│  PROCESSING │◀───┐
              │    │  (处理中)    │    │
              │    └──────┬──────┘    │
              │           │           │
              │      处理完成          │ 失败重试(最多3次)
              │           │           │
              │           ▼           │
              │    ┌─────────────┐    │
              └────│   RETRY     │────┘
                   │  (重试)     │
                   └──────┬──────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│   SUCCESS   │   │   FAILED    │   │  TIMEOUT    │
│  (成功)     │   │  (失败)     │   │  (超时)     │
└─────────────┘   └─────────────┘   └─────────────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
                          ▼
                  ┌─────────────┐
                  │  处理完成    │
                  │ 结果写入资源 │
                  └─────────────┘
```

---

# 三、RuoYi-Vue-Plus 原有功能扩展

## 3.1 租户管理扩展

在原有租户管理基础上，增加资源配额管理：

```java
-- 租户表扩展
ALTER TABLE sys_tenant 
ADD COLUMN storage_quota BIGINT DEFAULT 10737418240 COMMENT '存储配额(字节)',
ADD COLUMN storage_used BIGINT DEFAULT 0 COMMENT '已用存储(字节)',
ADD COLUMN audio_process_quota INT DEFAULT 1000 COMMENT '月音频处理次数',
ADD COLUMN audio_process_used INT DEFAULT 0 COMMENT '本月已用次数',
ADD COLUMN tts_quota INT DEFAULT 500 COMMENT '月TTS字符数(万)',
ADD COLUMN tts_used INT DEFAULT 0 COMMENT '本月已用TTS字符数';

```

## 3.2 用户管理扩展

扩展用户表，增加资源相关字段：

```sql
-- 用户表扩展
ALTER TABLE sys_user 
ADD COLUMN default_permission VARCHAR(20) DEFAULT 'view' COMMENT '默认资源权限',
ADD COLUMN resource_notify TINYINT DEFAULT 1 COMMENT '资源通知开关',
ADD COLUMN last_resource_time DATETIME COMMENT '最后操作资源时间';
```

## 3.3 菜单与权限扩展

新增以下菜单项：

| 菜单名称  | 父菜单  | 权限标识                    | 路由                       | 组件                      |
| ----- | ---- | ----------------------- | ------------------------ | ----------------------- |
| 资源池管理 | 系统管理 | tenant:resource:view    | /tenant/resource         | tenant/resource/index   |
| 音频处理  | 资源池  | tenant:audio:process    | /tenant/audio            | tenant/audio/index      |
| 资源共享  | 资源池  | tenant:share:view       | /tenant/share            | tenant/share/index      |
| 共享给我的 | 资源池  | tenant:share:received   | /tenant/share/received   | tenant/share/received   |
| 语音转文字 | 音频处理 | tenant:audio:transcribe | /tenant/audio/transcribe | tenant/audio/transcribe |
| 文字转语音 | 音频处理 | tenant:audio:tts        | /tenant/audio/tts        | tenant/audio/tts        |
| 音频分析  | 音频处理 | tenant:audio:analyze    | /tenant/audio/analyze    | tenant/audio/analyze    |

---

# 四、接口设计

## 4.1 资源管理接口

### 4.1.1 资源上传

```http
POST /tenant/resource/upload
Content-Type: multipart/form-data

参数：
- file: 文件(必填)
- resource_type: 资源类型(audio/text)(必填)
- permission_type: 权限类型(private/tenant_shared)(默认private)
- remark: 备注(可选)

响应：
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "resourceId": 1001,
        "resourceName": "test.mp3",
        "resourceType": "audio",
        "fileSize": 5242880,
        "ossUrl": "https://oss.xxx.com/tenant_1/2026/02/28/123.mp3",
        "permissionType": "private",
        "createTime": "2026-02-28 10:30:00",
        "taskId": 5001,  // 异步处理任务ID
        "taskStatus": "pending"
    }
}
```

### 4.1.2 资源列表查询

```http
GET /tenant/resource/list

参数：
- pageNum: 页码(默认1)
- pageSize: 每页条数(默认20)
- resourceType: 资源类型(audio/text)(可选)
- permissionType: 权限类型(可选)
- keyword: 关键词搜索(可选)
- startTime: 开始时间(可选)
- endTime: 结束时间(可选)
- creatorId: 创建者ID(可选)

响应：
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "total": 100,
        "rows": [
            {
                "resourceId": 1001,
                "resourceName": "test.mp3",
                "resourceType": "audio",
                "fileSize": "5.2 MB",
                "duration": "00:03:25",
                "permissionType": "private",
                "permissionTypeName": "私有",
                "creatorName": "张三",
                "creatorId": 1,
                "createTime": "2026-02-28 10:30:00",
                "canEdit": true,
                "canDelete": true,
                "canShare": true
            }
        ],
        "summary": {
            "totalAudio": 80,
            "totalText": 20,
            "totalSize": "15.6 GB",
            "usedQuota": "35%"
        }
    }
}
```

## 4.2 音频处理接口

### 4.2.1 语音转文字

```http
POST /tenant/audio/transcribe

请求体：
{
    "resourceId": 1001,
    "language": "zh",  // 语言代码
    "model": "base",   // 模型大小: tiny/base/small/medium/large
    "wordTimestamps": true,  // 是否返回时间戳
    "callbackUrl": "https://api.xxx.com/callback"  // 回调地址(可选)
}

响应：
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "taskId": 5001,
        "taskNo": "T202602281030001",
        "taskStatus": "pending",
        "estimatedTime": 15  // 预计耗时(秒)
    }
}
```

### 4.2.2 查询处理结果

```http
GET /tenant/audio/task/{taskId}

响应：
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "taskId": 5001,
        "taskNo": "T202602281030001",
        "taskStatus": "success",
        "taskType": "transcription",
        "result": {
            "text": "今天天气真好，我们一起去公园玩吧。",
            "segments": [
                {
                    "start": 0.5,
                    "end": 2.3,
                    "text": "今天天气真好"
                },
                {
                    "start": 2.3,
                    "end": 4.8,
                    "text": "我们一起去公园玩吧"
                }
            ],
            "language": "zh",
            "duration": 4.8
        },
        "resourceId": 1001,
        "resourceName": "test.mp3",
        "startTime": "2026-02-28 10:30:05",
        "endTime": "2026-02-28 10:30:20",
        "duration": 15
    }
}
```

### 4.2.3 文字转语音

```http
POST /tenant/audio/tts

请求体：
{
    "text": "欢迎使用VoiceText Weaver平台，您的语音助手。",
    "voice": "zh_female_1",  // 音色
    "speed": 1.0,  // 语速 0.5-2.0
    "pitch": 1.0,  // 音调
    "format": "mp3",  // 输出格式
    "callbackUrl": "https://api.xxx.com/callback"
}

响应：
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "taskId": 5002,
        "taskNo": "T202602281035001",
        "taskStatus": "pending",
        "estimatedTime": 3
    }
}
```

### 4.2.4 音频分析

```http
POST /tenant/audio/analyze

请求体：
{
    "resourceId": 1001,
    "analysisTypes": ["waveform", "spectrum", "vad", "pitch"],
    "parameters": {
        "waveform": {
            "width": 800,
            "height": 200
        },
        "vad": {
            "threshold": 0.1,
            "minSpeechDuration": 0.5
        }
    }
}

响应：
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "taskId": 5003,
        "taskStatus": "running"
    }
}

// 查询结果时
GET /tenant/audio/analysis/{taskId}

响应：
{
    "code": 200,
    "data": {
        "waveform": {
            "data": [0.1, 0.3, 0.5, ...],
            "sampleRate": 100,
            "duration": 10,
            "imageUrl": "https://oss.xxx.com/waveform/123.png"
        },
        "spectrum": {
            "imageUrl": "https://oss.xxx.com/spectrum/123.png",
            "features": {
                "mfcc": [...],
                "centroid": 1250.5,
                "rolloff": 3800.2
            }
        },
        "vad": {
            "segments": [
                {"start": 0.2, "end": 3.5, "speech": true},
                {"start": 3.8, "end": 7.2, "speech": true}
            ],
            "speechRatio": 0.65
        },
        "pitch": {
            "mean": 185.3,
            "max": 320.5,
            "min": 95.2,
            "contour": [180, 185, 190, ...]
        }
    }
}
```

## 4.3 资源共享接口

### 4.3.1 创建共享

```http
POST /tenant/share/create

请求体：
{
    "resourceId": 1001,
    "shareType": "tenant",  // user/tenant
    "targetTenantId": 2,    // 目标租户ID(shareType=tenant时必填)
    "targetUserId": 10,      // 目标用户ID(shareType=user时必填)
    "permissionCode": "view",  // view/edit/download
    "expireDays": 7,         // 过期天数(null为永久)
    "remark": "项目合作资料"
}

响应：
{
    "code": 200,
    "msg": "操作成功",
    "data": {
        "shareId": 2001,
        "shareCode": "SHR202602281050001",
        "shareUrl": "https://platform.xxx.com/s/SHR202602281050001",
        "expireTime": "2026-03-07 10:50:00"
    }
}
```

### 4.3.2 共享列表

```http
GET /tenant/share/list

参数：
- direction: out/in(发出的/收到的)
- status: active/expired

响应：
{
    "code": 200,
    "data": {
        "total": 15,
        "rows": [
            {
                "shareId": 2001,
                "resourceName": "project_intro.mp3",
                "sourceTenantName": "科技公司",
                "sourceUserName": "张三",
                "targetTenantName": "教育机构",
                "targetUserName": "李四",
                "permissionType": "view",
                "expireTime": "2026-03-07 10:50:00",
                "shareTime": "2026-02-28 10:50:00",
                "status": "active"
            }
        ]
    }
}
```

---

# 五、前端UI设计（基于Element Plus）

## 5.1 资源池主界面

```
┌─────────────────────────────────────────────────────────────┐
│  🏠 首页 > 资源池管理                                          │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐ │
│  │ [租户: 科技公司 ▼]  [👤 张三 ▼]  [存储空间: 35GB/100GB ███████░░░░]│
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  📁 资源列表  [上传] [新建文件夹] [批量操作 ▼] [🔍 搜索...] │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │ 筛选: [全部资源 ▼] [私有 ▼] [最近7天 ▼] [重置]            │   │
│  │ 快捷: [我的资源] [共享给我] [租户共享]                    │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │ ☐ 文件名           类型  大小   时长  创建者  权限  操作  │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │ ☐ 📊 项目会议.mp3   音频  5.2M  03:25  张三    🔒私有  ▶️ ⋮ │
│  │ ☐ 📊 培训课程.wav   音频  12.8M 08:12  李四    🔓租户 ▶️ ⋮ │
│  │ ☐ 📄 会议纪要.txt   文本  2.1K  -      王五    🔒私有  👁️ ⋮ │
│  │ ☐ 📊 语音笔记.m4a   音频  3.5M  02:18  张三    🔗跨租 ▶️ ⋮ │
│  │ ☐ 📄 项目计划.docx  文本  45K   -      赵六    🔓租户  👁️ ⋮ │
│  ├─────────────────────────────────────────────────────┤   │
│  │ [<<] 1 2 3 4 5 ... 20 [>>]  共156条                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 📊 资源统计                                           │   │
│  │ 音频: 128个 (12.5GB)  文本: 28个 (45.2MB)             │   │
│  │ 今日上传: 15个        处理中任务: 3个                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 5.2 音频详情/处理界面

```
┌─────────────────────────────────────────────────────────────┐
│  ← 返回列表  [项目会议.mp3]                                   │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐ │
│  │  ▶️ [播放/暂停]  00:00 / 03:25  ┌───────┬───────┐     │ │
│  │  ├──────────────────────────────┤ 波形图 │       │     │ │
│  │  │ ▁▂▃▄▅▆▇█▇▆▅▄▃▂▁▂▃▄▅▆▇█▇▆▅▄▃▂▁ │       │       │     │ │
│  │  └──────────────────────────────┴───────┴───────┘     │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
│  ┌─────────────┬─────────────────────────────────────────┐ │
│  │ [基本信息]  │ 文件名: 项目会议.mp3                       │ │
│  │ [处理工具]  │ 创建者: 张三 (科技公司)                    │ │
│  │ [权限设置]  │ 创建时间: 2026-02-28 10:30                │ │
│  │ [共享管理]  │ 文件大小: 5.2 MB                          │ │
│  │ [分析结果]  │ 音频时长: 03:25                           │ │
│  │             │ 采样率: 44.1kHz 声道: 双声道              │ │
│  │             │ 格式: MP3 比特率: 192kbps                 │ │
│  └─────────────┴─────────────────────────────────────────┘ │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ [处理工具]                                            │   │
│  │ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐       │   │
│  │ │语音转文字│ │文字转语音│ │格式转换 │ │音频裁剪 │       │   │
│  │ └────────┘ └────────┘ └────────┘ └────────┘       │   │
│  │ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐       │   │
│  │ │音量调整 │ │降噪处理 │ │频谱分析 │ │VAD检测  │       │   │
│  │ └────────┘ └────────┘ └────────┘ └────────┘       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ [语音转文字结果]   [导出] [复制]                     │   │
│  │ 今天天气真好，我们一起去公园玩吧。                      │   │
│  │                                                       │   │
│  │ 00:00 - 00:02  今天天气真好                           │   │
│  │ 00:02 - 00:04  我们一起去公园玩吧                      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 5.3 权限配置弹窗

```
┌─────────────────────────────────────────────┐
│ 权限配置 - 项目会议.mp3                       │
├─────────────────────────────────────────────┤
│ 基础权限:                                    │
│ ◎ 私有 (仅自己可见)                          │
│ ○ 租户内共享 (同租户用户可见)                 │
│ ○ 跨租户共享 (指定租户/用户可见)              │
│                                             │
│ [跨租户共享配置]                              │
│ ┌───────────────────────────────────────┐ │
│ │ 添加授权: 租户ID [______] 用户ID(可选) │ │
│ │ 权限: [可预览 ▼] 有效期: [7天 ▼] [添加]│ │
│ └───────────────────────────────────────┘ │
│                                             │
│ 已授权列表:                                  │
│ ┌───────────────────────────────────────┐ │
│ │ 租户: 教育机构 (所有用户) - 可预览 永久  [删除]│
│ │ 用户: 李四 (教育机构)    - 可下载 7天   [删除]│
│ └───────────────────────────────────────┘ │
│                                             │
│ 租户内操作权限:                               │
│ ☑ 可预览  ☑ 可下载  ☐ 可编辑  ☐ 可共享        │
│                                             │
│ [保存] [取消]                               │
└─────────────────────────────────────────────┘
```

---

# 六、部署与运维

## 6.1 服务部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                       负载均衡层                              │
│                    Nginx/OpenResty                           │
│                (路由/限流/SSL/静态资源)                       │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│   Web服务集群    │   │   Web服务集群    │   │   Web服务集群    │
│ RuoYi-Admin节点1│   │ RuoYi-Admin节点2│   │ RuoYi-Admin节点3│
│  (无状态)       │   │  (无状态)       │   │  (无状态)       │
└─────────────────┘   └─────────────────┘   └─────────────────┘
        │                      │                      │
        └──────────────────────┼──────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│    Redis集群    │   │   MySQL集群     │   │   SnailJob      │
│  (缓存/分布式锁) │   │ (主从/分库分表)  │   │  (任务调度中心)  │
└─────────────────┘   └─────────────────┘   └─────────────────┘
        │                      │                      │
        └──────────────────────┼──────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│    Minio集群    │   │   Neo4j图库     │   │   AI服务集群    │
│  (对象存储)     │   │  (知识图谱)     │   │ (Whisper/TTS)   │
└─────────────────┘   └─────────────────┘   └─────────────────┘
```

## 6.2 Docker Compose 配置（扩展）

```yaml
version: '3.8'

services:
  # MySQL - 使用原框架配置
  mysql:
    image: mysql:8.0
    # ... 原有配置保持不变

  # Redis - 使用原框架配置
  redis:
    image: redis:7.2
    # ... 原有配置保持不变

  # Minio - 使用原框架配置
  minio:
    image: minio/minio:latest
    # ... 原有配置保持不变

  # 新增：Whisper 语音识别服务
  whisper-service:
    image: onerahmet/openai-whisper-asr-webservice:latest
    container_name: whisper-service
    ports:
      - "9000:9000"
    environment:
      - ASR_MODEL=base
      - ASR_ENGINE=openai_whisper
    volumes:
      - ./data/whisper/models:/root/.cache/whisper
      - ./data/whisper/uploads:/app/uploads
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    restart: always
    networks:
      - ruoyi-network

  # 新增：Coqui TTS 服务
  tts-service:
    build:
      context: ./docker/tts
      dockerfile: Dockerfile
    container_name: tts-service
    ports:
      - "9001:9001"
    volumes:
      - ./data/tts/models:/app/models
      - ./data/tts/outputs:/app/outputs
    environment:
      - TTS_MODEL=tts_models/zh-CN/baker/tacotron2-DDC-GST
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
      - ruoyi-network

  # 新增：音频处理 Worker (处理FFmpeg/TarsosDSP任务)
  audio-worker:
    image: ruoyi-audio-worker:latest
    build:
      context: ./ruoyi-modules/ruoyi-audio-worker
      dockerfile: Dockerfile
    container_name: audio-worker
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - REDIS_HOST=redis
      - MYSQL_HOST=mysql
    volumes:
      - ./data/audio/uploads:/data/uploads
      - ./data/audio/outputs:/data/outputs
    depends_on:
      - mysql
      - redis
      - whisper-service
      - tts-service
    restart: always
    networks:
      - ruoyi-network

  # RuoYi-Admin 服务（扩展环境变量）
  ruoyi-admin:
    image: ruoyi-admin:latest
    build:
      context: ./ruoyi-admin
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - WHISPER_SERVICE_URL=http://whisper-service:9000
      - TTS_SERVICE_URL=http://tts-service:9001
      - MINIO_ENDPOINT=http://minio:9000
    # ... 其他配置保持不变
```

## 6.3 监控指标扩展

在原有SpringBoot-Admin基础上，增加音频处理监控：

| 监控指标                        | 说明           | 告警阈值 | 采集方式       |
| --------------------------- | ------------ | ---- | ---------- |
| audio.task.pending.count    | 待处理任务数       | >100 | Prometheus |
| audio.task.processing.count | 处理中任务数       | >50  | Prometheus |
| audio.task.failed.count     | 失败任务数(小时)    | >10  | 日志分析       |
| audio.task.avg.duration     | 任务平均耗时       | >30秒 | Prometheus |
| whisper.service.qps         | Whisper服务QPS | >10  | Nginx日志    |
| tts.service.qps             | TTS服务QPS     | >20  | Nginx日志    |
| audio.storage.usage         | 租户存储使用率      | >90% | 数据库查询      |
| audio.quota.usage           | 租户配额使用率      | >80% | 数据库查询      |

---

# 七、项目落地实施计划

## 7.1 分阶段实施路线图

```
阶段一：基础架构搭建 (2周)
├── 创建 ruoyi-common-audio 模块
├── 集成 TarsosDSP 基础音频处理
├── 集成 FFmpeg 格式转换
├── 扩展数据库表(资源表/共享表)
└── 基础资源管理CRUD

阶段二：租户资源池 (2周)
├── 扩展租户配额管理
├── 资源权限控制引擎
├── 资源共享机制
├── 资源列表/上传/下载
└── 租户目录隔离

阶段三：音频处理能力 (3周)
├── Whisper 服务集成
├── TTS 服务集成
├── 异步任务框架(SnailJob)
├── 音频波形/频谱分析
├── 语音活动检测
└── 处理结果持久化

阶段四：前端UI完善 (2周)
├── 资源池管理界面
├── 音频播放/可视化组件
├── 权限配置界面
├── 处理任务管理界面
└── 租户管理扩展

阶段五：性能优化与测试 (2周)
├── 分片上传/断点续传
├── 缓存优化
├── 并发压力测试
├── 安全审计
└── 文档完善

阶段六：生产部署 (1周)
├── Docker编排
├── 监控配置
├── 备份策略
└── 灰度发布
```

## 7.2 关键风险与应对

| 风险类型     | 风险描述                | 影响程度 | 应对措施                             |
| -------- | ------------------- | ---- | -------------------------------- |
| **技术风险** | Whisper/TTS GPU资源不足 | 高    | 1. 任务队列削峰 2. 支持模型降级 3. 预留GPU弹性扩容 |
|          | 音频处理性能瓶颈            | 中    | 1. 分片处理 2. 结果缓存 3. 异步非阻塞         |
|          | 大文件内存溢出             | 中    | 1. 流式处理 2. 分片上传 3. 限制单文件大小       |
| **业务风险** | 租户隔离穿透              | 高    | 1. 全链路权限校验 2. 路径注入防护 3. 定期安全审计   |
|          | 资源滥用/盗用             | 中    | 1. 接口限流 2. 访问日志 3. 异常行为检测        |
| **运维风险** | 模型服务宕机              | 中    | 1. 多副本部署 2. 自动重启 3. 降级服务         |
|          | 数据丢失                | 高    | 1. 每日备份 2. 多副本存储 3. 操作日志         |

---

# 八、总结

## 8.1 融合改造成果

本项目基于 **RuoYi-Vue-Plus** 框架，成功整合了多租户音频处理能力，实现了：

1. **架构融合**：充分利用原框架的多租户、权限、任务调度、OSS存储等能力，新增音频处理模块无缝集成
2. **租户隔离**：物理文件隔离 + 数据库租户ID过滤 + 全链路权限校验，三层保障数据安全
3. **资源共享**：支持私有/租户内/跨租户三级权限，灵活满足协作需求
4. **音频能力**：集成Whisper语音识别、TTS语音合成、TarsosDSP音频分析，形成完整音频处理链路
5. **用户体验**：Element Plus界面 + 音频可视化 + 实时处理状态，操作流畅直观

## 8.2 核心优势

- **开箱即用**：基于成熟框架扩展，避免重复造轮子
- **性能稳定**：分布式架构支持水平扩展，异步任务削峰填谷
- **安全可靠**：多租户隔离 + 细粒度权限 + 操作审计
- **易于维护**：模块化设计，各模块职责单一，便于扩展

## 8.3 后续规划

- **AI增强**：接入大模型，实现音频内容摘要、智能问答
- **实时通信**：WebRTC实时语音处理
- **更多格式**：支持视频中音频提取处理
- **声纹识别**：说话人识别与分离
- **国际多语言**：支持更多语言的语音识别与合成

---

**文档版本**: 1.0.0  
**最后更新**: 2026-02-28  
**文档状态**: 已评审待实施
