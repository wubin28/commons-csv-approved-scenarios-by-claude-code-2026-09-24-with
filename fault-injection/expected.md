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
# S6 故障注入预测

失败集合中，`01`–`19` 指同编号 approved 夹具；其他标识指 `RequiredHeadersContractTest` 方法。逗号分隔，无空格。

| 变异编号 | 改了什么 | 针对的夹具或测试 | 预期失败的完整集合（夹具编号 + 契约测试方法名） | 预期理由 |
|---|---|---|---|---|
| 01 | require extra column | 01 | 01,09 | 完整表头也须通过 |
| 02 | stop after first missing | 02 | 02,13,17,19 | 一次报齐所有缺失列 |
| 03 | require declared order | 03 | 03,04,06,07,11,16,18,shouldAcceptEmptyAndShortDataIfRequiredHeaderExists,shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped | 额外列与列顺序不影响存在性 |
| 04 | ignore case flag | 04 | 04 | 忽略大小写时大小写等价 |
| 05 | fold case unconditionally | 05 | 05 | 区分大小写时必须精确匹配 |
| 06 | ignore space setting | 06 | 06 | 开启周围空白忽略后列名匹配 |
| 07 | ignore trim setting | 07 | 07 | 开启 trim 后引号内空白也被去除 |
| 08 | trim all unquoted spaces | 08 | 08 | 关闭空白处理时空白仍有业务意义 |
| 09 | reject complete explicit mapping | 09 | 09,19,shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped | 显式映射完整时允许解析 |
| 10 | trust explicit mapping | 10 | 10,19,shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing | 显式映射不能代替物理表头 |
| 11 | disallow duplicate headers | 11 | 11 | 允许重复列时不能拒绝 |
| 12 | skip duplicate check | 12 | 12 | 禁止重复列应先报重复错误 |
| 13 | skip absent header validation | 13 | 13,17,19 | 空输入仍缺少必需表头 |
| 14 | reject disabled auto mode | 14 | 01,03,04,06,07,09,11,14,15,16,18,shouldDisableValidationIfRequiredHeadersAreNullOrEmpty,shouldPreserveOldParsingIfRequiredHeadersNeverSet | 关闭功能后自动表头按旧行为解析 |
| 15 | force skip explicit header | 15 | 15 | 关闭功能后显式表头按旧行为解析 |
| 16 | disallow blank optional header | 16 | 16,18,shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped | 允许空列名时必需列仍可匹配 |
| 17 | skip comment only validation | 17 | 17 | 仅注释输入仍缺必需列 |
| 18 | disallow empty duplicates | 18 | 18 | ALLOW_EMPTY 容许重复空列名 |
| 19 | skip empty explicit validation | 19 | 19 | 显式映射也不能让空输入通过 |
| 20 | keep disabled requirement | shouldDisableValidationIfRequiredHeadersAreNullOrEmpty | shouldDisableValidationIfRequiredHeadersAreNullOrEmpty | 传入 null 或空数组应关闭校验，不能保留旧必需列 |
| 21 | ignore invalid required names | shouldRejectInvalidNamesIfBuildingEnabledFormat | shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive,shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase,shouldRejectInvalidNamesIfBuildingEnabledFormat,shouldRejectRequiredHeadersIfExplicitMappingLacksName | 必需列名不得为空白或重复；构建期必须拒绝无效配置 |
| 22 | ignore case for duplicates | shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase | shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase | 最终忽略大小写时，大小写等价的必需名与显式映射应按同一列处理 |
| 23 | always fold required names | shouldAllowCaseDistinctNamesIfFinalCaseModeIsSensitive | shouldAllowCaseDistinctNamesIfFinalCaseModeIsSensitive,shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive | 最终区分大小写时，不同大小写的名称是不同列 |
| 24 | allow absent header mode | shouldRejectRequiredHeadersIfHeaderModeIsAbsent | shouldRejectRequiredHeadersIfHeaderModeIsAbsent | 声明必需列前必须选择表头模式，避免无物理表头可检查 |
| 25 | allow unskipped explicit header | shouldRejectRequiredHeadersIfExplicitHeaderIsNotSkipped | shouldRejectRequiredHeadersIfExplicitHeaderIsNotSkipped | 显式表头必须跳过物理表头记录，才能在读取数据前验证它 |
| 26 | allow unmapped required header | shouldRejectRequiredHeadersIfExplicitMappingLacksName | shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive,shouldRejectRequiredHeadersIfExplicitMappingLacksName | 显式映射必须包含每个必需列名，大小写规则按最终配置判断 |
| 27 | reject null optional mapping | shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped | shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped | 允许空白可选列名时，有效必需列仍可解析 |
| 28 | keep caller array | shouldCopyRequiredNamesIfInputArrayChanges | shouldCopyRequiredNamesIfInputArrayChanges | 外部修改调用方数组不能改变已构建格式的必需列 |
| 29 | drop required on builder copy | shouldPreserveRequiredNamesIfFormatIsCopiedOrRebuilt | 02,05,08,10,13,17,19,shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldAllowCaseDistinctNamesIfFinalCaseModeIsSensitive,shouldCopyRequiredNamesIfInputArrayChanges,shouldOverwriteApprovedIfApprovalIsExplicit,shouldPreserveRequiredNamesIfFormatIsCopiedOrRebuilt,shouldReenableValidationIfCalledAfterDisabling,shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing,shouldWriteReceivedAndFailIfResultDiffers | 复制或重建格式后，必需列约束仍须保留 |
| 30 | ignore required in equality | shouldChangeEqualityIfRequiredNamesChangeOrReorder | shouldChangeEqualityIfRequiredNamesChangeOrReorder | 必需列集合不同或顺序不同，格式值语义应不同 |
| 31 | compare required by identity | shouldKeepValueSemanticsIfRequiredNamesMatch | shouldKeepValueSemanticsIfRequiredNamesMatch | 相同必需列内容的两个格式应相等且哈希相同 |
| 32 | hide required in description | shouldShowRequiredNamesInOrderIfFormatIsRendered | shouldShowRequiredNamesInOrderIfFormatIsRendered | 格式描述应按声明顺序列出必需列 |
| 33 | reject print with required | shouldAllowPrintingIfRequiredHeadersAreConfigured | shouldAllowPrintingIfRequiredHeadersAreConfigured | 配置必需列不应禁止 CSV 打印 |
| 34 | skip explicit physical validation | shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing | 10,19,shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase,shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing | 显式映射不能代替物理表头；缺列应在解析器构造时拒绝 |
| 35 | reject short data row | shouldAcceptEmptyAndShortDataIfRequiredHeaderExists | shouldAcceptEmptyAndShortDataIfRequiredHeaderExists | 只要求表头存在，不要求每行数据非空或列数完整 |

