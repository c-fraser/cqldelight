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

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class StatementParserTest {

  @Test
  fun `parse movie statements`() {
    val source =
        StatementParserTest::class.java.classLoader.getResource("movie.cypher")?.file?.let(::File)
            ?: fail("Failed to get movies cypher file")
    val queries = StatementParser.parse(source).asIterable().iterator()
    assertEquals(findTomHanks, queries.next())
    assertEquals(findCloudAtlas, queries.next())
    assertEquals(find10People, queries.next())
    assertEquals(findNinetiesMovies, queries.next())
    assertEquals(findTomHanksMovies, queries.next())
    assertEquals(findWhoDirectedCloudAtlas, queries.next())
    assertEquals(findTomHanksCoActors, queries.next())
    assertEquals(findPeopleRelatedToCloudAtlas, queries.next())
    assertEquals(findSixDegreesOfKevinBacon, queries.next())
    assertEquals(findPathFromKevinBaconTo, queries.next())
    assertEquals(findRecommendedTomHanksCoActors, queries.next())
    assertEquals(findCoActorsBetweenTomHanksAnd, queries.next())
    assertFalse(queries.hasNext())
  }
}
