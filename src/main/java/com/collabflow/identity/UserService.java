package com.collabflow.identity;

import java.util.Optional;
import java.util.UUID;

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
    public boolean isAdmin(UUID userId) {
        return userRepository.findById(userId).map(User::isAdmin).orElse(false);
    }
}
