package me.prathmeshsadake.linkedin.connectionService.controller;

import lombok.RequiredArgsConstructor;
import me.prathmeshsadake.linkedin.connectionService.entity.Person;
import me.prathmeshsadake.linkedin.connectionService.service.ConnectionsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core")
public class ConnectionsController {
    private final ConnectionsService connectionsService;

    @PostMapping("/persons")
    public ResponseEntity<Person> createPerson(@RequestBody Person person) {
        Person saved = connectionsService.createPerson(person.getUserId(), person.getName());
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PostMapping("/{userId}/connect/{otherUserId}")
    public ResponseEntity<Void> connect(@PathVariable Long userId, @PathVariable Long otherUserId) {
        connectionsService.connect(userId, otherUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/first-degree")
    public ResponseEntity<List<Person>> getFirstDegreeConnections(@PathVariable Long userId) {
        List<Person> personList = connectionsService.getFirstDegreeConnectionsOfUser(userId);
        return ResponseEntity.ok(personList);
    }
}
