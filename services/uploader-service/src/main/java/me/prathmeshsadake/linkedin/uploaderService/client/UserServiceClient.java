package me.prathmeshsadake.linkedin.uploaderService.client;

import me.prathmeshsadake.linkedin.uploaderService.dto.ProfileImageRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", path = "/users")
public interface UserServiceClient {

    @PutMapping("/internal/{userId}/profile-image")
    void updateProfileImage(@PathVariable("userId") Long userId,
                             @RequestBody ProfileImageRequest request,
                             @RequestHeader("X-Internal-Token") String token);
}
