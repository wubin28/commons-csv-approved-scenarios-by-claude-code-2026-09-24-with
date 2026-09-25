<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements. See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License. You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Required Headers 实现后的单步调试

本指南用两条**独立**的既有自动化测试。成功路径沿用 S2 的 [文件读取对账测试](../src/test/java/org/apache/commons/csv/RequiredHeadersExistingFlowE2ETest.java) 和[实例化 CSV](../src/test/resources/org/apache/commons/csv/e2e/reconciliation-missing-currency.csv)：文件首行 `account_id,amount`，第二行 `A-001,12.50`；测试按列名取 `record.get("account_id")` 和 `record.get("currency")`，把结果放入业务映射。拒绝路径沿用 [`RequiredHeadersContractTest#shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing`](../src/test/java/org/apache/commons/csv/RequiredHeadersContractTest.java#L234)，其 CSV 是测试方法中的字符串；它单独启用 `setRequiredHeaders("currency")`，在读取数据记录前抛异常。两者不共享一次解析调用，无须新增测试数据或生产 API。

## 运行

在仓库根目录依次执行：

```bash
mvn -Dtest=RequiredHeadersExistingFlowE2ETest test
mvn -Dtest=RequiredHeadersContractTest#shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing test
```

两条命令各应出现 `Tests run: 1, Failures: 0, Errors: 0` 与 `BUILD SUCCESS`。第一条通过说明**关闭必需列功能时的旧行为被保留**；业务映射实际为 `{A-001=12.50}`。第二条通过说明构造解析器时抛出 `IllegalArgumentException`，消息逐字为 `Missing required CSV headers: [currency]`。

## VS Code Debug

1. 在 VS Code 打开本仓库根目录，安装 Extension Pack for Java，等 Maven 项目导入完成。打开 `RequiredHeadersExistingFlowE2ETest.java`，在方法 `shouldMapAmountAsCurrencyInExistingExplicitHeaderFlow` 上方点击 **Debug Test**；也可在 **Testing** 视图找该方法并点调试图标。
2. 成功测试建议在 `CSVParser.parse(csvFile, StandardCharsets.UTF_8, format)`、`CSVParser#createHeaders()` 的显式分支、`record.get("currency")` 设断点。调用路径：测试 → `CSVParser.parse(URL, Charset, CSVFormat)` → `CSVParser.Builder#get()` → `CSVParser` 构造函数 → `createHeaders()` 读走物理首行 → `CSVRecord#get(String)` 按声明映射取第 2 列 → 业务 `currencyByAccount.put(...)`。观察 `format.getRequiredHeaders()` 为空，物理表头 `[account_id, amount]`，声明映射 `[account_id, currency]`，`record.get("currency")` 为 `12.50`。
3. 另起一次 Debug：打开 `RequiredHeadersContractTest.java`，在 `shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing` 上方点击 **Debug Test**。建议在 `CSVParser.parse("account_id,amount...", format)`、`CSVParser#createHeaders()`、`CSVParser#validateRequiredHeaders()` 的 `missing.add(name)` 与抛异常行设断点。调用路径：测试 → `CSVFormat.Builder#get()` → `CSVParser.parse(String, CSVFormat)` → `CSVParser.parse(Reader, CSVFormat)` → `CSVParser.Builder#get()` → `CSVParser` 构造函数 → `createHeaders()` → `nextRecord()` 调 `Lexer#nextToken()` 只读物理表头 → `validateRequiredHeaders(physicalHeader, true)` → `IllegalArgumentException` → 测试中的 `assertThrows`。观察 `physicalHeader=[account_id, amount]`、`required=[currency]`、`missing=[currency]`；`A-001,12.50` 尚未读取。

## S2 预期与当前结果

| 阶段 | 设置 | 可观察结果 |
| --- | --- | --- |
| [S2 现状指南](required-headers-by-running-e2e-test.md) | 显式声明 `account_id,currency`，跳过真实首行，未启用必需列 | 取 `currency` 得 `12.50`，业务误把金额当币种；表征测试通过。 |
| [S2 spec 场景 10](business-facts.md#scenario-10) | 同一文件、同一映射，另启用 `setRequiredHeaders("currency")` | 拟在解析器构造期拒绝，消息 `Missing required CSV headers: [currency]`。 |
| 本阶段实现 | 旧测试保持功能关闭；独立契约测试启用必需列 | 旧测试仍观察 `{A-001=12.50}`；契约测试实际捕获预期异常。业务夹具 10 也验证同一拒绝事实。 |

## Sequence diagrams 与本机渲染

S2 仓库未保存 `docs/sequence-diagrams/before/` 图源或 PNG；这里依照 S2 测试源码重建 [before 图源](sequence-diagrams/before/existing-explicit-mapping.puml)和 [before PNG](sequence-diagrams/before/existing-explicit-mapping.png)。[after 图源](sequence-diagrams/after/required-headers-missing.puml)及 [after PNG](sequence-diagrams/after/required-headers-missing.png)只画独立的缺列拒绝测试；图中异常从 `CSVParser#validateRequiredHeaders` 抛出，经构造器和静态 `parse` 传播到测试。语法采用 [PlantUML 官方 sequence diagram 文档](https://plantuml.com/sequence-diagram)的参与者、消息、自调用和注释写法。

本机 macOS 使用 PlantUML `1.2025.10`。下载工具放 `/private/tmp`；仓库 `.gitignore` 也忽略 `docs/sequence-diagrams/tools/`。可复制执行：

```bash
curl -fL -o /private/tmp/plantuml-as-s7.jar https://github.com/plantuml/plantuml/releases/download/v1.2025.10/plantuml-1.2025.10.jar
java -jar /private/tmp/plantuml-as-s7.jar -tpng docs/sequence-diagrams/after/required-headers-missing.puml docs/sequence-diagrams/before/existing-explicit-mapping.puml
test -s docs/sequence-diagrams/after/required-headers-missing.png
test -s docs/sequence-diagrams/before/existing-explicit-mapping.png
sips -g format -g pixelWidth -g pixelHeight docs/sequence-diagrams/after/required-headers-missing.png docs/sequence-diagrams/before/existing-explicit-mapping.png
```

预期 PlantUML 命令退出码 0；两个 `test -s` 退出码 0；`sips` 显示两张图 `format: png`。本机实测 after 为 `2073×882`、before 为 `1177×676` 像素。
