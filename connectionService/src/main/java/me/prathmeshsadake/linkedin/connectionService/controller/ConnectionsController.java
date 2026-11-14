package me.prathmeshsadake.linkedin.connectionService.controller;

import lombok.RequiredArgsConstructor;
import me.prathmeshsadake.linkedin.connectionService.entity.Person;
import me.prathmeshsadake.linkedin.connectionService.repository.PersonRepository;
import me.prathmeshsadake.linkedin.connectionService.service.ConnectionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core")
public class ConnectionsController {
    private final PersonRepository personRepository;
    private final ConnectionsService connectionsService;

    @GetMapping("/{userId}/first-degree")
    public ResponseEntity<List<Person>> getFirstDegreeConnections(@PathVariable Long userId) {
        List<Person> personList = connectionsService.getFirstDegreeConnectionsOfUser(userId);
        return ResponseEntity.ok(personList);
    }
}
