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

依据：[S1–S2R 决策](decisions.md)。本文件定义待实现行为，不表示 API 已存在。场景期望由决策手工推导；S2R 不执行新 API。先看 [CSV 业务实例与现有源码关系](../required-headers-by-examples.md)，也可按 [现有端到端路径调试指南](../required-headers-by-running-e2e-test.md) 单步观察场景 10 的旧行为，再用本文件核对精确规则。

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

| 编号 | 规则                                                                                                                     | 决策    |
| -- | ---------------------------------------------------------------------------------------------------------------------- | ----- |
| R1 | 非空必需列核对真实表头。自动 `setHeader()` 读取首条有效记录；显式 `setHeader(names)` 只在 `skipHeaderRecord=true` 时读取并核对被跳过的首条有效记录。不能用显式声明代替真实表头。 | Q0、Q3 |
| R2 | 表头单元格先按现有 CSV 词法处理 `ignoreSurroundingSpaces` 与 `trim`，随后用最终 `ignoreHeaderCase` 配置比较；关闭的选项不额外折叠。缺失消息保留必需列原文。            | Q1、Q4 |
| R3 | 必需列各出现至少一次即满足；额外列与顺序不限。真实表头重复或空名称仍按既有 `DuplicateHeaderMode`、`allowMissingColumnNames` 处理；既有表头错误先于缺列错误。                 | Q2    |
| R4 | 非空必需列遇到空输入、无有效首条记录时，视为全部缺失。                                                                                            | Q3、Q4 |
| R5 | 缺列在解析器构造期抛 `IllegalArgumentException`，完整消息固定为 `Missing required CSV headers: [name1, name2]`；只列缺失列，顺序按调用者声明。           | Q4    |
| R6 | 功能关闭时维持既有解析与打印行为，包括没有表头、显式表头未跳过首行和缺列的输入。                                                                               | Q6、Q7 |
| R7 | 只核对列名存在；数据行空值、字段数、列位置及打印输出均不参与校验。                                                                                      | Q2、Q7 |

## 5. 构建期契约

| 编号 | 契约                                                                                         | 决策    |
| -- | ------------------------------------------------------------------------------------------ | ----- |
| C1 | `null` 数组与空数组关闭校验；未调用方法也关闭。                                                                | Q5、Q6 |
| C2 | 启用时拒绝 `null` 元素、空或全空白名称及重复名称；是否大小写等价按构建时最终 `ignoreHeaderCase` 决定，不受 Builder 调用顺序影响。        | Q1、Q5 |
| C3 | 启用非空必需列必须配置自动或显式表头；显式表头必须启用 `skipHeaderRecord=true`，且逻辑映射包含每个必需名称（按最终大小写配置比较）。不把数据首行误认作表头。 | Q0、Q3 |
| C4 | Builder 接收名称时防御性拷贝；格式对象的 `builder()`、`copy()` 保留配置；如有 getter，返回防御性拷贝。                      | Q5、Q6 |
| C5 | 必需列按声明顺序参与 `CSVFormat.equals()`、`hashCode()`、`toString()`；不同顺序的配置不相等。                      | Q6    |

## 6. 非目标

不校验数据行是否有值、字段数、列固定位置、上游额外列，也不约束 CSV 输出。不开启功能时不改变原有行为。本规格不规定构建期错误的具体消息措辞；构建期拒绝条件与异常类型由普通契约单测锁定。[Q6、Q7]

## 7. 验收标准

### 验收条件推导表

按 [Q9](decisions.md) 分三段读表：A 先判功能开关与构建配置；仅 A6/A7 进入 B，读取真实首条有效记录；仅 B3/B4 进入 C，先应用既有表头策略，再比较必需列。每列是一条规则；“—”表示该列走到动作时该条件无关，空格不用于省略条件。夹具的“路径”逐段记录命中的列。7.2 的普通单测在实现环节编写。

把三张表看成对账文件进入系统前的三道关卡：A 检查调用方是否打开校验、配置是否自洽；B 检查文件有没有可当表头的真实记录；C 检查这条记录是否合法、包含所需列。**沿一列从上往下读条件，命中动作后停止；只有“进入 B/C”才继续。**表中 A1、B1、C1 等编号是判定分支，不是 CSV 列号，也不是顺序执行的步骤。例如 `A6 → B3 → C2` 表示依次命中三张表中的一列。下面的例子是待实现行为的手工推导，不表示新 API 已运行。[Q12](decisions.md)

