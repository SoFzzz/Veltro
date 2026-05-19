package com.veltro.inventory.service;

import com.veltro.inventory.dto.dashboard.DashboardResponse;
import com.veltro.inventory.model.AlertType;
import com.veltro.inventory.security.TenantProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Dashboard service implementing Facade Pattern (B3-02).
 *
 * <p>Aggregates KPIs from multiple data sources into a single response:
 * <ul>
 *   <li>Today's sales and average ticket from POS module</li>
 *   <li>Out-of-stock products from Inventory module</li>
 *   <li>Low stock alerts from Alert module</li>
 *   <li>Estimated monthly profit calculation</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardQueryRepository dashboardQueryRepository;
    private final TenantProvider tenantProvider;
    @Value("${veltro.timezone:UTC}")
    private ZoneId timezone;
    @Value("${veltro.dashboard.profit-margin:0.20}")
    private BigDecimal profitMargin;
    @Value("${veltro.dashboard.recent-sales-limit:10}")
    private int recentSalesLimit;

    /**
     * Retrieves dashboard KPIs (Facade Pattern).
     *
     * @return aggregated dashboard response
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        log.info("Generating dashboard KPIs");

        Long businessId = tenantProvider.getBusinessId();

        LocalDate today = LocalDate.now(timezone);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // Today's sales metrics
        BigDecimal todaySales = dashboardQueryRepository.sumTodaySales(startOfDay, endOfDay, businessId);
        if (todaySales == null) {
            todaySales = BigDecimal.ZERO;
        }

        long todaySalesCount = dashboardQueryRepository.countTodaySales(startOfDay, endOfDay, businessId);

        BigDecimal averageTicket = todaySalesCount > 0
                ? todaySales.divide(BigDecimal.valueOf(todaySalesCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Out of stock products
        List<DashboardResponse.OutOfStockProduct> outOfStockList = dashboardQueryRepository.findOutOfStockProducts(businessId);
        long outOfStockCount = outOfStockList.size();

        // Low stock alerts count
        long lowStockAlertCount = dashboardQueryRepository.countActiveAlertsByType(AlertType.LOW_STOCK, businessId);

        // Estimated monthly profit (based on current month sales)
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDateTime startOfMonth = firstOfMonth.atStartOfDay();
        BigDecimal monthSales = dashboardQueryRepository.sumSalesBetween(startOfMonth, endOfDay, businessId);
        if (monthSales == null) {
            monthSales = BigDecimal.ZERO;
        }
        
        BigDecimal estimatedMonthlyProfit = monthSales.multiply(profitMargin)
                .setScale(2, RoundingMode.HALF_UP);

        List<DashboardResponse.RecentSale> recentSales = dashboardQueryRepository.findRecentSales(recentSalesLimit, businessId);

        DashboardResponse response = new DashboardResponse(
                todaySales.setScale(2, RoundingMode.HALF_UP),
                todaySalesCount,
                averageTicket,
                outOfStockCount,
                outOfStockList,
                estimatedMonthlyProfit,
                lowStockAlertCount,
                recentSales
        );

        log.info("Dashboard generated: todaySales={}, salesCount={}, outOfStock={}, lowStockAlerts={}",
                todaySales, todaySalesCount, outOfStockCount, lowStockAlertCount);

        return response;
    }
}


