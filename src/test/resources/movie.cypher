// Cypher for the https://github.com/neo4j-graph-examples/movies example graph

//cqldelight:findTomHanks
MATCH (tom:Person {name: 'Tom Hanks'})
RETURN tom;

//cqldelight:findCloudAtlas
MATCH (cloudAtlas:Movie {title: 'Cloud Atlas'})
RETURN cloudAtlas;

//cqldelight:find10People
MATCH (people:Person)
RETURN people.name
  LIMIT 10;

//cqldelight:findNinetiesMovies
MATCH (nineties:Movie)
  WHERE nineties.released >= 1990 AND nineties.released < 2000
RETURN nineties.title;

//cqldelight:findTomHanksMovies
MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(tomHanksMovies)
RETURN tom, tomHanksMovies;

//cqldelight:findWhoDirectedCloudAtlas
MATCH (cloudAtlas:Movie {title: 'Cloud Atlas'})<-[:DIRECTED]-(directors)
RETURN directors.name;

//cqldelight:findTomHanksCoActors
MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors)
RETURN DISTINCT coActors.name;

//cqldelight:findPeopleRelatedToCloudAtlas
MATCH (people:Person)-[relatedTo]-(:Movie {title: 'Cloud Atlas'})
RETURN people.name, type(relatedTo), relatedTo.roles;

//cqldelight:findSixDegreesOfKevinBacon
MATCH (bacon:Person {name: 'Kevin Bacon'})-[*1..6]-(hollywood)
RETURN DISTINCT hollywood;

//cqldelight:findPathFromKevinBaconTo
MATCH p = shortestPath((bacon:Person {name: 'Kevin Bacon'})-[*]-(meg:Person {name: $name}))
RETURN p;

//cqldelight:findRecommendedTomHanksCoActors
MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors),
      (coActors)-[:ACTED_IN]->(m2)<-[:ACTED_IN]-(cocoActors)
  WHERE NOT (tom)-[:ACTED_IN]->()<-[:ACTED_IN]-(cocoActors) AND tom <> cocoActors
RETURN cocoActors.name AS Recommended, COUNT(*) AS Strength
  ORDER BY Strength DESC;

//cqldelight:findCoActorsBetweenTomHanksAnd
MATCH (tom:Person {name: 'Tom Hanks'})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors),
      (coActors)-[:ACTED_IN]->(m2)<-[:ACTED_IN]-(p:Person {name: $name})
RETURN tom, m, coActors, m2, p;
