package me.prathmeshsadake.linkedin.postService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.postService.auth.AuthContextHolder;
import me.prathmeshsadake.linkedin.postService.client.ConnectionsServiceClient;
import me.prathmeshsadake.linkedin.postService.config.KafkaConfig;
import me.prathmeshsadake.linkedin.postService.dto.PersonDto;
import me.prathmeshsadake.linkedin.postService.dto.PostCreateRequestDto;
import me.prathmeshsadake.linkedin.postService.dto.PostDto;
import me.prathmeshsadake.linkedin.postService.entity.Post;
import me.prathmeshsadake.linkedin.postService.events.PostCreatedEvent;
import me.prathmeshsadake.linkedin.postService.exception.BadRequestException;
import me.prathmeshsadake.linkedin.postService.exception.ResourceNotFoundException;
import me.prathmeshsadake.linkedin.postService.repository.PostRepository;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;
    private final ConnectionsServiceClient connectionsServiceClient;
    private final KafkaTemplate<String, PostCreatedEvent> postCreatedKafkaTemplate;

    public PostDto createPost(PostCreateRequestDto postCreateRequestDto) {
        Long userId = currentUserId();
        log.info("Creating post {} for user {}", postCreateRequestDto, userId);
        Post post = modelMapper.map(postCreateRequestDto, Post.class);
        post.setUserId(userId);
        post = postRepository.save(post);
        notifyFirstDegreeConnections(post, userId);
        return modelMapper.map(post, PostDto.class);
    }

    private void notifyFirstDegreeConnections(Post post, Long authorId) {
        for (PersonDto connection : connectionsServiceClient.getFirstDegreeConnections(authorId)) {
            if (connection.getUserId() == null || connection.getUserId().equals(authorId)) {
                continue;
            }
            PostCreatedEvent event = PostCreatedEvent.builder()
                    .postId(post.getId())
                    .authorId(authorId)
                    .recipientUserId(connection.getUserId())
                    .content(post.getContent())
                    .mediaUrl(post.getMediaUrl())
                    .build();
            postCreatedKafkaTemplate.send(KafkaConfig.POST_CREATED_TOPIC, connection.getUserId().toString(), event);
            log.info("Published post-created for recipient {}", connection.getUserId());
        }
    }

    public List<PersonDto> getMyFirstDegreeConnections() {
        Long userId = currentUserId();
        log.info("Fetching first-degree connections for user {} via Feign", userId);
        return connectionsServiceClient.getFirstDegreeConnections(userId);
    }

    private Long currentUserId() {
        Long userId = AuthContextHolder.getCurrentUserId();
        if (userId == null) {
            throw new BadRequestException("Missing authenticated user");
        }
        return userId;
    }

    public PostDto getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with ID " + postId));
        return modelMapper.map(post, PostDto.class);
    }

    public List<PostDto> getAllPostsOfUser(Long userId) {
        log.info("Getting all posts of user {}", userId);
        List<Post> postList = postRepository.findByUserId(userId);

        return postList.stream()
                .map(element -> modelMapper.map(element, PostDto.class))
                .collect(Collectors.toList());
    }
}
