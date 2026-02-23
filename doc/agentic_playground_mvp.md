# BI Agentic Playground 改造方案（可执行草案）

> 分支：`feat/agentic-playground`  
> 更新时间：2026-02-23（UTC）

## 0. 现状快照（先用 `gh api` 获取）

### 0.1 仓库与分支
- `gh api repos/kkkano/BI`：仓库存在，`default_branch=main`，最近 `pushed_at=2026-02-23T13:58:28Z`
- `gh api repos/kkkano/BI/branches/feat/agentic-playground`：分支头部提交 `599f7d804bf011d85b5c8126e96961dcd11dd69d`
- `gh api repos/kkkano/BI/contents`：当前仓库已有 `doc/` 目录，可直接落地方案文档

### 0.2 代码现状（用于约束设计）
- 图表任务已有状态机：`wait -> running -> succeed | failed`（`ChartStatusEnum`）
- 已有异步执行模式：线程池 + MQ（`/chart/gen/async`、`/chart/gen/async/mq`）
- 已有轮询接口：`/chart/task/status`、`/chart/task/status/batch`
- 文件解析当前只支持 `xlsx/xls/csv`，统一转 `csv`（`ExcelUtils.fileToCsv`）

---

## 1. ReAct / Plan-Execute 决策规则（可量化）

目标：在服务端对每个 Agent 任务先做路由，避免“所有请求都走同一种推理模式”。

## 1.1 输入特征（可直接计算）

在 `AgentDecisionRequest` 中增加以下字段（可由后端预估）：

```json
{
  "taskId": "agt_20260223_xxx",
  "goal": "分析近一年渠道ROI并给出投放建议",
  "subTaskCount": 4,
  "dependencyDepth": 2,
  "toolTypeCount": 3,
  "hasHardConstraint": true,
  "needCitation": true,
  "freshnessDays": 7,
  "inputTokenEstimate": 3200,
  "historicalRetryCount": 1
}
```

字段定义：
- `subTaskCount`：任务拆分后的原子步骤数（NLP/规则估计）
- `dependencyDepth`：步骤依赖层级深度
- `toolTypeCount`：将调用工具类型数量（DB/API/Search/File等）
- `hasHardConstraint`：是否存在强约束（时限、格式、审计）
- `needCitation`：是否必须给证据引用
- `freshnessDays`：对时效性的要求（<=30 通常认为高时效）
- `inputTokenEstimate`：预计上下文长度
- `historicalRetryCount`：同任务历史失败重试次数

## 1.2 评分公式

```text
complexityScore =
  subTaskCount * 2 +
  dependencyDepth * 2 +
  toolTypeCount +
  (hasHardConstraint ? 2 : 0) +
  (inputTokenEstimate >= 3000 ? 1 : 0) +
  historicalRetryCount

uncertaintyScore =
  (needCitation ? 2 : 0) +
  (freshnessDays <= 30 ? 2 : 0)
```

## 1.3 路由判定（硬规则）

- 走 **Plan-Execute**（任一满足）：
  1. `complexityScore >= 10`
  2. `subTaskCount >= 4 && dependencyDepth >= 2`
  3. `hasHardConstraint == true && historicalRetryCount >= 1`
- 走 **ReAct**（同时满足）：
  1. `complexityScore < 10`
  2. `subTaskCount <= 3`
  3. `dependencyDepth <= 1`
- 兜底：默认 Plan-Execute（降低漏规划风险）

## 1.4 API 草案（可直接建 Controller）

### `POST /agent/decision/route`

请求：`AgentDecisionRequest`

响应：

```json
{
  "taskId": "agt_20260223_xxx",
  "decision": "PLAN_EXECUTE",
  "complexityScore": 13,
  "uncertaintyScore": 4,
  "matchedRules": ["R1_COMPLEXITY_GE_10"],
  "nextState": "PLANNING"
}
```

---

## 2. 外部搜索触发网关规则（Gateway）

目标：让“是否调用外部搜索”可审计、可限流、可回放。

## 2.1 决策状态机

```text
PENDING
  -> (规则评估) ALLOW | DENY | DEFER
ALLOW -> SEARCHING -> SEARCH_DONE | SEARCH_FAILED
DEFER -> WAITING_FOR_LOCAL_RETRY -> ALLOW | DENY
```

## 2.2 网关触发条件（满足任一即可 ALLOW）

