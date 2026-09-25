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

# 显式映射不能掩盖缺列

说明：系统预设账户和币种两列，但文件真实首行只有账户和金额。旧映射会把金额值当成币种值；新校验必须先拒绝文件。
规则：R1、R5
路径：A7 → B4 → C3

## 输入
```csv
account_id,amount
A-001,12.50
```

## 格式配置
```text
header: account_id,currency
skipHeaderRecord: true
requiredHeaders: currency
```

## 解析结果
```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency]
```
