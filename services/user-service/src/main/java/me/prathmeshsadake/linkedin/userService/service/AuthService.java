package me.prathmeshsadake.linkedin.userService.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.prathmeshsadake.linkedin.userService.auth.AuthContextHolder;
import me.prathmeshsadake.linkedin.userService.config.KafkaConfig;
import me.prathmeshsadake.linkedin.userService.dto.LoginRequestDto;
import me.prathmeshsadake.linkedin.userService.dto.SignupRequestDto;
import me.prathmeshsadake.linkedin.userService.dto.UserDto;
import me.prathmeshsadake.linkedin.userService.entity.User;
import me.prathmeshsadake.linkedin.userService.events.UserCreatedEvent;
import me.prathmeshsadake.linkedin.userService.exception.BadRequestException;
import me.prathmeshsadake.linkedin.userService.exception.ResourceNotFoundException;
import me.prathmeshsadake.linkedin.userService.repository.UserRepository;
import me.prathmeshsadake.linkedin.userService.utils.BCrypt;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final KafkaTemplate<String, UserCreatedEvent> userCreatedKafkaTemplate;

    @Value("${internal.token}")
    private String internalToken;


    public UserDto signup(SignupRequestDto signupRequestDto){
        boolean exists = userRepository.existsByEmail(signupRequestDto.getEmail());

        if(exists){
            throw new BadRequestException("User already exists");
        }

        User user = modelMapper.map(signupRequestDto, User.class);
        user.setPassword(BCrypt.hash(signupRequestDto.getPassword()));
        user = userRepository.save(user);
        userCreatedKafkaTemplate.send(
                KafkaConfig.USER_CREATED_TOPIC,
                user.getId().toString(),
                UserCreatedEvent.builder()
                        .userId(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build());
        log.info("Published user-created for {}", user.getId());
        return modelMapper.map(user, UserDto.class);

    }

    public UserDto updateMyProfileImage(String url) {
        Long userId = AuthContextHolder.getCurrentUserId();
        if (userId == null) {
            throw new BadRequestException("Missing authenticated user");
        }
        return updateProfileImage(userId, url);
    }

    public UserDto updateProfileImageFromUploader(Long userId, String url, String token) {
        if (token == null || !token.equals(internalToken)) {
            throw new BadRequestException("Invalid internal token");
        }
        return updateProfileImage(userId, url);
    }

    private UserDto updateProfileImage(Long userId, String url) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setProfileImageUrl(url);
        return modelMapper.map(userRepository.save(user), UserDto.class);
    }

    public String login(LoginRequestDto loginRequestDto){
        User user = userRepository
                .findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));

        boolean isPasswordMatched = BCrypt.match(loginRequestDto.getPassword(), user.getPassword());
        if (!isPasswordMatched) {
            throw new BadRequestException("Invalid credentials");
        }
        return jwtService.generateToken(user);
    }
}
