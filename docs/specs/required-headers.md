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

# Required headers 规格草案

依据：[S1–S2R 决策](decisions.md)。本文件定义待实现行为，不表示 API 已存在。场景期望由决策手工推导；S2R 不执行新 API。

## 1. 问题陈述

对账文件缺少必需列时，调用方目前要等到 `record.get("currency")` 才知道映射缺失。新增解析前校验：从上游文件真实首条记录取得表头，在 `CSVParser` 构造期间一次报告所有缺失列，避免错误数据进入业务流程。业务事实以产品经理可审阅的 CSV 夹具固化。

## 2. 术语

- **真实表头**：CSV 输入的首条有效记录。注释、空行按现有解析配置处理；不是 `setHeader(String...)` 声明。
- **逻辑表头**：解析器供 `record.get(name)` 使用的名称映射。自动模式取真实表头；显式模式取调用者声明。
- **必需列**：调用者指定必须出现在真实表头中的列名，按声明顺序保留。
- **功能关闭**：`setRequiredHeaders(null)`、无参调用或从未调用该方法，均无必需列要求。
- **构建期**：调用 `CSVFormat.Builder#get()` 创建格式时。**解析器构造期**：调用 `CSVFormat#parse` 或等价入口创建 `CSVParser` 时，早于获取首条数据记录。

## 3. API

`CSVFormat.Builder#setRequiredHeaders(String... names)` 配置必需列并返回 Builder；通过 `get()` 生成格式。`CSVFormat` 可提供 `getRequiredHeaders()`，若提供则返回数组防御性拷贝。配置必须参与 `builder()`、`copy()`、`equals()`、`hashCode()`、`toString()`。不引入新的异常类型。[Q5、Q6]

## 4. 解析规则

| 编号 | 规则 | 决策 |
| --- | --- | --- |
| R1 | 非空必需列核对真实表头。自动 `setHeader()` 读取首条有效记录；显式 `setHeader(names)` 只在 `skipHeaderRecord=true` 时读取并核对被跳过的首条有效记录。不能用显式声明代替真实表头。 | Q0、Q3 |
| R2 | 表头单元格先按现有 CSV 词法处理 `ignoreSurroundingSpaces` 与 `trim`，随后用最终 `ignoreHeaderCase` 配置比较；关闭的选项不额外折叠。缺失消息保留必需列原文。 | Q1、Q4 |
| R3 | 必需列各出现至少一次即满足；额外列与顺序不限。真实表头重复或空名称仍按既有 `DuplicateHeaderMode`、`allowMissingColumnNames` 处理；既有表头错误先于缺列错误。 | Q2 |
| R4 | 非空必需列遇到空输入、无有效首条记录时，视为全部缺失。 | Q3、Q4 |
| R5 | 缺列在解析器构造期抛 `IllegalArgumentException`，完整消息固定为 `Missing required CSV headers: [name1, name2]`；只列缺失列，顺序按调用者声明。 | Q4 |
| R6 | 功能关闭时维持既有解析与打印行为，包括没有表头、显式表头未跳过首行和缺列的输入。 | Q6、Q7 |
| R7 | 只核对列名存在；数据行空值、字段数、列位置及打印输出均不参与校验。 | Q2、Q7 |

## 5. 构建期契约

| 编号 | 契约 | 决策 |
| --- | --- | --- |
| C1 | `null` 数组与空数组关闭校验；未调用方法也关闭。 | Q5、Q6 |
| C2 | 启用时拒绝 `null` 元素、空或全空白名称及重复名称；是否大小写等价按构建时最终 `ignoreHeaderCase` 决定，不受 Builder 调用顺序影响。 | Q1、Q5 |
| C3 | 启用非空必需列必须配置自动或显式表头；显式表头必须启用 `skipHeaderRecord=true`，且逻辑映射包含每个必需名称（按最终大小写配置比较）。不把数据首行误认作表头。 | Q0、Q3 |
| C4 | Builder 接收名称时防御性拷贝；格式对象的 `builder()`、`copy()` 保留配置；如有 getter，返回防御性拷贝。 | Q5、Q6 |
| C5 | 必需列按声明顺序参与 `CSVFormat.equals()`、`hashCode()`、`toString()`；不同顺序的配置不相等。 | Q6 |

## 6. 非目标

不校验数据行是否有值、字段数、列固定位置、上游额外列，也不约束 CSV 输出。不开启功能时不改变原有行为。本规格不规定构建期错误的具体消息措辞；构建期拒绝条件与异常类型由普通契约单测锁定。[Q6、Q7]

## 7. 验收标准

### 验收条件推导表

推导顺序：先判功能是否启用；启用后判配置能否构建；可构建时判真实表头来源和是否存在；再判现有表头策略、列名匹配及缺列；最后检查格式对象契约和明确的非目标。各分支来自 [Q0–Q8](decisions.md)，不是实现代码的行为倒推。下表的“普通单测”指 7.2 待实现环节的契约测试；同一场景可覆盖多个决定。

