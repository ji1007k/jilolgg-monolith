package com.test.basic.lol.domain.match.manual;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "manual_match_overrides", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id"})
})
public class ManualMatchOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false, unique = true, length = 64)
    private String matchId;

    @Column(name = "override_start_time")
    private LocalDateTime overrideStartTime;

    @Column(name = "override_block_name", length = 100)
    private String overrideBlockName;

    @Column(name = "lock_start_time", nullable = false)
    private boolean lockStartTime;

    @Column(name = "lock_block_name", nullable = false)
    private boolean lockBlockName;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