1. `needCitation == true`
2. `freshnessDays <= 30`
3. 本地知识命中率 `< 0.6`（`localHitScore`）
4. 模型自评置信度 `< 0.75`（`modelConfidence`）
5. 用户问题含“最新/今天/近期/政策更新/财报”等时效关键词

## 2.3 强制拒绝条件（任一满足即 DENY）

1. 查询包含敏感数据原文（手机号、身份证、密钥等）
2. 相同 `queryHash` 在 10 分钟内重复且上次成功
3. 单任务调用次数超过阈值 `maxSearchPerTask=3`

## 2.4 API 与字段草案

### `POST /agent/search/gateway/evaluate`

请求：

```json
{
  "taskId": "agt_20260223_xxx",
  "query": "2026年中国AI Agent框架趋势",
  "needCitation": true,
  "freshnessDays": 7,
  "localHitScore": 0.42,
  "modelConfidence": 0.63,
  "hasSensitiveContent": false,
  "searchCountInTask": 1
}
```

响应：

```json
{
  "decision": "ALLOW",
  "reasonCodes": ["CITATION_REQUIRED", "LOW_LOCAL_HIT", "LOW_CONFIDENCE"],
  "maxSearchPerTask": 3,
  "remainingQuota": 2,
  "nextState": "SEARCHING"
}
```

### 建议新增表：`agent_search_log`

```sql
CREATE TABLE agent_search_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id VARCHAR(64) NOT NULL,
  query_hash VARCHAR(64) NOT NULL,
  query_text TEXT NOT NULL,
  decision VARCHAR(16) NOT NULL,
  reason_codes VARCHAR(256) NOT NULL,
  provider VARCHAR(32) NULL,
  latency_ms INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task_created (task_id, created_at),
  INDEX idx_query_hash (query_hash)
);
```

---

## 3. PDF/DOCX/CSV/Excel 统一解析数据模型草案

目标：统一“文件 -> 结构化数据集 -> Agent 可消费输入”，先解决可用性，再做高级语义。

## 3.1 统一对象模型

### `IngestFile`
- `fileId`：文件唯一ID
- `fileName`：原文件名
- `fileType`：`pdf|docx|csv|xlsx|xls`
- `storageUrl`：存储地址
- `parseStatus`：`uploaded|parsing|parsed|failed`
- `errorCode/errorMessage`

### `ParsedDataset`
- `datasetId`
- `fileId`
- `sourceType`：`table|text|mixed`
- `sheetName`：Excel Sheet名（CSV固定为 `default`）
- `rowCount/columnCount`
- `columns`：`List<ColumnSchema>`
- `rowsPreview`：预览前 N 行（建议 50）
- `fullDataRef`：完整数据引用（对象存储或DB分片）

### `ColumnSchema`
- `name`
- `index`
- `logicalType`：`string|number|date|boolean|currency|percentage`
- `nullableRatio`
- `uniqueRatio`
- `sampleValues`

### `DocumentChunk`（供文本推理）
- `chunkId`
- `datasetId`
- `pageNo`
- `paragraphNo`
- `content`
- `tokenCount`

## 3.2 建议数据库表（MVP 最小闭环）

```sql
CREATE TABLE ingest_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  file_name VARCHAR(256) NOT NULL,
  file_type VARCHAR(16) NOT NULL,
  storage_url VARCHAR(1024) NOT NULL,
  parse_status VARCHAR(16) NOT NULL DEFAULT 'uploaded',
  error_code VARCHAR(64) NULL,
  error_message TEXT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_created (user_id, created_at)
);

CREATE TABLE parsed_dataset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  file_id BIGINT NOT NULL,
  source_type VARCHAR(16) NOT NULL,
  sheet_name VARCHAR(128) NULL,
  row_count INT NOT NULL DEFAULT 0,
  column_count INT NOT NULL DEFAULT 0,
  schema_json JSON NOT NULL,
  preview_json JSON NULL,
  full_data_ref VARCHAR(1024) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_file (file_id)
);

CREATE TABLE document_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dataset_id BIGINT NOT NULL,
  page_no INT NULL,
  paragraph_no INT NULL,
  token_count INT NOT NULL DEFAULT 0,
  content MEDIUMTEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dataset (dataset_id)
);
```

## 3.3 统一解析 API 草案

