# String Extractor 

[![Java CI with Maven](https://github.com/tushar-naik/string-extractor/actions/workflows/actions.yml/badge.svg)](https://github.com/tushar-naik/string-extractor/actions/workflows/actions.yml)
[![Snapshot](https://img.shields.io/nexus/s/io.github.tushar-naik/string-extractor?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/tushar-naik/string-extractor/)
[![License](https://img.shields.io/github/license/tushar-naik/string-extractor)](https://github.com/tushar-naik/string-extractor/blob/main/LICENSE)
![Coverage](.github/badges/jacoco.svg)
![Coverage](.github/badges/branches.svg)

A simple utility that can be used to extract values from a string using a compiled blueprint

### Why?
The following example should give you an idea of what an extraction is:<br>
**Blueprint:** `io.${{domain:[a-zA-Z]+}}.${{user:[a-zA-Z]+}}.package}` <br>
**Input String:** `io.github.tushar.package` <br>
**Output:** The ExtractionResult would look like so (This is just a json representation of the `ExtractionResult` object): <br>
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

#### Why would this be required?
- This is essentially a naive implementation for the reverse of a [Handlebars.java](https://github.com/jknack/handlebars.java)
- Transformation of jmx metrics
  - `org.apache.kafka.common.metrics.kafka-sink_prd-001.org.dc.node3` into <br> `name=org.apache.kafka.common.metrics.kafka-sink_` and `host=prd-001.org.dc.node3` 
     

### Features
- Proper error handling
- Allows multiple extractions
- Supports different types of extractions
    1. Exact match              : `org.${{domain:apache}}.org.dc.node3`
    2. Regex Match              : `org.${{domain:[a-z]+}}.org.dc.node3`
    3. Last Variable            : `org.${{domain}}`  
    4. Discarded Variable       : `org.${{:[a-z]+}}.org.dc.node3`
    5. Discarded Exact Variable : `org.${{:apache}}.org.dc.node3`

#### Things to remember:
1. There is a cost associated with regex matching. The more regex variables are matched and extracted, the slower it will be. 
2. If you use exact match variables, last variables, etc, the cost if far lower. You are better off using this than a `string.split(delimiter)` approach.

## Getting started
### Maven dependency
Use the following dependency in your code.
```xml
<dependency>
    <groupId>io.github.tushar-naik</groupId>
    <artifactId>string-extractor</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Usage
```java
final String blueprint = "My name is ${{name:[A-Za-z]+}}";
final StringExtractor stringExtractor = new StringExtractor(blueprint); 
// do the above only once in your code, this is essentially a way of compiling the blueprint and the regexes involved

final String source = "My name is Tushar"
final ExtractionResult extractionResult = stringExtractor.extractFrom(source);
// You can run the above on several source Strings

```


## License
Apache License Version 2.0