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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The class that houses the core logic for extraction
 *
 * @author tushar.naik
 * @since 1.0.0
 */
public class StringExtractor implements Extractor {
    private static final Pattern STR_WITH_SPECIAL_CHARACTERS = Pattern.compile("[^a-zA-Z\\d]");

    private final String remains;
    private final boolean failOnStringRemainingAfterExtraction;
    private final List<ParsedComponent> parsedComponents;
    private final int numOfVariables;
    private static final VariableVisitor<Boolean> IS_LAST_VARIABLE = new VariableVisitor<Boolean>() {
        @Override
        public Boolean visit(final RegexMatchVariable regexMatchVariable) {
            return false;
        }

        @Override
        public Boolean visit(final DiscardedRegexMatchVariable discardedRegexMatchVariable) {
            return false;
        }

        @Override
        public Boolean visit(final LastVariable lastVariable) {
            return true;
        }

        @Override
        public Boolean visit(final ExactMatchVariable exactMatchVariable) {
            return false;
        }

        @Override
        public Boolean visit(final DiscardedExactMatchVariable discardedExactMatchVariable) {
            return false;
        }
    };
    private static final ParsedComponentVisitor<Boolean> IS_VARIABLE = new ParsedComponentVisitor<Boolean>() {
        @Override
        public Boolean visit(final ExactMatchComponent exactMatchComponent) {
            return false;
        }

        @Override
        public Boolean visit(final VariableComponent variableComponent) {
            return true;
        }
    };

    public StringExtractor(final String blueprint) throws BlueprintParseError {
        this(blueprint, false);
    }

    public StringExtractor(final String blueprint, final boolean failOnStringRemainingAfterExtraction)
            throws BlueprintParseError {
        this(blueprint, '$', '{', ':', '}', failOnStringRemainingAfterExtraction);
    }

