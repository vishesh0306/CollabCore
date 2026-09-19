package com.collabflow.notification;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One entry in someone's notifications tab.
 *
 * <p>The people are kept as plain ids, not as {@code @ManyToOne} links: notifications are
 * written in bulk right after something happens, and this way no user rows have to be loaded
 * to write them. The message is stored as text, so it keeps describing what happened even if
 * the task is renamed later.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID recipientId;

    private UUID actorId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;

    /** Where to go when it is clicked, e.g. "/tasks/PAY-12". */
    private String link;

    @CreationTimestamp
    private Instant createdAt;

    private Instant readAt;

    public Notification(UUID recipientId, UUID actorId, NotificationType type, String message, String link) {
        this.recipientId = recipientId;
        this.actorId = actorId;
        this.type = type;
        this.message = message;
        this.link = link;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }
}
