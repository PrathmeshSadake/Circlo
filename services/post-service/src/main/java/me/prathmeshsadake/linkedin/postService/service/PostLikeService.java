package me.prathmeshsadake.linkedin.postService.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.postService.auth.AuthContextHolder;
import me.prathmeshsadake.linkedin.postService.config.KafkaConfig;
import me.prathmeshsadake.linkedin.postService.entity.Post;
import me.prathmeshsadake.linkedin.postService.entity.PostLike;
import me.prathmeshsadake.linkedin.postService.events.PostLikedEvent;
import me.prathmeshsadake.linkedin.postService.exception.BadRequestException;
import me.prathmeshsadake.linkedin.postService.exception.ResourceNotFoundException;
import me.prathmeshsadake.linkedin.postService.repository.PostLikeRepository;
import me.prathmeshsadake.linkedin.postService.repository.PostRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final KafkaTemplate<String, PostLikedEvent> postLikedKafkaTemplate;

    @Transactional
    public void likePost(Long postId) {
        Long userId = currentUserId();
        log.info("Liking post like id {}", postId);
        Post post = postRepository.findById(postId).orElseThrow(()-> new ResourceNotFoundException("Post not found"));

        boolean hasAlreadyLiked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        if(hasAlreadyLiked){
            throw new BadRequestException("Post is already liked");
        }
        PostLike postLike = new PostLike();
        postLike.setUserId(userId);
        postLike.setPostId(postId);
        postLikeRepository.save(postLike);
        PostLikedEvent event = PostLikedEvent.builder()
                .postId(post.getId())
                .likedByUserId(userId)
                .postOwnerId(post.getUserId())
                .build();
        postLikedKafkaTemplate.send(KafkaConfig.POST_LIKED_TOPIC, post.getUserId().toString(), event);
        log.info("Published post-liked for owner {}", post.getUserId());
    }

    @Transactional
    public void unlikePost(Long postId){
        Long userId = currentUserId();
        log.info("Liking post like id {}", postId);
        Post post = postRepository.findById(postId).orElseThrow(()-> new ResourceNotFoundException("Post not found"));

        boolean hasAlreadyLiked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        if(!hasAlreadyLiked){
            throw new BadRequestException("You cannot unlike the post you have not liked");
        }

        postLikeRepository.deleteByUserIdAndPostId(userId, postId);
    }

    private Long currentUserId() {
        Long userId = AuthContextHolder.getCurrentUserId();
        if (userId == null) {
            throw new BadRequestException("Missing authenticated user");
        }
        return userId;
    }

}
