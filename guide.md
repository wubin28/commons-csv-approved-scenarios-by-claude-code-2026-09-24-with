+<!---
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
# 0. 环境与基线

- 源仓库：`/Users/binwu/OOR/katas/commons-csv`；克隆目录：`with/`。
- 克隆起点：`66a838202d64a9b05be4e74b846619688b26cb10`（当前 HEAD）。
- 工作分支：`feature/required-headers`。
- Agent：Codex；模型：GPT-6。
- Maven：3.9.16。
- JDK：26.0.1（执行 `mvn test` 的 Maven 进程）。
- 首次基线：`mvn test`，BUILD SUCCESS；测试 924，失败 0，错误 0，跳过 11。
- 新增文档后首次复测：Apache RAT 因文档缺许可证头而失败；修复后完整复测：BUILD SUCCESS，测试 924，失败 0，错误 0，跳过 11。

# 1. 业务事实一览

对账方要求在读取数据前发现上游 CSV 缺少的必需表头。[用 CSV 实例理解需求与现有源码](docs/required-headers-by-examples.md)适合先读；[业务事实汇总](docs/business-facts.md)与[规格草案](docs/specs/required-headers.md)提供场景和精确规则。19 个场景涵盖通过、拒绝、显式表头、空输入及功能关闭后的既有行为；期望结果依决策手工推导，尚未实现或执行新 API。

# 2. 如何运行 Approved Scenarios 测试

运行 `mvn -Dtest=ApprovedScenariosTest,RequiredHeadersContractTest test`。S3 预期失败：生产代码只有 `setRequiredHeaders` 签名桩，校验尚未实现。失败场景在夹具旁写入 `NN-xxx.received.md`；它已被 `.gitignore` 忽略。用 `diff -u src/test/resources/org/apache/commons/csv/approved/NN-xxx.approved.md src/test/resources/org/apache/commons/csv/approved/NN-xxx.received.md` 审查单个差异。

执行器自检由 `ApprovedScenariosTest` 内四个独立测试完成：读取非空与空 CSV 夹具；在临时目录验证不一致时写 received 且失败；在临时目录验证批准分支覆盖 approved。运行 `mvn -Dtest=ApprovedScenariosTest#shouldReadInputAndOptionsIfFixtureIsParsed+shouldReadEmptyInputIfFixtureHasEmptyCsvFence+shouldWriteReceivedAndFailIfResultDiffers+shouldOverwriteApprovedIfApprovalIsExplicit test` 可单独确认，不改源夹具。

仅在人工确认全部 diff 后，运行 `APPROVE=1 mvn -Dtest=ApprovedScenariosTest test` 更新 `.approved.md`。S3 不运行该命令；S3 夹具期望保持人工评审版本。

现有业务执行路径可先运行 `mvn -Dtest=RequiredHeadersExistingFlowE2ETest test`，再按 [VS Code 单步调试指南](docs/required-headers-by-running-e2e-test.md) 观察文件读取、显式表头映射与业务取值。此测试只用现有 API。

# 3. 故障注入与失败原因

待后续环节记录。

# 4. 踩坑记录

