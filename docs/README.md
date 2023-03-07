# CQLDelight

[![Test](https://github.com/c-fraser/cqldelight/workflows/Test/badge.svg)](https://github.com/c-fraser/cqldelight/actions)
[![Release](https://img.shields.io/github/v/release/c-fraser/cqldelight?logo=github&sort=semver)](https://github.com/c-fraser/cqldelight/releases)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.c-fraser/cqldelight.svg)](https://search.maven.org/search?q=g:io.github.c-fraser%20AND%20a:cqldelight)
[![Javadoc](https://javadoc.io/badge2/io.github.c-fraser/cqldelight/javadoc.svg)](https://javadoc.io/doc/io.github.c-fraser/cqldelight)
[![Apache License 2.0](https://img.shields.io/badge/License-Apache2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

`CQLDelight`, inspired by [SQLDelight](https://github.com/cashapp/sqldelight), is
a [gradle](https://github.com/gradle/gradle) plugin that
generates [executable queries](https://javadoc.io/doc/org.neo4j.driver/neo4j-java-driver/latest/org.neo4j.driver/org/neo4j/driver/Query.html)
from *Cypher* statements.

## Usage

The `cqldelight` library is accessible
via [Maven Central](https://search.maven.org/search?q=g:io.github.c-fraser%20AND%20a:cqldelight). Add the following to
your `build.gradle` configuration to apply the plugin.

```groovy
buildscript {
    dependencies {
        classpath "io.github.c-fraser:cqldelight+"
    }
}

apply plugin: 'io.github.cfraser.cqldelight'
```

The plugin automatically registers the `generateQueries`
and `generateTestQueries` tasks to your project. These tasks
generate [Query](https://javadoc.io/doc/org.neo4j.driver/neo4j-java-driver/latest/org.neo4j.driver/org/neo4j/driver/Query.html)
methods from **parseable** *Cypher* statements in `src/main/cypher/**/*.cypher` and `src/test/cypher/**/*.cypher`,
respectively. To be **parseable**, a *Cypher* statement must be formatted as shown below.

```text
//cqldelight:<query name>
<query>;
```

The `//cqldelight:` prefix denotes the beginning of a **parseable** statement. `<query name>` specifies the name of
the generated *Java* field or method. The `<query name>` should always be in lower camel case format. `<query>` is the
*Cypher* statement. The `<query>` can be styled to your preference, but it must be terminated by a single semicolon.

For these *Cypher* statements (referencing the [movies](https://github.com/neo4j-graph-examples/movies) graph)...

```cypher
//cqldelight:findTomHanksCoActors
MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors)
RETURN DISTINCT coActors.name;

//cqldelight:findCoActorsBetweenTomHanksAnd
MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors),
      (coActors)-[:ACTED_IN]->(m2)<-[:ACTED_IN]-(p:Person {name: $name})
RETURN tom, m, coActors, m2, p;
```

`CQLDelight` would generate (the equivalent of)...

```java
public final class MovieQueries {
    public static final Query FIND_TOM_HANKS_CO_ACTORS = new Query("""
            MATCH (tom:Person {
              name: 'Tom Hanks'
            })-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors)
            RETURN DISTINCT coActors.name""");

    private static final String FIND_CO_ACTORS_BETWEEN_TOM_HANKS_AND = """
            MATCH (tom:Person {
              name: 'Tom Hanks'
            })-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors), (coActors)-[:ACTED_IN]->(m2)<-[:ACTED_IN]-(p:Person {
              name: $name
            })
            RETURN tom, m, coActors, m2, p""";

    public static Query findCoActorsBetweenTomHanksAnd(Object name) {
        return new Query(FIND_CO_ACTORS_BETWEEN_TOM_HANKS_AND, Values.parameters("name", name));
    }
}
```

As demonstrated above, if a parsed *Cypher* statement is parameterized then a factory method will be generated,
otherwise a static field will be generated.

## License

    Copyright 2023 c-fraser
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        https://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
