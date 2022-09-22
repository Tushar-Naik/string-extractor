<p align="center">
  <h1 align="center">String Extractor</h1>
  <p align="center">Utility to extract values from a string using a compiled Blueprint<p>
  <p align="center">
    <a href="https://github.com/Tushar-Naik/string-extractor/actions">
    	<img src="https://github.com/tushar-naik/string-extractor/actions/workflows/actions.yml/badge.svg"/>
    </a>
    <a href="https://s01.oss.sonatype.org/content/repositories/releases/io/github/tushar-naik/string-extractor/">
    	<img src="https://img.shields.io/maven-central/v/io.github.tushar-naik/string-extractor"/>
    </a>
    <a href="https://github.com/Tushar-Naik/string-extractor/blob/master/LICENSE">
    	<img src="https://img.shields.io/github/license/tushar-naik/string-extractor" alt="license" />
    </a>
    <a href=".github/badges/jacoco.svg">
    	<img src=".github/badges/jacoco.svg"/>
    </a>
    <a href=".github/badges/branches.svg">
    	<img src=".github/badges/branches.svg"/>
    </a>
  </p>
</p>

### Why though?

- You might have run into situation, where you want to extract substrings from within a string, that match a regex
- Sometimes, you might need a reverse of [Handlebars.java](https://github.com/jknack/handlebars.java)
- You might be trying to transform some jmx metrics
    - `org.apache.kafka.network.prd-001.org.dc.node3`
      into <br> `name=org.apache.kafka.network` and `host=prd-001.org.dc.node3`

This library tries to address the above.
You will need to specify a few extraction blueprints, and run your strings through them to extract values.
Before you get started with the code, let us first understand blueprints.

### Syntax for blueprints aka rules

From here on, we will refer to all rules for extractions as blueprints.<br>
Before we start, it is important to understand the schema/language of a blueprint string:<br>
A simple blueprint would look like so:<br>

```text
someString${{variableName1:matchString1}}someMoreString${{variableName2:matchString2}}
```

where:

1. `variableName`s are **optional** (if not present, then that matched string will be discarded) eg: `${{:matchString}}`
    1. If `variableName` is present, the matched string will be put into the variable and returned as part of the
       result, and removed from the source string.
    2. If absent, the matched string will still be removed from the source string
2. `matchString` could be one of 2 things:
    1. **String**: which expects an exact match. eg: `${{variable1:value1}}`
    2. **Regex**: which expects a regex match. eg: `${{variable1:[a-zA-Z]+}}`
3. `${{` and `}}` are markers for start and end of a variable definition

## Extraction

The following example should give you an idea of what an extraction is (don't get confused seeing the output as a json,
it is just a json representation of the ExtractionResult.java object you will get as result):<br>
<table>
<tr><td>Blueprint</td>
<td>

`You are ${{adjective:[^.]+}}`
</td>
</tr>
<tr><td>Input String</td><td>

`You are beautiful.`
</td></tr>
<tr><td>Output</td><td>

  ```json
{
  "extractedString": "You are .",
  "extractions": {
    "adjective": "beautiful"
  },
  "error": false
}
  ```

</table>
Here is another example: 
<table>
<tr><td>Blueprint</td>
<td>

`io.${{domain:[a-zA-Z]+}}${{:.}}${{user:[a-zA-Z]+}}.package`
</td>
</tr>
<tr><td>Input String</td><td>

`io.github.tushar.package`
</td></tr>
<tr><td>Output</td><td>

  ```json
{
  "extractedString": "io.package",
  "extractions": {
    "domain": "github",
    "user": "tushar"
  },
  "error": false
}
  ```

</table>

## Features

- Proper error handling
- Allows multiple extractions.<br>
  You can extract into multiple variables using the same rule 
- Supports different types of extractions
    1. Exact match Variable     : `org.${{domain:apache}}.org.dc.node3`<br>
       The match happens only if the string had exactly `apache` in the specified position. Eg: org.**apache**.org.dc.node3. This match will be extracted and stored into the variable `domain`
    2. Regex Match Variable     : `org.${{domain:[a-z]+}}.org.dc.node3`<br>
       The match happens to the string the complies to the regex, after the string `org.`. Eg: org.**something**.org.dc.node3. This match will be extracted and stored into the variable `domain`
    3. Last Variable            : `org.${{domain}}`<br>
       The match happens to all text that follows the string `org.`. Eg: org.**something.org.dc.node3**. This match will be extracted and stored into the variable `domain`
    4. Discarded Regex Variable : `org.${{:[a-z]+}}.org.dc.node3`<br>
       The match happens to the string the complies to the regex, after the string `org.`. Eg: org.**something**.org.dc.node3. This match will be extracted out, but **NOT** stored into any variable
    5. Discarded Exact Match    : `org.${{:apache}}.org.dc.node3` <br>
       The match happens only if the string had exactly `apache` in the specified position. Eg: org.**apache**.org.dc.node3. This match will be extracted out, but **NOT** stored into any variable
- Skipping regex matched variables<br> 
  In scenarios where you want to skip variable extractions, for a regex, you can do so by supplying a set of blacklisted variables.
  ```java
    Extractor extractor = ExtractorBuilder.newBuilder().blueprint(blueprint)
                        .withSkippedVariable("skipped")
                        .build();
  ```
  With this: <br>
  `This is ${{skipped:[A-Za-z]+}}. Guns in my ${{place:}}`, the first regex will be skipped
- Adding string from a context<br>
  In scenarios where you want to add runtime variables from a context map to the final extracted string, you can use the following:
  ```java
    Extractor extractor = ExtractorBuilder.newBuilder().blueprint(blueprint)
                        .withContextMappedVariable("context")
                        .build();
    // and then pass in a context Map<String, String> during extraction
    extractor.extractFrom(source, ImmutableMap.of("location", "bangalore"));
  ```
  With this: <br>
  `This is ${{context:location}}.`, the value of location will be pulled from the map and added to the string
- Adding a static string<br>
  If you don't want to pull this from a map, but want to pass along a static string, you can do that too
  ```java
    Extractor extractor = ExtractorBuilder.newBuilder().blueprint(blueprint)
                        .withStaticAttachVariable("attach")
                        .build();
    extractor.extractFrom(source);
  ```
  With this: <br>
  `This is ${{attach:bangalore}}.`, the value bangalore added to the extracted string

### Things to remember:

1. There is a cost associated with regex matching. The more regex variables are matched and extracted, the slower it
   will be.
2. If you use exact match variables, last variables, etc, the cost if far lower. You are better off using this than
   a `string.split(delimiter)` approach.

# Getting started

### Maven dependency

Use the following dependency in your code.

```xml

<dependency>
    <groupId>io.github.tushar-naik</groupId>
    <artifactId>string-extractor</artifactId>
    <version>${extractor.version}</version> <!--look for the latest version on top-->
</dependency>
```

### Usage

The following shows a simple use-case where you want to extract from a single blueprint

```java
final String blueprint="My name is ${{name:[A-Za-z]+}}";
final StringExtractor stringExtractor=new StringExtractor(blueprint);
// do the above only once in your code, this is essentially a way of compiling the blueprint and the regexes involved

final String source="My name is Tushar"
final ExtractionResult extractionResult=stringExtractor.extractFrom(source);
// You can run the above on several source Strings

```

The following shows a more complicated use-case where you want to extract from several blueprints. Note that the first
match that happens, will be the exrtaction result

```java
extractor=ExtractorBuilder.newBuilder().blueprints(
        ImmutableList.of(
        "io.github.${{name:[a-z]+\\.[a-z]+}}.stringextractor",
        "org.apache.kafka.common.metrics.consumer-node-metrics.consumer-1.${{node:node-[0-9]+}}.outgoing-byte-rate",
        "org.perf.service.reminders.${{component:[A-Za-z]+}}.consumed.m5_rate",
        "kafkawriter.org.apache.kafka.common.metrics.producer-topic-metrics.kafka-sink_${{host:(stg|prd)-[a-z0-9]+.org.[a-z0-9]+}}.offerengine_source.record-send-total",
        "${{service:[^.]+}}.memory.pools.Metaspace.init"));

// do the above only once in your code, this is essentially a way of compiling the blueprints and the regexes involved
        ExtractionResult extractionResult1=extractor.extractFrom("io.github.tushar.naik.stringextractor");
        ExtractionResult extractionResult2=extractor.extractFrom("org.perf.service.reminders.rabbitmq.consumed.m5_rate");
// You can run the above on several source Strings

```

## License

Apache License Version 2.0