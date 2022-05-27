package io.github.tushar.naik.stringextractor;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtractorBuilderTest {

    @Test
    void testBuilderCreationWithVariables() throws BlueprintParseError {
        final Extractor extractor = ExtractorBuilder.newBuilder()
                .withVariableStart('#')
                .withVariablePrefix('@')
                .withVariableSuffix('^')
                .withRegexSeparator(':')
                .blueprint("Bruno mars says, when i see your #@@what:[a-z]+^^")
                .build();

        final ExtractionResult extractionResult = extractor.extractFrom("Bruno mars says, when i see your face");
        TestUtils.assertMapEquals(ImmutableMap.of("what", "face"), extractionResult.getExtractions());
        assertEquals("Bruno mars says, when i see your ", extractionResult.getExtractedString());
    }

    @Test
    void testErrorBuilderScenarios() {
        BlueprintParseError blueprintParseError = assertThrows(BlueprintParseError.class, () -> {
            ExtractorBuilder.newBuilder()
                    .withVariableStart('#')
                    .withVariablePrefix('@')
                    .withVariableSuffix('@')
                    .withRegexSeparator(':')
                    .blueprint("Bruno mars says, when i see your #@@what:[a-z]+^^")
                    .build();
        });
        assertEquals(BlueprintParseErrorCode.INVALID_CHARACTER_SETTINGS,
                     blueprintParseError.getBlueprintParseErrorCode());

        blueprintParseError = assertThrows(BlueprintParseError.class, () -> {
            ExtractorBuilder.newBuilder()
                    .withVariableStart('@')
                    .withVariablePrefix('@')
                    .withVariableSuffix('!')
                    .withRegexSeparator(':')
                    .blueprint("Bruno mars says, when i see your #@@what:[a-z]+^^")
                    .build();
        });
        assertEquals(BlueprintParseErrorCode.INVALID_CHARACTER_SETTINGS,
                     blueprintParseError.getBlueprintParseErrorCode());

        blueprintParseError = assertThrows(BlueprintParseError.class, () -> {
            ExtractorBuilder.newBuilder()
                    .withVariableStart('!')
                    .withVariablePrefix('@')
                    .withVariableSuffix('!')
                    .withRegexSeparator(':')
                    .blueprint("Bruno mars says, when i see your #@@what:[a-z]+^^")
                    .build();
        });
        assertEquals(BlueprintParseErrorCode.INVALID_CHARACTER_SETTINGS,
                     blueprintParseError.getBlueprintParseErrorCode());

        blueprintParseError = assertThrows(BlueprintParseError.class, () -> {
            ExtractorBuilder.newBuilder()
                    .withVariableStart('#')
                    .withVariablePrefix('@')
                    .withVariableSuffix('!')
                    .withRegexSeparator('#')
                    .blueprint("Bruno mars says, when i see your #@@what:[a-z]+^^")
                    .build();
        });
        assertEquals(BlueprintParseErrorCode.INVALID_CHARACTER_SETTINGS,
                     blueprintParseError.getBlueprintParseErrorCode());

        blueprintParseError = assertThrows(BlueprintParseError.class, () -> {
            ExtractorBuilder.newBuilder()
                    .build();
        });
        assertEquals(BlueprintParseErrorCode.INCORRECT_BUILDER_USAGE,
                     blueprintParseError.getBlueprintParseErrorCode());
    }
}