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

# 单步运行现有 CSV 对账路径

这份指南配合 [`RequiredHeadersExistingFlowE2ETest`](../src/test/java/org/apache/commons/csv/RequiredHeadersExistingFlowE2ETest.java) 使用。它从一份真正的 [测试 CSV 文件](../src/test/resources/org/apache/commons/csv/e2e/reconciliation-missing-currency.csv) 出发，经过 Commons CSV 的格式构建、文件读取、表头映射和记录迭代，最后执行“账户 → 币种”的业务归集。测试**只调用现有 API**，用于观察新需求要修复的旧行为；它不是 Required Headers 实现验收测试。

## 为什么选择这条测试

现有 [`CSVFileParserTest`](../src/test/java/org/apache/commons/csv/CSVFileParserTest.java) 会读取 CSV 文件，但主要核对原始字段；[`CSVParserTest`](../src/test/java/org/apache/commons/csv/CSVParserTest.java) 有按列名读取，却主要使用内联字符串。两者都没有一条可直接单步跟踪“业务文件 → 显式映射 → 业务取值”的用例。因此补一条小型端到端**现状表征**测试，输入与 [Approved Scenario 10](../src/test/resources/org/apache/commons/csv/approved/10-explicit-physical-missing.approved.md) 的 CSV 相同。

```csv
account_id,amount
A-001,12.50
```

测试格式声明 `account_id,currency`，并跳过文件真实首行。当前代码把声明的 `currency` 映射到第二列，所以业务归集得到 `A-001 → 12.50`，尽管第二列实际叫 `amount`。测试断言这个**现有错误映射**，以便调试时稳定复现。未来启用 `setRequiredHeaders("currency")` 后，场景 10 的拟定期望是解析器构造期报 `Missing required CSV headers: [currency]`；本测试不调用该新 API，因而未来仍可用于查看关闭功能时的旧路径。

## 命令行先运行一次

在本仓库根目录执行：

```bash
mvn -Dtest=RequiredHeadersExistingFlowE2ETest test
```

应看到 `Tests run: 1, Failures: 0, Errors: 0` 和 `BUILD SUCCESS`。这只验证当前生产代码的实际路径；[场景 10 的新期望](business-facts.md#scenario-10)仍由 S2 规格手工推导，尚未运行。

## 在 VS Code 中用 Debug 单步运行

1. 用 VS Code **打开本仓库根目录**（包含 `pom.xml` 的 `with/`），等待 Maven/Java 项目导入完成。安装 [Extension Pack for Java](https://code.visualstudio.com/docs/java/extensions)，其中包含 Test Runner for Java 与 Debugger for Java；该项目已在 `pom.xml` 使用 JUnit 5。VS Code 官方 [Java 测试指南](https://code.visualstudio.com/docs/java/java-testing)确认测试方法旁和 Testing 视图都可启动 Debug。
2. 打开 [测试类](../src/test/java/org/apache/commons/csv/RequiredHeadersExistingFlowE2ETest.java)。在 `shouldMapAmountAsCurrencyInExistingExplicitHeaderFlow` 方法上方点击 **Debug Test**；若未显示 CodeLens，在左侧 **Testing** 视图找到此测试并点调试图标。这里启动的是 JUnit 测试，不需要 `main()` 或 `launch.json`。
3. 先在 [测试类第 48 行](../src/test/java/org/apache/commons/csv/RequiredHeadersExistingFlowE2ETest.java#L48) 设置断点。调试暂停时查看 `csvFile`、`format`；`format` 的显式表头应是 `account_id,currency`，真实 CSV 文件首行却是 `account_id,amount`。
4. 继续或 Step Into，依次观察 [`CSVParser.parse(URL,...)`](../src/main/java/org/apache/commons/csv/CSVParser.java#L444) → [构造函数中的 `createHeaders()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L564) → [`createHeaders()` 显式分支](../src/main/java/org/apache/commons/csv/CSVParser.java#L601)。在该方法第 616 行附近设断点：`skipHeaderRecord=true` 时确实读走真实首行，但随后把 `formatHeader` 赋给 `headerRecord`，所以映射采用本地声明，没有校验被跳过字段。这里正是 Required Headers 将来需要增加真实表头检查的位置；当前代码尚无此检查。
5. 在 [业务归集行](../src/test/java/org/apache/commons/csv/RequiredHeadersExistingFlowE2ETest.java#L50) 再设断点，Step Into [`CSVRecord#get(String)`](../src/main/java/org/apache/commons/csv/CSVRecord.java#L125)。观察 `record.values()` 为 `[A-001, 12.50]`，`headerMap` 却为 `account_id → 0, currency → 1`；`record.get("currency")` 返回 `12.50`。
6. 继续到 [断言](../src/test/java/org/apache/commons/csv/RequiredHeadersExistingFlowE2ETest.java#L55)，查看 `currencyByAccount` 为 `{A-001=12.50}`，然后继续执行到测试通过。[VS Code 官方 Java 调试指南](https://code.visualstudio.com/docs/java/java-debugging)说明断点、Step Into/Over、Variables 和 Call Stack 的操作。

若要看自动表头分支，可在同一 [`createHeaders()`](../src/main/java/org/apache/commons/csv/CSVParser.java#L601) 对照第 608 行：空的 `setHeader()` 会把首条 CSV 记录直接作为映射；本测试走第 616 行的显式分支。若要看缺列晚报，调试时把 `record.get("currency")` 改成一个**未声明**的名字才会走 [`Mapping for ... not found`](../src/main/java/org/apache/commons/csv/CSVRecord.java#L132)；不要提交这种临时修改。

## 对照规格时看哪些点

| 调试观察 | 对应规格 |
| --- | --- |
| `Builder#get()` 后 `format` 含显式表头和 `skipHeaderRecord=true` | [A7 构建期配置](specs/required-headers.md)；C3 |
| 构造解析器时 `createHeaders()` 消耗真实首行，却只用显式声明建映射 | [B4 真实首行存在](specs/required-headers.md)；Q0/Q3/R1 |
| `record.get("currency")` 把金额 `12.50` 当币种 | [场景 10](business-facts.md#scenario-10)；新功能应在 C3 先拒绝缺列 |
| 本测试通过仍暴露业务错误 | [R6 关闭时旧行为](specs/required-headers.md)；启用后要由未来 Approved Scenarios 执行器验证新行为 |

测试不运行 `setRequiredHeaders`；源码目前不存在该方法。调试所得是**现有行为**，不能把它当成新功能已经实现或已通过的证据。

本轮以 Maven 单测和全量测试验证代码路径；VS Code 图形界面的点击步骤依据上述官方 Java 测试/调试文档，尚未在本机 GUI 逐步实测。
