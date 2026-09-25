<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# S1–S2R · Required headers 决策记录

本文件只记录需求决定，不是实现方案或测试规格。Q0 由用户直接决定；Q1–Q7 对应用户在 S1 第 2 轮交给 Agent 判断的 M2–M7，以及原需求明确的校验时机。Agent 代选理由列于下表；后续环节可据此复核。

| 问题 | 决定 | 理由 |
| --- | --- | --- |
| Q0：校验真实首行还是解析器逻辑表头？ | 校验上游 CSV 文件的真实首条记录所载表头，而非仅检查 `setHeader(String...)` 声明。 | 对账目标是发现上游缺列；显式表头只是本地映射，不能证明上游文件有该列。用户接受此建议。 |
| Q1（M2）：大小写、空白怎样匹配？ | 用现有 CSV 词法和映射语义。物理首行先按 `ignoreSurroundingSpaces`、`trim` 解析，再按 `ignoreHeaderCase` 与必需列名比较；关闭相应选项时不额外折叠大小写或空白。必需列名保持调用者给出的原文。 | 使新校验与同一格式的列名访问尽可能一致，不引入独立的“近似匹配”规则。用户要求沿用现有设置。 |
| Q2（M3）：额外列、顺序、重复列和空列名怎样处理？ | 必需列只需各出现至少一次；允许额外列和任意顺序。真实首行的重复列与空列名遵守现有 `DuplicateHeaderMode`、`allowMissingColumnNames`；原有表头错误优先于“缺必需列”。若当前模式允许重复，重复出现的必需列视为存在，不新增“恰好出现一次”要求。 | 避免把“必需列存在”偷换成完整 schema 或唯一性校验；沿用既有表头策略，减少意外规则。 |
| Q3（M4）：自动、显式、无表头和空输入怎样处理？ | `setHeader()` 自动模式检查读到的首条记录；显式 `setHeader(names)` 仅在 `skipHeaderRecord=true` 时检查被跳过的真实首条记录。显式映射还须包含每个必需名称，否则构建时拒绝，保证后续可按该名称访问。启用非空必需列却未配置表头，或显式表头配 `skipHeaderRecord=false`，构建时拒绝；不得把数据首行误认作表头。空输入、无有效首条记录时，解析器创建阶段报告全部必需列缺失。 | 自动模式天然有真实表头；显式模式只有跳过首行时才明确把它视作上游表头。配置错误提前暴露，空输入不能静默通过。 |
| Q4（M5）：何时抛错、何种异常与消息？ | 在 `CSVParser` 构造期间完成真实表头校验，早于向调用者返回首条数据记录。沿用现有表头错误的 `IllegalArgumentException`。消息使用 `Missing required CSV headers: [currency, amount]` 形式，一次列出全部缺失列，按调用者声明顺序排列；保留调用者原始拼写。 | 调用方能直接定位缺列；既有表头校验也使用 `IllegalArgumentException`；稳定顺序利于诊断。 |
| Q5（M6）：`setRequiredHeaders(String...)` 参数怎样处理？ | `null` 数组或空数组表示关闭校验。启用时拒绝 `null`、空白名称和重复名称；大小写等价是否重复，依 `ignoreHeaderCase` 的最终配置判定。构建格式时完成校验；必需列数组做防御性拷贝，不允许外部后续修改。 | 与 `setHeader(null)` 的关闭语义相近；构建期识别调用者错误；最终配置可能受 Builder 调用顺序影响。 |
| Q6（M7）：格式对象 API 契约怎样处理？ | 非空必需列配置随 `builder()`、`copy()` 保留；参与 `CSVFormat.equals()`、`hashCode()`、`toString()`，按声明顺序比较和展示。若提供 getter，返回防御性拷贝。未启用时保持现有解析与打印行为。 | `CSVFormat` 是不可变配置值；遗漏拷贝或值语义会导致解析器构造时丢失要求，或格式比较结果错误。 |
| Q7：明确不做什么？ | 不校验每行的值是否为空、每行字段数是否足够、列的固定位置、上游额外列、CSV 输出内容；不改变未启用功能时的解析行为。 | 本次目标仅是解析前识别上游缺少必需表头。 |

## S2R · 验收推导决定

