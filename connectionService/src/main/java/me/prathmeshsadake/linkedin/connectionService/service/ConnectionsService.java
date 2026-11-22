package me.prathmeshsadake.linkedin.connectionService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.connectionService.entity.Person;
import me.prathmeshsadake.linkedin.connectionService.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public Person createPerson(Long userId, String name) {
        Person person = personRepository.findByUserId(userId).orElseGet(Person::new);
        person.setUserId(userId);
        person.setName(name);
        return personRepository.save(person);
    }

    public void connect(Long userId, Long otherUserId) {
        if (personRepository.findByUserId(userId).isEmpty()
                || personRepository.findByUserId(otherUserId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Both people must exist before connecting");
        }
        personRepository.connect(userId, otherUserId);
    }
}
