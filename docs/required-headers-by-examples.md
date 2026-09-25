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

# 用 CSV 实例理解 Required Headers

本文解释 [规格](specs/required-headers.md) 的全部约束怎样接到 Commons CSV **现有**生产代码。下文标为“拟新增”的 API 和异常尚未实现；所有新期望来自 [Q0–Q10 决策](specs/decisions.md)与手工推导的 [Approved Scenarios](business-facts.md)，没有运行新 API 来取得期望结果。示例是脱敏对账账单，不代表仓库里存在这套业务调用代码。

## 先看一条数据怎样经过现有代码

| 步骤 | 现有生产代码 | 对新需求意味着什么 |
| --- | --- | --- |
| 1. 声明格式 | [`CSVFormat.Builder#setHeader(String...)`](../src/main/java/org/apache/commons/csv/CSVFormat.java#L621) 把 null、空数组、显式名称分别保存；[`Builder#get()`](../src/main/java/org/apache/commons/csv/CSVFormat.java#L319) 创建 `CSVFormat`。 | 拟新增 `setRequiredHeaders(String...)` 与现有配置一起构建。参数、表头模式、显式映射的错误在此拒绝（C1–C3）；目前源码没有该方法。 |
| 2. 创建解析器 | [`CSVFormat#parse`](../src/main/java/org/apache/commons/csv/CSVFormat.java#L2110) 创建 `CSVParser`；其[构造函数](../src/main/java/org/apache/commons/csv/CSVParser.java#L557) 随即调用 `createHeaders()`。 | 真实表头校验应在这一步完成，调用方尚未拿到首条数据记录（R1、R5）。 |
| 3. 读真实首行 | [`Lexer`](../src/main/java/org/apache/commons/csv/Lexer.java#L280) 拆分 CSV；[`createHeaders()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L601) 在自动表头模式读首条记录，显式模式只在 `skipHeaderRecord=true` 时消耗首条记录。 | 自动模式能直接核对读到的字段；显式模式须保留被跳过记录的字段供新校验使用，不能只看本地声明（Q0、Q3）。 |
| 4. 建名称映射 | [`createEmptyHeaderMap()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L593) 按 `ignoreHeaderCase` 选择大小写敏感或不敏感映射；[`createHeaders()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L625) 处理重复/空列名。 | 新校验复用列名解析语义，并让现有表头错误先报（R2、R3）；显式映射用于后续按名读取，也须包含全部必需名（C3）。 |
| 5. 业务按名取值 | [`CSVRecord#get(String)`](../src/main/java/org/apache/commons/csv/CSVRecord.java#L125) 从映射找下标；无映射名时抛 `Mapping for ... not found`，有映射但数据行太短时也可能抛错。 | 本需求只提前检查**表头列名存在**，不保证每行有值或长度足够（R7）。 |
| 6. 输出 CSV | [`CSVPrinter` 构造函数](../src/main/java/org/apache/commons/csv/CSVPrinter.java#L107) 使用格式中的输出表头。 | Required Headers 不增加输出校验；功能关闭时现有打印行为保持（R6、R7）。 |

## 例 1：自动表头，缺两列

上游只给账户列；对账需要币种、金额。业务期望在打开文件时一次收到完整缺列清单。[场景 02](../src/test/resources/org/apache/commons/csv/approved/02-two-missing.approved.md) 固定了输入和完整消息。

```csv
account_id
A-001
```

拟使用 `setHeader()` 自动读首行，并配置 `setRequiredHeaders("currency", "amount")`。**当前代码**在 `createHeaders()` 建立的映射中只有 `account_id`；之后业务调用 `record.get("currency")` 才会得到 `Mapping for currency not found, expected one of [account_id]`。**拟新增行为**在 `CSVParser` 构造期抛 `IllegalArgumentException`，完整消息为 `Missing required CSV headers: [currency, amount]`。两列按声明顺序列出；数据行不会先交给调用方。路径 **A6 → B3 → C3**。（Q0、Q3、Q4；R1、R5）

对照 [场景 01](../src/test/resources/org/apache/commons/csv/approved/01-auto-complete.approved.md)：首行三列齐全时走 **A6 → B3 → C2**，表头校验通过，后续记录仍照常读取。额外列与顺序变化也可通过，见 [场景 03](../src/test/resources/org/apache/commons/csv/approved/03-extra-reordered.approved.md)。（Q2；R3）

## 例 2：显式映射看似齐全，真实文件却缺币种

系统预设 `setHeader("account_id", "currency")`，并设置 `skipHeaderRecord=true`。上游实际送来：

```csv
account_id,amount
A-001,12.50
```

**当前代码**在 [`createHeaders()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L614) 只消耗首行，然后用声明的 `account_id,currency` 建映射；它不比较被跳过行的内容。于是 `record.get("currency")` 会按第二列下标返回 `12.50`，把金额误当币种。**拟新增行为**须核对被跳过的真实首行，构造解析器时抛 `Missing required CSV headers: [currency]`，见 [场景 10](../src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.approved.md)，路径 **A7 → B4 → C3**。（Q0、Q3、Q4；R1、R5）

显式映射要先具备三个条件：已启用表头、`skipHeaderRecord=true`、映射含全部必需名；否则在 `Builder#get()` 拒绝（A3/A4/A5，C3）。真实首行正确时见 [场景 09](../src/test/resources/org/apache/commons/csv/approved/09-explicit-complete.approved.md)。本需求不校验真实列与显式映射的**位置一一对应**：真实表头列名齐全但顺序不同，仍可能按旧映射读错值；Q2/Q7 将位置列为非目标，规格在此明确留下边界。

## 例 3：同一列名，格式选项决定是否匹配

同一上游文件：

```csv
account_id,CURRENCY
A-001,TWD
```

必需名为 `currency`。设置 `ignoreHeaderCase=true` 时，现有解析器用 [`String.CASE_INSENSITIVE_ORDER` 映射](../src/main/java/org/apache/commons/csv/CSVParser.java#L593)，新校验也应认作同一列：[场景 04](../src/test/resources/org/apache/commons/csv/approved/04-case-insensitive.approved.md) 通过。设为 `false` 时不折叠大小写，[场景 05](../src/test/resources/org/apache/commons/csv/approved/05-case-sensitive.approved.md) 报 `Missing required CSV headers: [currency]`。两者都从 A6/B3 出发，分别落到 C2/C3。（Q1；R2）

空白也沿用现有词法：[场景 06](../src/test/resources/org/apache/commons/csv/approved/06-ignore-surrounding-spaces.approved.md) 的未引用 ` currency ` 用 `ignoreSurroundingSpaces=true` 匹配；[场景 07](../src/test/resources/org/apache/commons/csv/approved/07-trim-quoted-header.approved.md) 的引号内 `" currency "` 用 `trim=true` 匹配；[场景 08](../src/test/resources/org/apache/commons/csv/approved/08-spaces-significant.approved.md) 关闭处理则拒绝。[`Lexer`](../src/main/java/org/apache/commons/csv/Lexer.java#L409) 处理未引用字段周围空白，[`CSVParser#addRecordValue()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L570) 应用 `trim`。必需名自身保持调用者拼写；构建时大小写等价的重复名按**最终**格式判定（C2）。

## 例 4：旧表头错误优先；必需列只要求“出现”

```csv
account_id,currency,currency
A-001,TWD,USD
```

必需名为 `amount`，并配置 `DuplicateHeaderMode.DISALLOW`。[`CSVParser#createHeaders()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L625) 当前就会先发现重复的 `currency` 并抛既有 `IllegalArgumentException`。新校验仍须让此错误优先，不能抢先报 `amount` 缺失；[场景 12](../src/test/resources/org/apache/commons/csv/approved/12-duplicate-rejected-first.approved.md) 锁定完整旧消息，路径 **A6 → B3 → C1**。（Q2；R3）

若格式允许重复，必需名出现一次或多次都满足；[场景 11](../src/test/resources/org/apache/commons/csv/approved/11-duplicate-allowed.approved.md) 用 `ALLOW_ALL`，而 [场景 18](../src/test/resources/org/apache/commons/csv/approved/18-allow-empty-duplicates.approved.md) 用 `ALLOW_EMPTY` 和 `allowMissingColumnNames=true` 表示重复空列名也可沿旧策略处理。[场景 16](../src/test/resources/org/apache/commons/csv/approved/16-blank-header-allowed.approved.md) 覆盖单个空列名。这里不建立完整 schema：额外列不拒绝，缺值与短数据行不在表头校验范围内。（Q2、Q7；R3、R7）

## 例 5：关闭功能或根本没有真实首行

仍是 `account_id,amount` 文件；不设置必需列时，[场景 14](../src/test/resources/org/apache/commons/csv/approved/14-disabled-auto.approved.md) 照旧解析，路径 **A1**。显式映射且不跳首行时，[场景 15](../src/test/resources/org/apache/commons/csv/approved/15-disabled-explicit.approved.md) 照旧把首行当数据。未调用新方法、传 null 数组或传空数组都表示关闭（C1、R6）；不能因新增功能让旧调用者在构建期失败。（Q5–Q7）

反过来，**开启**必需列却收到空文件时，没有真实首行可核对：[自动表头场景 13](../src/test/resources/org/apache/commons/csv/approved/13-empty-input.approved.md) 走 **A6 → B1**，[显式表头场景 19](../src/test/resources/org/apache/commons/csv/approved/19-explicit-empty-input.approved.md) 走 **A7 → B2**，都在解析器构造期报告全部必需列缺失。只有注释而无有效记录的 [场景 17](../src/test/resources/org/apache/commons/csv/approved/17-comment-only.approved.md) 与空文件相同；现有词法决定什么算有效记录。（Q3、Q4；R4、R5）

## 从实例回到规格的每一部分

| 规格位置 | 从实例看约束 | 与现有代码的接点／验收 |
| --- | --- | --- |
| §1 问题与 §2 术语 | 例 1 是缺列晚报；例 2 区分真实表头与逻辑映射。 | [`CSVRecord#get`](../src/main/java/org/apache/commons/csv/CSVRecord.java#L125) 与 [`CSVParser#createHeaders`](../src/main/java/org/apache/commons/csv/CSVParser.java#L601)；场景 02、10。 |
| §3 API、§5 C1–C3 | 例 2 的显式模式先过 A 表；例 5 的关闭模式直接终止。 | [`CSVFormat.Builder#get`](../src/main/java/org/apache/commons/csv/CSVFormat.java#L319) 和 [`CSVFormat` 构造校验](../src/main/java/org/apache/commons/csv/CSVFormat.java#L1616)；后续普通 JUnit 契约测试。 |
| §5 C4–C5 | 配置进入格式后不得被外部数组改写，复制后不丢失，比较时区分声明顺序。 | [`Builder` 拷贝构造](../src/main/java/org/apache/commons/csv/CSVFormat.java#L276)、[`builder()/copy()`](../src/main/java/org/apache/commons/csv/CSVFormat.java#L1663)、[`equals/hashCode/toString`](../src/main/java/org/apache/commons/csv/CSVFormat.java#L1677)；后续普通 JUnit 契约测试。 |
| §4 R1、R4、R5 | 例 1、2、5 分别展示自动、显式、无真实表头，以及构造期错误。 | [`CSVParser` 构造函数](../src/main/java/org/apache/commons/csv/CSVParser.java#L557) 与 [`createHeaders()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L601)；场景 02、09、10、13、17、19。 |
| §4 R2–R3 | 例 3 改匹配选项，例 4 保留重复/空表头策略；额外列和顺序放开。 | [`Lexer`](../src/main/java/org/apache/commons/csv/Lexer.java#L280)、[`createEmptyHeaderMap()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L593)、[`createHeaders()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L625)；场景 03–08、11、12、16、18。 |
| §4 R6–R7、§6 非目标 | 例 4、5 展示关闭后旧行为、列名存在即满足，不校验值、列位及输出。 | [`CSVRecord#get`](../src/main/java/org/apache/commons/csv/CSVRecord.java#L125)、[`CSVPrinter`](../src/main/java/org/apache/commons/csv/CSVPrinter.java#L107)；场景 14、15，后续 R7 普通单测。 |
| §7 验收 | 每例路径按 A 构建 → B 真实首行 → C 策略与匹配读；一旦终止，不再读后续条件。 | 19 份 [Approved Scenarios](business-facts.md) 固定业务输入与期望；C1–C5、R1/R5 的构造时机以及 R7 的数据行非目标由后续普通 JUnit 单测补足。 |

**与现有代码的边界**：本文链接的生产源码尚无 `setRequiredHeaders`。规格规定将新配置接入 Builder/CSVFormat，在解析器构造期使用真实首行校验；没有要求改动 `CSVRecord#get` 的取值语义，也没有要求 `CSVPrinter` 校验输出。构建期拒绝的具体消息未定；缺少真实必需表头的解析器构造期消息已定。场景中的新结果都应在实现后由 Approved Scenarios 执行器验证。
