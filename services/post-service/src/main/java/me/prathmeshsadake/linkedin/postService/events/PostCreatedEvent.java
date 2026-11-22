package me.prathmeshsadake.linkedin.postService.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent {
    private Long postId;
    private Long authorId;
    private Long recipientUserId;
    private String content;
    private String mediaUrl;
}