**A · 功能开关与构建期配置**（Q0、Q1、Q3、Q5、Q6；C1–C3、R6）

| 条件／动作        | A1       | A2 | A3 | A4 | A5 | A6                | A7       |
| ------------ | -------- | -- | -- | -- | -- | ----------------- | -------- |
| 非空必需列配置？     | 否        | 是  | 是  | 是  | 是  | 是                 | 是        |
| 名称参数有效？      | —        | 否  | 是  | 是  | 是  | 是                 | 是        |
| 表头模式         | —        | —  | 无  | 显式 | 显式 | 自动                | 显式       |
| 显式模式跳过首行？    | —        | —  | —  | 否  | 是  | —                 | 是        |
| 显式映射包含全部必需名？ | —        | —  | —  | —  | 否  | —                 | 是        |
| **动作：沿用旧行为** | ✓        |    |    |    |    |                   |          |
| **动作：构建期拒绝** |          | ✓  | ✓  | ✓  | ✓  |                   |          |
| **动作：进入 B**  |          |    |    |    |    | ✓                 | ✓        |
| **验收**       | 14、15；C1 | C2 | C3 | C3 | C3 | 01–08、11–13、16–18 | 09、10、19 |

**A 表条件行 → 当前生产代码入口**（以下行号以本轮仓库源码为准；`setRequiredHeaders` 尚未实现，因此链接指向现有配置与表头处理入口，不能把它们当作新增校验已存在的证据）：

