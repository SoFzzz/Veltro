package com.veltro.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * JPA entity representing an application user.
 *
 * Extends {@link AbstractAuditableEntity} for immutable audit fields (ADR-004).
 *
 * AC-06: {@code passwordHash} is excluded from {@link #toString()} to prevent
 * credentials leaking into logs.
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = "passwordHash")
@Entity
@Table(name = "users")
public class UserEntity extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** Multi-tenant: the business this user belongs to. */
    @Column(name = "business_id", nullable = false)
    private Long businessId;
}
