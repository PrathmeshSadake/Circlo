package me.prathmeshsadake.linkedin.connectionService.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRequestedEvent {
    private Long fromUserId;
    private String fromUserName;
    private Long toUserId;
}
