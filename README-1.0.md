# VoiceText Weaver 数据表与业务关联说明

## 一、核心业务数据流转全景图

```
┌─────────────────────────────────────────────────────────────────┐
│                        业务操作层                                │
│  Clone功能  Design功能  TTS功能  STT功能  历史记录查看           │
└──────────────────────┬──────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────────────────────┐
│                      数据交互层                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ 创建声音模型  │  │ 生成音频项目  │  │ 记录任务状态  │          │
│  │ 消耗模型配额  │  │ 占用存储空间  │  │ 更新使用统计  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼─────────────────┼─────────────────┼────────────────────┘
          ↓                 ↓                 ↓
┌─────────────────────────────────────────────────────────────────┐
│                        数据存储层                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ voice_model（声音模型表）                                    │ │
│  │ ├── 记录克隆/设计的声音                                      │ │
│  │ ├── 存储参考音频路径、模型参数                                │ │
│  │ └── 关联tenant_id实现租户隔离                                │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ audio_project（音频项目表）                                  │ │
│  │ ├── 记录每次生成操作                                        │ │
│  │ ├── 存储源音频/生成音频、文本内容                            │ │
│  │ ├── 记录渲染时间/文件大小/时长等性能指标                      │ │
│  │ └── 关联voice_model_id实现声模复用                           │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ tenant_quota（租户资源配额表）                               │ │
│  │ ├── 控制租户声模数量上限                                    │ │
│  │ ├── 控制存储空间上限                                        │ │
│  │ ├── 控制并发任务数                                          │ │
│  │ └── 实时更新已使用资源                                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ task_record（任务记录表）                                    │ │
│  │ ├── 跟踪异步任务执行状态                                    │ │
│  │ ├── 记录任务开始/结束时间                                   │ │
│  │ ├── 存储任务参数和错误信息                                  │ │
│  │ └── 支持任务重试和监控                                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、各功能模块与数据表详细关联

### 2.1 Clone（声音克隆）功能

| 业务步骤 | 操作内容 | 涉及数据表 | 字段变化 | 业务规则 |
|---------|---------|-----------|---------|---------|
| **1. 上传参考音频** | 上传3秒参考音频 | - | - | 音频格式校验 |
| **2. 保存声模** | 将音频和转录文本保存为声音模型 | `voice_model` | INSERT：<br>- model_name<br>- model_type='clone'<br>- ref_audio_url<br>- ref_text<br>- status=0(训练中)<br>- tenant_id | 需检查租户声模配额 |
| **3. 检查配额** | 验证租户是否可新建声模 | `tenant_quota` | SELECT：<br>- used_model_count<br>- max_model_count | used < max 才允许创建 |
| **4. 更新配额** | 声模创建成功后更新使用量 | `tenant_quota` | UPDATE：<br>- used_model_count = used_model_count + 1 | 原子性操作，使用分布式锁 |
| **5. 模型训练** | 异步训练声音模型 | `task_record` | INSERT：<br>- task_type='clone'<br>- task_status='pending'<br>- task_params={模型ID,音频路径} | 异步任务处理 |
| **6. 训练完成** | 更新模型状态 | `voice_model` | UPDATE：<br>- status=1(可用)<br>- model_path<br>- preview_audio_url | 成功后更新 |
| **7. 生成语音** | 使用克隆声模生成新语音 | `audio_project` | INSERT：<br>- project_type='clone'<br>- voice_model_id<br>- source_text<br>- target_audio_url<br>- duration/file_size<br>- status | 生成记录供历史展示 |

### 2.2 Design（声音设计）功能

| 业务步骤 | 操作内容 | 涉及数据表 | 字段变化 | 业务规则 |
|---------|---------|-----------|---------|---------|
| **1. 输入声音描述** | 描述理想声音特征 | - | - | 支持自然语言 |
| **2. 调节参数** | 调整年龄/音调/语速等 | - | - | UI滑块控制 |
| **3. 保存设计声模** | 将参数保存为声音模型 | `voice_model` | INSERT：<br>- model_name<br>- model_type='design'<br>- voice_params={年龄:25,性别:男,情绪:活力}<br>- status=1(直接可用)<br>- tenant_id | 设计声模无需训练，直接可用 |
| **4. 检查配额** | 验证租户是否可新建声模 | `tenant_quota` | SELECT used < max | 同克隆功能 |
| **5. 更新配额** | 声模创建成功后更新 | `tenant_quota` | UPDATE used_model_count | 同克隆功能 |
| **6. 测试生成** | 输入文本测试设计声音 | `audio_project` | INSERT：<br>- project_type='design'<br>- voice_model_id<br>- source_text<br>- target_audio_url<br>- 性能指标 | 生成试听记录 |

### 2.3 TTS（文本转语音）功能

| 业务步骤 | 操作内容 | 涉及数据表 | 字段变化 | 业务规则 |
|---------|---------|-----------|---------|---------|
| **1. 选择声模** | 从下拉列表选择声音 | `voice_model` | SELECT：<br>- id, model_name<br>- WHERE tenant_id AND status=1 | 只显示可用声模 |
| **2. 输入文本** | 输入要合成的文本 | - | - | 文本长度限制 |
| **3. 检查并发** | 验证租户并发任务数 | `tenant_quota` | SELECT：<br>- current_tasks<br>- concurrent_tasks | current < max 才允许提交 |
| **4. 更新并发** | 任务提交后增加并发计数 | `tenant_quota` | UPDATE：<br>- current_tasks = current_tasks + 1 | 任务开始前加锁 |
| **5. 创建任务** | 创建TTS异步任务 | `task_record` | INSERT：<br>- task_type='tts'<br>- task_status='pending'<br>- task_params={模型ID,文本}<br>- tenant_id | 分布式任务调度 |
| **6. 任务执行** | 调用TTS服务生成音频 | - | - | AI网关路由 |
| **7. 任务完成** | 更新任务状态和结果 | `task_record` | UPDATE：<br>- task_status='success'<br>- result_url<br>- end_time | 失败则记录错误信息 |
| **8. 保存结果** | 保存生成记录到项目表 | `audio_project` | INSERT：<br>- project_type='tts'<br>- voice_model_id<br>- source_text<br>- target_audio_url<br>- duration/file_size/render_time<br>- task_id<br>- status | 用于历史记录展示 |
| **9. 释放并发** | 任务完成后减少并发计数 | `tenant_quota` | UPDATE：<br>- current_tasks = current_tasks - 1 | 无论成功失败都要释放 |
| **10. 更新存储** | 增加已用存储空间 | `tenant_quota` | UPDATE：<br>- used_storage_mb = used_storage_mb + 文件大小 | 存储空间累计 |

### 2.4 STT（语音转文本）功能

| 业务步骤 | 操作内容 | 涉及数据表 | 字段变化 | 业务规则 |
|---------|---------|-----------|---------|---------|
| **1. 上传音频** | 上传待识别音频 | - | - | 格式/大小校验 |
| **2. 检查存储** | 验证存储空间是否充足 | `tenant_quota` | SELECT：<br>- used_storage_mb<br>- max_storage_mb | used + 文件大小 < max |
| **3. 创建任务** | 创建STT异步任务 | `task_record` | INSERT：<br>- task_type='stt'<br>- task_status='pending'<br>- task_params={音频URL,语言}<br>- tenant_id | 支持语言自动检测 |
| **4. 任务执行** | 调用Whisper识别 | - | - | AI网关路由 |
| **5. 任务完成** | 更新任务状态和结果 | `task_record` | UPDATE：<br>- task_status='success'<br>- result_url(文本结果)<br>- end_time | 失败则记录错误 |
| **6. 保存结果** | 保存转录记录到项目表 | `audio_project` | INSERT：<br>- project_type='stt'<br>- source_audio_url<br>- target_text(识别结果)<br>- duration/file_size<br>- task_id<br>- status | 用于历史记录展示 |
| **7. 更新存储** | 增加已用存储空间 | `tenant_quota` | UPDATE：<br>- used_storage_mb = used_storage_mb + 文件大小 | 上传的音频占用空间 |

---

## 三、历史记录功能与数据表关联

| 历史记录元素 | 数据来源 | 字段映射 | 展示逻辑 |
|-------------|---------|---------|---------|
| **功能标识** | `audio_project.project_type` | clone/design/tts/stt | 对应不同图标和颜色 |
| **内容预览** | `audio_project.source_text` 或 `audio_project.source_audio_url` | 文本截取前20字/文件名 | TTS/STT/Design显示文本，Clone显示文件名 |
| **时间戳** | `audio_project.create_time` | 转换为 just now / 1h ago / 36m ago | 动态计算时间差 |
| **音频时长** | `audio_project.duration` | 格式化为 0:08 / 0:09 | 秒转换为分:秒 |
| **文件大小** | `audio_project.file_size` | 格式化为 385.2KB / 434.3KB | 自动转换单位 |
| **播放按钮** | `audio_project.target_audio_url` | 音频文件OSS地址 | 点击播放预览 |
| **清空历史** | DELETE FROM `audio_project` WHERE tenant_id = ? | 物理删除或逻辑删除 | 需二次确认 |

**历史记录查询SQL示例：**
```sql
SELECT 
    project_type,
    CASE 
        WHEN project_type IN ('tts','design') THEN LEFT(source_text, 20)
        WHEN project_type = 'clone' THEN CONCAT('声模:', voice_model_id)
        WHEN project_type = 'stt' THEN CONCAT('音频:', SUBSTRING_INDEX(source_audio_url, '/', -1))
    END as preview,
    create_time,
    duration,
    file_size,
    target_audio_url
