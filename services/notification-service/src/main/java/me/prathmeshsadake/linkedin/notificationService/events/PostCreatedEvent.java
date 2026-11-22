package me.prathmeshsadake.linkedin.notificationService.events;

import lombok.Data;

@Data
public class PostCreatedEvent {
    private Long postId;
    private Long authorId;
    private Long recipientUserId;
    private String content;
    private String mediaUrl;
}
