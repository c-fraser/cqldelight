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

internal val findTomHanks =
    Statement(
            Options("findTomHanks"),
            """|MATCH (tom:Person {name: 'Tom Hanks'})
            |RETURN tom"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 3
        }

internal val findCloudAtlas =
    Statement(
            Options("findCloudAtlas"),
            """|MATCH (cloudAtlas:Movie {title: 'Cloud Atlas'})
            |RETURN cloudAtlas"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 7
        }

internal val find10People =
    Statement(
            Options("find10People"),
            """|MATCH (people:Person)
            |RETURN people.name
            |  LIMIT 10"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 11
        }

internal val findNinetiesMovies =
    Statement(
            Options("findNinetiesMovies"),
            """|MATCH (nineties:Movie)
            |  WHERE nineties.released >= 1990 AND nineties.released < 2000
            |RETURN nineties.title"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 16
        }

internal val findTomHanksMovies =
    Statement(
            Options("findTomHanksMovies"),
            """|MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(tomHanksMovies)
            |RETURN tom, tomHanksMovies"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 21
        }

internal val findWhoDirectedCloudAtlas =
    Statement(
            Options("findWhoDirectedCloudAtlas"),
            """|MATCH (cloudAtlas:Movie {title: 'Cloud Atlas'})<-[:DIRECTED]-(directors)
            |RETURN directors.name"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 25
        }

internal val findTomHanksCoActors =
    Statement(
            Options("findTomHanksCoActors"),
            """|MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors)
            |RETURN DISTINCT coActors.name"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 29
        }

internal val findPeopleRelatedToCloudAtlas =
    Statement(
            Options("findPeopleRelatedToCloudAtlas"),
            """|MATCH (people:Person)-[relatedTo]-(:Movie {title: 'Cloud Atlas'})
            |RETURN people.name, type(relatedTo), relatedTo.roles"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 33
        }

internal val findSixDegreesOfKevinBacon =
    Statement(
            Options("findSixDegreesOfKevinBacon"),
            """|MATCH (bacon:Person {name: 'Kevin Bacon'})-[*1..6]-(hollywood)
            |RETURN DISTINCT hollywood"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 37
        }

internal val findPathFromKevinBaconTo =
    Statement(
            Options("findPathFromKevinBaconTo"),
            """|MATCH p = shortestPath((bacon:Person {name: 'Kevin Bacon'})-[*]-(meg:Person {name: ${'$'}name}))
            |RETURN p"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 41
        }

internal val findRecommendedTomHanksCoActors =
    Statement(
            Options("findRecommendedTomHanksCoActors"),
            """|MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors),
            |      (coActors)-[:ACTED_IN]->(m2)<-[:ACTED_IN]-(cocoActors)
            |  WHERE NOT (tom)-[:ACTED_IN]->()<-[:ACTED_IN]-(cocoActors) AND tom <> cocoActors
            |RETURN cocoActors.name AS Recommended, COUNT(*) AS Strength
            |  ORDER BY Strength DESC"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 45
        }

internal val findCoActorsBetweenTomHanksAnd =
    Statement(
            Options("findCoActorsBetweenTomHanksAnd"),
            """|MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors),
            |      (coActors)-[:ACTED_IN]->(m2)<-[:ACTED_IN]-(p:Person {name: ${'$'}name})
            |RETURN tom, m, coActors, m2, p"""
                .trimMargin())
        .apply {
          file = "movie.cypher"
          lineNumber = 52
        }
