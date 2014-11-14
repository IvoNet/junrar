JUnrar
=====

Adds support to read and extract a rar.




---

If you want the origional code please go to this repositories parent

Usage of the origional code:

```java
final File rar = new File("foo.rar");  
final File destinationFolder = new File("destinationFolder");  
ExtractArchive extractArchive = new ExtractArchive();  
extractArchive.extractArchive(rar, destinationFolder);  
```

Dependency on maven:  

```xml
<dependency>  
  <groupId>com.github.junrar</groupId>  
  <artifactId>junrar</artifactId>
  <version>0.7</version>  
</dependency>  
```

