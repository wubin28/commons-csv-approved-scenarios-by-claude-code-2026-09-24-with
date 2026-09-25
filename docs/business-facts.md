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

# 对账业务事实 · Approved Scenarios

本页由 `ScenarioBookTest` 从 `src/test/resources/org/apache/commons/csv/approved/` 生成。期望结果由 S2/S2R 规格手工推导；此测试只核对文档同步，不运行待实现 API。规则定义见 [required-headers.md](specs/required-headers.md)。

## 场景目录

| 编号 | 业务场景 | 对应规则 | 判定路径 |
| --- | --- | --- | --- |
| 01 | [账单列齐全](#scenario-01) | R1、R7 | A6 → B3 → C2 |
| 02 | [一次报告两项缺列](#scenario-02) | R1、R5 | A6 → B3 → C3 |
| 03 | [额外列和换序可接受](#scenario-03) | R3 | A6 → B3 → C2 |
| 04 | [忽略大小写后匹配](#scenario-04) | R2 | A6 → B3 → C2 |
| 05 | [严格大小写时拒绝](#scenario-05) | R2、R5 | A6 → B3 → C3 |
| 06 | [忽略未引用表头周围空白](#scenario-06) | R2 | A6 → B3 → C2 |
| 07 | [修剪引号内表头空白](#scenario-07) | R2 | A6 → B3 → C2 |
| 08 | [未启用空白处理时拒绝](#scenario-08) | R2、R5 | A6 → B3 → C3 |
| 09 | [显式映射核对真实首行](#scenario-09) | R1 | A7 → B4 → C2 |
| 10 | [显式映射不能掩盖缺列](#scenario-10) | R1、R5 | A7 → B4 → C3 |
| 11 | [允许重复列时仍算存在](#scenario-11) | R3 | A6 → B3 → C2 |
| 12 | [重复表头错误优先](#scenario-12) | R3 | A6 → B3 → C1 |
| 13 | [空文件全部缺列](#scenario-13) | R4、R5 | A6 → B1 |
| 14 | [关闭功能维持自动表头行为](#scenario-14) | R6 | A1 |
| 15 | [关闭功能维持显式表头行为](#scenario-15) | R6 | A1 |
| 16 | [允许空列名时仍可核对必需列](#scenario-16) | R3 | A6 → B3 → C2 |
| 17 | [只有注释，没有表头](#scenario-17) | R4、R5 | A6 → B1 |
| 18 | [重复空列名可接受](#scenario-18) | R3 | A6 → B3 → C2 |
| 19 | [显式表头遇到空文件](#scenario-19) | R1、R4、R5 | A7 → B2 |

## <a id="scenario-01"></a>01 · 账单列齐全

业务说明：A6 的配置条件由格式构建后进入真实表头读取来验收；B3 读到首行，C2 确认账户、币种、金额都存在，手工推导校验通过。

对应规则：R1、R7

判定路径：A6 → B3 → C2

输入（原样 CSV）：

```csv
account_id,currency,amount
A-001,TWD,12.50
```

格式配置：

```text
header: auto
requiredHeaders: account_id,currency,amount
```

期望结果：

```text
状态：通过
实际表头：[account_id, currency, amount]
记录：
- [A-001, TWD, 12.50]
```

## <a id="scenario-02"></a>02 · 一次报告两项缺列

业务说明：账单只有账户列。旧流程要在按列名取值时才发现缺列；新校验让运营在解析器创建时一次看到缺少币种和金额。

对应规则：R1、R5

判定路径：A6 → B3 → C3

输入（原样 CSV）：

```csv
account_id
A-001
```

格式配置：

```text
header: auto
requiredHeaders: currency,amount
```

期望结果：

```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency, amount]
```

## <a id="scenario-03"></a>03 · 额外列和换序可接受

业务说明：上游调整列顺序并新增批次号。必需列仍齐全。

对应规则：R3

判定路径：A6 → B3 → C2

输入（原样 CSV）：

```csv
batch,amount,currency,account_id
B-7,12.50,TWD,A-001
```

格式配置：

```text
header: auto
requiredHeaders: account_id,currency,amount
```

期望结果：

```text
状态：通过
实际表头：[batch, amount, currency, account_id]
记录：
- [B-7, 12.50, TWD, A-001]
```

## <a id="scenario-04"></a>04 · 忽略大小写后匹配

业务说明：上游把币种列写成大写。当前格式允许忽略大小写。

对应规则：R2

判定路径：A6 → B3 → C2

输入（原样 CSV）：

```csv
account_id,CURRENCY
A-001,TWD
```

格式配置：

```text
header: auto
requiredHeaders: currency
ignoreHeaderCase: true
```

期望结果：

```text
状态：通过
实际表头：[account_id, CURRENCY]
记录：
- [A-001, TWD]
```

## <a id="scenario-05"></a>05 · 严格大小写时拒绝

业务说明：上游把币种列写成大写；当前格式要求精确大小写。

对应规则：R2、R5

判定路径：A6 → B3 → C3

输入（原样 CSV）：

```csv
account_id,CURRENCY
A-001,TWD
```

格式配置：

```text
header: auto
requiredHeaders: currency
ignoreHeaderCase: false
```

期望结果：

```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency]
```

## <a id="scenario-06"></a>06 · 忽略未引用表头周围空白

业务说明：上游在币种列名前后留了空格。格式已开启周围空白忽略。

对应规则：R2

判定路径：A6 → B3 → C2

输入（原样 CSV）：

```csv
account_id, currency 
A-001,TWD
```

格式配置：

```text
header: auto
requiredHeaders: currency
ignoreSurroundingSpaces: true
```

期望结果：

```text
状态：通过
实际表头：[account_id, currency]
记录：
- [A-001, TWD]
```

## <a id="scenario-07"></a>07 · 修剪引号内表头空白

业务说明：上游把带空白的币种列名放在引号内。格式启用 trim。

对应规则：R2

判定路径：A6 → B3 → C2

输入（原样 CSV）：

```csv
account_id," currency "
A-001,TWD
```

格式配置：

```text
header: auto
requiredHeaders: currency
trim: true
```

期望结果：

```text
状态：通过
实际表头：[account_id, currency]
记录：
- [A-001, TWD]
```

## <a id="scenario-08"></a>08 · 未启用空白处理时拒绝

业务说明：币种列名前后多了空格；当前格式不忽略这些空格。

对应规则：R2、R5

判定路径：A6 → B3 → C3

输入（原样 CSV）：

```csv
account_id, currency 
A-001,TWD
```

格式配置：

```text
header: auto
requiredHeaders: currency
ignoreSurroundingSpaces: false
trim: false
```

期望结果：

```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency]
```

## <a id="scenario-09"></a>09 · 显式映射核对真实首行

业务说明：系统预设账户和币种两列，并跳过文件表头。文件真实首行也有两列。

对应规则：R1

判定路径：A7 → B4 → C2

输入（原样 CSV）：

```csv
account_id,currency
A-001,TWD
```

格式配置：

```text
header: account_id,currency
skipHeaderRecord: true
requiredHeaders: account_id,currency
```

期望结果：

```text
状态：通过
实际表头：[account_id, currency]
记录：
- [A-001, TWD]
```

## <a id="scenario-10"></a>10 · 显式映射不能掩盖缺列

业务说明：A7 的显式映射、跳首行、必需名齐全三项条件均满足，故进入 B4；被跳过的真实首行 account_id,amount 在 C3 缺 currency，手工推导构造解析器时拒绝。

对应规则：R1、R5

判定路径：A7 → B4 → C3

输入（原样 CSV）：

```csv
account_id,amount
A-001,12.50
```

格式配置：

```text
header: account_id,currency
skipHeaderRecord: true
requiredHeaders: currency
```

期望结果：

```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency]
```

## <a id="scenario-11"></a>11 · 允许重复列时仍算存在

业务说明：上游重复发送币种列。当前格式允许重复表头，币种仍已出现。

对应规则：R3

判定路径：A6 → B3 → C2

输入（原样 CSV）：

```csv
account_id,currency,currency
A-001,TWD,USD
```

格式配置：

```text
header: auto
requiredHeaders: currency
duplicateHeaderMode: ALLOW_ALL
```

期望结果：

```text
状态：通过
实际表头：[account_id, currency, currency]
记录：
- [A-001, TWD, USD]
```

## <a id="scenario-12"></a>12 · 重复表头错误优先

业务说明：A6 自动表头配置有效，B3 读到真实首行；虽然缺 amount，C1 因禁止重复先报告既有 currency 重复错误，不再做缺列比较。

对应规则：R3

判定路径：A6 → B3 → C1

输入（原样 CSV）：

```csv
account_id,currency,currency
A-001,TWD,USD
```

格式配置：

```text
header: auto
requiredHeaders: amount
duplicateHeaderMode: DISALLOW
```

期望结果：

```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：The header contains a duplicate name: "currency" in [account_id, currency, currency]. If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().
```

## <a id="scenario-13"></a>13 · 空文件全部缺列

业务说明：A6 自动表头配置有效，但上游为空，B1 找不到真实首条有效记录；手工推导在解析器构造期报告全部必需列缺失，不进入 C。

对应规则：R4、R5

判定路径：A6 → B1

输入（原样 CSV）：

```csv
```

格式配置：

```text
header: auto
requiredHeaders: currency,amount
```

期望结果：

```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency, amount]
```

## <a id="scenario-14"></a>14 · 关闭功能维持自动表头行为

业务说明：未设置非空必需列命中 A1；场景验收其“沿用旧行为”动作：只有账户和金额的账单照旧解析，不进入 B/C。

对应规则：R6

判定路径：A1

输入（原样 CSV）：

```csv
account_id,amount
A-001,12.50
```

格式配置：

```text
header: auto
requiredHeaders: off
```

期望结果：

```text
状态：通过
实际表头：[account_id, amount]
记录：
- [A-001, 12.50]
```

## <a id="scenario-15"></a>15 · 关闭功能维持显式表头行为

业务说明：必需列功能关闭命中 A1；即使显式映射且未跳过首行，也沿用旧行为，把文件首行当作数据，不进入 B/C。

对应规则：R6

判定路径：A1

输入（原样 CSV）：

```csv
account_id,amount
A-001,12.50
```

格式配置：

```text
header: account_id,currency
skipHeaderRecord: false
requiredHeaders: off
```

期望结果：

```text
状态：通过
实际表头：[account_id, amount]
记录：
- [account_id, amount]
- [A-001, 12.50]
```

## <a id="scenario-16"></a>16 · 允许空列名时仍可核对必需列

业务说明：上游有一列未命名，另一列为币种。格式允许空列名。

对应规则：R3

判定路径：A6 → B3 → C2

输入（原样 CSV）：

```csv
,currency
unused,TWD
```

格式配置：

```text
header: auto
requiredHeaders: currency
allowMissingColumnNames: true
```

期望结果：

```text
状态：通过
实际表头：[, currency]
记录：
- [unused, TWD]
```

## <a id="scenario-17"></a>17 · 只有注释，没有表头

业务说明：上游只送来一行文件注释，没有可核对的表头。币种和金额都算缺失。

对应规则：R4、R5

判定路径：A6 → B1

输入（原样 CSV）：

```csv
# 本批次暂无数据
```

格式配置：

```text
header: auto
requiredHeaders: currency,amount
commentMarker: #
```

期望结果：

```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency, amount]
```

## <a id="scenario-18"></a>18 · 重复空列名可接受

业务说明：上游有两列没有名称，币种列仍存在。当前格式允许重复空列名。

对应规则：R3

判定路径：A6 → B3 → C2

输入（原样 CSV）：

```csv
,currency,
unused,TWD,other
```

格式配置：

```text
header: auto
requiredHeaders: currency
allowMissingColumnNames: true
duplicateHeaderMode: ALLOW_EMPTY
```

期望结果：

```text
状态：通过
实际表头：[, currency, ]
记录：
- [unused, TWD, other]
```

## <a id="scenario-19"></a>19 · 显式表头遇到空文件

业务说明：系统预设账户与币种列，并准备跳过文件首行。上游文件为空，无法证明真实表头存在，两个必需列都算缺失。

对应规则：R1、R4、R5

判定路径：A7 → B2

输入（原样 CSV）：

```csv
```

格式配置：

```text
header: account_id,currency
skipHeaderRecord: true
requiredHeaders: account_id,currency
```

期望结果：

```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [account_id, currency]
```
