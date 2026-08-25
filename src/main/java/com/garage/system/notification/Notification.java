package com.garage.system.notification;

import com.garage.system.user.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 50) private String type;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, columnDefinition = "text") private String message;
    @Column(length = 500) private String link;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "read_at") private OffsetDateTime readAt;

    public Long getId() { return id; } public User getUser() { return user; } public String getType() { return type; }
    public String getTitle() { return title; } public String getMessage() { return message; } public String getLink() { return link; }
    public OffsetDateTime getCreatedAt() { return createdAt; } public OffsetDateTime getReadAt() { return readAt; }
    public void setUser(User user) { this.user = user; } public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; } public void setMessage(String message) { this.message = message; }
    public void setLink(String link) { this.link = link; } public void setReadAt(OffsetDateTime readAt) { this.readAt = readAt; }
}
