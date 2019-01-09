# Fat Jar Plugin

A maven plugin for creating fat jar; 

All of the libraries of the current project will be added into the fat jar including current project jar file;

## usage

```xml
<plugin>
    <groupId>com.laomei.test</groupId>
    <artifactId>fat-jar-plugin</artifactId>
    <version>1.0-SNAPSHOT</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>repackage</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## result

A new jar file will be created which the name will be append `'-fat'` at the end of the name;

The structure of fat jar

```text
.
├── lib
│   ├── commons-logging-1.2.jar
│   ├── demo-1.0-SNAPSHOT.jar
│   ├── spring-aop-4.3.16.RELEASE.jar
│   ├── spring-beans-4.3.16.RELEASE.jar
│   ├── spring-boot-1.5.14.RELEASE.jar
│   ├── spring-boot-autoconfigure-1.5.14.RELEASE.jar
│   ├── spring-context-4.3.16.RELEASE.jar
│   ├── spring-core-4.3.17.RELEASE.jar
│   └── spring-expression-4.3.16.RELEASE.jar
└── META-INF
    └── MANIFEST.MF
```

- `demo-1.0-SNAPSHOT.jar` jar file of the current project
- `other jar files` declared in the maven pom.xml
