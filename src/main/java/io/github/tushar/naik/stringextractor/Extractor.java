package io.github.tushar.naik.stringextractor;

import java.util.Map;

public interface Extractor {
    ExtractionResult extractFrom(String source, Map<String, String> contextMap);
}
