package com.veltro.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-product alert configuration storing stock thresholds (B2-03).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alert_configuration")
public class AlertConfigurationEntity extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private ProductEntity product;

    @Column(name = "critical_stock", nullable = false)
    private Integer criticalStock;

    @Column(name = "min_stock", nullable = false)
    private Integer minStock;

    @Column(name = "overstock_threshold", nullable = false)
    private Integer overstockThreshold;

    @Column(name = "business_id", nullable = false)
    private Long businessId;
}
