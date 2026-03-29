package com.test.basic.lol.domain.match.mapping;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "match_external_mapping",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_match_external_mapping_provider_external", columnNames = {"provider", "external_match_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MatchExternalMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "external_match_id", nullable = false, length = 64)
    private String externalMatchId;

    @Column(name = "match_id", nullable = false, length = 64)
    private String matchId;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
