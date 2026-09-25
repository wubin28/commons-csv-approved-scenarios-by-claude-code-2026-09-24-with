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

# 账单列齐全

说明：A6 的配置条件由格式构建后进入真实表头读取来验收；B3 读到首行，C2 确认账户、币种、金额都存在，手工推导校验通过。
规则：R1、R7
路径：A6 → B3 → C2

## 输入
```csv
account_id,currency,amount
A-001,TWD,12.50
```

## 格式配置
```text
header: auto
requiredHeaders: account_id,currency,amount
```

## 解析结果
```text
状态：通过
实际表头：[account_id, currency, amount]
记录：
- [A-001, TWD, 12.50]
```
