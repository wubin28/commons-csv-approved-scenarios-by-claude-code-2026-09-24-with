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

# 重复表头错误优先

说明：上游重复币种列，同时缺少金额。当前格式禁止重复，先报告既有表头错误。
规则：R3
路径：A6 → B3 → C1

## 输入
```csv
account_id,currency,currency
A-001,TWD,USD
```

## 格式配置
```text
header: auto
requiredHeaders: amount
duplicateHeaderMode: DISALLOW
```

## 解析结果
```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：The header contains a duplicate name: "currency" in [account_id, currency, currency]. If this is valid then use CSVFormat.Builder.setDuplicateHeaderMode().
```
