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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class may be used for extracting from multiple blueprints.
 * Note: The first blueprint that matches with a successful extraction, will be returned as a result
 *
 * @author tushar.naik
 * @since 1.0.0
 */
public class BulkStringExtractor implements Extractor {
    private final List<StringExtractor> stringExtractors;

    public BulkStringExtractor(final List<String> blueprints,
                               final char variableStart,
                               final char variablePrefix,
                               final char regexSeparator,
                               final char variableSuffix,
                               final boolean failOnStringRemainingAfterExtraction,
                               final Set<String> skippedVariables) throws BlueprintParseError {
        stringExtractors = new ArrayList<>();
        for (final String blueprint : blueprints) {
            stringExtractors.add(new StringExtractor(blueprint,
                                                     variableStart,
                                                     variablePrefix,
                                                     regexSeparator,
                                                     variableSuffix,
                                                     failOnStringRemainingAfterExtraction,
                                                     skippedVariables));
        }
    }

    /**
     * Given the precompiled set of blueprints(as part of the constructor),
     * try to extract from source string.
     *
     * @param source string used as source
     * @return ExtractionResult if any of the blueprint extractions were successful, else
     * {@link ExtractionResult#error()} if none match
     */
    @Override
    public ExtractionResult extractFrom(final String source) {
        for (final StringExtractor stringExtractor : stringExtractors) {
            final ExtractionResult extractionResult = stringExtractor.extractFrom(source);
            if (!extractionResult.isError()) {
                return extractionResult;
            }
        }
        return ExtractionResult.error();
    }
}