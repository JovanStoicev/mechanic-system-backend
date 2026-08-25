package com.garage.system.notification;

import com.garage.system.user.User;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    public NotificationService(NotificationRepository repository) { this.repository = repository; }
    public Notification create(User user, String type, String title, String message, String link) {
        Notification notification = new Notification(); notification.setUser(user); notification.setType(type);
        notification.setTitle(title); notification.setMessage(message); notification.setLink(link);
        return repository.save(notification);
    }
}
