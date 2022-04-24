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

public class BlueprintParseError extends Exception {
    @Getter
    private final BlueprintParseErrorCode blueprintParseErrorCode;

    public BlueprintParseError(final BlueprintParseErrorCode blueprintParseErrorCode) {
        super(blueprintParseErrorCode.getErrorMessage());
        this.blueprintParseErrorCode = blueprintParseErrorCode;
    }

    public BlueprintParseError(final BlueprintParseErrorCode blueprintParseErrorCode, Throwable t) {
        super(blueprintParseErrorCode.getErrorMessage(), t);
        this.blueprintParseErrorCode = blueprintParseErrorCode;
    }
}