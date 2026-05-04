package com.standupiq.standup_iq.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "standups")
@Data
public class Standup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "standup_text", nullable = false, columnDefinition = "TEXT")
    private String standupText;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "commit_count", nullable = false)
    private int commitCount;

    @Column(name = "pr_count", nullable = false)
    private int prCount;

    @PrePersist
    public void prePersist() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
    }
}
