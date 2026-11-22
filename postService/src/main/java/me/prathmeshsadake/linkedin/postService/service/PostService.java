package me.prathmeshsadake.linkedin.postService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.postService.auth.AuthContextHolder;
import me.prathmeshsadake.linkedin.postService.client.ConnectionsServiceClient;
import me.prathmeshsadake.linkedin.postService.dto.PersonDto;
import me.prathmeshsadake.linkedin.postService.dto.PostCreateRequestDto;
import me.prathmeshsadake.linkedin.postService.dto.PostDto;
import me.prathmeshsadake.linkedin.postService.entity.Post;
import me.prathmeshsadake.linkedin.postService.exception.BadRequestException;
import me.prathmeshsadake.linkedin.postService.exception.ResourceNotFoundException;
import me.prathmeshsadake.linkedin.postService.repository.PostRepository;
import org.modelmapper.ModelMapper;
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

    public PostDto createPost(PostCreateRequestDto postCreateRequestDto) {
        Long userId = currentUserId();
        log.info("Creating post {} for user {}", postCreateRequestDto, userId);
        Post post = modelMapper.map(postCreateRequestDto, Post.class);
        post.setUserId(userId);
        post = postRepository.save(post);
        return modelMapper.map(post, PostDto.class);
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
