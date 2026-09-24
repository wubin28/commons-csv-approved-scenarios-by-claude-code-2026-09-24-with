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

# S1 · Required headers 决策记录

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
