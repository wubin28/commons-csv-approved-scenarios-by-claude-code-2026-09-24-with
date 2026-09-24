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

# 允许空列名时仍可核对必需列

说明：上游有一列未命名，另一列为币种。格式允许空列名。
规则：R3

## 输入
```csv
,currency
unused,TWD
```

## 格式配置
```text
header: auto
requiredHeaders: currency
allowMissingColumnNames: true
```

## 解析结果
```text
状态：通过
实际表头：[, currency]
记录：
- [unused, TWD]
```
