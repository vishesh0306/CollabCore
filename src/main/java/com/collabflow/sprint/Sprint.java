package com.collabflow.sprint;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.collabflow.team.Team;
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
import org.hibernate.annotations.UpdateTimestamp;

/** A time-boxed period (e.g. 15 days) in which a team works towards a target. */
@Entity
@Table(name = "sprints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    private String name;

    private String target;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private SprintStatus status;

    private Instant startedAt;

    private Instant completedAt;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Sprint(Team team, String name, String target, LocalDate startDate, LocalDate endDate) {
        this.team = team;
        this.name = name;
        this.target = target;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = SprintStatus.PLANNED;
    }

    public void update(String name, String target, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.target = target;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void start() {
        status = SprintStatus.ACTIVE;
        startedAt = Instant.now();
    }

    public void complete() {
        status = SprintStatus.COMPLETED;
        completedAt = Instant.now();
    }

    /** Planned and active sprints can still change and receive tasks; completed ones can't. */
    public boolean isOpen() {
        return status != SprintStatus.COMPLETED;
    }
}
