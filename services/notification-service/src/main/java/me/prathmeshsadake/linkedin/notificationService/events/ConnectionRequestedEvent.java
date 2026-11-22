package me.prathmeshsadake.linkedin.notificationService.events;

import lombok.Data;

@Data
public class ConnectionRequestedEvent {
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
}
