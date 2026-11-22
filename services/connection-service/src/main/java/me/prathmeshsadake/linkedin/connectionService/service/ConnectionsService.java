package me.prathmeshsadake.linkedin.connectionService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.connectionService.auth.AuthContextHolder;
import me.prathmeshsadake.linkedin.connectionService.config.KafkaConfig;
import me.prathmeshsadake.linkedin.connectionService.entity.Person;
import me.prathmeshsadake.linkedin.connectionService.events.ConnectionRequestedEvent;
import me.prathmeshsadake.linkedin.connectionService.repository.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsService {

    private final PersonRepository personRepository;
    private final KafkaTemplate<String, ConnectionRequestedEvent> connectionRequestedKafkaTemplate;

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

    public void sendRequest(Long targetUserId) {
        Long currentUserId = currentUserId();
        if (currentUserId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a connection request to yourself");
        }
        Person sender = requirePerson(currentUserId);
        requirePerson(targetUserId);
        if (personRepository.countConnections(currentUserId, targetUserId) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "People are already connected. You cannot send the connection request to this person again.");
        }
        if (personRepository.countRequests(currentUserId, targetUserId) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A connection request is already pending. Cancel it if you do not want to keep it.");
        }
        personRepository.createRequest(currentUserId, targetUserId);
        connectionRequestedKafkaTemplate.send(
                KafkaConfig.CONNECTION_REQUESTED_TOPIC,
                targetUserId.toString(),
                ConnectionRequestedEvent.builder()
                        .fromUserId(currentUserId)
                        .fromUserName(sender.getName())
                        .toUserId(targetUserId)
                        .build());
        log.info("Connection request {} -> {}", currentUserId, targetUserId);
    }

    public void cancelRequest(Long targetUserId) {
        Long currentUserId = currentUserId();
        if (personRepository.countRequests(currentUserId, targetUserId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending connection request to cancel");
        }
        personRepository.deleteRequest(currentUserId, targetUserId);
    }

    public void acceptRequest(Long requesterUserId) {
        Long currentUserId = currentUserId();
        if (personRepository.countRequests(requesterUserId, currentUserId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending connection request from this user");
        }
        personRepository.acceptRequest(requesterUserId, currentUserId);
        log.info("Accepted connection {} -> {}", requesterUserId, currentUserId);
    }

    public void declineRequest(Long requesterUserId) {
        Long currentUserId = currentUserId();
        if (personRepository.countRequests(requesterUserId, currentUserId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No pending connection request from this user");
        }
        personRepository.deleteRequest(requesterUserId, currentUserId);
    }

    private Person requirePerson(Long userId) {
        return personRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found for user " + userId));
    }

    private Long currentUserId() {
        Long userId = AuthContextHolder.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing authenticated user");
        }
        return userId;
    }
}
