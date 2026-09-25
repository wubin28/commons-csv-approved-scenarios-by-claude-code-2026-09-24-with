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

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

/**
 * Characterizes the existing file-to-business flow behind approved scenario 10.
 * No proposed Required Headers API is called here.
 */
class RequiredHeadersExistingFlowE2ETest {

    @Test
    void shouldMapAmountAsCurrencyInExistingExplicitHeaderFlow() throws Exception {
        // Arrange: physical CSV header has amount, while local mapping claims currency.
        final URL csvFile = Objects.requireNonNull(getClass().getResource("/org/apache/commons/csv/e2e/reconciliation-missing-currency.csv"));
        final CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("account_id", "currency")
                .setSkipHeaderRecord(true)
                .get();
        final Map<String, String> currencyByAccount = new LinkedHashMap<>();

        // Act: load the real CSV file and run the reconciliation lookup.
        try (CSVParser parser = CSVParser.parse(csvFile, StandardCharsets.UTF_8, format)) {
            for (final CSVRecord record : parser) {
                currencyByAccount.put(record.get("account_id"), record.get("currency"));
            }
        }

        // Assert: characterize the existing wrong mapping, not the proposed behavior.
        assertEquals(1, currencyByAccount.size());
        assertEquals("12.50", currencyByAccount.get("A-001"));
    }
}
