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
# 0. 环境与基线

- 源仓库：`/Users/binwu/OOR/katas/commons-csv`；克隆目录：`with/`。
- 克隆起点：`66a838202d64a9b05be4e74b846619688b26cb10`（当前 HEAD）。
- 工作分支：`feature/required-headers`。
- Agent：Codex；模型：GPT-6。
- Maven：3.9.16。
- JDK：26.0.1（执行 `mvn test` 的 Maven 进程）。
- 首次基线：`mvn test`，BUILD SUCCESS；测试 924，失败 0，错误 0，跳过 11。
- 新增文档后首次复测：Apache RAT 因文档缺许可证头而失败；修复后完整复测：BUILD SUCCESS，测试 924，失败 0，错误 0，跳过 11。
- S6 最终全量基线与 S7 复跑：`mvn test`，BUILD SUCCESS；Tests run: **975**，Failures: **0**，Errors: **0**，Skipped: **11**。

# 1. 业务事实一览

对账方要求在读取数据前发现上游 CSV 缺少的必需表头。[用 CSV 实例理解需求与现有源码](docs/required-headers-by-examples.md)适合先读；[业务事实汇总](docs/business-facts.md)与[规格草案](docs/specs/required-headers.md)提供场景和精确规则。19 个场景涵盖通过、拒绝、显式表头、空输入及功能关闭后的既有行为；期望结果依决策手工推导，尚未实现或执行新 API。**末句记录 S2 写作时的历史状态；S4 起这 19 份夹具已由真实生产实现执行通过。**

