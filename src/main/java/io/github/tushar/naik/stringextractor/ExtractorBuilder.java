package io.github.tushar.naik.stringextractor;

import java.util.ArrayList;
import java.util.List;

import static io.github.tushar.naik.stringextractor.BlueprintParseErrorCode.INCORRECT_BUILDER_USAGE;

public class ExtractorBuilder {

    private final List<String> blueprints = new ArrayList<>();
    private char variableStart = '$';
    private char variablePrefix = '{';
    private char regexSeparator = ':';
    private char variableSuffix = '}';
    private boolean failOnStringRemainingAfterExtraction = false;
    private String skippedVariable = "";
    private String contextMappedVariable = "";
    private String staticAttachVariable = "";

    public static ExtractorBuilder newBuilder() {
        return new ExtractorBuilder();
    }

    public ExtractorBuilder blueprints(List<String> blueprints) {
        this.blueprints.addAll(blueprints);
        return this;
    }

    public ExtractorBuilder blueprint(String blueprint) {
        this.blueprints.add(blueprint);
        return this;
    }

    public ExtractorBuilder withVariableStart(char variableStart) {
        this.variableStart = variableStart;
        return this;
    }

    public ExtractorBuilder withVariablePrefix(char variablePrefix) {
        this.variablePrefix = variablePrefix;
        return this;
    }

    public ExtractorBuilder withRegexSeparator(char regexSeparator) {
        this.regexSeparator = regexSeparator;
        return this;
    }

    public ExtractorBuilder withVariableSuffix(char variableSuffix) {
        this.variableSuffix = variableSuffix;
        return this;
    }

    public ExtractorBuilder withSkippedVariable(String skippedVariable) {
        this.skippedVariable = skippedVariable;
        return this;
    }

    public ExtractorBuilder withContextMappedVariable(String contextMappedVariable) {
        this.contextMappedVariable = contextMappedVariable;
        return this;
    }

    public ExtractorBuilder withStaticAttachVariable(String staticAttachVariable) {
        this.staticAttachVariable = staticAttachVariable;
        return this;
    }

    public ExtractorBuilder failOnStringRemainingAfterExtraction(boolean failOnStringRemainingAfterExtraction) {
        this.failOnStringRemainingAfterExtraction = failOnStringRemainingAfterExtraction;
        return this;
    }

    public Extractor build() throws BlueprintParseError {
        if (blueprints.isEmpty()) {
            throw new BlueprintParseError(INCORRECT_BUILDER_USAGE);
        }
        if (blueprints.size() == 1) {
            return new StringExtractor(blueprints.stream().findAny().orElse(""), variableStart, variablePrefix,
                                       regexSeparator, variableSuffix, failOnStringRemainingAfterExtraction,
                                       skippedVariable, contextMappedVariable, staticAttachVariable);
        }
        return new BulkStringExtractor(blueprints, variableStart, variablePrefix, regexSeparator, variableSuffix,
                                       failOnStringRemainingAfterExtraction, skippedVariable, contextMappedVariable,
                                       staticAttachVariable);
    }

}
