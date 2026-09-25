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

# 关闭功能维持自动表头行为

说明：未设置非空必需列，A1 关闭校验；只有账户和金额的账单即使没有币种列，也按旧行为解析，不进入 B/C。
规则：R6
路径：A1

## 输入
```csv
account_id,amount
A-001,12.50
```

## 格式配置
```text
header: auto
requiredHeaders: off
```

## 解析结果
```text
状态：通过
实际表头：[account_id, amount]
记录：
- [A-001, 12.50]
```
