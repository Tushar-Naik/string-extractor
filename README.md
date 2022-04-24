# String Extractor 

[![Java CI with Maven](https://github.com/tushar-naik/string-extractor/actions/workflows/actions.yml/badge.svg)](https://github.com/tushar-naik/string-extractor/actions/workflows/actions.yml)
[![Snapshot](https://img.shields.io/nexus/s/io.github.tushar-naik/string-extractor?server=https%3A%2F%2Fs01.oss.sonatype.org)](https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/tushar-naik/string-extractor/)

A simple utility that can be used to extract values from a string using a compiled blueprint

### Why?
The following example should give you an idea of what an extraction is:<br>
Blueprint: `My name is ${{name:[A-Za-z]+}}` <br>
Input String: `My name is Tushar` <br>
Output: The ExtractionResult would look like so (This is just a json representation of the `ExtractionResult` object): <br>
```json
{
  "extractedString": "My name is ",
  "extractions": {
    "name": "Tushar"
  },
  "error": false
}
```

#### Why would this be required?
- Transformation of jmx metrics
  - org.apache.kafka.common.metrics.kafka-sink_prd-001.org.dc.node3 into name->org.apache.kafka.common.metrics.kafka-sink_ and host->prd-001.org.dc.node3 
     

### Features
- Supports extraction using Regexes
- Proper error handling
- Allows multiple extractions


# Getting started
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