    /**
     * @param blueprint                            the string that essentially represents the variable extraction rules
     *                                             "io.${{domain:[a-zA-Z]+}}.${{user:[a-zA-Z]+}}.package"
     * @param variableStart                        character representing the start of a variable
     * @param variablePrefix                       character representing the prefix after start
     * @param regexSeparator                       character that separates the variable from the regex
     * @param variableSuffix                       character that
     * @param failOnStringRemainingAfterExtraction set this to true if
     * @throws BlueprintParseError any error while parsing the blueprint
     */
    public StringExtractor(final String blueprint,
                           final char variableStart,
                           final char variablePrefix,
                           final char regexSeparator,
                           final char variableSuffix,
                           final boolean failOnStringRemainingAfterExtraction) throws BlueprintParseError {
        this.failOnStringRemainingAfterExtraction = failOnStringRemainingAfterExtraction;
        this.parsedComponents = new ArrayList<>();

        /* Just to be clear, I'm not proud of what the code below, need to move it to a proper language parser */

        final char[] chars = blueprint.toCharArray();
        final StringBuilder collected = new StringBuilder();
        final StringBuilder remainsBuilder = new StringBuilder();
        final StringBuilder variableName = new StringBuilder();

        boolean variableIsBeingExtracted = false;
        boolean lastVariableInvolved = false;
        int index = 0;
        while (index < chars.length) {
            if (lastVariableInvolved) {
                /* if there was a LastVariable without regex, then there should be no more collected string */
                throw new BlueprintParseError(BlueprintParseErrorCode.TEXT_AFTER_LAST_VARIABLE);
            }
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
                if (Boolean.TRUE.equals(variable.accept(IS_LAST_VARIABLE))) {
                    lastVariableInvolved = true;
                }
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
        numOfVariables = (int) parsedComponents.stream().filter(k -> k.accept(IS_VARIABLE)).count();
    }

    /**
     * perform extractions from a source string using the compiled blueprint
     *
     * @param source source string
     * @return result after extraction
     */
    @Override
    public ExtractionResult extractFrom(final String source) {
        final Map<String, Object> extractions = new HashMap<>(numOfVariables);
        String drain = source;

        for (final ParsedComponent parsedComponent : parsedComponents) {
            final String finalDrain = drain;
            drain = parsedComponent.accept(generateDrainFromComponent(extractions, finalDrain));
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

        /* validations */
        if (Utils.isNullOrEmpty(variableNameRegex)) {
            throw new BlueprintParseError(BlueprintParseErrorCode.EMPTY_VARIABLE_REGEX);
        }
        final String[] variableRegexSplits = variableNameRegex.split(String.valueOf(regexSeparator));
        if (variableRegexSplits.length > 2) { // handles ${{a:b:c}}
            throw new BlueprintParseError(BlueprintParseErrorCode.INCORRECT_VARIABLE_REPRESENTATION);
        }
        if (variableRegexSplits.length == 0) { // handles ${{:}}
            throw new BlueprintParseError(BlueprintParseErrorCode.EMPTY_VARIABLE_REGEX);
        }


        if (variableRegexSplits.length == 1) {
            return new LastVariable(variableRegexSplits[0]);
        }

        if (Utils.isNullOrEmpty(variableRegexSplits[0])
                && !Utils.isNullOrEmpty(variableRegexSplits[1])) {
            if (!STR_WITH_SPECIAL_CHARACTERS.matcher(variableRegexSplits[1]).find()) {
                return new DiscardedExactMatchVariable(variableRegexSplits[1]);
            }
            try {
                final Pattern compile = Pattern.compile(variableRegexSplits[1]);
                return new DiscardedRegexMatchVariable(compile);
            } catch (PatternSyntaxException e) {
                return new DiscardedExactMatchVariable(variableRegexSplits[1]);
            }
        }

        try {
            if (!STR_WITH_SPECIAL_CHARACTERS.matcher(variableRegexSplits[1]).find()) {
                return new ExactMatchVariable(variableRegexSplits[0], variableRegexSplits[1]);
            }
            final Pattern compile = Pattern.compile(variableRegexSplits[1]);
            return new RegexMatchVariable(variableRegexSplits[0], compile);
        } catch (PatternSyntaxException exception) {
            return new ExactMatchVariable(variableRegexSplits[0], variableRegexSplits[1]);
        }
    }

    private ParsedComponentVisitor<String> generateDrainFromComponent(final Map<String, Object> extractions,
                                                                      final String finalDrain) {
        return new ParsedComponentVisitor<String>() {
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
                return variable.accept(generateDrainByExtractingVariable());
            }

            private VariableVisitor<String> generateDrainByExtractingVariable() {
                return new VariableVisitor<String>() {
                    @Override
                    public String visit(final RegexMatchVariable regexMatchVariable) {
                        final Pattern pattern = regexMatchVariable.getPattern();
                        final Matcher matcher = pattern.matcher(finalDrain);
                        if (matcher.find()) {
                            final String firstMatch = matcher.group(0);
                            extractions.put(regexMatchVariable.getVariableName(), firstMatch);
                            return finalDrain.substring(firstMatch.length());
                        }
                        return null;
                    }

                    @Override
                    public String visit(final ExactMatchVariable exactMatchVariable) {
                        if (finalDrain.startsWith(exactMatchVariable.getMatchString())) {
                            extractions.put(exactMatchVariable.getVariableName(), exactMatchVariable.getMatchString());
                            return finalDrain.substring(exactMatchVariable.getMatchString().length());
                        }
                        return null;
                    }

                    @Override
                    public String visit(final DiscardedRegexMatchVariable discardedRegexMatchVariable) {
                        final Pattern pattern = discardedRegexMatchVariable.getPattern();
                        final Matcher matcher = pattern.matcher(finalDrain);
                        if (matcher.find()) {
                            final String firstMatch = matcher.group(0);
                            return finalDrain.substring(firstMatch.length());
                        }
                        return null;
                    }

                    @Override
                    public String visit(final LastVariable lastVariable) {
                        if (!Utils.isNullOrEmpty(finalDrain)) {
                            extractions.put(lastVariable.getVariableName(), finalDrain);
                            return "";
                        }
                        return null;
                    }

                    @Override
                    public String visit(final DiscardedExactMatchVariable discardedExactMatchVariable) {
                        if (finalDrain.startsWith(discardedExactMatchVariable.getMatchString())) {
                            return finalDrain.substring(discardedExactMatchVariable.getMatchString().length());
                        }
                        return null;
                    }
                };
            }
        };
    }

    public long numberOfVariables() {
        return numOfVariables;
    }
}