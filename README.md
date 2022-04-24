# String Extractor

A simple utility that can be used to extract values from a string using a compiled blueprint

### Features
- Supports Regexes
- Proper error handling
- Creates multiple  

### Samples

Blueprint: `My name is ${{name:[A-Za-z]+}}` <br>
Input String: `My name is Tushar` <br>
Output: Represented as a json, the ExtractionResult would look like: <br>
```json
{
  "extractedString": "My name is ",
  "extractions": {
    "name": "Tushar"
  },
  "error": false
}
```


# Getting started
### Maven dependency
Use the following dependency in your code.
```
<dependency>
    <groupId>io.github.tushar-naik</groupId>
    <artifactId>string-extractor</artifactId>
    <version>1.0.0</version>
</dependency>
```

You might have to include the following repository
```
<repository>
    <id>clojars</id>
    <name>Clojars repository</name>
    <url>https://clojars.org/repo</url>
</repository>
```

### Usage
    final String blueprint = "My name is ${{name:[A-Za-z]+}}";
    final StringExtractor stringExtractor = new StringExtractor(blueprint); 
    // do the above only once in your code, this is essentially a way of compiling the blueprint and the regexes involved
    
    final String source = "My name is Tushar"
    final ExtractionResult extractionResult = stringExtractor.extractFrom(source);
    // You can run the above on several source Strings


## License
Apache License Version 2.0