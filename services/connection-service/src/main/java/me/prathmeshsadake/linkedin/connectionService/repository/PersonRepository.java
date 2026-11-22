package me.prathmeshsadake.linkedin.connectionService.repository;

import me.prathmeshsadake.linkedin.connectionService.entity.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends Neo4jRepository<Person, Long> {
    Optional<Person> findByUserId(Long userId);

    @Query(
            "match (personA: Person) -[:CONNECTED_TO]- (personB: Person) "+
                    "where personA.userId = $userId " +
                    "return personB"
    )
    List<Person> getFirstDegreeConnections(Long userId);

    @Query("""
            MATCH (a:Person {userId: $fromId})-[:CONNECTED_TO]-(b:Person {userId: $toId})
            RETURN count(a)
            """)
    long countConnections(@Param("fromId") Long fromId, @Param("toId") Long toId);

    @Query("""
            MATCH (a:Person {userId: $fromId})-[r:CONNECTION_REQUEST]->(b:Person {userId: $toId})
            RETURN count(r)
            """)
    long countRequests(@Param("fromId") Long fromId, @Param("toId") Long toId);

    @Query("""
            MATCH (a:Person {userId: $fromId}), (b:Person {userId: $toId})
            MERGE (a)-[:CONNECTION_REQUEST]->(b)
            """)
    void createRequest(@Param("fromId") Long fromId, @Param("toId") Long toId);

    @Query("""
            MATCH (a:Person {userId: $fromId})-[r:CONNECTION_REQUEST]->(b:Person {userId: $toId})
            DELETE r
            """)
    void deleteRequest(@Param("fromId") Long fromId, @Param("toId") Long toId);

    @Query("""
            MATCH (a:Person {userId: $fromId})-[r:CONNECTION_REQUEST]->(b:Person {userId: $toId})
            DELETE r
            WITH a, b
            MERGE (a)-[:CONNECTED_TO]-(b)
            """)
    void acceptRequest(@Param("fromId") Long fromId, @Param("toId") Long toId);
}
