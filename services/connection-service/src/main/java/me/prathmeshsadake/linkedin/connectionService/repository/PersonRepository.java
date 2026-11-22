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
            MATCH (personA:Person {userId: $userId}), (personB:Person {userId: $otherUserId})
            MERGE (personA)-[:CONNECTED_TO]-(personB)
            """)
    void connect(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
