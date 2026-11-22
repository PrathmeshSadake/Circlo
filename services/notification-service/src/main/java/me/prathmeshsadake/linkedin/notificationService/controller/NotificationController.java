package me.prathmeshsadake.linkedin.notificationService.controller;

import lombok.RequiredArgsConstructor;
import me.prathmeshsadake.linkedin.notificationService.auth.AuthContextHolder;
import me.prathmeshsadake.linkedin.notificationService.entity.Notification;
import me.prathmeshsadake.linkedin.notificationService.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public List<Notification> myNotifications() {
        Long userId = AuthContextHolder.getCurrentUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing authenticated user");
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
