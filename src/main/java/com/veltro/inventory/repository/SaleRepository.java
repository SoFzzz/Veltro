package com.veltro.inventory.repository;

import com.veltro.inventory.model.SaleEntity;
import com.veltro.inventory.model.SaleStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<SaleEntity, Long> {

    @EntityGraph(attributePaths = {"details"})
    Optional<SaleEntity> findByIdAndActiveTrueAndBusinessId(Long id, Long businessId);

    @Query(value = "SELECT nextval('sale_number_seq')", nativeQuery = true)
    Long getNextSaleSequenceValue();

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM SaleEntity s " +
           "WHERE s.status = :status AND s.completedAt BETWEEN :start AND :end " +
           "AND s.businessId = :businessId")
    BigDecimal sumTotalByStatusAndDateRange(
            @Param("status") SaleStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("businessId") Long businessId);

    @Query("SELECT COUNT(s) FROM SaleEntity s " +
           "WHERE s.status = :status AND s.completedAt BETWEEN :start AND :end " +
           "AND s.businessId = :businessId")
    long countByStatusAndDateRange(
            @Param("status") SaleStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("businessId") Long businessId);

    @Query("SELECT COALESCE(SUM(d.quantity), 0) FROM SaleDetailEntity d " +
           "JOIN d.sale s " +
           "WHERE s.status = :status AND s.completedAt BETWEEN :start AND :end " +
           "AND s.businessId = :businessId")
    long sumItemsSoldByStatusAndDateRange(
            @Param("status") SaleStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("businessId") Long businessId);

    @Query("SELECT p.id, p.name, p.sku, SUM(d.quantity), SUM(d.subtotal), p.costPrice " +
           "FROM SaleDetailEntity d " +
           "JOIN d.sale s, ProductEntity p " +
           "WHERE d.productId = p.id " +
           "AND s.status = :status AND s.completedAt BETWEEN :start AND :end " +
           "AND s.businessId = :businessId " +
           "GROUP BY p.id, p.name, p.sku, p.costPrice " +
           "ORDER BY SUM(d.subtotal) DESC")
    List<Object[]> getProductProfitabilityBreakdown(
            @Param("status") SaleStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("businessId") Long businessId);
}