### `POST /ingest/files`
- 入参：`multipart/form-data`（`file`）
- 出参：`fileId + parseStatus=uploaded`

### `POST /ingest/files/{fileId}/parse`
- 行为：按文件类型分流 parser
  - CSV/Excel：复用现有 `ExcelUtils` 能力，输出 `ParsedDataset`
  - PDF/DOCX：先抽文本 + 表格（MVP 可先文本优先，表格能力灰度）
- 出参：`datasetId + parseStatus=parsed`

### `GET /ingest/files/{fileId}/datasets`
- 返回 `ParsedDataset[]`（含 schema + preview）

## 3.4 与当前 `Chart` 流程的对接

当前 `chart.chartData` 存 CSV 文本，建议过渡策略：
1. 继续保留 `chartData`（兼容旧逻辑）
2. 新增可选字段 `datasetId`（关联 `parsed_dataset.id`）
3. Agent 生成优先使用 `datasetId`，无则降级到 `chartData`

建议 SQL（兼容改造）：

```sql
ALTER TABLE chart
  ADD COLUMN datasetId BIGINT NULL COMMENT '解析数据集ID（新链路）' AFTER chartData,
  ADD INDEX idx_dataset_id (datasetId);
```

---

## 4. Agent 执行状态机草案（可直接实现）

新增任务状态枚举 `AgentTaskStatusEnum`：

- `queued`：任务入队
- `deciding`：路由决策（ReAct/Plan-Execute）
- `planning`：仅 Plan-Execute 需要
- `acting`：工具调用中
- `searching`：外部搜索中
- `synthesizing`：汇总答案
- `succeed`
- `failed`
- `cancelled`

状态转移（约束）：

```text
queued -> deciding
  deciding -> planning | acting | failed
  planning -> acting | failed
  acting -> searching | synthesizing | failed
  searching -> acting | synthesizing | failed
  synthesizing -> succeed | failed
  queued/deciding/planning/acting/searching/synthesizing -> cancelled
```

与旧 `chart.status` 映射（向后兼容）：
- `queued/deciding/planning/acting/searching/synthesizing` -> `running`
- `succeed` -> `succeed`
- `failed/cancelled` -> `failed`

---

## 5. 第 1 周 MVP 可执行任务拆分（含验收标准）

> 目标：在 5 个工作日内交付“可提交 -> 可观察 -> 可验证”的 Agent Playground 最小闭环。

### Task W1-01：阶段枚举落地（后端状态机）

- 范围
  - 新增 `AgentTaskStatusEnum`：`queued/deciding/planning/acting/searching/synthesizing/succeed/failed/cancelled`
  - 在任务状态写入链路增加合法跳转校验（拒绝非法状态迁移）
  - 建立 `AgentTaskStatusEnum -> ChartStatusEnum` 映射，保证旧接口兼容
- 代码落点
  - `src/main/java/com/yupi/springbootinit/model/enums/AgentTaskStatusEnum.java`
  - `src/main/java/com/yupi/springbootinit/service/*TaskState*`（新增或改造状态流转服务）
  - `src/main/java/com/yupi/springbootinit/controller/ChartController.java`（返回子状态字段）
- 验收标准
  1. 单元测试覆盖全部允许/禁止的状态迁移（>= 12 条断言）
  2. `/chart/task/status` 响应新增 `taskPhase` 字段，旧字段 `status` 不回归
  3. 非法迁移返回明确错误码（如 `TASK_PHASE_TRANSITION_INVALID`）并写入日志

### Task W1-02：上下文集成（决策 + 搜索 + 数据集）

- 范围
  - 在任务上下文对象中收敛 `decision/search/dataset` 三类信息
  - 路由决策结果写入上下文：`decision/complexityScore/matchedRules`
  - 搜索网关结果写入上下文：`searchDecision/reasonCodes/remainingQuota`
  - 数据输入优先读取 `datasetId`，缺省时回退 `chartData`
- 代码落点
  - `src/main/java/com/yupi/springbootinit/model/dto/chart/ChartTaskContext.java`
  - `src/main/java/com/yupi/springbootinit/service/impl/ChartServiceImpl.java`
  - `src/main/java/com/yupi/springbootinit/controller/AgentDecisionController.java`
  - `src/main/java/com/yupi/springbootinit/controller/SearchGatewayController.java`
