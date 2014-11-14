# JUnrar

Adds support to read and extract a rar.


## Prerequisites

* Java 1.8 (JDK)
* Maven 3+


## build


```java
mvn package
```

## Usage

### Commandline


```bash
java -jar junrar-jar-with-dependencies <rar> <output dir>
```


### Code


```java
RarExtractor rar = new RarExtractor();
rar.extractArchive('path/to/rarfile.rar', 'path/to/output/folder')

```

See also the testcase(s) for more code samples





