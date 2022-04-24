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

package com.github.tusharnaik.stringextractor;

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
        System.out.printf("%s evaluations took %fms", numberOfEvaluations, evaluationTime);
        Assertions.assertTrue(evaluationTime < maxEvalTimeExpected);
    }

    private static Stream<Arguments> evaluations() {
        return Stream.of(
                Arguments.of(10000,
                             "A ${{what:[A-Za-z]+}}",
                             "A successful",
                             100),
                Arguments.of(10000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}}",
                             "A successful man ",
                             100),
                Arguments.of(10000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}}",
                             "A successful man is one",
                             100),
                Arguments.of(10000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}} who can "
                                     + "${{where:[A-Za-z]+}}",
                             "A successful man is one who can lay",
                             100),
                Arguments.of(10000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}} who can "
                                     + "${{where:[A-Za-z]+}} a ${{what2:[A-Za-z]+ [A-Za-z]+}}",
                             "A successful man is one who can lay a firm foundation",
                             100),
                Arguments.of(10000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}} who can "
                                     + "${{where:[A-Za-z]+}} a ${{what2:[A-Za-z]+ [A-Za-z]+}} with the "
                                     + "${{what3:[A-Za-z]+}}",
                             "A successful man is one who can lay a firm foundation with the bricks",
                             100),
                Arguments.of(10000,
                             "A ${{what:[A-Za-z]+}} ${{who:[A-Za-z]+}} is ${{one:[A-Za-z]+}} who can "
                                     + "${{where:[A-Za-z]+}} a ${{what2:[A-Za-z]+ [A-Za-z]+}} with the "
                                     + "${{what3:[A-Za-z]+}} others have thrown at ${{whom:[A-Za-z]+ [A-Za-z]+}}",
                             "A successful man is one who can lay a firm foundation with the bricks others have thrown at him",
                             100)
                );
    }

    @Test
    void testPerformance() throws BlueprintParseError {
        String blueprint = "This is ${{name:[A-Za-z]+}}. You are ${{adjective:[A-Za-z]+}} and "
                + "${{adjective2:[A-Za-z]+}} ";
        PerformanceEvaluator performanceEvaluator = new PerformanceEvaluator();
        float evaluation = performanceEvaluator
                .evaluateTime(100000,
                              () -> {
                                     try {
                                         final StringExtractor stringExtractor = new StringExtractor(blueprint);
                                         stringExtractor.extractFrom("This is Tushar. You are Good");
                                     } catch (BlueprintParseError e) {
                                         log.error("Error parsing blueprint", e);
                                     }
                                 });

        evaluation = performanceEvaluator
                .evaluateTime(100000,
                              () -> {
                                     try {
                                         final StringExtractor stringExtractor = new StringExtractor(blueprint);
                                         stringExtractor.extractFrom("This is Tushar. You are Good and kind");
                                     } catch (BlueprintParseError e) {
                                         log.error("Error parsing blueprint", e);
                                     }
                                 });
        System.out.println("100000 evaluations took = " + evaluation);

        final StringExtractor stringExtractor = new StringExtractor(blueprint);
        evaluation = performanceEvaluator
                .evaluateTime(1000000,
                              () -> {
                                     stringExtractor.extractFrom("This is Tushar. You are Good and kind");
                                 });
        System.out.println("100000 evaluations took = " + evaluation);

        evaluation = performanceEvaluator
                .evaluateTime(1000000,
                              () -> {
                                     try {
                                         final StringExtractor stringExtractors = new StringExtractor(blueprint);
                                         stringExtractor.extractFrom("This is Tushar. You are Good and kind");
                                     } catch (BlueprintParseError e) {
                                         log.error("Error parsing blueprint", e);
                                     }
                                 });
        System.out.println("100000 evaluations took = " + evaluation);
    }
}