FROM audio_project 
WHERE tenant_id = ? 
ORDER BY create_time DESC 
LIMIT 20
```

---

## 四、租户资源控制与数据表关联

### 4.1 资源控制流程

```
用户操作 → 检查tenant_quota → 是否超限？ → 是 → 提示升级/清理
                ↓ 否
            允许操作
                ↓
        更新used计数/存储
                ↓
        记录到voice_model/audio_project
                ↓
        历史记录展示
```

### 4.2 资源类型与控制规则

| 资源类型 | 控制字段 | 更新时机 | 涉及操作 | 超限处理 |
|---------|---------|---------|---------|---------|
| **声模数量** | used_model_count / max_model_count | 创建声模时+1<br>删除声模时-1 | Clone保存声模<br>Design保存声模 | 提示"声模数量已达上限，请删除无用声模或升级套餐" |
| **存储空间** | used_storage_mb / max_storage_mb | 上传音频时+文件大小<br>删除音频时-文件大小 | Clone上传参考音频<br>STT上传音频<br>TTS生成音频 | 提示"存储空间不足，请清理历史记录或升级套餐" |
| **并发任务** | current_tasks / concurrent_tasks | 提交任务时+1<br>任务完成时-1 | TTS提交<br>STT提交<br>Clone训练 | 提示"当前任务队列已满，请稍后再试" |

### 4.3 资源统计SQL示例

```sql
-- 统计租户声模使用量
SELECT COUNT(*) FROM voice_model WHERE tenant_id = ? AND status IN (1,2)

