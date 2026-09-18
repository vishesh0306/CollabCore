package com.collabflow.team;

import java.time.Instant;
import java.util.UUID;

import com.collabflow.identity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A user's membership in a team. This is a many-to-many link between users and teams
 * that carries its own data (the role and the joined date), so it is an entity itself.
 */
@Entity
@Table(name = "team_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // LAZY: the team and user rows are only loaded from the database if the code uses them.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // Stored as text ("MANAGER"). Storing the position (0, 1) would silently break if the
    // enum's values were ever reordered.
    @Enumerated(EnumType.STRING)
    private TeamRole role;

    @CreationTimestamp
    private Instant joinedAt;

    public TeamMember(Team team, User user, TeamRole role) {
        this.team = team;
        this.user = user;
        this.role = role;
    }

    public void changeRole(TeamRole role) {
        this.role = role;
    }
}
