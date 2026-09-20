package com.collabflow.notification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.collabflow.notification.dto.NotificationResponse;
import com.collabflow.shared.PageResponse;
import com.collabflow.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes notifications, and serves each user their own notifications tab. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Saves one notification per recipient, never for the person who caused it.
     *
     * <p>Runs in a transaction of its own: the listeners that call this run *after* the
     * original change was committed, so there is no transaction left to take part in.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAll(Collection<UUID> recipients, UUID actorId, NotificationType type,
                          String message, String link) {
        List<Notification> newOnes = recipients.stream()
                .distinct()
                .filter(recipient -> !recipient.equals(actorId))
                .map(recipient -> new Notification(recipient, actorId, type, message, link))
                .toList();
        if (!newOnes.isEmpty()) {
            notificationRepository.saveAll(newOnes);
        }
    }

    /** The caller's notifications, newest first. The order is fixed, so only page and size are used. */
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(UUID userId, boolean unreadOnly, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadAtIsNull(userId, newestFirst)
                : notificationRepository.findByRecipientId(userId, newestFirst);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification not found"))
                .markRead();
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllRead(userId, Instant.now());
    }
}
