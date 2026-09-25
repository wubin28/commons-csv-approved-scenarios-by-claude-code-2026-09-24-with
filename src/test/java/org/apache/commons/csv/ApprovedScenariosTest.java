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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

/** Executes reviewed business scenarios from source files, preserving their original text. */
class ApprovedScenariosTest {

    private static final Path FIXTURES = Paths.get("src/test/resources/org/apache/commons/csv/approved");
    private static final String RESULT_MARKER = "## 解析结果\n```text\n";

    @TestFactory
    Stream<DynamicTest> shouldMatchApprovedResultIfFixtureIsExecuted() throws IOException {
        // Arrange
        final List<Path> fixtures = new ArrayList<>();
        try (Stream<Path> paths = Files.list(FIXTURES)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".approved.md")).sorted().forEach(fixtures::add);
        }
        assertFalse(fixtures.isEmpty(), "No approved scenarios found");

        // Act
        return fixtures.stream().map(path -> DynamicTest.dynamicTest(path.getFileName().toString(), () -> verify(path, "1".equals(System.getenv("APPROVE")))));
    }

    @Test
    void shouldReadInputAndOptionsIfFixtureIsParsed() throws IOException {
        // Arrange
        final Path path = FIXTURES.resolve("01-auto-complete.approved.md");
        final String source = read(path);
        // Act
        final String input = fenced(source, "## 输入", "csv");
        final Map<String, String> config = options(fenced(source, "## 格式配置", "text"));
        // Assert
        assertEquals("account_id,currency,amount\nA-001,TWD,12.50", input);
        assertEquals("auto", config.get("header"));
        assertEquals("account_id,currency,amount", config.get("requiredHeaders"));
    }

    @Test
    void shouldReadEmptyInputIfFixtureHasEmptyCsvFence() throws IOException {
        // Arrange
        final Path path = FIXTURES.resolve("13-empty-input.approved.md");
        final String source = read(path);
        // Act
        final String input = fenced(source, "## 输入", "csv");
        // Assert
        assertEquals("", input);
    }

    @Test
    void shouldWriteReceivedAndFailIfResultDiffers(@TempDir final Path temp) throws IOException {
        // Arrange
        final Path approved = changedFixture(temp);
        final Path received = temp.resolve("02-two-missing.received.md");
        // Act
        assertThrows(AssertionFailedError.class, () -> verify(approved, false));
        // Assert
        assertTrue(Files.exists(received));
        assertTrue(read(received).contains("状态：拒绝"));
        assertTrue(read(approved).contains("状态：待核对"));
    }

    @Test
    void shouldOverwriteApprovedIfApprovalIsExplicit(@TempDir final Path temp) throws IOException {
        // Arrange
        final Path approved = changedFixture(temp);
        // Act
        verify(approved, true);
        // Assert
        assertTrue(read(approved).contains("状态：拒绝"));
        assertFalse(Files.exists(temp.resolve("02-two-missing.received.md")));
    }

    private static Path changedFixture(final Path temp) throws IOException {
        final Path source = FIXTURES.resolve("02-two-missing.approved.md");
        final Path copy = temp.resolve(source.getFileName());
        Files.copy(source, copy);
        Files.write(copy, read(copy).replace("状态：拒绝", "状态：待核对").getBytes(StandardCharsets.UTF_8));
        return copy;
    }

    private static String read(final Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void verify(final Path path, final boolean approve) throws IOException {
        final String approved = read(path);
        final String csv = fenced(approved, "## 输入", "csv");
        final Map<String, String> options = options(fenced(approved, "## 格式配置", "text"));
        final CSVFormat format = format(options);
        final String result = result(csv, format);
        final int start = approved.indexOf(RESULT_MARKER);
        if (start < 0 || approved.indexOf(RESULT_MARKER, start + 1) >= 0) {
            throw new IllegalArgumentException("Missing or repeated result marker: " + path);
        }
        final int bodyStart = start + RESULT_MARKER.length();
        final int bodyEnd = approved.indexOf("\n```", bodyStart);
        if (bodyEnd < 0) {
            throw new IllegalArgumentException("Unclosed result fence: " + path);
        }
        final String received = approved.substring(0, bodyStart) + result + approved.substring(bodyEnd);
        final Path receivedPath = path.resolveSibling(path.getFileName().toString().replace(".approved.md", ".received.md"));
        if (approve) {
            Files.write(path, received.getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (!approved.equals(received)) {
            Files.write(receivedPath, received.getBytes(StandardCharsets.UTF_8));
        } else {
            Files.deleteIfExists(receivedPath);
        }
        // Assert
        assertEquals(approved, received, "Review " + receivedPath + " against " + path);
    }

    private static String fenced(final String source, final String heading, final String language) {
        final String marker = heading + "\n```" + language + "\n";
        final int markerAt = source.indexOf(marker);
        if (markerAt < 0 || source.indexOf(marker, markerAt + 1) >= 0) {
            throw new IllegalArgumentException("Missing or repeated section: " + heading);
        }
        final int start = markerAt + marker.length();
        if (source.startsWith("```\n", start)) {
            return "";
        }
        final int end = source.indexOf("\n```", start);
        if (end < 0) {
            throw new IllegalArgumentException("Unclosed section: " + heading);
        }
        return source.substring(start, end);
    }

    private static Map<String, String> options(final String text) {
        final Map<String, String> result = new TreeMap<>();
        for (final String line : text.split("\n")) {
            final int colon = line.indexOf(": ");
            if (colon < 0 || result.put(line.substring(0, colon), line.substring(colon + 2)) != null) {
                throw new IllegalArgumentException("Invalid option: " + line);
            }
        }
        return result;
    }

    private static CSVFormat format(final Map<String, String> options) {
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder();
        final String header = options.remove("header");
        if (header == null) {
            throw new IllegalArgumentException("Missing header option");
        }
        builder.setHeader("auto".equals(header) ? new String[0] : header.split(",", -1));
        final String required = options.remove("requiredHeaders");
        if (required == null) {
            throw new IllegalArgumentException("Missing requiredHeaders option");
        }
        if (!"off".equals(required)) {
            builder.setRequiredHeaders(required.split(",", -1));
        }
        for (final Map.Entry<String, String> option : options.entrySet()) {
            switch (option.getKey()) {
            case "skipHeaderRecord": builder.setSkipHeaderRecord(Boolean.parseBoolean(option.getValue())); break;
            case "ignoreHeaderCase": builder.setIgnoreHeaderCase(Boolean.parseBoolean(option.getValue())); break;
            case "ignoreSurroundingSpaces": builder.setIgnoreSurroundingSpaces(Boolean.parseBoolean(option.getValue())); break;
            case "trim": builder.setTrim(Boolean.parseBoolean(option.getValue())); break;
            case "allowMissingColumnNames": builder.setAllowMissingColumnNames(Boolean.parseBoolean(option.getValue())); break;
            case "duplicateHeaderMode": builder.setDuplicateHeaderMode(DuplicateHeaderMode.valueOf(option.getValue())); break;
            case "commentMarker": builder.setCommentMarker(option.getValue().charAt(0)); break;
            default: throw new IllegalArgumentException("Unknown option: " + option.getKey());
            }
        }
        return builder.get();
    }

    private static String result(final String csv, final CSVFormat format) throws IOException {
        try (CSVParser parser = CSVParser.parse(csv, format)) {
            final StringBuilder body = new StringBuilder("状态：通过\n实际表头：");
            // The fixture records the physical header; explicit parser mappings can differ from it.
            final CSVFormat physicalFormat = format.builder().setRequiredHeaders((String[]) null).setHeader().setSkipHeaderRecord(false).get();
            try (CSVParser physical = CSVParser.parse(csv, physicalFormat)) {
                body.append(physical.getHeaderNames());
            }
            body.append("\n记录：");
            for (final CSVRecord record : parser) {
                body.append("\n- ").append(Arrays.toString(record.values()));
            }
            return body.toString();
        } catch (final IllegalArgumentException ex) {
            return "状态：拒绝\n异常类型：" + ex.getClass().getSimpleName() + "\n完整消息：" + ex.getMessage();
        }
    }
}