-- 统计租户存储使用量
SELECT SUM(file_size) FROM audio_project WHERE tenant_id = ?

-- 统计租户当前任务数
SELECT COUNT(*) FROM task_record WHERE tenant_id = ? AND task_status IN ('pending','running')
```

---

## 五、数据表间关联关系图

```
┌─────────────────┐       ┌─────────────────┐
│   tenant_quota  │       │   voice_model   │
├─────────────────┤       ├─────────────────┤
│ tenant_id (PK)  │←──────│ tenant_id       │
│ max_model_count │       │ id (PK)         │
│ used_model_count│──────→│ model_name      │
│ max_storage_mb  │       │ model_type      │
│ used_storage_mb │       │ ref_audio_url   │
│ concurrent_tasks│       │ voice_params    │
│ current_tasks   │       │ status          │
└─────────────────┘       └────────┬────────┘
                                   │
                                   │
                                   ↓
┌─────────────────┐       ┌─────────────────┐
│  task_record    │       │  audio_project  │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)         │
│ tenant_id       │──────→│ tenant_id       │
│ task_type       │       │ project_type    │
│ task_status     │       │ voice_model_id ─┘
│ task_params     │       │ source_text     │
│ result_url      │←──────│ target_text     │
│ start_time      │       │ source_audio_url│
│ end_time        │       │ target_audio_url│
└─────────────────┘       │ duration        │
                           │ file_size       │
                           │ render_time     │
                           │ task_id ────────┘
                           └─────────────────┘
```

---

## 六、业务数据统计与监控

### 6.1 租户仪表盘数据

| 统计项 | 数据来源 | 计算方式 | 业务意义 |
|-------|---------|---------|---------|
| 总声模数 | `voice_model` | COUNT(*) WHERE tenant_id | 展示声模库规模 |
| 可用声模数 | `voice_model` | COUNT(*) WHERE status=1 | 实际可用声音 |
| 总项目数 | `audio_project` | COUNT(*) WHERE tenant_id | 历史操作总量 |
| 今日生成量 | `audio_project` | COUNT(*) WHERE DATE(create_time)=CURDATE() | 日活跃度 |
| 存储使用率 | `tenant_quota` | used_storage_mb / max_storage_mb * 100% | 资源饱和度 |
| 任务成功率 | `task_record` | 成功数/总数 * 100% | 服务质量 |

### 6.2 系统监控数据

| 监控项 | 数据来源 | 监控目的 | 告警阈值 |
|-------|---------|---------|---------|
| 任务堆积数 | `task_record` WHERE status='pending' | 检测任务积压 | >100 |
| 失败任务数 | `task_record` WHERE status='failed' | 检测服务异常 | >10/小时 |
| 高频租户 | `audio_project` GROUP BY tenant_id | 识别异常使用 | >1000/天 |
| 存储增长 | `tenant_quota` SUM(used_storage_mb) | 容量规划 | >80% |

---

## 七、总结：表与业务的关系本质

| 数据表 | 业务角色 | 核心价值 | 关联功能 |
|-------|---------|---------|---------|
| **voice_model** | 声音资产库 | 存储租户定制的声音模型，是TTS服务的核心资产 | Clone、Design、TTS |
| **audio_project** | 操作流水账 | 记录每一次生成操作，提供历史回溯和统计分析 | 全部四个功能、历史记录 |
| **tenant_quota** | 资源控制器 | 确保租户资源公平使用，防止滥用 | 全部功能的前置校验 |
| **task_record** | 任务跟踪器 | 管理异步任务生命周期，支持失败重试和监控 | Clone训练、TTS、STT |

**一句话总结**：这四张表共同构成了一个完整的**租户隔离、资源可控、操作可追溯**的语音处理业务闭环，每个功能操作都会在这四张表中留下对应的数据足迹，形成从**资源检查→任务创建→结果存储→历史展示**的完整链路。