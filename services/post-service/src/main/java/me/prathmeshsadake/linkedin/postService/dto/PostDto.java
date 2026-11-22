package me.prathmeshsadake.linkedin.postService.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostDto {
    private Long id;
    private String content;
    private String mediaUrl;
    private Long userId;
    private LocalDateTime createdAt;
}
