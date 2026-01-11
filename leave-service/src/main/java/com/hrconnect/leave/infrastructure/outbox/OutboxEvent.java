package com.hrconnect.leave.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entité représentant un événement dans la table Outbox
 * Pattern Transactional Outbox pour garantir la cohérence
 */
@Entity
@Table(name = "leave_outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false, length = 255)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Boolean published;

    private Instant publishedAt;

    @Column(nullable = false)
    private Integer retryCount;

    private String errorMessage;
}