## 首轮预测修订

首轮运行前，上表失败集合只列直接目标。Surefire XML 解析修正后，以下编号发现连带失败；现表逐项列出完整集合：

- 01：把“必需列数等于物理列数”误判为不足，也会拒绝显式完整表头 09。
- 02：只报第一个缺失列，除 02 外，空输入和仅注释输入的 13、17、19 也失去完整错误消息。
- 03：把存在性误写成位置相等，还会拒绝大小写/空白处理后的表头、重复列、可选空列和短记录场景，以及可选空映射契约。
- 09：显式映射等于必需集合时错误拒绝，还会影响空物理输入、大小写等价映射和可选空映射契约。
- 10：以显式映射代替物理表头检查，还会影响空物理输入及对应两个构建/解析契约。
- 13：仅有物理表头时才校验，会同时放过自动空输入、仅注释输入、显式空输入。
- 14：把功能关闭误作构建错误，也会打红所有需要在执行器中构造关闭校验的物理表头格式，以及关闭功能和旧行为契约。
- 16：禁止可选空列名，同时影响 16、18 和可选空映射契约。
- 21：跳过必需名校验会使无效名、大小写重复及显式映射相关构建契约一起失效。
- 22：必需名集合失去大小写折叠，除大小写重复检查外，还影响大小写等价的显式映射。
- 23：必需名集合总是折叠大小写，除允许大小写不同必需名外，还影响大小写敏感的显式映射拒绝。
- 26：漏掉显式映射包含必需名检查，也放过大小写敏感时名称不同的映射。
- 28：初版只去掉 Builder 复制，被构造函数再次复制抵消；改为共用复制函数返回原数组，目标契约打红。
- 29：Builder 从格式复制时丢失必需名，会影响多份夹具及复制、重新启用、数组和值语义等契约。
- 34：显式映射不检查物理表头，还会放过 10、19，并破坏大小写等价显式映射契约。
