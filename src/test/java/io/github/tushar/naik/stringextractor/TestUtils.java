package io.github.tushar.naik.stringextractor;

import lombok.experimental.UtilityClass;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

@UtilityClass
public class TestUtils {
    public void assertMapEquals(final Map<String, Object> expected,
                                final Map<String, Object> actual) {
        if (!areEqual(expected, actual)) {
            fail("Expected: " + expected + " actual:" + actual);
        }
    }

    public boolean areEqual(final Map<String, Object> first,
                            final Map<String, Object> second) {
        if (first.size() != second.size()) {
            return false;
        }
        return first.entrySet().stream()
                .allMatch(e -> e.getValue().equals(second.get(e.getKey())));
    }
}
