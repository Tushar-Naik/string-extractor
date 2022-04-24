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


import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ExtractionResult {
    String extractedString;
    Map<String, Object> extractions;
    boolean error;

    public static ExtractionResult error() {
        return ExtractionResult.builder().error(true).build();
    }
}