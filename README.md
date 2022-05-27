<p align="center">
  <h2 align="center">String Extractor</h2>
  <p align="center">Utility to extract values from a string using a compiled Blueprint<p>
  <p align="center">
    <a href="https://github.com/Tushar-Naik/string-extractor/actions">
    	<img src="https://github.com/tushar-naik/string-extractor/actions/workflows/actions.yml/badge.svg"/>
    </a>
    <a href="https://s01.oss.sonatype.org/content/repositories/releases/io/github/tushar-naik/string-extractor/">
    	<img src="https://img.shields.io/maven-central/v/io.github.tushar-naik/string-extractor"/>
    </a>
    <a href="license">
    	<img src="https://img.shields.io/github/license/tushar-naik/string-extractor" alt="license mit" />
    </a>
    <a href=".github/badges/jacoco.svg">
    	<img src=".github/badges/branches.svg"/>
    </a>
    <a href=".github/badges/jacoco.svg">
    	<img src=".github/badges/branches.svg"/>
    </a>
  </p>
</p>

### Why though?

- You might have run into situation, where you want to extract substrings from within a string, that match a regex
- Sometimes, you might need a reverse of [Handlebars.java](https://github.com/jknack/handlebars.java)
- Could be used for transformation of jmx metrics
    - `org.apache.kafka.network.prd-001.org.dc.node3`
      into <br> `name=org.apache.kafka.network` and `host=prd-001.org.dc.node3`

This library tries to address the above.
You will need to specify a few extraction blueprints, and run your strings through them to extract values.
Before you get started with the code, let us first understand blueprints.

### Syntax for extraction blueprints aka rules

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

### So what is an extraction?

The following example should give you an idea of what an extraction is:<br>



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
    "adjective": "beautiful",
  },
  "error": false
}
  ```
</table>


Here is another example: 
<table>
<tr><td>Blueprint</td>
<td>

`io.${{domain:[a-zA-Z]+}}.${{user:[a-zA-Z]+}}.package}`
</td>
</tr>
<tr><td>Input String</td><td>

`io.github.tushar.package`
</td></tr>


<tr><td>Output</td><td>

  ```json
{
  "extractedString": "io..package ",
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
- Allows multiple extractions
- Supports different types of extractions
    1. Exact match Variable     : `org.${{domain:apache}}.org.dc.node3`
    2. Regex Match Variable     : `org.${{domain:[a-z]+}}.org.dc.node3`
    3. Last Variable            : `org.${{domain}}`
    4. Discarded Regex Variable : `org.${{:[a-z]+}}.org.dc.node3`
    5. Discarded Exact Match    : `org.${{:apache}}.org.dc.node3`

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
extractor=new BulkStringExtractor(
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