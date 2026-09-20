package com.collabflow.identity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.collabflow.shared.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * How other features (teams, tasks...) look up users. They use this service instead of
 * UserRepository, so the identity module stays in charge of its own table.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(User.normalizeEmail(email));
    }

    @Transactional(readOnly = true)
    public User getById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    /**
     * Names for a set of users, in one query. Used where a list would otherwise look up each
     * name on its own, e.g. a page of audit entries.
     */
    @Transactional(readOnly = true)
    public Map<UUID, String> namesOf(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(UUID userId) {
        return userRepository.findById(userId).map(User::isAdmin).orElse(false);
    }
}
