package io.github.tushar.naik.stringextractor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkStringExtractorTest {
    static Extractor extractor;

    @BeforeAll
    static void beforeAll() throws BlueprintParseError {
        extractor = ExtractorBuilder.newBuilder().blueprints(
                ImmutableList.of(
                        "io.github.${{name:[a-z]+\\.[a-z]+}}.stringextractor",
                        "org.apache.kafka.common.metrics.consumer-node-metrics.consumer-1.${{node:node-[0-9]+}}"
                                + ".outgoing-byte-rate",
                        "org.perf.service.reminders.${{component:[A-Za-z]+}}.consumed.m5_rate",
                        "kafkawriter.org.apache.kafka.common.metrics.producer-topic-metrics.kafka-sink_${{host:"
                                + "(stg|prd)-[a-z0-9]+.org.[a-z0-9]+}}.offerengine_source.record-send-total",
                        "${{service:[^.]+}}.memory.pools.Metaspace.init",
                        "${{processor:[^.]+}}.kafkasourcev2.org.apache.kafka.common.metrics"
                                + ".consumer-fetch-manager-metrics.consumer-kratos-shadow-processor-1.${{topic:[^"
                                + ".]+}}${{:.[0-9]+}}.records-lead-min"
                                )).build();
    }

    public static Stream<Arguments> bulkExtractions() {
        return Stream.of(
                Arguments.of("io.github.tushar.naik.stringextractor",
                             "io.github..stringextractor",
                             ImmutableMap.of("name", "tushar.naik"),
                             false),

                Arguments.of(
                        "org.apache.kafka.common.metrics.consumer-node-metrics.consumer-1.node-2147482643"
                                + ".outgoing-byte-rate",
                        "org.apache.kafka.common.metrics.consumer-node-metrics.consumer-1..outgoing-byte-rate",
                        ImmutableMap.of("node", "node-2147482643"),
                        false),

                Arguments.of("org.perf.service.reminders.rabbitmq.consumed.m5_rate",
                             "org.perf.service.reminders..consumed.m5_rate",
                             ImmutableMap.of("component", "rabbitmq"),
                             false),

                Arguments.of(
                        "kafkawriter.org.apache.kafka.common.metrics.producer-topic-metrics"
                                + ".kafka-sink_prd-framesoss008.org.nm5.offerengine_source.record-send-total",
                        "kafkawriter.org.apache.kafka.common.metrics.producer-topic-metrics.kafka-sink_"
                                + ".offerengine_source.record-send-total",
                        ImmutableMap.of("host", "prd-framesoss008.org.nm5"),
                        false),
                Arguments.of("kill-switch-notification.memory.pools.Metaspace.init",
                             ".memory.pools.Metaspace.init",
                             ImmutableMap.of("service", "kill-switch-notification"),
                             false),
                Arguments.of(
                        "kratos_shadow_processor.kafkasourcev2.org.apache.kafka.common.metrics"
                                + ".consumer-fetch-manager-metrics.consumer-kratos-shadow-processor-1.kratos_events"
                                + ".13.records-lead-min",
                        ".kafkasourcev2.org.apache.kafka.common.metrics"
                                + ".consumer-fetch-manager-metrics.consumer-kratos-shadow-processor-1."
                                + ".records-lead-min",
                        ImmutableMap.of("processor", "kratos_shadow_processor",
                                        "topic", "kratos_events"),
                        false),
                Arguments.of(
                        "perftest7._st_com.phonepe.app.v4.nativeapps.contacts.common.ui.view.activity"
                                + ".Navigator_ContactPickerActivity_slow_frames",
                        "perftest7._st_com.phonepe.app.v4.nativeapps.contacts.common.ui.view.activity"
                                + ".Navigator_ContactPickerActivity_slow_frames",
                        ImmutableMap.of(),
                        true)
                        );
    }

    @ParameterizedTest
    @MethodSource("bulkExtractions")
    void testBulkExtractions(final String source,
                             final String result,
                             final Map<String, Object> extractedMap,
                             final boolean error) {
        final ExtractionResult extractionResult = extractor.extractFrom(source);
        if (error) {
            assertTrue(extractionResult.isError());
            return;
        }
        assertEquals(result, extractionResult.getExtractedString());
        TestUtils.assertMapEquals(extractedMap, extractionResult.getExtractions());
        assertFalse(extractionResult.isError());
    }

    @ParameterizedTest
    @MethodSource("bulkExtractions")
    void perfTest(final String source,
                  final String result,
                  final Map<String, Object> extractedMap,
                  final boolean error) {
        PerformanceEvaluator performanceEvaluator = new PerformanceEvaluator();
        float evaluationTime = performanceEvaluator
                .evaluateTime(10000, () -> extractor.extractFrom(source));
        System.out.printf("%s evaluations for blueprint took %fms\n", 10000, evaluationTime);
        Assertions.assertTrue(evaluationTime < 500);
    }
}