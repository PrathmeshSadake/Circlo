package me.prathmeshsadake.linkedin.connectionService.controller;

import lombok.RequiredArgsConstructor;
import me.prathmeshsadake.linkedin.connectionService.entity.Person;
import me.prathmeshsadake.linkedin.connectionService.service.ConnectionsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/core")
public class ConnectionsController {
    private final ConnectionsService connectionsService;

    @PostMapping("/requests/{targetUserId}")
    public ResponseEntity<Void> sendRequest(@PathVariable Long targetUserId) {
        connectionsService.sendRequest(targetUserId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/requests/{targetUserId}")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long targetUserId) {
        connectionsService.cancelRequest(targetUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{requesterUserId}/accept")
    public ResponseEntity<Void> acceptRequest(@PathVariable Long requesterUserId) {
        connectionsService.acceptRequest(requesterUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{requesterUserId}/decline")
    public ResponseEntity<Void> declineRequest(@PathVariable Long requesterUserId) {
        connectionsService.declineRequest(requesterUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/first-degree")
    public ResponseEntity<List<Person>> getFirstDegreeConnections(@PathVariable Long userId) {
        return ResponseEntity.ok(connectionsService.getFirstDegreeConnectionsOfUser(userId));
    }
}
