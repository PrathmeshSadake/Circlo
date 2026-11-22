package me.prathmeshsadake.linkedin.notificationService.events;

import lombok.Data;

@Data
public class PostLikedEvent {
    private Long postId;
    private Long likedByUserId;
    private Long postOwnerId;
}
