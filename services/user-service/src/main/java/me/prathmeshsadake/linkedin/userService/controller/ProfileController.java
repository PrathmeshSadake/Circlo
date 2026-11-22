package me.prathmeshsadake.linkedin.userService.controller;

import lombok.RequiredArgsConstructor;
import me.prathmeshsadake.linkedin.userService.dto.ProfileImageRequest;
import me.prathmeshsadake.linkedin.userService.dto.UserDto;
import me.prathmeshsadake.linkedin.userService.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;

    @GetMapping("/profile/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(authService.currentUser());
    }

    @GetMapping("/people")
    public ResponseEntity<java.util.List<UserDto>> people() {
        return ResponseEntity.ok(authService.listPeople());
    }

    @PutMapping("/profile/image")
    public ResponseEntity<UserDto> updateMyProfileImage(@RequestBody ProfileImageRequest request) {
        return ResponseEntity.ok(authService.updateMyProfileImage(request.getUrl()));
    }

    @PutMapping("/internal/{userId}/profile-image")
    public ResponseEntity<UserDto> updateProfileImageFromUploader(
            @PathVariable Long userId,
            @RequestBody ProfileImageRequest request,
            @RequestHeader("X-Internal-Token") String token) {
        return ResponseEntity.ok(authService.updateProfileImageFromUploader(userId, request.getUrl(), token));
    }
}