- 环节 S0 仓库检查 · `zsh:1: no matches found: .mvn/**` → zsh 把未加引号的搜索模式当作 shell 通配符展开，目录不存在时直接报错 → 给 `rg -g` 的模式加引号，重新检查成功。
- 环节 S0 环境检查 · `context-mode: Build tool redirected.` → 直接运行 Maven 的 shell 工具被上下文模式重定向 → 经 `ctx_execute` 在 `with/` 执行完整 `mvn test`，读取测试汇总及退出结果。
- 环节 S0 文档复测 · `Counter(s) UNAPPROVED exceeded minimum or maximum values.` → Apache RAT 把新增的 `guide.md` 和 `dialogs/dialogs.md` 识别为缺许可证头 → 按仓库现有 Markdown 格式补入 Apache License 2.0 头，重新运行完整 `mvn test`。
- 环节 S2 文件检索 · `grep: option `--files' is ambiguous` → 环境里的 `rg` 被代理成不接受该选项的命令 → 改用 `rtk proxy rg` 明确调用 ripgrep，重新读取目标文件。
- 环节 S2 构建入口 · `context-mode: Build tool redirected.` → 直接经 shell 运行 Maven 被上下文模式拦截，未真正执行测试 → 按提示经 `ctx_execute` 执行 Maven 并读取结果。
- 环节 S2 暂存提交 · `fatal: Unable to create '.git/index.lock': Operation not permitted` → 沙箱仅允许读取 `.git`，默认权限无法写入索引 → 按授权流程提升本仓库 `git add` 权限后成功暂存。
- 环节 S2 差异检查 · `trailing whitespace.` → 场景 06、08 的原样 CSV 故意保留表头末尾空格，用于验证空白匹配；汇总文档复制同一输入 → 保留这四处业务输入空格，清除其他文件末尾多余空行。
- 环节 S3 构建入口 · `context-mode: Build tool redirected.` → shell 中的 Maven 命令被上下文模式拦截，未执行测试 → 改用 `ctx_execute` 执行，确认 Surefire 输出 44 个用例、失败 26、错误 0。
- 环节 S3 空输入夹具 · 场景 13、19 的 received 把后续 Markdown 标题当作 CSV → 空代码围栏的结束标记紧贴内容起点，原提取器只查找前置换行的结束围栏 → 显式处理空围栏，增加独立自检并重跑两类测试。
- 环节 S3 暂存提交 · `fatal: Unable to create '.git/index.lock': Operation not permitted` → 沙箱默认只读 `.git`，无法写索引 → 按授权流程提升本仓库 `git add` 权限后暂存成功。
- 环节 S4 执行器自检 · 必需列功能实现后，`ApprovedScenariosTest` 的两项自检失败 → S3 自检把场景 02 实际状态固定假定为“通过”，生产代码转绿后该假定失效 → 只在临时目录把期望状态改为“待核对”制造差异，再断言 received 与显式批准分支均还原真实“拒绝”结果；源 `.approved.md` 未改。
- 环节 S4 显式映射 · 忽略表头大小写且显式映射含可选 `null` 名称时，构建格式抛 `NullPointerException` → 新增 `TreeSet` 映射校验直接加入了 `null` → 跳过空映射名，只核对必需名；新增回归单测确认可解析。
- 环节 S4 检索与构建入口 · `rg --files` 被代理成 grep，直接执行 Maven 只返回 `context-mode: Build tool redirected.` → 当前环境对这两个命令分别设有代理与构建拦截 → 用 `rtk proxy rg` 搜索、经 `ctx_execute` 执行完整 Maven 测试。
- 环节 S4 暂存提交 · `fatal: Unable to create '.git/index.lock': Operation not permitted` → 沙箱对 `.git` 仅开放读取 → 按授权流程提升本仓库 `git add` 权限，成功暂存。
- 环节 S5 契约测试补丁 · 一次跨多处修改的补丁因末尾上下文不匹配而未应用 → 补丁覆盖范围过大，定位不够精确 → 拆成小补丁逐处应用，并重跑三个目标测试类确认通过。
- 环节 S5 暂存提交 · `fatal: Unable to create '.git/index.lock': Operation not permitted` → 沙箱仍只允许读取 `.git` → 按授权流程提升本仓库 `git add` 权限，成功暂存。

# 5. 与 without 对照

待后续环节填写；本环节未修改 `without/`。

# 6. 环节与 tag

- S0 克隆与基线：`as-s0-baseline`。
- S3 Approved Scenarios 红态：`as-s3-red`；下一环节实现必需列表头行为。
- S4 Approved Scenarios 绿态：`as-s4-green`；下一环节 S5 待指定。
- S5 行为保持重构：`as-s5-refactor`；完整 `mvn test` 与 S4 一致，975 次、失败 0、错误 0、跳过 11。
