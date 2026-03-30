package com.veltro.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Multi-tenant business entity.
 *
 * Each business represents an isolated namespace — all data (products, sales,
 * inventory, etc.) is scoped by {@code business_id}. An ADMIN user creates a
 * business during registration and then invites workers (CASHIER / WAREHOUSE).
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = "owner")
@Entity
@Table(name = "business")
public class BusinessEntity extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * The ADMIN user who owns this business.
     * Nullable initially (set after user is created due to circular reference).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;
}