| A 表条件行 | 现有文件和行号 | 当前代码可查到什么；尚待实现什么 |
| --- | --- | --- |
| 非空必需列配置？ | [`CSVFormat.java:240–250`](../../src/main/java/org/apache/commons/csv/CSVFormat.java#L240)、[`CSVFormat.java:324–325`](../../src/main/java/org/apache/commons/csv/CSVFormat.java#L324) | Builder 现有配置字段和 `get()` 建格式入口；当前没有 `requiredHeaders` 字段或 `setRequiredHeaders`。Q5/C1 要在此配置链新增“非空启用、`null`/空数组关闭”判定。 |
| 名称参数有效？ | [`CSVFormat.java:727–729`](../../src/main/java/org/apache/commons/csv/CSVFormat.java#L727)、[`CSVFormat.java:1616–1639`](../../src/main/java/org/apache/commons/csv/CSVFormat.java#L1616)、[`CSVFormat.java:1459`](../../src/main/java/org/apache/commons/csv/CSVFormat.java#L1459) | `setIgnoreHeaderCase` 保存选项，构造函数复制最终配置并调用 `validate()`，`isBlank` 是现有空白判断入口。Q1/Q5/C2 的必需名非空、非重复及最终大小写判重尚待加入构建期校验；现有 `validate()` 未校验必需名。 |
| 表头模式 | [`CSVFormat.java:621–624`](../../src/main/java/org/apache/commons/csv/CSVFormat.java#L621)、[`CSVParser.java:604–621`](../../src/main/java/org/apache/commons/csv/CSVParser.java#L604) | `setHeader(null)` 表示无表头、`setHeader()` 的空数组表示自动、非空数组表示显式；`createHeaders()` 现按此分支读取或使用表头。Q0/Q3/C3 尚需在启用必需列时于构建期排除无表头模式。 |
| 显式模式跳过首行？ | [`CSVFormat.java:861–864`](../../src/main/java/org/apache/commons/csv/CSVFormat.java#L861)、[`CSVParser.java:615–621`](../../src/main/java/org/apache/commons/csv/CSVParser.java#L615) | `setSkipHeaderRecord` 保存布尔值；当前显式模式仅在值为 `true` 时消费文件首行。Q3/C3 尚需在启用必需列且显式模式时，于构建期拒绝 `false`。 |
| 显式映射包含全部必需名？ | [`CSVFormat.java:621–624`](../../src/main/java/org/apache/commons/csv/CSVFormat.java#L621)、[`CSVParser.java:621–646`](../../src/main/java/org/apache/commons/csv/CSVParser.java#L621)、[`CSVParser.java:588–591`](../../src/main/java/org/apache/commons/csv/CSVParser.java#L588) | `setHeader(names)` 保存本地名称，`createHeaders()` 当前以该数组建立名称映射；映射的大小写行为取自最终格式。Q3/C3 尚需在构建期比较显式映射与必需名；即使映射齐全，也不能代替 B/C 对真实首行的核验。 |

**最末行“验收”怎样覆盖条件与动作：**它指向观测该列**整条规则**的夹具或 7.2 普通契约单测，不是把“验收”当成第六个输入条件。譬如 A2：配置 `setHeader()`、`setRequiredHeaders("currency", "currency")`，名称参数因重复而无效；[7.2 的 C2](#72-接口契约--普通-junit-契约单测) 要断言 `Builder#get()` 在构建期拒绝。这里的 **C2 是“构建期契约 C2”**，不是后面 C 表的“C2 通过表头校验”分支。A3/A4/A5 分别用 C3 断言无表头、显式未跳首行、显式映射缺名均构建期拒绝。A1 用 [场景 14](../../src/test/resources/org/apache/commons/csv/approved/14-disabled-auto.approved.md)/15 和 C1 断言关闭后沿用旧行为；A6 用 [场景 01](../../src/test/resources/org/apache/commons/csv/approved/01-auto-complete.approved.md) 断言自动模式能进入 B3/C2；A7 用 [场景 10](../../src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.approved.md) 断言显式映射齐全仍进入 B4/C3 并发现真实缺列。A2–A5 的错误配置没有业务 CSV 夹具，因其在读文件前就终止。上述新 API 断言均为后续实现环节的验收计划，当前仍由规格与夹具手工推导。[Q13](decisions.md)

名称有效性按构建时最终 `ignoreHeaderCase` 判断，setter 顺序无关；空数组、null 数组属于 A1。A4 已终止，无须再问映射是否齐全。A5 仅在跳过首行后判定映射。A1 的 `header`、`skipHeaderRecord` 组合沿用旧行为。

**怎样从决定推到 A：**Q5/Q6 先给开关语义：没有非空必需列就走 A1，像未启用门禁，文件照旧解析；[场景 14](../../src/test/resources/org/apache/commons/csv/approved/14-disabled-auto.approved.md) 的 `account_id,amount` 即使没有 `currency` 也通过。启用后，Q1/Q5 要求名称有效，所以无效名落 A2；Q0/Q3 要求有可核对的真实表头来源，于是无表头落 A3，显式表头却不跳首行落 A4，显式映射缺必需名落 A5。这四类配置在 `Builder#get()` 时拒绝，尚未读 CSV。配置有效且使用 `setHeader()` 自动模式，走 A6；使用 `setHeader("account_id", "currency")` 并跳过首行，且映射包含必需名 `currency`，走 A7。A7 仅证明**本地映射**齐全，不能证明上游文件有 `currency`；[场景 10](../../src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.approved.md) 因此还要进入 B、C。

**B · 真实首条有效记录**（Q0、Q3、Q4；R1、R4、R5）

| 条件／动作            | B1    | B2      | B3                | B4      |
| ---------------- | ----- | ------- | ----------------- | ------- |
| 从 A 进入的表头模式      | 自动    | 显式且跳过首行 | 自动                | 显式且跳过首行 |
| 有真实首条有效记录？       | 否     | 否       | 是                 | 是       |
| **动作：报告全部必需列缺失** | ✓     | ✓       |                   |         |
| **动作：进入 C**      |       |         | ✓                 | ✓       |
| **验收**           | 13、17 | 19      | 01–08、11、12、16、18 | 09、10   |

自动模式取首条有效记录，显式模式取被跳过的真实记录；注释和空行仍按原有 CSV 格式处理。B1/B2 在解析器构造期抛 R5 的固定消息。

**怎样从决定推到 B：**Q0/Q3 指定要看上游的真实首行，Q4 要求在解析器构造期报告缺失。把 B 想成验收员打开箱子找清单：自动模式从首条有效记录取清单；显式且跳首行模式从**被跳过的文件记录**取清单，本地声明不能代替它。若箱子是空的，[场景 13](../../src/test/resources/org/apache/commons/csv/approved/13-empty-input.approved.md) 从 A6 落 B1；若显式模式遇空文件，[场景 19](../../src/test/resources/org/apache/commons/csv/approved/19-explicit-empty-input.approved.md) 从 A7 落 B2。两者都把配置的 `currency,amount` 全部列为缺失：`Missing required CSV headers: [currency, amount]`。有真实记录则自动模式落 B3、显式模式落 B4，继续查记录内容；`account_id,amount` 虽缺 `currency`，仍**有**首条记录，[场景 10](../../src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.approved.md) 因而是 B4，不是 B2。

**C · 表头策略、名称匹配与结果**（Q1、Q2、Q4、Q7；R2、R3、R5、R7）

| 条件／动作              | C1 | C2                         | C3          |
| ------------------ | -- | -------------------------- | ----------- |
| 既有重复/空表头策略允许真实表头？  | 否  | 是                          | 是           |
| 按最终格式处理后，全部必需名出现？  | —  | 是                          | 否           |
| **动作：先抛既有表头错误**    | ✓  |                            |             |
| **动作：通过表头校验**      |    | ✓                          |             |
| **动作：解析器构造期报告缺失列** |    |                            | ✓           |
| **验收**             | 12 | 01、03、04、06、07、09、11、16、18 | 02、05、08、10 |

C 的第二行按现有 CSV 词法处理 `ignoreSurroundingSpaces`、`trim`，再按最终 `ignoreHeaderCase` 比较；缺失列按声明顺序和原文报告。03 覆盖额外列及换序，11/18 覆盖允许的重复，12 覆盖旧错误优先，04–08 覆盖匹配选项。数据行值、字段数、固定列位及打印不进入 C 判定，由 R7 普通单测覆盖。

**怎样从决定推到 C：**先按 Q2 让既有表头策略验货，再按 Q1/Q4 对照必需名清单。譬如必需名为 `amount`、文件首行为 `account_id,currency,currency` 且禁止重复，[场景 12](../../src/test/resources/org/apache/commons/csv/approved/12-duplicate-rejected-first.approved.md) 命中 C1，先报旧的重复列错误；此时第二个条件是“—”，不再比较 `amount`。若策略允许，再检查词法处理和最终大小写设置后的名称：[场景 01](../../src/test/resources/org/apache/commons/csv/approved/01-auto-complete.approved.md) 的 `account_id,currency,amount` 三项都在，走 C2；额外列、换序或被允许的重复列仍可走 C2。若文件首行为 `account_id,amount`、必需名为 `currency`，[场景 10](../../src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.approved.md) 走 C3，在解析器构造期报告 `Missing required CSV headers: [currency]`。数据行 `A-001,12.50` 不能补足缺失表头；即使 C2 通过，也不在本关卡检查数据行值或列位。[Q12](decisions.md)

**横向契约与覆盖核对**（Q5–Q9）

| 条件或保证                | 验收                                 | 路径关系              |
| -------------------- | ---------------------------------- | ----------------- |
| 关闭、重新开启、参数拒绝及最终大小写判重 | C1、C2 普通单测                         | A1/A2，不改变 B/C     |
| 无表头、显式未跳首行、显式映射缺名    | C3 普通单测                            | A3/A4/A5，构建期终止    |
| 防御性拷贝、格式拷贝、按序值语义     | C4、C5 普通单测                         | 对 A6/A7 生成的格式独立检查 |
| 解析器构造期错误及完整消息        | R1、R5 普通单测；02、05、08、10、13、17、19 夹具 | B1/B2/C3          |
| 关闭时旧解析与打印行为；启用后仅校验列名 | 14、15 夹具；R7 普通单测                   | A1 或 C2 后的横向保证    |

覆盖核对：A1–A7、B1–B4、C1–C3 均有业务夹具或 7.2 契约落点；不为独立选项做完整笛卡尔积。显式映射与真实表头**顺序不一致**时，Q2 只保证必需列存在，不保证按显式名称读取的数据仍对应物理列位；这是 Q7 的位置非目标，实施前需业务方确认是否接受这一既定边界。构建期错误的具体消息仍未定，7.2 只锁定拒绝条件和异常类型。

### 7.1 业务事实 → Approved Scenarios 夹具

以下每行对应 `src/test/resources/org/apache/commons/csv/approved/NN-*.approved.md`。每份夹具写明真实 CSV、与本功能有关的配置、实际表头与记录或异常类型及完整消息。`docs/business-facts.md` 由这些夹具生成；[实例讲解](../required-headers-by-examples.md)把代表场景接到现有生产代码入口。

| 场景 | 业务情境与判断                         | 规则       |
| -- | ------------------------------- | -------- |
| 01 | 自动表头齐全，对账数据进入处理。                | R1、R7    |
| 02 | 上游同时少两列，创建解析器时一次告知。             | R1、R5    |
| 03 | 上游增加列并调整顺序，仍可处理。                | R3       |
| 04 | 开启大小写忽略后，不同大小写视为同一列。            | R2       |
| 05 | 未开启大小写忽略时，大小写不同算缺列。             | R2、R5    |
| 06 | 开启周围空白忽略后，未引用表头的周围空白不妨碍匹配。      | R2       |
| 07 | 开启 trim 后，引号内表头值两侧空白也可匹配。       | R2       |
| 08 | 未开启空白处理时，带空白的列名不能冒充必需列。         | R2、R5    |
| 09 | 显式映射且跳过首行，真实表头齐全时通过。            | R1       |
| 10 | 显式映射齐全但文件首行缺列，仍拒绝。              | R1、R5    |
| 11 | 允许重复列时，必需列出现两次仍算存在。             | R3       |
| 12 | 禁止重复列时，既有重复表头错误先报。              | R3       |
| 13 | 空文件报告全部必需列缺失。                   | R4、R5    |
| 14 | 功能关闭后，自动表头缺列仍按旧行为解析。            | R6       |
| 15 | 功能关闭后，显式映射不跳首行仍把首行当数据。          | R6       |
| 16 | 允许空表头名称时，别的必需列仍可满足。             | R3       |
| 17 | 文件只有注释，没有有效首条记录时报告全部缺列。         | R4、R5    |
| 18 | ALLOW_EMPTY 允许重复空列名，已出现的必需列仍通过。 | R3       |
| 19 | 显式表头且跳过首行，空文件也报告全部必需列缺失。        | R1、R4、R5 |

场景 10 的原样 CSV 另存为 [可执行测试数据](../../src/test/resources/org/apache/commons/csv/e2e/reconciliation-missing-currency.csv)，供 [现有路径端到端测试](../../src/test/java/org/apache/commons/csv/RequiredHeadersExistingFlowE2ETest.java) 读取并做“账户 → 币种”归集。该测试只表征未启用新功能时显式映射会把金额当币种；场景 10 的新异常期望仍是手工推导，尚待后续执行器验证。[Q11](decisions.md)

### 7.2 接口契约 → 普通 JUnit 契约单测

后续实现环节添加普通 JUnit 契约单测，不在 S2 调用新 API。建议按下表逐项覆盖；7.1 场景留给 Approved Scenarios 执行器。

| 契约    | 必测断言                                                   | 决策    |
| ----- | ------------------------------------------------------ | ----- |
| C1    | 未调用、`null` 数组、空数组均关闭校验；重新调用可切换开关。                      | Q5、Q6 |
| C2    | `null` 元素、空名、全空白名、重复名拒绝；大小写等价随最终配置改变，交换 setter 顺序结果相同。 | Q1、Q5 |
| C3    | 无表头拒绝；显式表头未跳过首行拒绝；显式映射缺必需名拒绝；最终大小写配置适用于映射检查。           | Q0、Q3 |
| C4    | 修改传入数组或 getter 返回数组不影响格式；`builder()`、`copy()` 保留要求。    | Q5、Q6 |
| C5    | 相同配置相等且 hash 相同；增删或调换顺序改变相等性；`toString()` 按顺序展示。       | Q6    |
| R1、R5 | 异常发生在解析器构造期，早于读取数据；场景夹具的消息在实现后由执行器逐字核对。                | Q0、Q4 |
| R7    | 必需列表头下，空值、短记录及打印均不触发必需列表头错误。                           | Q7    |
