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

# 显式映射核对真实首行

说明：系统预设账户和币种两列，并跳过文件表头。文件真实首行也有两列。
规则：R1

## 输入
```csv
account_id,currency
A-001,TWD
```

## 格式配置
```text
header: account_id,currency
skipHeaderRecord: true
requiredHeaders: account_id,currency
```

## 解析结果
```text
状态：通过
实际表头：[account_id, currency]
记录：
- [A-001, TWD]
```