- 验收标准
  1. 任一异步任务都可在持久化记录中看到 `decision + reasonCodes + datasetId`
  2. `datasetId` 存在时不再读取 `chartData` 作为首选输入
  3. 至少 10 条样例请求回放可复现相同路由与搜索决策（幂等）

### Task W1-03：前端展示（任务阶段可视化）

- 范围
  - 在异步图表任务列表与详情页展示 `taskPhase`（中文标签 + 时间线）
  - 为 `searching/failed/cancelled` 增加差异化 UI 提示（图标与文案）
  - 轮询接口对接 `taskPhase` 与 `execMessage`，失败态支持展开错误原因
- 代码落点
  - `BI-front/src/pages/AddChart/index.tsx`
  - `BI-front/src/components/ChartTaskProgress/*`
  - `BI-front/src/services/chartController.ts`
- 验收标准
  1. 前端可正确展示 8 种阶段，且 `running` 期间能看到实时子阶段变化
  2. 失败任务展示“阶段 + 错误码 + 错误信息”，可复制问题定位文本
  3. 移动端与桌面端均不出现布局溢出（375px 与 1440px 基线验证）

### Task W1-04：契约测试（接口与前端联调基线）

- 范围
  - 为 `/agent/decision/route`、`/agent/search/gateway/evaluate`、`/chart/task/status` 补充契约测试
  - 固化关键字段类型、必填项与向后兼容字段（`status`）
  - 增加前端 mock 契约快照，防止字段漂移导致渲染失败
- 代码落点
  - `src/test/java/com/yupi/springbootinit/controller/AgentDecisionControllerContractTest.java`
  - `src/test/java/com/yupi/springbootinit/controller/SearchGatewayControllerContractTest.java`
  - `src/test/java/com/yupi/springbootinit/controller/ChartTaskStatusContractTest.java`
  - `BI-front/src/__tests__/contract/chart-task-status.contract.spec.ts`
- 验收标准
  1. 契约测试覆盖成功/失败/降级三类响应样例，每类 >= 2 条
  2. CI 中契约测试默认执行，失败时阻断合并
  3. 字段 `taskPhase/matchedRules/reasonCodes` 任一缺失时测试必然失败

### W1 日程排布（建议）

- Day 1：完成 W1-01 设计与后端枚举落地
- Day 2：完成 W1-02 的上下文对象与后端写链路
- Day 3：完成 W1-03 前端展示与轮询接入
- Day 4：完成 W1-04 契约测试 + 前后端联调
- Day 5：回归测试、修复、发布说明与验收走查

### 本周完成定义（DoD）

- 端到端流程可跑通：提交任务 -> 查看阶段推进 -> 成功/失败可解释
- 后端关键链路具备日志与测试护栏：状态迁移、决策路由、搜索网关
- 前端展示与接口契约一致，主流程无阻断缺陷

### 非本周范围（放入下一迭代）

- `searching/acting` 自动重试策略
- 100 并发压测与 P95 可观测性看板
- PDF/DOCX 表格结构化精度优化

---

## 6. 主要风险与应对

1. **PDF/DOCX 表格抽取质量波动**
   - 风险：结构化列识别不稳定
   - 应对：MVP 先“文本可靠优先”，表格抽取失败时降级为 chunk 文本

2. **外部搜索成本与时延不可控**
   - 风险：高峰期 latency 与费用上升
   - 应对：网关阈值 + 每任务配额 + query 去重缓存

3. **状态机与旧 chart 状态并存导致歧义**
   - 风险：前端显示与真实执行阶段不一致
   - 应对：提供映射函数 + 单一写入口（`AgentTaskStateService`）

4. **模型输出不稳定（ReAct 自由度高）**
   - 风险：工具调用链抖动
   - 应对：复杂任务优先 Plan-Execute，ReAct 仅在低复杂任务启用

---

## 7. 建议本次改造最小代码落点（BI 仓库）

- `src/main/java/com/yupi/springbootinit/controller/AgentDecisionController.java`
- `src/main/java/com/yupi/springbootinit/controller/SearchGatewayController.java`
- `src/main/java/com/yupi/springbootinit/controller/IngestController.java`
- `src/main/java/com/yupi/springbootinit/model/enums/AgentTaskStatusEnum.java`
- `src/main/java/com/yupi/springbootinit/model/dto/agent/*`
- `sql/create_table.sql`（追加新表与 `chart.datasetId`）

该草案可直接作为下一步实现任务拆分输入。
