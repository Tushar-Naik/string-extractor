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

import lombok.Getter;

public enum BlueprintParseErrorCode {
    EMPTY_VARIABLE_REGEX("empty variable in blueprint, like - ${}"),
    EMPTY_VARIABLE_NAME("variable name missing in blueprint, like - ${:[a-z]}"),
    EMPTY_REGEX("regex missing in blueprint, like - ${name:}"),
    INCORRECT_VARIABLE_REPRESENTATION("Incorrect variable representation:  not expressed as ${variable:regex}"),
    PATTERN_SYNTAX("Invalid Regex Pattern"),
    VARIABLE_NOT_CLOSED("Variable in blueprint is unclosed, like - ${{some");

    @Getter
    private final String errorMessage;

    BlueprintParseErrorCode(final String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
