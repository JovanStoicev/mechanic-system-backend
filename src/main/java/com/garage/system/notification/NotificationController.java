package com.garage.system.notification;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.List;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repository;
    public NotificationController(NotificationRepository repository) { this.repository = repository; }

    @GetMapping public List<NotificationResponse> list(Authentication auth) {
        return repository.findAllByUser_IdOrderByCreatedAtDesc((Long) auth.getPrincipal()).stream().map(NotificationResponse::from).toList();
    }
    @GetMapping("/unread-count") public UnreadCount unreadCount(Authentication auth) {
        return new UnreadCount(repository.countByUser_IdAndReadAtIsNull((Long) auth.getPrincipal()));
    }
    @PatchMapping("/{id}/read") @Transactional public NotificationResponse markRead(Authentication auth, @PathVariable Long id) {
        var notification = repository.findById(id).filter(item -> item.getUser().getId().equals((Long) auth.getPrincipal()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));
        if (notification.getReadAt() == null) notification.setReadAt(OffsetDateTime.now());
        return NotificationResponse.from(notification);
    }
    @PatchMapping("/read-all") @Transactional public void markAllRead(Authentication auth) {
        for (var item : repository.findAllByUser_IdOrderByCreatedAtDesc((Long) auth.getPrincipal()))
            if (item.getReadAt() == null) item.setReadAt(OffsetDateTime.now());
    }

    public record UnreadCount(long count) {}
    public record NotificationResponse(Long id, String type, String title, String message, String link,
                                       OffsetDateTime createdAt, OffsetDateTime readAt) {
        static NotificationResponse from(Notification n) { return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(), n.getCreatedAt(), n.getReadAt()); }
    }
}
