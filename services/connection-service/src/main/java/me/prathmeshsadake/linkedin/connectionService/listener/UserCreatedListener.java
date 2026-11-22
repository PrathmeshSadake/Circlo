package me.prathmeshsadake.linkedin.connectionService.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.connectionService.events.UserCreatedEvent;
import me.prathmeshsadake.linkedin.connectionService.service.ConnectionsService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserCreatedListener {

    private final ConnectionsService connectionsService;

    @KafkaListener(topics = "user-created", containerFactory = "userCreatedFactory")
    public void onUserCreated(UserCreatedEvent event) {
        log.info("Creating graph person for user {}", event.getUserId());
        connectionsService.createPerson(event.getUserId(), event.getName());
    }
}
