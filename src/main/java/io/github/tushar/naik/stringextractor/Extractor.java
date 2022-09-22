package io.github.tushar.naik.stringextractor;

import java.util.Collections;
import java.util.Map;

public interface Extractor {
    default ExtractionResult extractFrom(String source) {
        return extractFrom(source, Collections.emptyMap());
    }
    ExtractionResult extractFrom(String source, Map<String, String> contextMap);
}
