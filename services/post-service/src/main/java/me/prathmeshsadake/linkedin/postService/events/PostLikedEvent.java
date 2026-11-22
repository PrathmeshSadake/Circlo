package me.prathmeshsadake.linkedin.postService.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostLikedEvent {
    private Long postId;
    private Long likedByUserId;
    private Long postOwnerId;
}
