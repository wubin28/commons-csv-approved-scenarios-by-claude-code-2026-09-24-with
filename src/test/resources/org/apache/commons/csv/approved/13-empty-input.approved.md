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

# 空文件全部缺列

说明：上游传来空文件。所有必需列都无法确认存在。
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
