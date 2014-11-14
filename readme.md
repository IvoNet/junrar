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
rar.extractArchive("path/to/rarfile.rar", "path/to/output/folder");

```

or in memory:

```java
RarToMemory rar = new RarToMemory();
final Memory memory = rar.extractArchive("path/to/a/file.rar"));
```

See also the testcase(s) for more code samples



### Maven

At this time this verion of junrar is not found in maven central (yet) as I'm not sure if I may do this under a new groupId?

So this is the current way:
* clone this repository
* follow the build instructions
* add the dependency below to your pom.


```xml
<dependency>
    <groupId>nl.ivonet</groupId>
    <artifactId>junrar</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

