package com.parkyc.poelens.mechanics.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "mechanics", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "game_version"}))
public class MechanicEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "game_version", nullable = false)
    private String gameVersion;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String explanation;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "collected_at", nullable = false)
    private LocalDate collectedAt;

    @Column(nullable = false)
    private boolean reviewed;

    protected MechanicEntity() {
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public LocalDate getCollectedAt() {
        return collectedAt;
    }

    public boolean isReviewed() {
        return reviewed;
    }
}
