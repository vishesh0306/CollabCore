package com.collabflow.identity;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required by JPA, not meant for our code
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;

    private String email;

    private String passwordHash;

    @Column(name = "is_admin")
    private boolean admin;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public User(String name, String email, String passwordHash, boolean admin) {
        this.name = name;
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.admin = admin;
    }

    public void changeEmail(String email) {
        this.email = normalizeEmail(email);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** "Priya@Example.com " and "priya@example.com" must be the same account. */
    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
