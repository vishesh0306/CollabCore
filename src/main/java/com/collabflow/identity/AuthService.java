package com.collabflow.identity;

import java.util.Optional;

import com.collabflow.identity.dto.LoginRequest;
import com.collabflow.identity.dto.RegisterRequest;
import com.collabflow.identity.dto.TokenResponse;
import com.collabflow.shared.error.ConflictException;
import com.collabflow.shared.error.UnauthorizedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /** Checked when the email is unknown, so both failure cases take the same time. */
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password");
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("This email is already registered");
        }
        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), false);
        try {
            // saveAndFlush runs the INSERT now, so a database error surfaces here and not at commit.
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two sign-ups with the same email at the same moment: both passed the check above,
            // but the database's unique rule rejected the second one.
            throw new ConflictException("This email is already registered");
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(User.normalizeEmail(request.email()));

        // Always run the (slow) password check. If we skipped it for unknown emails, those
        // requests would be faster, and an attacker could time them to learn who is registered.
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (user.isEmpty() || !passwordMatches) {
            // Same message for both cases, for the same reason.
            throw new UnauthorizedException("Invalid email or password");
        }
        return tokenService.createToken(user.get());
    }
}
