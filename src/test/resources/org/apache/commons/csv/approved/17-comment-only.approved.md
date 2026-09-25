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

# 只有注释，没有表头

说明：上游只送来一行文件注释，没有可核对的表头。币种和金额都算缺失。
规则：R4、R5
路径：A6 → B1

## 输入
```csv
# 本批次暂无数据
```

## 格式配置
```text
header: auto
requiredHeaders: currency,amount
commentMarker: #
```

## 解析结果
```text
状态：拒绝
异常类型：IllegalArgumentException
完整消息：Missing required CSV headers: [currency, amount]
```
