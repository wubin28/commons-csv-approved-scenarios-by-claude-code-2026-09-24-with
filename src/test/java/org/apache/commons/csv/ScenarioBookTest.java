/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.csv;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Publishes product-readable approved scenarios without invoking the pending API.
 */
class ScenarioBookTest {

    private static final String LICENSE = "<!---\n"
            + " Licensed to the Apache Software Foundation (ASF) under one or more\n"
            + " contributor license agreements.  See the NOTICE file distributed with\n"
            + " this work for additional information regarding copyright ownership.\n"
            + " The ASF licenses this file to You under the Apache License, Version 2.0\n"
            + " (the \"License\"); you may not use this file except in compliance with\n"
            + " the License.  You may obtain a copy of the License at\n\n"
            + "      https://www.apache.org/licenses/LICENSE-2.0\n\n"
            + " Unless required by applicable law or agreed to in writing, software\n"
            + " distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + " See the License for the specific language governing permissions and\n"
            + " limitations under the License.\n"
            + "-->\n";

    private static final Pattern SCENARIO = Pattern.compile("(?s).*?\\n# ([^\\n]+)\\n\\n说明：([^\\n]+)\\n规则：(R\\d+(?:、R\\d+)*)"
            + "\\n路径：(A\\d+(?: → B\\d+)?(?: → C\\d+)?)"
            + "\\n\\n## 输入\\n```csv\\n(.*?)```\\n\\n## 格式配置\\n```text\\n(.*?)```\\n\\n## 解析结果\\n```text\\n(.*?)```\\n*");

    @Test
    void shouldMatchScenarioBookIfFixturesAreApproved() throws IOException {
        // Arrange
        final Path root = Paths.get("").toAbsolutePath();
        final Path approved = root.resolve("src/test/resources/org/apache/commons/csv/approved");
        final Path book = root.resolve("docs/business-facts.md");
        final Path received = root.resolve("docs/business-facts.received.md");
        final List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(approved, "*.approved.md")) {
            for (final Path file : stream) {
                files.add(file);
            }
        }
        Collections.sort(files);
        assertTrue(!files.isEmpty(), "Approved Scenarios directory must contain fixtures");
        final StringBuilder toc = new StringBuilder();
        final StringBuilder chapters = new StringBuilder();
        int number = 1;
        for (final Path file : files) {
            final String name = file.getFileName().toString();
            assertTrue(name.matches(String.format("%02d-[a-z0-9-]+\\.approved\\.md", Integer.valueOf(number))), name);
            final String fixture = read(file);
            final Matcher match = SCENARIO.matcher(fixture);
            assertTrue(match.matches(), "Invalid Approved Scenario structure: " + name);
            final String title = match.group(1);
            final String description = match.group(2);
            final String rules = match.group(3);
            final String path = match.group(4);
            final String input = match.group(5);
            final String config = match.group(6);
            final String result = match.group(7);
            assertTrue(config.contains("requiredHeaders:"), name);
            assertTrue(config.contains("header:"), name);
            assertTrue(result.startsWith("状态：通过\n实际表头：") && result.contains("\n记录：\n")
                    || result.startsWith("状态：拒绝\n异常类型：") && result.contains("\n完整消息："), name);
            toc.append("| ").append(String.format("%02d", Integer.valueOf(number))).append(" | [").append(title).append("](#scenario-")
                    .append(String.format("%02d", Integer.valueOf(number))).append(") | ").append(rules).append(" | ").append(path).append(" |\n");
            chapters.append("## <a id=\"scenario-").append(String.format("%02d", Integer.valueOf(number))).append("\"></a>")
                    .append(String.format("%02d", Integer.valueOf(number))).append(" · ").append(title).append("\n\n")
                    .append("业务说明：").append(description).append("\n\n")
                    .append("对应规则：").append(rules).append("\n\n")
                    .append("判定路径：").append(path).append("\n\n")
                    .append("输入（原样 CSV）：\n\n```csv\n").append(input).append("```\n\n")
                    .append("格式配置：\n\n```text\n").append(config).append("```\n\n")
                    .append("期望结果：\n\n```text\n").append(result).append("```\n\n");
            number++;
        }
        final String generated = (LICENSE + "\n# 对账业务事实 · Approved Scenarios\n\n"
                + "本页由 `ScenarioBookTest` 从 `src/test/resources/org/apache/commons/csv/approved/` 生成。"
                + "期望结果由 S2/S2R 规格手工推导；此测试只核对文档同步，不运行待实现 API。"
                + "规则定义见 [required-headers.md](specs/required-headers.md)。\n\n"
                + "## 场景目录\n\n| 编号 | 业务场景 | 对应规则 | 判定路径 |\n| --- | --- | --- | --- |\n"
                + toc + "\n" + chapters).replaceFirst("\\n+\\z", "\n");

        // Act
        if ("1".equals(System.getenv("APPROVE"))) {
            Files.write(book, generated.getBytes(StandardCharsets.UTF_8));
        } else if (!Files.exists(book) || !generated.equals(read(book))) {
            Files.write(received, generated.getBytes(StandardCharsets.UTF_8));
        }

        // Assert
        assertTrue(generated.equals(read(book)),
                "Business facts changed; inspect docs/business-facts.received.md or run APPROVE=1 mvn test");
    }

    private static String read(final Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
