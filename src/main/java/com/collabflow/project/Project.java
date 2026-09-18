package com.collabflow.project;

import java.time.Instant;
import java.util.UUID;

import com.collabflow.identity.User;
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

@Entity
@Table(name = "projects")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    /** Set once and never changed: task keys like "PAY-12" are built from it. */
    private String code;

    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id")
    private User lead;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Project(Team team, String code, String name, String description, User lead) {
        this.team = team;
        this.code = code;
        this.name = name;
        this.description = description;
        this.lead = lead;
        this.status = ProjectStatus.ACTIVE;
    }

    public void update(String name, String description, User lead) {
        this.name = name;
        this.description = description;
        this.lead = lead;
    }

    public boolean isCompleted() {
        return status == ProjectStatus.COMPLETED;
    }

    public void complete() {
        this.status = ProjectStatus.COMPLETED;
    }

    public void reopen() {
        this.status = ProjectStatus.ACTIVE;
    }
}