| 场景编号 | 业务标题 | 结论 | 对应规则 |
| --- | --- | --- | --- |
| [01](docs/business-facts.md#scenario-01) | 账单列齐全 | 通过 | R1、R7 |
| [02](docs/business-facts.md#scenario-02) | 一次报告两项缺列 | 拒绝 | R1、R5 |
| [03](docs/business-facts.md#scenario-03) | 额外列和换序可接受 | 通过 | R3 |
| [04](docs/business-facts.md#scenario-04) | 忽略大小写后匹配 | 通过 | R2 |
| [05](docs/business-facts.md#scenario-05) | 严格大小写时拒绝 | 拒绝 | R2、R5 |
| [06](docs/business-facts.md#scenario-06) | 忽略未引用表头周围空白 | 通过 | R2 |
| [07](docs/business-facts.md#scenario-07) | 修剪引号内表头空白 | 通过 | R2 |
| [08](docs/business-facts.md#scenario-08) | 未启用空白处理时拒绝 | 拒绝 | R2、R5 |
| [09](docs/business-facts.md#scenario-09) | 显式映射核对真实首行 | 通过 | R1 |
| [10](docs/business-facts.md#scenario-10) | 显式映射不能掩盖缺列 | 拒绝 | R1、R5 |
| [11](docs/business-facts.md#scenario-11) | 允许重复列时仍算存在 | 通过 | R3 |
| [12](docs/business-facts.md#scenario-12) | 重复表头错误优先 | 拒绝 | R3 |
| [13](docs/business-facts.md#scenario-13) | 空文件全部缺列 | 拒绝 | R4、R5 |
| [14](docs/business-facts.md#scenario-14) | 关闭功能维持自动表头行为 | 通过 | R6 |
| [15](docs/business-facts.md#scenario-15) | 关闭功能维持显式表头行为 | 通过 | R6 |
| [16](docs/business-facts.md#scenario-16) | 允许空列名时仍可核对必需列 | 通过 | R3 |
| [17](docs/business-facts.md#scenario-17) | 只有注释，没有表头 | 拒绝 | R4、R5 |
| [18](docs/business-facts.md#scenario-18) | 重复空列名可接受 | 通过 | R3 |
| [19](docs/business-facts.md#scenario-19) | 显式表头遇到空文件 | 拒绝 | R1、R4、R5 |

逐项 CSV、配置、完整期望与业务解释见 [docs/business-facts.md](docs/business-facts.md)。


# 2. 如何运行 Approved Scenarios 测试

以下命令在本仓库根目录复制执行。S3 红态描述保留在后面作历史记录；本阶段预期均为 `BUILD SUCCESS`。

| 目的 | 命令 | 预期 |
| --- | --- | --- |
| 只跑 19 份业务夹具与 4 个执行器自检 | `mvn -Dtest=ApprovedScenariosTest test` | Surefire `Tests run: 23, Failures: 0, Errors: 0`；无新 `.received.md`。 |
| 只跑契约单测 | `mvn -Dtest=RequiredHeadersContractTest test` | Surefire 无失败、无错误；检查构建期配置、值语义及解析时机。 |
| 跑全量 | `mvn test` | `BUILD SUCCESS`；S6 基线 975 次、失败 0、错误 0、跳过 11。 |
| 检查业务事实汇总是否与夹具一致 | `mvn -Dtest=ScenarioBookTest test` | 一致时通过；漂移时写 `docs/business-facts.received.md` 并失败。 |
| 确认后重新生成汇总 | `APPROVE=1 mvn -Dtest=ScenarioBookTest test` | 测试通过，写 `docs/business-facts.md`；随后执行 `git diff -- docs/business-facts.md` 人工评审改动。 |
| 故障注入 | `bash fault-injection/run-fault-injection.sh` | `Mutations: 35; mismatches: 0`；每个变异显示 `YES`，不留注入源码或 `.received.md`。 |

夹具红灯时先找同名 `.received.md`，例如：

```bash
diff -u src/test/resources/org/apache/commons/csv/approved/02-two-missing.approved.md src/test/resources/org/apache/commons/csv/approved/02-two-missing.received.md
```

`-` 是已评审的 approved，`+` 是本次运行收到的 received；`diff` 退出码 1 表示两者有差异。核对 CSV、配置、规则、异常类型与完整消息；确认行为变化确实应被接受后，才运行：

```bash
APPROVE=1 mvn -Dtest=ApprovedScenariosTest test
git diff -- src/test/resources/org/apache/commons/csv/approved
mvn -Dtest=ApprovedScenariosTest test
```

预期先将收到的解析结果写回 approved，再由 `git diff` 展示待人工评审的变化，最后无 `APPROVE` 重跑全绿。**新增一个场景**：在 `src/test/resources/org/apache/commons/csv/approved/` 新增一份含 Apache 许可证头、业务标题、说明、规则、路径、`## 输入` CSV 围栏、`## 格式配置` text 围栏、`## 解析结果` text 围栏的 `NN-name.approved.md`；`NN` 使用下一个连续编号（当前下一个为 `20`）；手工推导并评审输入与期望；运行夹具测试、评审 received/approved diff，再按上法接受；运行 `ScenarioBookTest` 生成并评审业务事实汇总。执行器按文件名扫描并只检查集合非空，**只需新增夹具并评审它，不用改执行器**。

[S2 现状调试指南](docs/required-headers-by-running-e2e-test.md)、[实现后调试与渲染指南](docs/required-headers-by-running-e2e-test-after.md)、[before 图](docs/sequence-diagrams/before/existing-explicit-mapping.png)与[after 缺列拒绝图](docs/sequence-diagrams/after/required-headers-missing.png)对应两个独立测试入口。

### S3 红态的历史记录

S3 当时运行 `mvn -Dtest=ApprovedScenariosTest,RequiredHeadersContractTest test`，预期失败：生产代码只有 `setRequiredHeaders` 签名桩，校验尚未实现。当前运行同一命令应为 49 次、失败 0、错误 0。失败场景在夹具旁写入 `NN-xxx.received.md`；它已被 `.gitignore` 忽略。用 `diff -u src/test/resources/org/apache/commons/csv/approved/NN-xxx.approved.md src/test/resources/org/apache/commons/csv/approved/NN-xxx.received.md` 审查单个差异。

执行器自检由 `ApprovedScenariosTest` 内四个独立测试完成：读取非空与空 CSV 夹具；在临时目录验证不一致时写 received 且失败；在临时目录验证批准分支覆盖 approved。运行 `mvn -Dtest=ApprovedScenariosTest#shouldReadInputAndOptionsIfFixtureIsParsed+shouldReadEmptyInputIfFixtureHasEmptyCsvFence+shouldWriteReceivedAndFailIfResultDiffers+shouldOverwriteApprovedIfApprovalIsExplicit test` 可单独确认，不改源夹具。

仅在人工确认全部 diff 后，运行 `APPROVE=1 mvn -Dtest=ApprovedScenariosTest test` 更新 `.approved.md`。S3 当时不运行该命令；S3 夹具期望保持人工评审版本。

现有业务执行路径可先运行 `mvn -Dtest=RequiredHeadersExistingFlowE2ETest test`，再按 [VS Code 单步调试指南](docs/required-headers-by-running-e2e-test.md) 观察文件读取、显式表头映射与业务取值。此测试只用现有 API。

# 3. 故障注入与失败原因

S6 将 35 个单点错误行为分别保存为 `fault-injection/NN-*.patch`。先读
[`expected.md`](fault-injection/expected.md) 的完整失败集合预测，再运行
`bash fault-injection/run-fault-injection.sh`。脚本先确认生产源码与索引无改动、没有预先存在的 received，
并跑两类测试基线；每轮只应用一个补丁，读取 Surefire XML 的失败用例，保存 approved
与 received 的差异摘要到 `fault-injection/diffs/NN.diff`，回滚并清除 received。
中断时 EXIT trap 也会回滚。S6 留存的那次输出见 `fault-injection/last-run.log`；当前重跑的汇总由脚本写到终端。

最终 35 个变异的预期与实际失败集合全部一致。01–19 各为同编号业务夹具设计；
20–35 覆盖契约测试的构建期约束、禁用/重启、大小写、数组防御性复制、值语义、
格式描述、打印和短记录。第 28 号初版只绕过 Builder 的复制，构造函数仍复制数组，
未能打红测试；改为在共用复制函数注入后命中。其余预测修订均是漏算同一错误行为
影响的其他夹具或契约方法，未改变已评审业务事实或测试。

## 逐个变异：补丁、红灯与业务原因

以下 `改动` 原样取自对应 `.patch` 的 unified diff（省略文件许可证头）；`approved / received` 取自 `fault-injection/diffs/NN.diff`。失败集合与 [`expected.md`](fault-injection/expected.md) 的实测一致。`# no received files` 表示错误发生在执行器写结果前，或只有契约测试失败；此时以 Surefire 失败断言为证据。每个编号只注入一个补丁。

### 01 · require extra column

改动（[补丁](fault-injection/01-require-extra-column.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -690,1 +690,1 @@
-            if (!physicalNames.containsKey(name)) {
+            if (!physicalNames.containsKey(name) || physicalNames.size() == required.length) {
```

变红：`01,09`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/01.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/01-auto-complete.approved.md	2026-09-25 13:54:03
+++ src/test/resources/org/apache/commons/csv/approved/01-auto-complete.received.md	2026-09-25 18:55:15
@@ -35,8 +35,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[account_id, currency, amount]
-记录：
-- [A-001, TWD, 12.50]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：Missing required CSV headers: [account_id, currency, amount]
 ```
```

业务解释：完整表头也须通过。同时变红：09。这些输入或契约也依赖同一规则；完整逐项失败记录见 [01.diff](fault-injection/diffs/01.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：把“必需列数等于物理列数”误判为不足，也会拒绝显式完整表头 09。

### 02 · stop after first missing

改动（[补丁](fault-injection/02-stop-after-first-missing.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -691,1 +691,1 @@
-                missing.add(name);
+                missing.add(name); break;
```

变红：`02,13,17,19`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/02.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/02-two-missing.approved.md	2026-09-25 10:21:08
+++ src/test/resources/org/apache/commons/csv/approved/02-two-missing.received.md	2026-09-25 18:55:19
@@ -37,5 +37,5 @@
 ```text
 状态：拒绝
 异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [currency, amount]
+完整消息：Missing required CSV headers: [currency]
 ```
```

业务解释：一次报齐所有缺失列。同时变红：13、17、19。这些输入或契约也依赖同一规则；完整逐项失败记录见 [02.diff](fault-injection/diffs/02.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：只报第一个缺失列，除 02 外，空输入和仅注释输入的 13、17、19 也失去完整错误消息。

### 03 · require declared order

改动（[补丁](fault-injection/03-require-declared-order.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -690,1 +690,1 @@
-            if (!physicalNames.containsKey(name)) {
+            if (!physicalNames.containsKey(name) || physicalNames.get(name).intValue() != Arrays.asList(required).indexOf(name)) {
```

变红：`03,04,06,07,11,16,18,shouldAcceptEmptyAndShortDataIfRequiredHeaderExists,shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/03.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/03-extra-reordered.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/03-extra-reordered.received.md	2026-09-25 18:55:23
@@ -35,8 +35,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[batch, amount, currency, account_id]
-记录：
-- [B-7, 12.50, TWD, A-001]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：Missing required CSV headers: [account_id, currency, amount]
 ```
```

业务解释：额外列与列顺序不影响存在性。同时变红：04、06、07、11、16、18、shouldAcceptEmptyAndShortDataIfRequiredHeaderExists、shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped。这些输入或契约也依赖同一规则；完整逐项失败记录见 [03.diff](fault-injection/diffs/03.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：把存在性误写成位置相等，还会拒绝大小写/空白处理后的表头、重复列、可选空列和短记录场景，以及可选空映射契约。

### 04 · ignore case flag

改动（[补丁](fault-injection/04-ignore-case-flag.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -589,1 +589,1 @@
-        return format.getIgnoreHeaderCase() ?
+        return false ?
```

变红：`04`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/04.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/04-case-insensitive.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/04-case-insensitive.received.md	2026-09-25 18:55:27
@@ -36,8 +36,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[account_id, CURRENCY]
-记录：
-- [A-001, TWD]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：Missing required CSV headers: [currency]
 ```
```

业务解释：忽略大小写时大小写等价。其他测试未变红。

### 05 · fold case unconditionally

改动（[补丁](fault-injection/05-fold-case-unconditionally.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -589,1 +589,1 @@
-        return format.getIgnoreHeaderCase() ?
+        return true ?
```

变红：`05`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/05.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/05-case-sensitive.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/05-case-sensitive.received.md	2026-09-25 18:55:31
@@ -36,7 +36,8 @@
<blank context>
 ## 解析结果
 ```text
-状态：拒绝
-异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [currency]
+状态：通过
+实际表头：[account_id, CURRENCY]
+记录：
+- [A-001, TWD]
 ```
```

业务解释：区分大小写时必须精确匹配。其他测试未变红。

### 06 · ignore space setting

改动（[补丁](fault-injection/06-ignore-space-setting.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/Lexer.java b/src/main/java/org/apache/commons/csv/Lexer.java
--- a/src/main/java/org/apache/commons/csv/Lexer.java
+++ b/src/main/java/org/apache/commons/csv/Lexer.java
@@ -60,1 +60,1 @@
-        this.ignoreSurroundingSpaces = format.getIgnoreSurroundingSpaces();
+        this.ignoreSurroundingSpaces = false;
```

变红：`06`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/06.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/06-ignore-surrounding-spaces.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/06-ignore-surrounding-spaces.received.md	2026-09-25 18:55:36
@@ -36,8 +36,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[account_id, currency]
-记录：
-- [A-001, TWD]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：Missing required CSV headers: [currency]
 ```
```

业务解释：开启周围空白忽略后列名匹配。其他测试未变红。

### 07 · ignore trim setting

改动（[补丁](fault-injection/07-ignore-trim-setting.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2620,1 +2620,1 @@
-        return getTrim() ? value.trim() : value;
+        return value;
```

变红：`07`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/07.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/07-trim-quoted-header.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/07-trim-quoted-header.received.md	2026-09-25 18:55:40
@@ -36,8 +36,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[account_id, currency]
-记录：
-- [A-001, TWD]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：Missing required CSV headers: [currency]
 ```
```

业务解释：开启 trim 后引号内空白也被去除。其他测试未变红。

### 08 · trim all unquoted spaces

改动（[补丁](fault-injection/08-trim-all-unquoted-spaces.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/Lexer.java b/src/main/java/org/apache/commons/csv/Lexer.java
--- a/src/main/java/org/apache/commons/csv/Lexer.java
+++ b/src/main/java/org/apache/commons/csv/Lexer.java
@@ -60,1 +60,1 @@
-        this.ignoreSurroundingSpaces = format.getIgnoreSurroundingSpaces();
+        this.ignoreSurroundingSpaces = true;
```

变红：`08`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/08.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/08-spaces-significant.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/08-spaces-significant.received.md	2026-09-25 18:55:44
@@ -37,7 +37,8 @@
<blank context>
 ## 解析结果
 ```text
-状态：拒绝
-异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [currency]
+状态：通过
+实际表头：[account_id, currency]
+记录：
+- [A-001, TWD]
 ```
```

业务解释：关闭空白处理时空白仍有业务意义。其他测试未变红。

### 09 · reject complete explicit mapping

改动（[补丁](fault-injection/09-reject-complete-explicit-mapping.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2698,1 +2698,1 @@
-                if (!mapped.containsAll(names)) {
+                if (!mapped.containsAll(names) || mapped.size() == names.size()) {
```

变红：`09,19,shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/09.diff)）：

```diff
# no received files
```

业务解释：显式映射完整时允许解析。该变异在构建配置时已失败，执行器来不及写 received；同时变红：19、shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase、shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped。这些输入或契约也依赖同一规则；完整逐项失败记录见 [09.diff](fault-injection/diffs/09.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：显式映射等于必需集合时错误拒绝，还会影响空物理输入、大小写等价映射和可选空映射契约。

### 10 · trust explicit mapping

改动（[补丁](fault-injection/10-trust-explicit-mapping.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -649,1 +649,1 @@
-            validateRequiredHeaders(physicalHeader, formatHeader.length > 0);
+            validateRequiredHeaders(formatHeader.length > 0 ? formatHeader : physicalHeader, formatHeader.length > 0);
```

变红：`10,19,shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/10.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.approved.md	2026-09-25 13:54:03
+++ src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.received.md	2026-09-25 18:55:52
@@ -36,7 +36,8 @@
<blank context>
 ## 解析结果
 ```text
-状态：拒绝
-异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [currency]
+状态：通过
+实际表头：[account_id, amount]
+记录：
+- [A-001, 12.50]
 ```
```

业务解释：显式映射不能代替物理表头。同时变红：19、shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase、shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing。这些输入或契约也依赖同一规则；完整逐项失败记录见 [10.diff](fault-injection/diffs/10.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：以显式映射代替物理表头检查，还会影响空物理输入及对应两个构建/解析契约。

### 11 · disallow duplicate headers

改动（[补丁](fault-injection/11-disallow-duplicate-headers.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -657,1 +657,1 @@
-        if (duplicate && mode != DuplicateHeaderMode.ALLOW_ALL && !(blank && mode == DuplicateHeaderMode.ALLOW_EMPTY)) {
+        if (duplicate && !(blank && mode == DuplicateHeaderMode.ALLOW_EMPTY)) {
```

变红：`11`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/11.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/11-duplicate-allowed.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/11-duplicate-allowed.received.md	2026-09-25 18:55:56
@@ -36,8 +36,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[account_id, currency, currency]
-记录：
-- [A-001, TWD, USD]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：The header contains a duplicate name: "currency" in [account_id, currency, currency]. If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().
 ```
```

业务解释：允许重复列时不能拒绝。其他测试未变红。

### 12 · skip duplicate check

改动（[补丁](fault-injection/12-skip-duplicate-check.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -638,1 +638,1 @@
-                    checkDuplicateHeader(headerRecord, header, blankHeader, containsHeader);
+                    // duplicate check omitted
```

变红：`12`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/12.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/12-duplicate-rejected-first.approved.md	2026-09-25 13:18:04
+++ src/test/resources/org/apache/commons/csv/approved/12-duplicate-rejected-first.received.md	2026-09-25 18:56:00
@@ -38,5 +38,5 @@
 ```text
 状态：拒绝
 异常类型：IllegalArgumentException
-完整消息：The header contains a duplicate name: "currency" in [account_id, currency, currency]. If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().
+完整消息：Missing required CSV headers: [amount]
 ```
```

业务解释：禁止重复列应先报重复错误。其他测试未变红。

### 13 · skip absent header validation

改动（[补丁](fault-injection/13-skip-absent-header-validation.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -649,1 +649,1 @@
-            validateRequiredHeaders(physicalHeader, formatHeader.length > 0);
+            if (physicalHeader != null) { validateRequiredHeaders(physicalHeader, formatHeader.length > 0); }
```

变红：`13,17,19`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/13.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/13-empty-input.approved.md	2026-09-25 13:18:04
+++ src/test/resources/org/apache/commons/csv/approved/13-empty-input.received.md	2026-09-25 18:56:04
@@ -33,7 +33,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：拒绝
-异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [currency, amount]
+状态：通过
+实际表头：[]
+记录：
 ```
```

业务解释：空输入仍缺少必需表头。同时变红：17、19。这些输入或契约也依赖同一规则；完整逐项失败记录见 [13.diff](fault-injection/diffs/13.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：仅有物理表头时才校验，会同时放过自动空输入、仅注释输入、显式空输入。

### 14 · reject disabled auto mode

改动（[补丁](fault-injection/14-reject-disabled-auto-mode.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -666,1 +666,1 @@
-        if (required == null || required.length == 0) {
+        if (required == null) { throw new IllegalArgumentException("Required CSV headers must be configured"); } if (required.length == 0) {
```

变红：`01,03,04,06,07,09,11,14,15,16,18,shouldDisableValidationIfRequiredHeadersAreNullOrEmpty,shouldPreserveOldParsingIfRequiredHeadersNeverSet`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/14.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/01-auto-complete.approved.md	2026-09-25 13:54:03
+++ src/test/resources/org/apache/commons/csv/approved/01-auto-complete.received.md	2026-09-25 18:56:08
@@ -35,8 +35,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[account_id, currency, amount]
-记录：
-- [A-001, TWD, 12.50]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：Required CSV headers must be configured
 ```
```

业务解释：关闭功能后自动表头按旧行为解析。同时变红：01、03、04、06、07、09、11、15、16、18、shouldDisableValidationIfRequiredHeadersAreNullOrEmpty、shouldPreserveOldParsingIfRequiredHeadersNeverSet。这些输入或契约也依赖同一规则；完整逐项失败记录见 [14.diff](fault-injection/diffs/14.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：把功能关闭误作构建错误，也会打红所有需要在执行器中构造关闭校验的物理表头格式，以及关闭功能和旧行为契约。

### 15 · force skip explicit header

改动（[补丁](fault-injection/15-force-skip-explicit-header.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -618,1 +618,1 @@
-                if (format.getSkipHeaderRecord()) {
+                if (true) {
```

变红：`15`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/15.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/15-disabled-explicit.approved.md	2026-09-25 13:54:03
+++ src/test/resources/org/apache/commons/csv/approved/15-disabled-explicit.received.md	2026-09-25 18:56:12
@@ -39,6 +39,5 @@
 状态：通过
 实际表头：[account_id, amount]
 记录：
-- [account_id, amount]
 - [A-001, 12.50]
 ```
```

业务解释：关闭功能后显式表头按旧行为解析。其他测试未变红。

### 16 · disallow blank optional header

改动（[补丁](fault-injection/16-disallow-blank-optional-header.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -634,1 +634,1 @@
-                    if (blankHeader && !format.getAllowMissingColumnNames()) {
+                    if (blankHeader) {
```

变红：`16,18,shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/16.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/16-blank-header-allowed.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/16-blank-header-allowed.received.md	2026-09-25 18:56:16
@@ -36,8 +36,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[, currency]
-记录：
-- [unused, TWD]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：A header name is missing in [, currency]
 ```
```

业务解释：允许空列名时必需列仍可匹配。同时变红：18、shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped。这些输入或契约也依赖同一规则；完整逐项失败记录见 [16.diff](fault-injection/diffs/16.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：禁止可选空列名，同时影响 16、18 和可选空映射契约。

### 17 · skip comment only validation

改动（[补丁](fault-injection/17-skip-comment-only-validation.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -649,1 +649,1 @@
-            validateRequiredHeaders(physicalHeader, formatHeader.length > 0);
+            if (physicalHeader != null || format.getCommentMarker() == null) { validateRequiredHeaders(physicalHeader, formatHeader.length > 0); }
```

变红：`17`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/17.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/17-comment-only.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/17-comment-only.received.md	2026-09-25 18:56:20
@@ -35,7 +35,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：拒绝
-异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [currency, amount]
+状态：通过
+实际表头：[]
+记录：
 ```
```

业务解释：仅注释输入仍缺必需列。其他测试未变红。

### 18 · disallow empty duplicates

改动（[补丁](fault-injection/18-disallow-empty-duplicates.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -657,1 +657,1 @@
-        if (duplicate && mode != DuplicateHeaderMode.ALLOW_ALL && !(blank && mode == DuplicateHeaderMode.ALLOW_EMPTY)) {
+        if (duplicate && mode != DuplicateHeaderMode.ALLOW_ALL) {
```

变红：`18`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/18.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/18-allow-empty-duplicates.approved.md	2026-09-25 09:41:55
+++ src/test/resources/org/apache/commons/csv/approved/18-allow-empty-duplicates.received.md	2026-09-25 18:56:24
@@ -37,8 +37,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：通过
-实际表头：[, currency, ]
-记录：
-- [unused, TWD, other]
+状态：拒绝
+异常类型：IllegalArgumentException
+完整消息：The header contains a duplicate name: "" in [, currency, ]. If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().
 ```
```

业务解释：ALLOW_EMPTY 容许重复空列名。其他测试未变红。

### 19 · skip empty explicit validation

改动（[补丁](fault-injection/19-skip-empty-explicit-validation.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -649,1 +649,1 @@
-            validateRequiredHeaders(physicalHeader, formatHeader.length > 0);
+            if (physicalHeader != null || formatHeader.length == 0) { validateRequiredHeaders(physicalHeader, formatHeader.length > 0); }
```

变红：`19`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/19.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/19-explicit-empty-input.approved.md	2026-09-25 09:41:39
+++ src/test/resources/org/apache/commons/csv/approved/19-explicit-empty-input.received.md	2026-09-25 18:56:28
@@ -34,7 +34,7 @@
<blank context>
 ## 解析结果
 ```text
-状态：拒绝
-异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [account_id, currency]
+状态：通过
+实际表头：[]
+记录：
 ```
```

业务解释：显式映射也不能让空输入通过。其他测试未变红。

### 20 · keep disabled requirement

改动（[补丁](fault-injection/20-keep-disabled-requirement.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -637,1 +637,1 @@
-            this.requiredHeaders = CSVFormat.clone(names);
+            if (names != null && names.length > 0) { this.requiredHeaders = CSVFormat.clone(names); }
```

变红：`shouldDisableValidationIfRequiredHeadersAreNullOrEmpty`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/20.diff)）：

```diff
# no received files
```

业务解释：传入 null 或空数组应关闭校验，不能保留旧必需列。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldDisableValidationIfRequiredHeadersAreNullOrEmpty。这些契约方法检验同一规则；完整逐项失败记录见 [20.diff](fault-injection/diffs/20.diff) 与 [expected.md](fault-injection/expected.md)。

### 21 · ignore invalid required names

改动（[补丁](fault-injection/21-ignore-invalid-required-names.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2681,1 +2681,1 @@
-                if (isBlank(name) || !names.add(name)) {
+                if (false) {
```

变红：`shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive,shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase,shouldRejectInvalidNamesIfBuildingEnabledFormat,shouldRejectRequiredHeadersIfExplicitMappingLacksName`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/21.diff)）：

```diff
# no received files
```

业务解释：必需列名不得为空白或重复；构建期必须拒绝无效配置。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive、shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase、shouldRejectInvalidNamesIfBuildingEnabledFormat、shouldRejectRequiredHeadersIfExplicitMappingLacksName。这些契约方法检验同一规则；完整逐项失败记录见 [21.diff](fault-injection/diffs/21.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：跳过必需名校验会使无效名、大小写重复及显式映射相关构建契约一起失效。

### 22 · ignore case for duplicates

改动（[补丁](fault-injection/22-ignore-case-for-duplicates.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2641,1 +2641,1 @@
-        return ignoreHeaderCase ? new TreeSet<>(String.CASE_INSENSITIVE_ORDER) : new HashSet<>();
+        return new HashSet<>();
```

变红：`shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/22.diff)）：

```diff
# no received files
```

业务解释：最终忽略大小写时，大小写等价的必需名与显式映射应按同一列处理。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase、shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase。这些契约方法检验同一规则；完整逐项失败记录见 [22.diff](fault-injection/diffs/22.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：必需名集合失去大小写折叠，除大小写重复检查外，还影响大小写等价的显式映射。

### 23 · always fold required names

改动（[补丁](fault-injection/23-always-fold-required-names.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2641,1 +2641,1 @@
-        return ignoreHeaderCase ? new TreeSet<>(String.CASE_INSENSITIVE_ORDER) : new HashSet<>();
+        return new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
```

变红：`shouldAllowCaseDistinctNamesIfFinalCaseModeIsSensitive,shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/23.diff)）：

```diff
# no received files
```

业务解释：最终区分大小写时，不同大小写的名称是不同列。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldAllowCaseDistinctNamesIfFinalCaseModeIsSensitive、shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive。这些契约方法检验同一规则；完整逐项失败记录见 [23.diff](fault-injection/diffs/23.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：必需名集合总是折叠大小写，除允许大小写不同必需名外，还影响大小写敏感的显式映射拒绝。

### 24 · allow absent header mode

改动（[补丁](fault-injection/24-allow-absent-header-mode.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2685,1 +2685,1 @@
-            if (headers == null) {
+            if (false) {
```

变红：`shouldRejectRequiredHeadersIfHeaderModeIsAbsent`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/24.diff)）：

```diff
# no received files
```

业务解释：声明必需列前必须选择表头模式，避免无物理表头可检查。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldRejectRequiredHeadersIfHeaderModeIsAbsent。这些契约方法检验同一规则；完整逐项失败记录见 [24.diff](fault-injection/diffs/24.diff) 与 [expected.md](fault-injection/expected.md)。

### 25 · allow unskipped explicit header

改动（[补丁](fault-injection/25-allow-unskipped-explicit-header.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2689,1 +2689,1 @@
-                if (!skipHeaderRecord) {
+                if (false) {
```

变红：`shouldRejectRequiredHeadersIfExplicitHeaderIsNotSkipped`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/25.diff)）：

```diff
# no received files
```

业务解释：显式表头必须跳过物理表头记录，才能在读取数据前验证它。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldRejectRequiredHeadersIfExplicitHeaderIsNotSkipped。这些契约方法检验同一规则；完整逐项失败记录见 [25.diff](fault-injection/diffs/25.diff) 与 [expected.md](fault-injection/expected.md)。

### 26 · allow unmapped required header

改动（[补丁](fault-injection/26-allow-unmapped-required-header.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2698,1 +2698,1 @@
-                if (!mapped.containsAll(names)) {
+                if (false) {
```

变红：`shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive,shouldRejectRequiredHeadersIfExplicitMappingLacksName`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/26.diff)）：

```diff
# no received files
```

业务解释：显式映射必须包含每个必需列名，大小写规则按最终配置判断。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive、shouldRejectRequiredHeadersIfExplicitMappingLacksName。这些契约方法检验同一规则；完整逐项失败记录见 [26.diff](fault-injection/diffs/26.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：漏掉显式映射包含必需名检查，也放过大小写敏感时名称不同的映射。

### 27 · reject null optional mapping

改动（[补丁](fault-injection/27-reject-null-optional-mapping.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2694,1 +2694,1 @@
-                    if (header != null) {
+                    if (true) {
```

变红：`shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/27.diff)）：

```diff
# no received files
```

业务解释：允许空白可选列名时，有效必需列仍可解析。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped。这些契约方法检验同一规则；完整逐项失败记录见 [27.diff](fault-injection/diffs/27.diff) 与 [expected.md](fault-injection/expected.md)。

### 28 · keep caller array

改动（[补丁](fault-injection/28-keep-caller-array.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -1441,1 +1441,1 @@
-        return values == null ? null : values.clone();
+        return values;
```

变红：`shouldCopyRequiredNamesIfInputArrayChanges`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/28.diff)）：

```diff
# no received files
```

业务解释：外部修改调用方数组不能改变已构建格式的必需列。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldCopyRequiredNamesIfInputArrayChanges。这些契约方法检验同一规则；完整逐项失败记录见 [28.diff](fault-injection/diffs/28.diff) 与 [expected.md](fault-injection/expected.md)。

### 29 · drop required on builder copy

改动（[补丁](fault-injection/29-drop-required-on-builder-copy.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -293,1 +293,1 @@
-            this.requiredHeaders = CSVFormat.clone(csvFormat.requiredHeaders);
+            this.requiredHeaders = null;
```

变红：`02,05,08,10,13,17,19,shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldAllowCaseDistinctNamesIfFinalCaseModeIsSensitive,shouldCopyRequiredNamesIfInputArrayChanges,shouldOverwriteApprovedIfApprovalIsExplicit,shouldPreserveRequiredNamesIfFormatIsCopiedOrRebuilt,shouldReenableValidationIfCalledAfterDisabling,shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing,shouldWriteReceivedAndFailIfResultDiffers`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/29.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/02-two-missing.approved.md	2026-09-25 10:21:08
+++ src/test/resources/org/apache/commons/csv/approved/02-two-missing.received.md	2026-09-25 18:57:10
@@ -35,7 +35,8 @@
<blank context>
 ## 解析结果
 ```text
-状态：拒绝
-异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [currency, amount]
+状态：通过
+实际表头：[account_id]
+记录：
+- [A-001]
 ```
```

业务解释：复制或重建格式后，必需列约束仍须保留。同时变红：02、05、08、10、13、17、19、shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase、shouldAllowCaseDistinctNamesIfFinalCaseModeIsSensitive、shouldCopyRequiredNamesIfInputArrayChanges、shouldOverwriteApprovedIfApprovalIsExplicit、shouldPreserveRequiredNamesIfFormatIsCopiedOrRebuilt、shouldReenableValidationIfCalledAfterDisabling、shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing、shouldWriteReceivedAndFailIfResultDiffers。这些输入或契约也依赖同一规则；完整逐项失败记录见 [29.diff](fault-injection/diffs/29.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：Builder 从格式复制时丢失必需名，会影响多份夹具及复制、重新启用、数组和值语义等契约。

### 30 · ignore required in equality

改动（[补丁](fault-injection/30-ignore-required-in-equality.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -1711,1 +1711,1 @@
-                Arrays.equals(requiredHeaders, other.requiredHeaders) &&
+                true &&
```

变红：`shouldChangeEqualityIfRequiredNamesChangeOrReorder`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/30.diff)）：

```diff
# no received files
```

业务解释：必需列集合不同或顺序不同，格式值语义应不同。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldChangeEqualityIfRequiredNamesChangeOrReorder。这些契约方法检验同一规则；完整逐项失败记录见 [30.diff](fault-injection/diffs/30.diff) 与 [expected.md](fault-injection/expected.md)。

### 31 · compare required by identity

改动（[补丁](fault-injection/31-compare-required-by-identity.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -1711,1 +1711,1 @@
-                Arrays.equals(requiredHeaders, other.requiredHeaders) &&
+                requiredHeaders == other.requiredHeaders &&
```

变红：`shouldKeepValueSemanticsIfRequiredNamesMatch`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/31.diff)）：

```diff
# no received files
```

业务解释：相同必需列内容的两个格式应相等且哈希相同。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldKeepValueSemanticsIfRequiredNamesMatch。这些契约方法检验同一规则；完整逐项失败记录见 [31.diff](fault-injection/diffs/31.diff) 与 [expected.md](fault-injection/expected.md)。

### 32 · hide required in description

改动（[补丁](fault-injection/32-hide-required-in-description.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2614,1 +2614,1 @@
-            sb.append("RequiredHeaders:").append(Arrays.toString(requiredHeaders));
+            sb.append("Header:").append(Arrays.toString(headers));
```

变红：`shouldShowRequiredNamesInOrderIfFormatIsRendered`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/32.diff)）：

```diff
# no received files
```

业务解释：格式描述应按声明顺序列出必需列。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldShowRequiredNamesInOrderIfFormatIsRendered。这些契约方法检验同一规则；完整逐项失败记录见 [32.diff](fault-injection/diffs/32.diff) 与 [expected.md](fault-injection/expected.md)。

### 33 · reject print with required

改动（[补丁](fault-injection/33-reject-print-with-required.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVFormat.java b/src/main/java/org/apache/commons/csv/CSVFormat.java
--- a/src/main/java/org/apache/commons/csv/CSVFormat.java
+++ b/src/main/java/org/apache/commons/csv/CSVFormat.java
@@ -2162,1 +2162,1 @@
-    public CSVPrinter print(final Appendable out) throws IOException {
+    public CSVPrinter print(final Appendable out) throws IOException { if (requiredHeaders != null) { throw new IllegalArgumentException("Required CSV headers need input"); }
```

变红：`shouldAllowPrintingIfRequiredHeadersAreConfigured`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/33.diff)）：

```diff
# no received files
```

业务解释：配置必需列不应禁止 CSV 打印。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldAllowPrintingIfRequiredHeadersAreConfigured。这些契约方法检验同一规则；完整逐项失败记录见 [33.diff](fault-injection/diffs/33.diff) 与 [expected.md](fault-injection/expected.md)。

### 34 · skip explicit physical validation

改动（[补丁](fault-injection/34-skip-explicit-physical-validation.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -649,1 +649,1 @@
-            validateRequiredHeaders(physicalHeader, formatHeader.length > 0);
+            if (formatHeader.length == 0) { validateRequiredHeaders(physicalHeader, false); }
```

变红：`10,19,shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/34.diff)）：

```diff
--- src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.approved.md	2026-09-25 13:54:03
+++ src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.received.md	2026-09-25 18:57:31
@@ -36,7 +36,8 @@
<blank context>
 ## 解析结果
 ```text
-状态：拒绝
-异常类型：IllegalArgumentException
-完整消息：Missing required CSV headers: [currency]
+状态：通过
+实际表头：[account_id, amount]
+记录：
+- [A-001, 12.50]
 ```
```

业务解释：显式映射不能代替物理表头；缺列应在解析器构造时拒绝。同时变红：10、19、shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase、shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing。这些输入或契约也依赖同一规则；完整逐项失败记录见 [34.diff](fault-injection/diffs/34.diff) 与 [expected.md](fault-injection/expected.md)。

连带红灯原因：显式映射不检查物理表头，还会放过 10、19，并破坏大小写等价显式映射契约。

### 35 · reject short data row

改动（[补丁](fault-injection/35-reject-short-data-row.patch)，原样 diff）：

```diff
diff --git a/src/main/java/org/apache/commons/csv/CSVParser.java b/src/main/java/org/apache/commons/csv/CSVParser.java
--- a/src/main/java/org/apache/commons/csv/CSVParser.java
+++ b/src/main/java/org/apache/commons/csv/CSVParser.java
@@ -965,1 +965,1 @@
-            recordNumber++;
+            recordNumber++; if (headers != null && format.getRequiredHeaders() != null && recordList.size() < headers.headerMap.size()) { throw new IllegalArgumentException("Short CSV record"); }
```

变红：`shouldAcceptEmptyAndShortDataIfRequiredHeaderExists`。

approved / received 关键差异（[完整摘要](fault-injection/diffs/35.diff)）：

```diff
# no received files
```

业务解释：只要求表头存在，不要求每行数据非空或列数完整。该变异仅打红契约断言，没有业务夹具 received；变红方法：shouldAcceptEmptyAndShortDataIfRequiredHeaderExists。这些契约方法检验同一规则；完整逐项失败记录见 [35.diff](fault-injection/diffs/35.diff) 与 [expected.md](fault-injection/expected.md)。


# 4. 踩坑记录

| 环节 | 现象 | 原因 | 解决 | 怎样提前避开 |
| --- | --- | --- | --- | --- |
| 环节 S0 仓库检查 | `zsh:1: no matches found: .mvn/**` | zsh 把未加引号的搜索模式当作 shell 通配符展开，目录不存在时直接报错 | 给 `rg -g` 的模式加引号，重新检查成功。 | 给 shell 通配符加引号，或先检查目录是否存在 |
| 环节 S0 环境检查 | `context-mode: Build tool redirected.` | 直接运行 Maven 的 shell 工具被上下文模式重定向 | 经 `ctx_execute` 在 `with/` 执行完整 `mvn test`，读取测试汇总及退出结果。 | 先确认 Maven 命令实际运行及退出码 |
| 环节 S0 文档复测 | `Counter(s) UNAPPROVED exceeded minimum or maximum values.` | Apache RAT 把新增的 `guide.md` 和 `dialogs/dialogs.md` 识别为缺许可证头 | 按仓库现有 Markdown 格式补入 Apache License 2.0 头，重新运行完整 `mvn test`。 | 创建文档时使用许可证头模板 |
| 环节 S2 文件检索 | `grep: option `--files' is ambiguous` | 环境里的 `rg` 被代理成不接受该选项的命令 | 改用 `rtk proxy rg` 明确调用 ripgrep，重新读取目标文件。 | 文件列表命令使用 `rtk proxy rg --files` |
| 环节 S2 构建入口 | `context-mode: Build tool redirected.` | 直接经 shell 运行 Maven 被上下文模式拦截，未真正执行测试 | 按提示经 `ctx_execute` 执行 Maven 并读取结果。 | 经 `ctx_execute` 执行构建并核验日志 |
| 环节 S2 暂存提交 | `fatal: Unable to create '.git/index.lock': Operation not permitted` | 沙箱仅允许读取 `.git`，默认权限无法写入索引 | 按授权流程提升本仓库 `git add` 权限后成功暂存。 | 提交前确认 `.git` 写入审批 |
| 环节 S2 差异检查 | `trailing whitespace.` | 场景 06、08 的原样 CSV 故意保留表头末尾空格，用于验证空白匹配；汇总文档复制同一输入 | 保留这四处业务输入空格，清除其他文件末尾多余空行。 | 对故意保留空格的 CSV 单独核对 diff |
| 环节 S3 构建入口 | `context-mode: Build tool redirected.` | shell 中的 Maven 命令被上下文模式拦截，未执行测试 | 改用 `ctx_execute` 执行，确认 Surefire 输出 44 个用例、失败 26、错误 0。 | 构建后确认 Surefire 报告时间和结果 |
| 环节 S3 空输入夹具 | 场景 13、19 的 received 把后续 Markdown 标题当作 CSV | 空代码围栏的结束标记紧贴内容起点，原提取器只查找前置换行的结束围栏 | 显式处理空围栏，增加独立自检并重跑两类测试。 | 给空 CSV 围栏写提取器自检 |
| 环节 S3 暂存提交 | `fatal: Unable to create '.git/index.lock': Operation not permitted` | 沙箱默认只读 `.git`，无法写索引 | 按授权流程提升本仓库 `git add` 权限后暂存成功。 | 提交前确认 `.git` 写入审批 |
| 环节 S4 执行器自检 | 必需列功能实现后，`ApprovedScenariosTest` 的两项自检失败 | S3 自检把场景 02 实际状态固定假定为“通过”，生产代码转绿后该假定失效 | 只在临时目录把期望状态改为“待核对”制造差异，再断言 received 与显式批准分支均还原真实“拒绝”结果；源 `.approved.md` 未改。 | 让执行器自检构造临时差异，不依赖某场景业务状态 |
| 环节 S4 显式映射 | 忽略表头大小写且显式映射含可选 `null` 名称时，构建格式抛 `NullPointerException` | 新增 `TreeSet` 映射校验直接加入了 `null` | 跳过空映射名，只核对必需名；新增回归单测确认可解析。 | 显式映射校验先过滤可选 `null` 列名 |
| 环节 S4 检索与构建入口 | `rg --files` 被代理成 grep，直接执行 Maven 只返回 `context-mode: Build tool redirected.` | 当前环境对这两个命令分别设有代理与构建拦截 | 用 `rtk proxy rg` 搜索、经 `ctx_execute` 执行完整 Maven 测试。 | 检索走 `rtk proxy rg`，构建走 `ctx_execute` |
| 环节 S4 暂存提交 | `fatal: Unable to create '.git/index.lock': Operation not permitted` | 沙箱对 `.git` 仅开放读取 | 按授权流程提升本仓库 `git add` 权限，成功暂存。 | 提交前确认 `.git` 写入审批 |
| 环节 S5 契约测试补丁 | 一次跨多处修改的补丁因末尾上下文不匹配而未应用 | 补丁覆盖范围过大，定位不够精确 | 拆成小补丁逐处应用，并重跑三个目标测试类确认通过。 | 跨多处编辑时分块打补丁并即时编译 |
| 环节 S5 暂存提交 | `fatal: Unable to create '.git/index.lock': Operation not permitted` | 沙箱仍只允许读取 `.git` | 按授权流程提升本仓库 `git add` 权限，成功暂存。 | 提交前确认 `.git` 写入审批 |
| 环节 S6 脚本基线 | `Unexpected count for UNAPPROVED, limit is [0,0]. Count: 36` | RAT 将新增 patch 与日志判为缺许可证 | 给 patch、日志和 diff 加 Apache 许可证头，基线恢复通过。 | patch、日志和 diff 都带许可证头 |
| 环节 S6 补丁应用 | `patch failed: src/main/java/org/apache/commons/csv/CSVParser.java:690` | 生成的是零上下文 unified diff，Git 默认不接受 | 应用及回滚统一使用 `git apply --unidiff-zero`。 | 零上下文补丁指定 `--unidiff-zero` |
| 环节 S6 失败集合提取 | 首轮多个变异误报 `shouldShowRequiredNamesInOrderIfFormatIsRendered` | 正则跨过 Surefire 自闭合的通过用例，把后续失败挂到前一用例 | 排除自闭合 `testcase` 后重跑，按正确 XML 节点提取失败。 | 只解析含 failure/error 的完整 testcase 节点 |
| 环节 S6 第 28 号变异 | 预期 `shouldCopyRequiredNamesIfInputArrayChanges` 打红但实际无失败 | Builder 绕过复制后，CSVFormat 构造函数仍再次复制 | 改为在共用数组复制函数注入，单独验证命中，整轮复核一致。 | 注入前追踪 Builder 与构造函数两次复制 |
| 环节 S6 暂存提交 | `fatal: Unable to create '.git/index.lock': Operation not permitted` | 沙箱对 `.git` 仅开放读取 | 按授权流程提升本仓库 `git add` 权限，暂存成功。 | 提交前确认 `.git` 写入审批 |
| 环节 S6 暂存差异检查 | `fault-injection/diffs/01.diff:19: trailing whitespace.` | unified diff 的空上下文行只有一个空格，作为新文件内容被 Git 判为尾随空白 | 脚本将该摘要行显式写成 `<blank context>`，修复已生成摘要并重新检查。 | 生成 diff 时把空上下文行替换为 `<blank context>` |
| 环节 S7 新增场景流程 | 第 20 份夹具会被 `assertEquals(19, fixtures.size())` 拒绝 | 执行器自检写死场景总数 | 改为只检查扫描集合非空；动态测试继续逐份运行 | 新增夹具前查找执行器是否有固定数量断言 |
| 环节 S7 图源核对 | S2 tag 中没有 `docs/sequence-diagrams/before/` | S2 只留下文字调试指南与测试源码 | 从 S2 测试重建 before 图并注明出处；after 图另画拒绝路径 | 先用 `git ls-tree` 核对历史图源，再选可追溯测试入口 |
| 环节 S7 渲染 | 首次 Java 渲染以 signal 6 退出 | 本机沙箱限制 Java 进程 | 获批后在 macOS 本机重跑 PlantUML，`sips` 确认两张 PNG | 渲染后同时查退出码、文件非空和 `sips` 格式 |
| 环节 S7 命令汇总 | zsh 报 `read-only variable: status` | 验证脚本误用 zsh 内置只读变量名 | 改用 `task_exit` 保存退出码并重跑全部 Maven 命令 | Shell 汇总脚本避开 `status` 等内置变量名 |


# 5. 与 without 对照

只读对照 [`../without/RequiredHeadersTest.java`](../without/src/test/java/org/apache/commons/csv/RequiredHeadersTest.java)。以下三组分别是“额外列与换序”“严格大小写”“空文件”；左侧 JUnit 方法、右侧 `.approved.md` 均从当前磁盘逐字取出，HTML 转义仅为并排展示原文。

### 03-extra-reordered · 额外列和换序可接受

<table><thead><tr><th>without：JUnit 测试方法原文</th><th>with：.approved.md 原文</th></tr></thead><tbody><tr><td><pre>    @Test
    void testAllPresentWithExtraColumnsInAnyOrder() throws IOException {
        final CSVFormat format = fileHeaderBuilder().setRequiredHeaders("id", "currency", "amount").get();
        assertEquals("CNY", firstValue(format, "amount,note,currency,id\n10,x,CNY,1\n", "currency"));
    }</pre></td><td><pre>&lt;!---
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
--&gt;

# 额外列和换序可接受

说明：上游调整列顺序并新增批次号。必需列仍齐全。
规则：R3
路径：A6 → B3 → C2

## 输入
```csv
batch,amount,currency,account_id
B-7,12.50,TWD,A-001
```

## 格式配置
```text
header: auto
requiredHeaders: account_id,currency,amount
```

## 解析结果
```text
状态：通过
实际表头：[batch, amount, currency, account_id]
记录：
- [B-7, 12.50, TWD, A-001]
```
</pre></td></tr></tbody></table>

### 05-case-sensitive · 严格大小写时拒绝

<table><thead><tr><th>without：JUnit 测试方法原文</th><th>with：.approved.md 原文</th></tr></thead><tbody><tr><td><pre>    @Test
    void testCaseSensitiveByDefault() {
        final CSVFormat format = fileHeaderBuilder().setRequiredHeaders("id", "currency", "amount").get();
        assertMissingHeaders(format, "ID,Currency,amount\n1,CNY,10\n", "Missing required header name(s) [id, currency] in header [ID, Currency, amount]");
    }</pre></td><td><pre>&lt;!---
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
--&gt;

# 严格大小写时拒绝

说明：上游把币种列写成大写；当前格式要求精确大小写。
规则：R2、R5
路径：A6 → B3 → C3

## 输入
```csv
account_id,CURRENCY
A-001,TWD
```

## 格式配置
```text
header: auto
requiredHeaders: currency
ignoreHeaderCase: false
```

## 解析结果
```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency]
```
</pre></td></tr></tbody></table>

### 13-empty-input · 空文件全部缺列

<table><thead><tr><th>without：JUnit 测试方法原文</th><th>with：.approved.md 原文</th></tr></thead><tbody><tr><td><pre>    @Test
    void testEmptyInputReportsAllMissing() {
        final CSVFormat format = fileHeaderBuilder().setRequiredHeaders("id", "currency", "amount").get();
        assertMissingHeaders(format, "", "Missing required header name(s) [id, currency, amount] in header []");
    }</pre></td><td><pre>&lt;!---
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
--&gt;

# 空文件全部缺列

说明：A6 自动表头配置有效，但上游为空，B1 找不到真实首条有效记录；手工推导在解析器构造期报告全部必需列缺失，不进入 C。
规则：R4、R5
路径：A6 → B1

## 输入
```csv
```

## 格式配置
```text
header: auto
requiredHeaders: currency,amount
```

## 解析结果
```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency, amount]
```
</pre></td></tr></tbody></table>

## 覆盖对照

| 规则 | without 中的测试方法 | with 中的夹具或契约测试 |
| --- | --- | --- |
| R1 | testMissingColumnFailsBeforeFirstRecord、testExplicitHeaderMissingColumn、testExplicitHeaderCheckedEvenIfSkippedLineHasColumn | 01、02、09、10、19；shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing |
| R2 | testIgnoreHeaderCaseMatchesAnyCase、testCaseSensitiveByDefault、testTrimmedHeaderSatisfiesRequirement、testUntrimmedHeaderComparedExactly | 04–08 |
| R3 | testAllPresentWithExtraColumnsInAnyOrder | 03、11、12、16、18 |
| R4 | testEmptyInputReportsAllMissing | 13、17、19 |
| R5 | testMissingColumnFailsBeforeFirstRecord、testMultipleMissingColumnsReportedInDeclaredOrder | 02、05、08、10、13、17、19；shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing |
| R6 | testEmptyOrNullDisablesCheck | 14、15；shouldPreserveOldParsingIfRequiredHeadersNeverSet、shouldDisableValidationIfRequiredHeadersAreNullOrEmpty |
| R7 | testPrinterIgnoresRequiredHeaders | 01；shouldAcceptEmptyAndShortDataIfRequiredHeaderExists、shouldAllowPrintingIfRequiredHeadersAreConfigured |
| C1 | testEmptyOrNullDisablesCheck | shouldDisableValidationIfRequiredHeadersAreNullOrEmpty、shouldReenableValidationIfCalledAfterDisabling |
| C2 | testNullRequiredNameFailsAtBuild、testBlankRequiredNameFailsAtBuild | shouldRejectInvalidNamesIfBuildingEnabledFormat、shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase |
| C3 | testRequiredHeadersWithoutHeaderFailsAtBuild；显式映射缺名、未跳首行的构建期测试：无 | shouldRejectRequiredHeadersIfHeaderModeIsAbsent、shouldRejectRequiredHeadersIfExplicitHeaderIsNotSkipped、shouldRejectRequiredHeadersIfExplicitMappingLacksName |
| C4 | testGetterReturnsDefensiveCopy、testCopyAndEquality | shouldCopyRequiredNamesIfInputArrayChanges、shouldPreserveRequiredNamesIfFormatIsCopiedOrRebuilt |
| C5 | testCopyAndEquality；顺序影响相等性的单独测试：无 | shouldKeepValueSemanticsIfRequiredNamesMatch、shouldChangeEqualityIfRequiredNamesChangeOrReorder、shouldShowRequiredNamesInOrderIfFormatIsRendered |

without 的三个方法把 CSV、配置与断言放在 Java 语句中，阅读结果要跟进 `firstValue` 或 `assertMissingHeaders` 辅助函数。with 的夹具把 CSV、配置、规则编号和解析结果放在同一份 Markdown。without 的期望异常在测试方法里写成 `CSVException` 的消息；with 的拒绝夹具写 `IllegalArgumentException` 与完整消息。with 的执行器对全文比较，变化时写 `.received.md`，可直接审阅统一 diff。两份代码都单独覆盖了换序、严格大小写与空输入。

# 6. 环节与 tag

| tag | commit | 做了什么 | `mvn test` 结果 |
| --- | --- | --- | --- |
| `as-s0-baseline` | `9e3c36a4` | 克隆并记录环境基线 | 924 次；失败 0、错误 0、跳过 11 |
| `as-s1-clarified` | `f4e8f063` | 澄清业务决策 | 924 次；失败 0、错误 0、跳过 11 |
| `as-s2-spec-draft` | `3a29ceae` | 规格草案、业务事实 | 925 次；失败 0、错误 0、跳过 11 |
| `as-s2-spec-approved` | `e1588ef0` | 完成评审，补端到端现状表征 | 926 次；失败 0、错误 0、跳过 11 |
| `as-s3-red` | `7db8c489` | 加夹具执行器与契约红态 | 974 次；失败 26、错误 0、跳过 11（预期红态） |
| `as-s4-green` | `b7b8de34` | 实现物理表头必需列校验 | 975 次；失败 0、错误 0、跳过 11 |
| `as-s5-refactor` | `3b897c04` | 行为保持重构 | 975 次；失败 0、错误 0、跳过 11 |
| `as-s6-fault-injection` | `0f33cafa` | 35 个单点变异验证 | 975 次；失败 0、错误 0、跳过 11 |
| `as-s7-guide` | 本表所在提交，运行 `git rev-parse as-s7-guide` 查询 | 补理解指南、两阶段图和文档验证 | 975 次；失败 0、错误 0、跳过 11 |

以下保留各环节当时的简记；上表为可核查的 commit 与测试索引。

- S0 克隆与基线：`as-s0-baseline`。
- S3 Approved Scenarios 红态：`as-s3-red`；下一环节实现必需列表头行为。
- S4 Approved Scenarios 绿态：`as-s4-green`；下一环节 S5 待指定。
- S5 行为保持重构：`as-s5-refactor`；完整 `mvn test` 与 S4 一致，975 次、失败 0、错误 0、跳过 11。
- S6 故障注入：`as-s6-fault-injection`；35 个变异全部匹配预测；完整 `mvn test` 通过，975 次、失败 0、错误 0、跳过 11。
