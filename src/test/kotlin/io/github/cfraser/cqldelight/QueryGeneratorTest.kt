/*
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
*/
package io.github.cfraser.cqldelight

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class QueryGeneratorTest {

  @Test
  fun `generate movie queries`(@TempDir directory: Path) {
    val packageName = "io.github.cfraser.cqldelight"
    val className = "MovieQueries"
    val generator = QueryGenerator(directory, packageName)
    generator.generate(
        className,
        sequenceOf(
            findTomHanks,
            findCloudAtlas,
            find10People,
            findNinetiesMovies,
            findTomHanksMovies,
            findWhoDirectedCloudAtlas,
            findTomHanksCoActors,
            findPeopleRelatedToCloudAtlas,
            findSixDegreesOfKevinBacon,
            findPathFromKevinBaconTo,
            findRecommendedTomHanksCoActors,
            findCoActorsBetweenTomHanksAnd))
    assertEquals(
        QueryGeneratorTest::class
            .java
            .classLoader
            .getResourceAsStream("MovieQueries.java")
            ?.reader()
            ?.readText(),
        directory.resolve("${packageName.replace('.', '/')}/$className.java").readText())
  }
}
