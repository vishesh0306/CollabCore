package com.collabflow.identity;

import java.util.UUID;

import com.collabflow.identity.dto.ChangeEmailRequest;
import com.collabflow.identity.dto.ChangePasswordRequest;
import com.collabflow.shared.error.BadRequestException;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.shared.error.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The logged-in user's own account: viewing it and changing email or password. */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("This account no longer exists"));
    }

    @Transactional
    public User changeEmail(UUID userId, ChangeEmailRequest request) {
        User user = getUser(userId);
        requireCurrentPassword(user, request.currentPassword());

        String newEmail = User.normalizeEmail(request.newEmail());
        if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("This email is already registered");
        }
        user.changeEmail(newEmail);
        try {
            userRepository.flush(); // run the UPDATE now, so a duplicate email is caught here
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This email is already registered");
        }
        return user;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        requireCurrentPassword(user, request.currentPassword());

        // No save() needed: the user was loaded inside this transaction, so JPA notices the
        // change ("dirty checking") and writes it to the database when the transaction commits.
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
    }
}
