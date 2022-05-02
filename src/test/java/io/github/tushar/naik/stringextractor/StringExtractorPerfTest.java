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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@Slf4j
class StringExtractorPerfTest {

    @ParameterizedTest
    @MethodSource("evaluations")
    void perfTest(final int numberOfEvaluations,
                  final String blueprint,
                  final String source,
                  final int maxEvalTimeExpected) throws BlueprintParseError {
        PerformanceEvaluator performanceEvaluator = new PerformanceEvaluator();
        final StringExtractor stringExtractor = new StringExtractor(blueprint);
        float evaluationTime = performanceEvaluator
                .evaluateTime(numberOfEvaluations, () -> stringExtractor.extractFrom(source));
        System.out.printf("%s evaluations for blueprint with %d variables took %fms\n", numberOfEvaluations,
                          stringExtractor.numberOfVariables(),
                          evaluationTime);
        Assertions.assertTrue(evaluationTime < maxEvalTimeExpected);
    }

    private static Stream<Arguments> evaluations() {
        return Stream.of(
                Arguments.of(100000,
                             "A ${{what:[A-Za-z]+}}",
                             "A successful",
                             1000),
                Arguments.of(100000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}}",
                             "A successful man ",
                             1000),
                Arguments.of(100000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}}",
                             "A successful man is one",
                             1000),
                Arguments.of(100000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}} who can "
                                     + "${{where:[A-Za-z]+}}",
                             "A successful man is one who can lay",
                             1000),
                Arguments.of(100000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}} who can "
                                     + "${{where:[A-Za-z]+}} a ${{what2:[A-Za-z]+ [A-Za-z]+}}",
                             "A successful man is one who can lay a firm foundation",
                             1000),
                Arguments.of(100000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}} who can "
                                     + "${{where:[A-Za-z]+}} a ${{what2:[A-Za-z]+ [A-Za-z]+}} with the "
                                     + "${{what3:[A-Za-z]+}}",
                             "A successful man is one who can lay a firm foundation with the bricks",
                             1000),
                Arguments.of(100000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}} who can "
                                     + "${{where:[A-Za-z]+}} a ${{what2:[A-Za-z]+ [A-Za-z]+}} with the "
                                     + "${{what3:[A-Za-z]+}} others have thrown at ${{whom:[A-Za-z]+ [A-Za-z]+}}",
                             "A successful man is one who can lay a firm foundation with the bricks others have "
                                     + "thrown at him",
                             1000),

                /* no regex - should be really fast */
                Arguments.of(100000,
                             "org.apache.kafka.common.metrics.kafka-sink_${{host:}}",
                             "org.apache.kafka.common.metrics.kafka-sink_prd-001.org.dc.node3",
                             100)
                        );
    }

    @Test
    void testSplitStrategyBeingSlow() throws BlueprintParseError {
        final PerformanceEvaluator performanceEvaluator = new PerformanceEvaluator();
        String metric = "org.apache.kafka.common.metrics.kafka-sink_prd-001.org.dc.node3";

        /* this is expensive */
        final long timeForEvaluationWithSplit = performanceEvaluator.evaluateTime(100000,
                                                                                  () -> metric.split("\\."));
        System.out.println("split = " + timeForEvaluationWithSplit);

        final StringExtractor stringExtractor = new StringExtractor(
                "org.apache.kafka.common.metrics.kafka-sink_${{host:}}");

        final long timeForEvaluation = performanceEvaluator.evaluateTime(100000,
                                                                         () -> stringExtractor.extractFrom(metric));
        System.out.println("stringExtractor = " + timeForEvaluation);
        Assertions.assertTrue(timeForEvaluation < timeForEvaluationWithSplit);

    }
}