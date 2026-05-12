package com.veltro.inventory.service;

import com.veltro.inventory.dto.dashboard.DashboardResponse;
import com.veltro.inventory.model.AlertType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Query interface for dashboard-specific data (B3-02).
 *
 * <p>Provides optimized queries for dashboard KPIs without coupling
 * to specific repository implementations.
 */
public interface DashboardQueryRepository {
    BigDecimal sumTodaySales(LocalDateTime startOfDay, LocalDateTime endOfDay, Long businessId);

    long countTodaySales(LocalDateTime startOfDay, LocalDateTime endOfDay, Long businessId);

    BigDecimal sumSalesBetween(LocalDateTime startDate, LocalDateTime endDate, Long businessId);

    List<DashboardResponse.OutOfStockProduct> findOutOfStockProducts(Long businessId);

    long countActiveAlertsByType(AlertType type, Long businessId);

    List<DashboardResponse.RecentSale> findRecentSales(int limit, Long businessId);
}

