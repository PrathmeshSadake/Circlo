package me.prathmeshsadake.linkedin.notificationService.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.notificationService.entity.Notification;
import me.prathmeshsadake.linkedin.notificationService.events.ConnectionRequestedEvent;
import me.prathmeshsadake.linkedin.notificationService.events.PostCreatedEvent;
import me.prathmeshsadake.linkedin.notificationService.events.PostLikedEvent;
import me.prathmeshsadake.linkedin.notificationService.repository.NotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListeners {

    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "post-created", containerFactory = "postCreatedFactory")
    public void onPostCreated(PostCreatedEvent event) {
        save(event.getRecipientUserId(), "POST_CREATED",
                "User " + event.getAuthorId() + " published a post", event.getPostId());
    }

    @KafkaListener(topics = "post-liked", containerFactory = "postLikedFactory")
    public void onPostLiked(PostLikedEvent event) {
        save(event.getPostOwnerId(), "POST_LIKED",
                "User " + event.getLikedByUserId() + " liked your post", event.getPostId());
    }

    @KafkaListener(topics = "connection-requested", containerFactory = "connectionRequestedFactory")
    public void onConnectionRequested(ConnectionRequestedEvent event) {
        String name = event.getFromUserName() == null ? "Someone" : event.getFromUserName();
        save(event.getToUserId(), "CONNECTION_REQUESTED",
                name + " sent you a connection request", event.getFromUserId());
    }

    private void save(Long userId, String type, String message, Long referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notificationRepository.save(notification);
        log.info("Stored {} notification for user {}", type, userId);
    }
}
