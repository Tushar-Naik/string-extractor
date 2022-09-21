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

import io.github.tushar.naik.stringextractor.variable.ContextMappedVariable;
import io.github.tushar.naik.stringextractor.variable.DiscardedExactMatchVariable;
import io.github.tushar.naik.stringextractor.variable.DiscardedRegexMatchVariable;
import io.github.tushar.naik.stringextractor.variable.ExactMatchVariable;
import io.github.tushar.naik.stringextractor.variable.LastVariable;
import io.github.tushar.naik.stringextractor.variable.RegexMatchVariable;
import io.github.tushar.naik.stringextractor.variable.StaticAttachVariable;
import io.github.tushar.naik.stringextractor.variable.Variable;
import io.github.tushar.naik.stringextractor.variable.VariableVisitor;
import lombok.Value;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    /**
     * a visitor on the variable types, which returns true if it was of type {@link LastVariable}
     */
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

        @Override
        public Boolean visit(final ContextMappedVariable contextMappedVariable) {
            return false;
        }

        @Override
        public Boolean visit(final StaticAttachVariable staticAttachVariable) {
            return false;
        }
    };
    /**
     * A visitor on the component, that returns true if applied on a {@link VariableComponent} typed object
     */
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
    private final boolean failOnStringRemainingAfterExtraction;
    private final List<ParsedComponent> parsedComponents;
    private final int numberOfVariables;
    private final String skippedVariable;
    private final String contextMappingVariable;
    private final String staticAttachVariable;

    public StringExtractor(final String blueprint) throws BlueprintParseError {
        this(blueprint, false);
    }

    public StringExtractor(final String blueprint, final boolean failOnStringRemainingAfterExtraction)
            throws BlueprintParseError {
        this(blueprint, '$', '{', ':', '}', failOnStringRemainingAfterExtraction, "", "", "");
    }

    /**
     * @param blueprint                            the string that essentially represents the variable extraction rules
     *                                             "io.${{domain:[a-zA-Z]+}}.${{user:[a-zA-Z]+}}.package"
     * @param variableStart                        character representing the start of a variable
     * @param variablePrefix                       character representing the prefix after start
     * @param regexSeparator                       character that separates the variable from the regex
     * @param variableSuffix                       character that represents the suffix
     * @param failOnStringRemainingAfterExtraction set this to true if you want to ignore if there are dangling
     *                                             characters after the last variable
     * @param skippedVariable
     * @throws BlueprintParseError any error while parsing the blueprint
     */
    @SuppressWarnings("java:S107")
    public StringExtractor(final String blueprint,
                           final char variableStart,
                           final char variablePrefix,
                           final char regexSeparator,
                           final char variableSuffix,
                           final boolean failOnStringRemainingAfterExtraction,
                           final String skippedVariable,
                           final String contextMappingVariable,
                           final String staticAttachVariable) throws BlueprintParseError {

        /* a base condition check */
        checkCondition(variableStart == variablePrefix ||
                               variableStart == regexSeparator ||
                               variableStart == variableSuffix ||
                               variablePrefix == regexSeparator ||
                               variablePrefix == variableSuffix ||
                               regexSeparator == variableSuffix,
                       BlueprintParseErrorCode.INVALID_CHARACTER_SETTINGS);

        this.failOnStringRemainingAfterExtraction = failOnStringRemainingAfterExtraction;
        this.parsedComponents = new ArrayList<>();
        this.skippedVariable = skippedVariable;
        this.contextMappingVariable = contextMappingVariable;
        this.staticAttachVariable = staticAttachVariable;

        /* Just to be clear, I'm not proud of the code below, need to move it to a proper LL(1) language parser */

        val chars = blueprint.toCharArray();
        val collected = new StringBuilder();
        val variableName = new StringBuilder();

        boolean variableIsBeingExtracted = false;
        boolean lastVariableInvolved = false;
        int index = 0;
        while (index < chars.length) {
            checkCondition(lastVariableInvolved, BlueprintParseErrorCode.TEXT_AFTER_LAST_VARIABLE);
            if (isVariableStart(variableStart, variablePrefix, chars, index)) {
                if (collected.length() != 0) {
                    final String collectedString = collected.toString();
                    parsedComponents.add(new ExactMatchComponent(collectedString));
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
        }
        checkCondition(variableIsBeingExtracted, BlueprintParseErrorCode.VARIABLE_NOT_CLOSED);
        numberOfVariables = (int) parsedComponents.stream().filter(k -> k.accept(IS_VARIABLE)).count();
    }

    /**
     * perform extractions from a source string using the compiled blueprint
     *
     * @param source     source string
     * @param contextMap a context that acn be passed and used for replacing values during extraction
     * @return result after extraction
     */
    @Override
    public ExtractionResult extractFrom(final String source, final Map<String, String> contextMap) {
        final Map<String, Object> extractions = new HashMap<>(numberOfVariables);

        /* drain or string_yet_to_be_parsed represents how much of the string is remaining. We start with the source */
        String drain = source;
        StringBuilder extractedString = new StringBuilder();

        for (final ParsedComponent parsedComponent : parsedComponents) {
            /* an effective final variable to pass along the current value into the generator function below */
            val finalDrain = drain;

            Optional<ExtractedResult> extractedResult =
                    parsedComponent.accept(generateDrainFromComponent(extractions, finalDrain, contextMap));
            if (!extractedResult.isPresent()) {
                return ExtractionResult.error();
            }
            drain = extractedResult.get().getDrain();
            extractedString.append(extractedResult.get().getExtraction());
        }

        if (failOnStringRemainingAfterExtraction && !Utils.isNullOrEmpty(drain)) {
            return ExtractionResult.error();
        }

        return ExtractionResult.builder()
                .extractedString(extractedString + drain)
                .extractions(extractions)
                .build();
    }

    public long numberOfVariables() {
        return numberOfVariables;
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
        checkCondition(Utils.isNullOrEmpty(variableNameRegex), BlueprintParseErrorCode.EMPTY_VARIABLE_REGEX);
        val variableRegexSplits = variableNameRegex.split(String.valueOf(regexSeparator));
        checkCondition(variableRegexSplits.length > 2, BlueprintParseErrorCode.INCORRECT_VARIABLE_REPRESENTATION);
        checkCondition(variableRegexSplits.length == 0, BlueprintParseErrorCode.EMPTY_VARIABLE_REGEX);


        final String lhs = variableRegexSplits[0];

        /* if only lhs exists, it has to be the last variable */
        if (variableRegexSplits.length == 1) {
            return new LastVariable(lhs);
        }

        final String rhs = variableRegexSplits[1];

        /* if lhs is empty but rhs is provided, it is meant for some form of discarded variable (remove the matched
        value from source string) */
        if (Utils.isNullOrEmpty(lhs)
                && !Utils.isNullOrEmpty(rhs)) {
            if (!STR_WITH_SPECIAL_CHARACTERS.matcher(rhs).find()) {
                return new DiscardedExactMatchVariable(rhs);
            }
            try {
                val compile = Pattern.compile(rhs);
                return new DiscardedRegexMatchVariable(compile);
            } catch (PatternSyntaxException e) {
                return new DiscardedExactMatchVariable(rhs);
            }
        }

        if (lhs.equals(contextMappingVariable)) {
            return new ContextMappedVariable(rhs);
        }

        if (lhs.equals(staticAttachVariable)) {
            return new ContextMappedVariable(rhs);
        }

        try {
            /* very naive way of checking if the rhs is a regex */
            if (!STR_WITH_SPECIAL_CHARACTERS.matcher(rhs).find()) {
                return new ExactMatchVariable(lhs, rhs);
            }
            val compile = Pattern.compile(rhs);
            return new RegexMatchVariable(lhs, compile);
        } catch (PatternSyntaxException exception) {
            return new ExactMatchVariable(lhs, rhs);
        }
    }

    private ParsedComponentVisitor<Optional<ExtractedResult>> generateDrainFromComponent(
            final Map<String, Object> extractions,
            final String drain,
            final Map<String, String> contextMap) {
        return new ParsedComponentVisitor<Optional<ExtractedResult>>() {
            @Override
            public Optional<ExtractedResult> visit(final ExactMatchComponent exactMatchComponent) {
                if (drain.startsWith(exactMatchComponent.getCharacters())) {
                    val remainingString = drain.substring(exactMatchComponent.getCharacters().length());
                    return ExtractedResult.of(exactMatchComponent.getCharacters(), remainingString);
                }
                return Optional.empty();
            }

            @Override
            public Optional<ExtractedResult> visit(final VariableComponent variableComponent) {
                val variable = variableComponent.getVariable();
                return variable.accept(generateDrainByExtractingVariable());
            }

            private VariableVisitor<Optional<ExtractedResult>> generateDrainByExtractingVariable() {
                return new VariableVisitor<Optional<ExtractedResult>>() {
                    @Override
                    public Optional<ExtractedResult> visit(final RegexMatchVariable regexMatchVariable) {
                        val pattern = regexMatchVariable.getPattern();
                        val matcher = pattern.matcher(drain);
                        if (matcher.find()) {
                            val firstMatch = matcher.group(0);
                            String extraction = "";
                            if (skippedVariable.equals(regexMatchVariable.getVariableName())) {
                                extraction = firstMatch;
                            } else {
                                extractions.put(regexMatchVariable.getVariableName(), firstMatch);
                            }
                            val remainingString = drain.substring(firstMatch.length());
                            return ExtractedResult.of(extraction, remainingString);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<ExtractedResult> visit(final ExactMatchVariable exactMatchVariable) {
                        if (drain.startsWith(exactMatchVariable.getMatchString())) {
                            String extraction = "";
                            if (skippedVariable.equals(exactMatchVariable.getVariableName())) {
                                extraction = exactMatchVariable.getMatchString();
                            } else {
                                extractions.put(exactMatchVariable.getVariableName(),
                                                exactMatchVariable.getMatchString());
                            }
                            extractions.put(exactMatchVariable.getVariableName(), exactMatchVariable.getMatchString());
                            val remainingString = drain.substring(exactMatchVariable.getMatchString().length());
                            return ExtractedResult.of(extraction, remainingString);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<ExtractedResult> visit(
                            final DiscardedRegexMatchVariable discardedRegexMatchVariable) {
                        val pattern = discardedRegexMatchVariable.getPattern();
                        val matcher = pattern.matcher(drain);
                        if (matcher.find()) {
                            val firstMatch = matcher.group(0);
                            val remainingString = drain.substring(firstMatch.length());
                            return ExtractedResult.of("", remainingString);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<ExtractedResult> visit(final LastVariable lastVariable) {
                        if (!Utils.isNullOrEmpty(drain)) {
                            String extraction = "";
                            if (skippedVariable.equals(lastVariable.getVariableName())) {
                                extraction = drain;
                            } else {
                                extractions.put(lastVariable.getVariableName(), drain);
                            }
                            extractions.put(lastVariable.getVariableName(), drain);
                            return ExtractedResult.of(extraction, "");
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<ExtractedResult> visit(
                            final DiscardedExactMatchVariable discardedExactMatchVariable) {
                        if (drain.startsWith(discardedExactMatchVariable.getMatchString())) {
                            val remainingString = drain.substring(
                                    discardedExactMatchVariable.getMatchString().length());
                            return ExtractedResult.of("", remainingString);
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Optional<ExtractedResult> visit(final ContextMappedVariable contextMappedVariable) {
                        if (contextMap != null &&
                                contextMap.containsKey(contextMappedVariable.getMappingString())) {
                            return ExtractedResult.of(contextMap.get(contextMappedVariable.getMappingString()), drain);
                        }
                        return ExtractedResult.of("", drain);
                    }

                    @Override
                    public Optional<ExtractedResult> visit(final StaticAttachVariable staticAttachVariable) {
                        return ExtractedResult.of(staticAttachVariable.getStaticAttachString(), drain);
                    }
                };
            }
        };
    }

    private void checkCondition(final boolean condition, final BlueprintParseErrorCode invalidCharacterSettings)
            throws BlueprintParseError {
        if (condition) {
            throw new BlueprintParseError(invalidCharacterSettings);
        }
    }

    @Value
    private static class ExtractedResult {
        String extraction;
        String drain;

        public static Optional<ExtractedResult> of(String extraction, String drain) {
            return Optional.of(new ExtractedResult(extraction, drain));
        }
    }
}