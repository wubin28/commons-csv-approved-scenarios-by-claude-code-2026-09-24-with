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
import java.io.StringWriter;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Construction and cross-cutting contracts from required-headers spec §7.2. */
class RequiredHeadersContractTest {

    @Test
    void shouldPreserveOldParsingIfRequiredHeadersNeverSet() throws IOException {
        // Arrange
        final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().get();
        // Act
        try (CSVParser parser = format.parse(new java.io.StringReader("account_id\nA-001\n"))) {
            // Assert
            assertEquals(1, parser.getRecords().size());
        }
    }

    @ParameterizedTest
    @MethodSource("disabledRequiredHeaders")
    void shouldDisableValidationIfRequiredHeadersAreNullOrEmpty(final String[] names) throws IOException {
        // Arrange
        final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency").setRequiredHeaders(names).get();
        // Act
        try (CSVParser parser = CSVParser.parse("account_id\nA-001\n", format)) {
            // Assert
            assertEquals(1, parser.getRecords().size());
        }
    }

    private static Stream<Arguments> disabledRequiredHeaders() {
        return Stream.of(Arguments.of((Object) null), Arguments.of((Object) new String[0]));
    }

    @Test
    void shouldReenableValidationIfCalledAfterDisabling() {
        // Arrange
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency").setRequiredHeaders((String[]) null);
        // Act
        final CSVFormat format = builder.setRequiredHeaders("currency").get();
        // Assert
        assertThrows(IllegalArgumentException.class, () -> CSVParser.parse("account_id\nA-001\n", format));
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void shouldRejectInvalidNamesIfBuildingEnabledFormat(final String[] names) {
        // Arrange
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders(names);
        // Act and Assert
        assertThrows(IllegalArgumentException.class, builder::get);
    }

    private static Stream<Arguments> invalidNames() {
        return Stream.of(Arguments.of((Object) new String[] {null}), Arguments.of((Object) new String[] {""}),
                Arguments.of((Object) new String[] {" \t "}), Arguments.of((Object) new String[] {"currency", "currency"}));
    }

    @ParameterizedTest
    @MethodSource("caseInsensitiveDuplicateBuilders")
    void shouldRejectCaseEquivalentNamesIfFinalCaseModeIgnoresCase(final CSVFormat.Builder builder) {
        // Arrange: builder order supplied by method source.
        // Act and Assert
        assertThrows(IllegalArgumentException.class, builder::get);
    }

    private static Stream<Arguments> caseInsensitiveDuplicateBuilders() {
        return Stream.of(Arguments.of(CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency", "CURRENCY").setIgnoreHeaderCase(true)),
                Arguments.of(CSVFormat.DEFAULT.builder().setHeader().setIgnoreHeaderCase(true).setRequiredHeaders("currency", "CURRENCY")));
    }

    @Test
    void shouldAllowCaseDistinctNamesIfFinalCaseModeIsSensitive() {
        // Arrange
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setHeader().setIgnoreHeaderCase(true)
                .setRequiredHeaders("currency", "CURRENCY").setIgnoreHeaderCase(false);
        // Act
        final CSVFormat format = builder.get();
        // Assert
        assertThrows(IllegalArgumentException.class, () -> CSVParser.parse("other\n", format));
    }

    @Test
    void shouldRejectRequiredHeadersIfHeaderModeIsAbsent() {
        // Arrange
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setRequiredHeaders("currency");
        // Act and Assert
        assertThrows(IllegalArgumentException.class, builder::get);
    }

    @Test
    void shouldRejectRequiredHeadersIfExplicitHeaderIsNotSkipped() {
        // Arrange
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setHeader("currency").setRequiredHeaders("currency");
        // Act and Assert
        assertThrows(IllegalArgumentException.class, builder::get);
    }

    @Test
    void shouldRejectRequiredHeadersIfExplicitMappingLacksName() {
        // Arrange
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setHeader("account_id").setSkipHeaderRecord(true).setRequiredHeaders("currency");
        // Act and Assert
        assertThrows(IllegalArgumentException.class, builder::get);
    }

    @ParameterizedTest
    @MethodSource("explicitMappingCaseBuilders")
    void shouldAcceptCaseEquivalentExplicitMappingIfFinalCaseModeIgnoresCase(final CSVFormat.Builder builder) {
        // Arrange: builder order supplied by method source.
        // Act
        final CSVFormat format = builder.get();
        // Assert
        assertThrows(IllegalArgumentException.class, () -> CSVParser.parse("other\n", format));
    }

    private static Stream<Arguments> explicitMappingCaseBuilders() {
        return Stream.of(Arguments.of(CSVFormat.DEFAULT.builder().setHeader("CURRENCY").setSkipHeaderRecord(true)
                .setRequiredHeaders("currency").setIgnoreHeaderCase(true)), Arguments.of(CSVFormat.DEFAULT.builder()
                .setIgnoreHeaderCase(true).setHeader("CURRENCY").setSkipHeaderRecord(true).setRequiredHeaders("currency")));
    }

    @Test
    void shouldRejectCaseDifferentExplicitMappingIfFinalCaseModeIsSensitive() {
        // Arrange
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setHeader("CURRENCY").setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true).setRequiredHeaders("currency").setIgnoreHeaderCase(false);
        // Act and Assert
        assertThrows(IllegalArgumentException.class, builder::get);
    }

    @Test
    void shouldAllowMissingOptionalExplicitNameIfRequiredNameIsMapped() throws IOException {
        // Arrange
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setHeader((String) null, "currency")
                .setAllowMissingColumnNames(true).setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setRequiredHeaders("currency");
        // Act
        final CSVFormat format = builder.get();
        // Assert
        try (CSVParser parser = CSVParser.parse(",currency\n,TWD\n", format)) {
            assertEquals(1, parser.getRecords().size());
        }
    }

    @Test
    void shouldCopyRequiredNamesIfInputArrayChanges() {
        // Arrange
        final String[] names = {"currency"};
        final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders(names).get();
        // Act
        names[0] = "amount";
        // Assert
        assertThrows(IllegalArgumentException.class, () -> CSVParser.parse("amount\n1\n", format));
    }

    @Test
    void shouldPreserveRequiredNamesIfFormatIsCopiedOrRebuilt() {
        // Arrange
        final CSVFormat original = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency").get();
        // Act
        final CSVFormat copied = original.copy();
        final CSVFormat rebuilt = original.builder().get();
        // Assert
        assertThrows(IllegalArgumentException.class, () -> CSVParser.parse("amount\n1\n", copied));
        assertThrows(IllegalArgumentException.class, () -> CSVParser.parse("amount\n1\n", rebuilt));
    }

    @Test
    void shouldKeepValueSemanticsIfRequiredNamesMatch() {
        // Arrange
        final CSVFormat first = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency", "amount").get();
        // Act
        final CSVFormat second = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency", "amount").get();
        // Assert
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldChangeEqualityIfRequiredNamesChangeOrReorder() {
        // Arrange
        final CSVFormat first = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency", "amount").get();
        // Act
        final CSVFormat removed = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency").get();
        final CSVFormat added = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency", "amount", "account_id").get();
        final CSVFormat reordered = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("amount", "currency").get();
        // Assert
        assertFalse(first.equals(removed));
        assertFalse(first.equals(added));
        assertFalse(first.equals(reordered));
    }

    @Test
    void shouldShowRequiredNamesInOrderIfFormatIsRendered() {
        // Arrange
        final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency", "amount").get();
        // Act
        final String description = format.toString();
        // Assert
        assertTrue(description.contains("currency") && description.indexOf("currency") < description.indexOf("amount"));
    }

    @Test
    void shouldRejectDuringParserConstructionIfPhysicalHeaderIsMissing() {
        // Arrange
        final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("account_id", "currency").setSkipHeaderRecord(true)
                .setRequiredHeaders("currency").get();
        // Act
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> CSVParser.parse("account_id,amount\nA-001,12.50\n", format));
        // Assert
        assertEquals("Missing required CSV headers: [currency]", failure.getMessage());
    }

    @Test
    void shouldAcceptEmptyAndShortDataIfRequiredHeaderExists() throws IOException {
        // Arrange
        final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency").get();
        // Act
        try (CSVParser parser = CSVParser.parse("account_id,currency\nA-001,\nA-002\n", format)) {
            // Assert
            assertEquals(2, parser.getRecords().size());
        }
    }

    @Test
    void shouldAllowPrintingIfRequiredHeadersAreConfigured() throws IOException {
        // Arrange
        final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setRequiredHeaders("currency").get();
        final StringWriter output = new StringWriter();
        // Act
        try (CSVPrinter printer = format.print(output)) {
            printer.printRecord("A-001", "TWD");
        }
        // Assert
        assertTrue(output.toString().contains("A-001,TWD"));
    }
}
