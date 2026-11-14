package me.prathmeshsadake.linkedin.connectionService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.connectionService.entity.Person;
import me.prathmeshsadake.linkedin.connectionService.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsService {

    private final PersonRepository personRepository;

    public List<Person> getFirstDegreeConnectionsOfUser(Long userId) {
        log.info("Getting first degree connections of user {}", userId);
        return personRepository.getFirstDegreeConnections(userId);
    }
}