| 决策问题 | 条件分支 → 可观察结论 | 规则/契约 | 验收落点 |
| --- | --- | --- | --- |
| Q0、Q3 | 启用且自动表头 → 用真实首条有效记录；启用且显式表头并跳过首行 → 用被跳过的真实记录，不信任显式声明。 | R1、C3 | 01、02、09、10；C3 普通单测 |
| Q3、Q4 | 启用但空文件或只有注释 → 没有有效首条记录，全部必需列缺失。 | R4、R5 | 13、17 |
| Q0、Q3、Q5 | 启用却无表头、显式表头不跳首行或显式映射缺必需名 → 构建期拒绝。 | C3 | C3 普通单测 |
| Q1、Q4 | 大小写忽略开/关 → 等价名称通过/严格不匹配报缺列；周围空白与 trim 开/关 → 按现有词法处理。 | R2、R5 | 04–08；C2 普通单测覆盖最终大小写配置 |
| Q2 | 必需列只是子集 → 额外列和顺序可变；允许重复 → 出现一次即算存在。 | R3 | 03、11 |
| Q2 | 真实表头重复/空名 → 分别按 DISALLOW、ALLOW_EMPTY、allowMissingColumnNames 的既有规则；原有错误优先。 | R3 | 12、16、18 |
| Q3、Q4 | 必需列缺失 → 构造解析器时抛固定类型和消息；多个缺失按声明顺序一次报告。 | R5 | 02、05、08、10、13、17；R1/R5 普通单测锁定时机 |
| Q5 | 参数为 null 数组或空数组 → 关闭；启用时有 null、空白、重复名称 → 构建期拒绝，按最终 ignoreHeaderCase 判重。 | C1、C2 | C1、C2 普通单测 |
| Q5、Q6 | 必需列数组防御性拷贝；格式拷贝和按序值语义保持。 | C4、C5 | C4、C5 普通单测 |
| Q6、Q7 | 功能关闭 → 旧解析与打印行为；启用后仍只看列名存在，数据行值和字段数不校验。 | R6、R7 | 14、15；R7 普通单测 |
| Q8 | 每个业务可判的分支进 7.1；配置、对象语义与非目标进 7.2。 | 验收组织 | 本表、下方 7.1/7.2 |

覆盖边界：不为选项的全部排列组合逐一造样例；每个独立决定至少有一个可观察落点。显式映射与真实表头**顺序不一致**时，Q2 只保证必需列存在，不保证按显式名称读取的数据仍对应物理列位；这是 Q7 的位置非目标，实施前需业务方确认是否接受这一既定边界。构建期错误的具体消息仍未定，7.2 只锁定拒绝条件和异常类型。

### 7.1 业务事实 → Approved Scenarios 夹具

以下每行对应 `src/test/resources/org/apache/commons/csv/approved/NN-*.approved.md`。每份夹具写明真实 CSV、与本功能有关的配置、实际表头与记录或异常类型及完整消息。`docs/business-facts.md` 由这些夹具生成。

| 场景 | 业务情境与判断 | 规则 |
| --- | --- | --- |
| 01 | 自动表头齐全，对账数据进入处理。 | R1、R7 |
| 02 | 上游同时少两列，创建解析器时一次告知。 | R1、R5 |
| 03 | 上游增加列并调整顺序，仍可处理。 | R3 |
| 04 | 开启大小写忽略后，不同大小写视为同一列。 | R2 |
| 05 | 未开启大小写忽略时，大小写不同算缺列。 | R2、R5 |
| 06 | 开启周围空白忽略后，未引用表头的周围空白不妨碍匹配。 | R2 |
| 07 | 开启 trim 后，引号内表头值两侧空白也可匹配。 | R2 |
| 08 | 未开启空白处理时，带空白的列名不能冒充必需列。 | R2、R5 |
| 09 | 显式映射且跳过首行，真实表头齐全时通过。 | R1 |
| 10 | 显式映射齐全但文件首行缺列，仍拒绝。 | R1、R5 |
| 11 | 允许重复列时，必需列出现两次仍算存在。 | R3 |
| 12 | 禁止重复列时，既有重复表头错误先报。 | R3 |
| 13 | 空文件报告全部必需列缺失。 | R4、R5 |
| 14 | 功能关闭后，自动表头缺列仍按旧行为解析。 | R6 |
| 15 | 功能关闭后，显式映射不跳首行仍把首行当数据。 | R6 |
| 16 | 允许空表头名称时，别的必需列仍可满足。 | R3 |
| 17 | 文件只有注释，没有有效首条记录时报告全部缺列。 | R4、R5 |
| 18 | ALLOW_EMPTY 允许重复空列名，已出现的必需列仍通过。 | R3 |

### 7.2 接口契约 → 普通 JUnit 契约单测

后续实现环节添加普通 JUnit 契约单测，不在 S2 调用新 API。建议按下表逐项覆盖；7.1 场景留给 Approved Scenarios 执行器。

| 契约 | 必测断言 | 决策 |
| --- | --- | --- |
| C1 | 未调用、`null` 数组、空数组均关闭校验；重新调用可切换开关。 | Q5、Q6 |
| C2 | `null` 元素、空名、全空白名、重复名拒绝；大小写等价随最终配置改变，交换 setter 顺序结果相同。 | Q1、Q5 |
| C3 | 无表头拒绝；显式表头未跳过首行拒绝；显式映射缺必需名拒绝；最终大小写配置适用于映射检查。 | Q0、Q3 |
| C4 | 修改传入数组或 getter 返回数组不影响格式；`builder()`、`copy()` 保留要求。 | Q5、Q6 |
| C5 | 相同配置相等且 hash 相同；增删或调换顺序改变相等性；`toString()` 按顺序展示。 | Q6 |
| R1、R5 | 异常发生在解析器构造期，早于读取数据；场景夹具的消息在实现后由执行器逐字核对。 | Q0、Q4 |
| R7 | 必需列表头下，空值、短记录及打印均不触发必需列表头错误。 | Q7 |
