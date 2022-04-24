/*
 * Copyright 2022. Tushar Naik
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package io.github.tushar.naik.stringextractor;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class StringExtractorTest {

    @ParameterizedTest
    @MethodSource("happyScenarioBlueprints")
    void testHappyScenarios(final String blueprint,
                            final String source,
                            final String result,
                            final Map<String, Object> extractedMap) throws BlueprintParseError {

        final StringExtractor stringExtractor = new StringExtractor(blueprint);
        final ExtractionResult extractionResult = stringExtractor.extractFrom(source);
        assertEquals(result, extractionResult.getExtractedString());
        assertMapEquals(extractedMap, extractionResult.getExtractions());
        assertFalse(extractionResult.isError());
    }

    @ParameterizedTest
    @MethodSource("exceptionScenarioBlueprints")
    void testExtractorParseException(final String blueprint,
                                     final BlueprintParseErrorCode blueprintParseErrorCode) {

        final BlueprintParseError blueprintParseError
                = assertThrows(BlueprintParseError.class, () -> new StringExtractor(blueprint));
        assertEquals(blueprintParseErrorCode, blueprintParseError.getBlueprintParseErrorCode());
    }

    @ParameterizedTest
    @MethodSource("noMatchBlueprints")
    void testNoMatchUnhappyScenarios(final String blueprint,
                                     final String source) throws BlueprintParseError {
        final StringExtractor stringExtractor = new StringExtractor(blueprint, true);
        final ExtractionResult extractionResult = stringExtractor.extractFrom(source);
        assertTrue(extractionResult.isError());
        assertNull(extractionResult.getExtractions());
        assertNull(extractionResult.getExtractedString());
    }

    public static Stream<Arguments> happyScenarioBlueprints() {
        return Stream.of(
                /* one variable extraction */
                Arguments.of("This is ${{name:[A-Za-z]+}}",
                             "This is Tushar",
                             "This is ",
                             ImmutableMap.of("name", "Tushar")),

                /* two variable extraction */
                Arguments.of("This is ${{name:[A-Za-z]+}}. You are ${{adjective:[A-Za-z]+}}",
                             "This is Tushar. You are Good",
                             "This is . You are ",
                             ImmutableMap.of("name", "Tushar", "adjective", "Good")),

                /* start with a variable */
                Arguments.of("${{name:[A-Za-z]+}} is my name.",
                             "Tushar is my name.",
                             " is my name.",
                             ImmutableMap.of("name", "Tushar")),

                /* one variable in the middle */
                Arguments.of("This is ${{name:[A-Za-z]+}}. Who are you",
                             "This is Tushar. Who are you",
                             "This is . Who are you",
                             ImmutableMap.of("name", "Tushar")),

                /* regex with {,} characters */
                Arguments.of("This is ${{name:[A-Za-z]{3}}}har. Who are you",
                             "This is Tushar. Who are you",
                             "This is har. Who are you",
                             ImmutableMap.of("name", "Tus")),

                /* check if remains are sent back */
                Arguments.of("This is ${{name:[A-Za-z]{3}}}har. ",
                             "This is Tushar. Who are you?",
                             "This is har. Who are you?",
                             ImmutableMap.of("name", "Tus")),

                /* check if remains are sent back */
                Arguments.of("May you ${{what:[A-Za-z]+ [A-Za-z]+ [A-Za-z]+}} days of your life",
                             "May you live all the days of your life",
                             "May you  days of your life",
                             ImmutableMap.of("what", "live all the")),

                /* regex with {,} characters */
                Arguments.of("com.org.app.executor.containers.api.instance.${{component:[^.]+}}"
                                     + ".cpu_absolute_per_ms",
                             "com.org.app.executor.containers.api.instance.99db78be-08de-4e92-a93a-ca2dfac09bc2"
                                     + ".cpu_absolute_per_ms",
                             "com.org.app.executor.containers.api.instance..cpu_absolute_per_ms",
                             ImmutableMap.of("component", "99db78be-08de-4e92-a93a-ca2dfac09bc2")),

                /* regex with {,} characters */
                Arguments.of("com.org.app.executor.containers.api.instance.${{component:[^.]+\\.}}"
                                     + "cpu_absolute_per_ms",
                             "com.org.app.executor.containers.api.instance.99db78be-08de-4e92-a93a-ca2dfac09bc2"
                                     + ".cpu_absolute_per_ms",
                             "com.org.app.executor.containers.api.instance.cpu_absolute_per_ms",
                             ImmutableMap.of("component", "99db78be-08de-4e92-a93a-ca2dfac09bc2."))

                        );
    }

    public static Stream<Arguments> exceptionScenarioBlueprints() {
        return Stream.of(
                Arguments.of("This is ${{:[A-Za-z]+}}", BlueprintParseErrorCode.EMPTY_VARIABLE_NAME),
                Arguments.of("This is ${{}}. You are ${{adjective:[A-Za-z]+}}",
                             BlueprintParseErrorCode.EMPTY_VARIABLE_REGEX),
                Arguments.of("This is ${{name::[A-Z]+}}", BlueprintParseErrorCode.INCORRECT_VARIABLE_REPRESENTATION),
                Arguments.of("This is ${{name:[A-Z]}}, you are ${{name2}}", BlueprintParseErrorCode.EMPTY_REGEX),
                Arguments.of("This is ${{name:[A-Z]}}, you are ${{name2:}}", BlueprintParseErrorCode.EMPTY_REGEX),
                Arguments.of("This is ${{name:[A-Z}}, you are ${{name2:[a]}}", BlueprintParseErrorCode.PATTERN_SYNTAX)
                        );
    }

    public static Stream<Arguments> noMatchBlueprints() {
        return Stream.of(
                Arguments.of("This is ${{variable:[A-Za-z]+}}", "This is some shit"),
                Arguments.of("${{blue:[A-Za-z]+}}", "This is "),
                Arguments.of("Some BS ${{blue:[A-Za-z]+}}", "This is who")
                        );
    }

    private static void assertMapEquals(final Map<String, Object> expected,
                                        final Map<String, Object> actual) {
        if (!areEqual(expected, actual)) {
            fail("Expected: " + expected + " actual:" + actual);
        }
    }

    private static boolean areEqual(final Map<String, Object> first,
                                    final Map<String, Object> second) {
        if (first.size() != second.size()) {
            return false;
        }
        return first.entrySet().stream()
                .allMatch(e -> e.getValue().equals(second.get(e.getKey())));
    }
}