# Fat Jar Plugin

A maven plugin for creating fat jar; 

All of the libraries for the current project will be added into the fat jar including current project jar file;

## usage

```xml
<plugin>
    <groupId>com.laomei.github</groupId>
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
│   ├── demo-1.0-SNAPSHOT.jar
│   ├── spring-core-4.3.17.RELEASE.jar
└── META-INF
    └── MANIFEST.MF
```

- `demo-1.0-SNAPSHOT.jar` jar file of the current project
- `spring-core-4.3.17.RELEASE.jar` jar file that declared in the pom.xml

# Fat Jar ClassLoader

The classloader that can load class from fat jar;

One fat jar corresponds to one `FatJarClassLoader`; The project which contains multiple fat jar will has multiple `FatJarClassLoader`;

In fact users should use `FatJarDelegateClassLoader` which will manage all `FatJarClassLoader`;
 
`FatJarDelegateClassLoader` expected 3 args in constructors:
 
1. urls
2. parent classloader
3. the prefix name for class which will be load by `FatJarDelegateClassLoader`

>FatJarClassLoader was write with spring-boot-loader;

## How to use

`xxx.jar` is a project jar file witch contains some fat jars;
We only need to give the `xxx.jar` url to `FatJarDelegateClassLoader`; `FatJarDelegateClassLoader` will search all fat jar files in the giving urls and creating multiple `FatJarClassLoader`; The class which name is begin with `com.xxx` will be load by `FatJarDelegateClassLoader`;


```java
URL url = lastClassLoader.getResource("xxx.jar");
new FatJarDelegateClassLoader(
        new URL[] { url },
        null,
        Collections.singleton("com.xxx")
);
```
