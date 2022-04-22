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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringExtractor {
    private final String remains;
    private final boolean failOnStringRemainingAfterExtraction;
    private final List<ParsedComponent> parsedComponents;

    public StringExtractor(final String blueprint) throws BlueprintParseError {
        this(blueprint, false);
    }

    public StringExtractor(final String blueprint, final boolean failOnStringRemainingAfterExtraction)
            throws BlueprintParseError {
        this(blueprint, '$', '{', ':', '}', failOnStringRemainingAfterExtraction);
    }

    public StringExtractor(final String blueprint,
                           final char variableStart,
                           final char variablePrefix,
                           final char regexSeparator,
                           final char variableSuffix,
                           final boolean failOnStringRemainingAfterExtraction) throws BlueprintParseError {
        this.failOnStringRemainingAfterExtraction = failOnStringRemainingAfterExtraction;
        this.parsedComponents = new ArrayList<>();

        final char[] chars = blueprint.toCharArray();
        final StringBuilder collected = new StringBuilder();
        final StringBuilder remainsBuilder = new StringBuilder();
        final StringBuilder variableName = new StringBuilder();

        boolean variableIsBeingExtracted = false;
        int index = 0;
        while (index < chars.length) {
            if (isVariableStart(variableStart, variablePrefix, chars, index)) {
                if (collected.length() != 0) {
                    final String collectedString = collected.toString();
                    parsedComponents.add(new ExactMatchComponent(collectedString));
                    remainsBuilder.append(collectedString);
                    Utils.clearStringBuilder(collected);
                }

                variableIsBeingExtracted = true;
                index += 3; /* skip both, the start and the prefix characters */
                continue;
            }
            if (variableIsBeingExtracted && isVariableEnd(variableSuffix, chars, index)) {
                final String variableNameRegex = variableName.toString();
                final Variable variable = extractVariable(regexSeparator, variableNameRegex);
                parsedComponents.add(new VariableComponent(variable));
                variableIsBeingExtracted = false;
                Utils.clearStringBuilder(variableName);
                index += 2;

            } else if (variableIsBeingExtracted) {
                variableName.append(chars[index]);
                index++;
            } else {
                collected.append(chars[index]);
                index++;
            }
        }
        /* don't forget the remaining collected string */
        if (collected.length() != 0) {
            final String collectedString = collected.toString();
            parsedComponents.add(new ExactMatchComponent(collectedString));
            remainsBuilder.append(collected);
        }
        if (variableIsBeingExtracted) {
            throw new BlueprintParseError(BlueprintParseErrorCode.VARIABLE_NOT_CLOSED);
        }
        remains = remainsBuilder.toString();
    }

    private boolean isVariableEnd(final char variableSuffix, final char[] chars, final int index) {
        return index + 1 < chars.length && chars[index] == variableSuffix
                && chars[index + 1] == variableSuffix
                && ((index + 2) == chars.length || chars[index + 2] != variableSuffix); // this last condition is to
        // handle things like: ${{some:[A-Z]{3}}} -> here it ends with }}}
    }

    private boolean isVariableStart(final char variableStart, final char variablePrefix, final char[] chars,
                                    final int index) {
        return (index + 2) < chars.length
                && chars[index] == variableStart
                && chars[index + 1] == variablePrefix
                && chars[index + 2] == variablePrefix;
    }

    private Variable extractVariable(final char regexSeparator, final String variableNameRegex)
            throws BlueprintParseError {
        if (Utils.isNullOrEmpty(variableNameRegex)) {
            throw new BlueprintParseError(BlueprintParseErrorCode.EMPTY_VARIABLE_REGEX);
        }
        final String[] variableRegexSplits = variableNameRegex.split(String.valueOf(regexSeparator));
        if (variableRegexSplits.length > 2) {
            throw new BlueprintParseError(BlueprintParseErrorCode.INCORRECT_VARIABLE_REPRESENTATION);
        }
        if (Utils.isNullOrEmpty(variableRegexSplits[0])) {
            throw new BlueprintParseError(BlueprintParseErrorCode.EMPTY_VARIABLE_NAME);
        }
        if (variableRegexSplits.length == 1) {
            throw new BlueprintParseError(BlueprintParseErrorCode.EMPTY_REGEX);
        }

        try {
            final Pattern compile = Pattern.compile(variableRegexSplits[1]);
            return new Variable(variableRegexSplits[0], variableRegexSplits[1],
                                compile);
        } catch (PatternSyntaxException exception) {
            throw new BlueprintParseError(BlueprintParseErrorCode.PATTERN_SYNTAX, exception);
        }
    }

    public ExtractionResult extractFrom(final String source) {
        final Map<String, Object> extractions = new HashMap<>();
        String drain = source;

        for (final ParsedComponent parsedComponent : parsedComponents) {
            final String finalDrain = drain;
            drain = parsedComponent.accept(new ParsedComponentVisitor<String>() {
                @Override
                public String visit(final ExactMatchComponent exactMatchComponent) {
                    if (finalDrain.startsWith(exactMatchComponent.getCharacters())) {
                        return finalDrain.substring(exactMatchComponent.getCharacters().length());
                    }
                    return null;
                }

                @Override
                public String visit(final VariableComponent variableComponent) {
                    final Variable variable = variableComponent.getVariable();
                    final Pattern pattern = variable.getPattern();
                    final Matcher matcher = pattern.matcher(finalDrain);
                    if (matcher.find()) {
                        final String firstMatch = matcher.group(0);
                        extractions.put(variable.getVariableName(), firstMatch);
                        return matcher.replaceFirst("");
                    }
                    return null;
                }
            });
            if (drain == null) {
                return ExtractionResult.error();
            }
        }

        if (failOnStringRemainingAfterExtraction && !Utils.isNullOrEmpty(drain)) {
            return ExtractionResult.error();
        }

        return ExtractionResult.builder()
                .extractedString(remains + drain)
                .extractions(extractions)
                .build();
    }
}