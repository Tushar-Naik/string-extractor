/*
 * Copyright 2022. America Naik
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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringExtractorTest {

    public static Stream<Arguments> happyScenarioBlueprints() {

        //language=RegExp
        String regex = "[^.]+";

        return Stream.of(
                /* one variable extraction */
                Arguments.of("This is ${{name:[A-Za-z]+}}",
                             "This is America",
                             null,
                             "This is ",
                             ImmutableMap.of("name", "America")),

                /* two variable extraction */
                Arguments.of("This is ${{name:[A-Za-z]+}}. You are ${{adjective:[A-Za-z]+}}",
                             "This is America. You are Good",
                             null,
                             "This is . You are ",
                             ImmutableMap.of("name", "America", "adjective", "Good")),

                /* start with a variable */
                Arguments.of("${{name:[A-Za-z]+}} is my name.",
                             "America is my name.",
                             null,
                             " is my name.",
                             ImmutableMap.of("name", "America")),

                /* one variable in the middle */
                Arguments.of("This is ${{name:[A-Za-z]+}}. Who are you",
                             "This is America. Who are you",
                             null,
                             "This is . Who are you",
                             ImmutableMap.of("name", "America")),

                /* regex with {,} characters */
                Arguments.of("This is ${{name:[A-Za-z]{3}}}rica.",
                             "This is America.",
                             null,
                             "This is rica.",
                             ImmutableMap.of("name", "Ame")),

                /* check if remains are sent back */
                Arguments.of("This is ${{name:[A-Za-z]{3}}}rica. ",
                             "This is America. Who are you?",
                             null,
                             "This is rica. Who are you?",
                             ImmutableMap.of("name", "Ame")),

                /* check if remains are sent back */
                Arguments.of("May you ${{what:[A-Za-z]+ [A-Za-z]+ [A-Za-z]+}} days of your life",
                             "May you live all the days of your life",
                             null,
                             "May you  days of your life",
                             ImmutableMap.of("what", "live all the")),

                /* regex with {,} characters */
                Arguments.of("com.org.app.executor.containers.api.instance.${{component:[^.]+}}"
                                     + ".cpu_absolute_per_ms",
                             "com.org.app.executor.containers.api.instance.99db78be-08de-4e92-a93a-ca2dfac09bc2"
                                     + ".cpu_absolute_per_ms",
                             null,
                             "com.org.app.executor.containers.api.instance..cpu_absolute_per_ms",
                             ImmutableMap.of("component", "99db78be-08de-4e92-a93a-ca2dfac09bc2")),

                /* regex with {,} characters */
                Arguments.of("com.org.app.executor.containers.api.instance.${{component:[^.]+\\.}}"
                                     + "cpu_absolute_per_ms",
                             "com.org.app.executor.containers.api.instance.99db78be-08de-4e92-a93a-ca2dfac09bc2"
                                     + ".cpu_absolute_per_ms",
                             null,
                             "com.org.app.executor.containers.api.instance.cpu_absolute_per_ms",
                             ImmutableMap.of("component", "99db78be-08de-4e92-a93a-ca2dfac09bc2.")),

                /* test last variable */
                Arguments.of("This is ${{name:[A-Za-z]+}}",
                             "This is America",
                             null,
                             "This is ",
                             ImmutableMap.of("name", "America")),

                /* test last variable when multiple variables */
                Arguments.of("This is ${{name:[A-Za-z]+}}. Guns in my ${{place:}}",
                             "This is America. Guns in my area",
                             null,
                             "This is . Guns in my ",
                             ImmutableMap.of("name", "America", "place", "area")),

                /* test last variable (without a delimiter :) */
                Arguments.of("This is ${{name:[A-Za-z]+}}. Guns in my ${{place}}",
                             "This is America. Guns in my area",
                             null,
                             "This is . Guns in my ",
                             ImmutableMap.of("name", "America", "place", "area")),

                /* test discarded exact match variable */
                Arguments.of("This is ${{:America}}. Guns in my ${{place}}",
                             "This is America. Guns in my area",
                             null,
                             "This is . Guns in my ",
                             ImmutableMap.of("place", "area")),

                /* test discarded exact match variable (with special characters) */
                Arguments.of("This is ${{:Amer[ica}}. Guns in my ${{place}}",
                             "This is Amer[ica. Guns in my area",
                             null,
                             "This is . Guns in my ",
                             ImmutableMap.of("place", "area")),

                /* test exact match variable */
                Arguments.of("This is ${{name:America}}. Guns in my ${{place}}",
                             "This is America. Guns in my area",
                             null,
                             "This is . Guns in my ",
                             ImmutableMap.of("name", "America", "place", "area")),

                /* test exact match variable with special characters */
                Arguments.of("This is ${{name:Amer[ica}}. Guns in my ${{place}}",
                             "This is Amer[ica. Guns in my area",
                             null,
                             "This is . Guns in my ",
                             ImmutableMap.of("name", "Amer[ica", "place", "area")),

                /* test discarded variable */
                Arguments.of("This is ${{:[A-Za-z]+}}. Guns in my ${{place}}",
                             "This is America. Guns in my area",
                             null,
                             "This is . Guns in my ",
                             ImmutableMap.of("place", "area")),

                /* test context mapped variable */
                Arguments.of("This is ${{:[A-Za-z]+}}. Guns in my ${{context:which}} ${{place}}",
                             "This is America. Guns in my  area",
                             ImmutableMap.of("which", "new"),
                             "This is . Guns in my new ",
                             ImmutableMap.of("place", "area")),

                /* test static attach variable */
                Arguments.of("This is ${{:[A-Za-z]+}}. Guns in my ${{attach:which}} ${{place}}",
                             "This is America. Guns in my  area",
                             ImmutableMap.of("which", "new"),
                             "This is . Guns in my which ",
                             ImmutableMap.of("place", "area")),

                /* test retained variables */
                Arguments.of("This is ${{skipped:[A-Za-z]+}}. Guns in my ${{place:}}",
                             "This is America. Guns in my area",
                             null,
                             "This is America. Guns in my ",
                             ImmutableMap.of("place", "area")),

                /* test retained variables */
                Arguments.of(
                        "com.phonepe.drove.executor.containers.${{skipped:[^.]+}}.${{:instance.}}${{instance:[^"
                                + ".]+}}${{:.}}",
                        "com.phonepe.drove.executor.containers.gandalf.instance.ea2c2e4d-0d96-4039-bae9-74ff12ce1cb6"
                                + ".network_rx_bytes.field.value",
                        null,
                        "com.phonepe.drove.executor.containers.gandalf.network_rx_bytes.field.value",
                        ImmutableMap.of("instance", "ea2c2e4d-0d96-4039-bae9-74ff12ce1cb6")),

                /* test retained variables */
                Arguments.of(
                        "com.phonepe.drove.executor.containers.${{context:cluster}}${{attach:.}}"
                                + "${{skipped:[^.]+}}.${{:instance.}}${{instance:[^.]+}}${{:.}}",
                        "com.phonepe.drove.executor.containers.gandalf.instance.ea2c2e4d-0d96-4039-bae9-74ff12ce1cb6"
                                + ".network_rx_bytes.field.value",
                        ImmutableMap.of("cluster", "prd-platdrove"),
                        "com.phonepe.drove.executor.containers.prd-platdrove.gandalf.network_rx_bytes.field.value",
                        ImmutableMap.of("instance", "ea2c2e4d-0d96-4039-bae9-74ff12ce1cb6")),

                /* test retained variables */
                Arguments.of(
                        "hdpfoxtrotconnect-MirrorSourceConnector"
                                + ".org_apache_kafka_common_metrics_JmxReporter$KafkaMbean"
                                + ".nb1_nm5_${{topic:^((?!_[0-9]+).)+}}_${{partition:[^.]+}}${{:.}}",
                        "hdpfoxtrotconnect-MirrorSourceConnector"
                                + ".org_apache_kafka_common_metrics_JmxReporter$KafkaMbean.nb1_nm5_events_android_12"
                                + ".replication-latency-ms",
                        null,
                        "hdpfoxtrotconnect-MirrorSourceConnector"
                                + ".org_apache_kafka_common_metrics_JmxReporter$KafkaMbean"
                                + ".nb1_nm5__replication-latency-ms",
                        ImmutableMap.of("topic", "events_android", "partition", "12")),

                /* test retained variables */
                Arguments.of(
                        "${{skipped:[^.]+}}"
                                + ".org_apache_kafka_common_metrics_JmxReporter$KafkaMbean"
                                + ".${{skipped:[a-z0-9]+_[a-z0-9]+}}_${{topic:^((?!_[0-9]+).)+}}_${{partition:[^"
                                + ".]+}}${{:.}}",
                        "prd-hdpaccnmtomh-mm2-MirrorSourceConnector"
                                + ".org_apache_kafka_common_metrics_JmxReporter$KafkaMbean"
                                + ".mhb_nm5_accounting_plutus_analytics_0.replication-latency-ms",
                        null,
                        "prd-hdpaccnmtomh-mm2-MirrorSourceConnector"
                                + ".org_apache_kafka_common_metrics_JmxReporter$KafkaMbean"
                                + ".mhb_nm5__replication-latency-ms",
                        ImmutableMap.of("topic", "accounting_plutus_analytics", "partition", "0"))
                        );

    }

    public static Stream<Arguments> exceptionScenarioBlueprints() {
        return Stream.of(
                Arguments.of("This is ${{}}. You are ${{adjective:[A-Za-z]+}}",
                             BlueprintParseErrorCode.EMPTY_VARIABLE_REGEX),
                Arguments.of("This is ${{:}}. You are ${{adjective:[A-Za-z]+}}",
                             BlueprintParseErrorCode.EMPTY_VARIABLE_REGEX),
                Arguments.of("This is ${{name::[A-Z]+}}", BlueprintParseErrorCode.INCORRECT_VARIABLE_REPRESENTATION),
                Arguments.of("This is ${{name:}} more text", BlueprintParseErrorCode.TEXT_AFTER_LAST_VARIABLE),
                Arguments.of("This is ${{name: more text", BlueprintParseErrorCode.VARIABLE_NOT_CLOSED));
    }

    public static Stream<Arguments> noMatchBlueprints() {
        return Stream.of(
                Arguments.of("This is ${{variable:[A-Za-z]+}}", "This is some shit"),
                Arguments.of("${{blue:[A-Za-z]+}}", "This is "),
                Arguments.of("Some BS ${{blue:[A-Za-z]+}}", "This is who")
                        );
    }

    @ParameterizedTest
    @MethodSource("happyScenarioBlueprints")
    void testHappyScenarios(final String blueprint,
                            final String source,
                            final Map<String, String> context,
                            final String result,
                            final Map<String, Object> extractedMap) throws BlueprintParseError {
        final Extractor stringExtractor =
                ExtractorBuilder.newBuilder().blueprint(blueprint)
                        .withSkippedVariable("skipped")
                        .withContextMappedVariable("context")
                        .withStaticAttachVariable("attach")
                        .build();
        final ExtractionResult extractionResult = stringExtractor.extractFrom(source, context);
        assertEquals(result, extractionResult.getExtractedString());
        TestUtils.assertMapEquals(extractedMap, extractionResult.getExtractions());
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
}