| 问题 | 决定 | 理由 |
| --- | --- | --- |
| Q8：怎样证明 7.1 与 7.2 从 Q0–Q7 推导且没有遗漏？ | 先按功能开关、表头来源、真实首行状态、名称匹配、既有表头策略、构建期配置与对象值语义分支，再逐分支映射到业务场景或普通 JUnit 契约。业务可观察的输入和结果用 Approved Scenarios；纯 Builder/值对象契约及数据行非目标用普通单测。每个场景标注 R 编号，规格给出“问题 → 规则/契约 → 验收”矩阵和剩余边界。 | 用户在 S2R 要求看清每个验收条件的来由与遗漏；矩阵能显式暴露无验收落点的决定，也避免为所有配置笛卡尔积制造重复样例。这是验收组织决定，不改变 Q0–Q7 的业务语义。 |
| Q9：验收推导如何呈现判定顺序？ | 采用串接决策表：A 判功能开关与构建配置，B 判真实首行来源及存在性，C 判既有表头策略与必需列匹配。每列是一条互斥规则；“—”表示后续条件无关；终止列直接给出动作，通过列进入下一张表。夹具记录所走 A/B/C 路径，生成的业务事实同步展示；对象值语义和数据行非目标作为横向契约单列。 | 用户提供的 Decision-table.md 以条件行、规则列、动作行和 don't-care 单元格表达决策。分阶段表比把所有输入做笛卡尔积更容易核查，也能直接看到哪条路径缺少验收落点。只改变规格和追溯格式，不改变 Q0–Q7 的行为。 |
| Q10：怎样用业务实例解释规格与现有生产代码的关系？ | 用少量代表性 CSV 贯穿自动表头、显式映射、匹配选项、既有表头错误和关闭/空输入；逐例分开写当前代码行为与拟新增行为，链接现有源码入口、规则与 Approved Scenario。另用一张索引覆盖规格的 API、R/C、非目标和验收，避免把说明文档写成第二份独立规格。 | 用户要求能从真实业务输入看懂需求与源码关系。只增加解释与追溯，不变更 Q0–Q7 的行为或任何夹具期望。 |
| Q11：如何单步观察最接近新需求的现有业务路径？ | 新增一条只调用现有 API 的文件到业务归集测试：以与场景 10 相同的真实 CSV 文件为输入，显式映射 `account_id,currency` 且跳过真实首行，再用 `record.get("currency")` 归集“账户 → 币种”。测试锁定当前把 `amount` 值映射成 `currency` 的行为，作为 Debug 入口；它不验证新功能。为这份纯数据 CSV 按仓库 RAT 约定加精确排除。 | 现有 `CSVFileParserTest` 读文件却不做按名业务处理，`CSVParserTest` 按名读取却用内联字符串；单条可调试用例更能说明 Q0 为什么必须检查真实首行。新测试的数据和断言只表征旧代码；场景 10 的拟新增异常仍由规格手工推导。 |
| Q12：怎样解释 A/B/C 三张验收表的推导？ | 在规格每张表后写“上游决定 → 条件分支 → 动作”的读表示例；以功能关闭、空文件、显式映射掩盖缺列、重复表头错误优先及正常通过等既有场景演示路径。代表夹具的业务说明写明命中条件与手工推导的动作；不改 CSV、配置、期望结果及 Q0–Q7 语义。 | 评审需要理解每列为何存在、何时停止及为何进入下一张表；实例和关卡比喻帮助把抽象判定路径连到真实 CSV，又避免把说明误当成新 API 的运行结果。 |
| Q13：怎样让 A 表条件和“验收”可查证？ | 在规格 A 表后逐条件标出当前生产代码的文件及行号，同时明确 `setRequiredHeaders` 尚未实现；把 A 表“验收”解释为整列条件与动作的观察点。A1、A6、A7 用代表性业务夹具说明，A2–A5 用 7.2 的 C2/C3 构建期契约单测规划说明，特别区分契约 C2 与决策表 C2。 | 现有源码只提供 Builder、表头模式和解析器映射入口，不能虚称新增校验已有源码行号；构建错误在读取 CSV 前发生，宜用普通单测验证，而业务夹具保留手工推导的 CSV 结果。 |

## 假设体检结果

| 编号 | 初始假设 | 结论 |
| --- | --- | --- |
| A1 | 只校验解析输入，不约束打印。 | 确认：Q7。 |
| A2 | “必需”只指列名存在，不要求各行非空。 | 确认：Q7。 |
| A3 | 必需列是子集，额外列和顺序可变。 | 确认：Q2。 |
| A4 | 缺列在读取首条数据记录前报告。 | 确认并收紧为解析器构造期：Q4。 |
| A5 | 未启用时现有行为保持。 | 确认：Q6、Q7。 |
| A6 | 多个缺失列一次报告。 | 确认：Q4。 |

## 仓库事实与样例边界

当前仓库是 Commons CSV 库，没有对账业务的调用配置或数据文件；不能从仓库确认用户实际使用的 Builder 链。下面是**示意**，不是用户现有配置，也不是已实现代码：

```java
CSVFormat format = CSVFormat.DEFAULT.builder()
    .setHeader()
    .setRequiredHeaders("account_id", "currency", "amount")
    .get();
```

```csv
account_id,amount
A-001,12.50
```

按以上决定，示意输入缺少 `currency`，应在 `CSVParser` 构造期间报 `Missing required CSV headers: [currency]`。

源码依据：`CSVFormat.Builder#setHeader(String...)` 接受自动或显式表头；`CSVParser#createHeaders()` 在构造期读取自动表头，显式表头仅在 `skipHeaderRecord=true` 时消费文件首条记录；大小写映射使用 `String.CASE_INSENSITIVE_ORDER`；现有表头错误使用 `IllegalArgumentException`。参见 `src/main/java/org/apache/commons/csv/CSVFormat.java`、`CSVParser.java`、`Lexer.java` 与 `CSVRecord.java`。
