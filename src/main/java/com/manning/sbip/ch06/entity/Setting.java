package com.manning.sbip.ch06.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing organization-wide settings including JWT keys
 */
@Entity
@Table(name = "setting")
@Data
@NoArgsConstructor
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_code", unique = true, nullable = false, length = 100)
    private String organizationCode;

    @Column(name = "organization_name", length = 255)
    private String organizationName;

    @Column(name = "jwt_private_key", columnDefinition = "TEXT")
    private String jwtPrivateKey;

    @Column(name = "jwt_public_key", columnDefinition = "TEXT")
    private String jwtPublicKey;

    @Column(name = "jwt_algorithm", length = 20)
    private String jwtAlgorithm = "RS256";

    @Column(name = "jwt_issuer", length = 255)
    private String jwtIssuer;

    @Column(name = "jwt_expiration_minutes")
    private Integer jwtExpirationMinutes = 60;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
