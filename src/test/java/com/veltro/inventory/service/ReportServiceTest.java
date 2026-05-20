package com.veltro.inventory.service;

import com.veltro.inventory.dto.report.ProfitabilityReport;
import com.veltro.inventory.dto.report.ReportType;
import com.veltro.inventory.repository.SaleRepository;
import com.veltro.inventory.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Long BUSINESS_ID = 100L;

    @Mock private SaleRepository saleRepository;
    @Mock private ReportExporter pdfExporter;
    @Mock private ReportExporter excelExporter;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        TenantContext.setOverride(BUSINESS_ID, 10L, "admin");

        when(pdfExporter.getType()).thenReturn(ReportType.PDF);
        when(excelExporter.getType()).thenReturn(ReportType.EXCEL);

        reportService = new ReportService(saleRepository, List.of(pdfExporter, excelExporter));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clearOverride();
    }

    @Test
    @DisplayName("exportReport — PDF delega al exportador PDF")
    void exportReport_pdf_delegatesToPdfExporter() {
        ProfitabilityReport report = new ProfitabilityReport(
                LocalDate.now().minusDays(7), LocalDate.now(),
                BigDecimal.valueOf(1000), BigDecimal.valueOf(600),
                BigDecimal.valueOf(400), BigDecimal.valueOf(40),
                10, 50, List.of());

        byte[] expectedBytes = new byte[]{1, 2, 3};
        when(pdfExporter.export(report)).thenReturn(expectedBytes);

        byte[] result = reportService.exportReport(report, ReportType.PDF);

        assertThat(result).isEqualTo(expectedBytes);
        verify(pdfExporter).export(report);
    }

    @Test
    @DisplayName("exportReport — Excel delega al exportador Excel")
    void exportReport_excel_delegatesToExcelExporter() {
        ProfitabilityReport report = new ProfitabilityReport(
                LocalDate.now().minusDays(7), LocalDate.now(),
                BigDecimal.valueOf(2000), BigDecimal.valueOf(1200),
                BigDecimal.valueOf(800), BigDecimal.valueOf(40),
                20, 100, List.of());

        byte[] expectedBytes = new byte[]{4, 5, 6};
        when(excelExporter.export(report)).thenReturn(expectedBytes);

        byte[] result = reportService.exportReport(report, ReportType.EXCEL);

        assertThat(result).isEqualTo(expectedBytes);
        verify(excelExporter).export(report);
    }

    @Test
    @DisplayName("exportReport — tipo no soportado lanza IllegalArgumentException")
    void exportReport_unsupportedType_throwsIllegalArgument() {
        // Crear service sin exportadores
        ReportService emptyService = new ReportService(saleRepository, List.of());

        ProfitabilityReport report = new ProfitabilityReport(
                LocalDate.now(), LocalDate.now(),
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO,
                0, 0, List.of());

        assertThatThrownBy(() -> emptyService.exportReport(report, ReportType.PDF))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported export type");
    }

    @Test
    @DisplayName("getContentType — devuelve content type del exportador")
    void getContentType_returnsCorrectType() {
        when(pdfExporter.getContentType()).thenReturn("application/pdf");

        String result = reportService.getContentType(ReportType.PDF);
        assertThat(result).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("getFileExtension — devuelve extensión del exportador")
    void getFileExtension_returnsCorrectExtension() {
        when(excelExporter.getFileExtension()).thenReturn(".xlsx");

        String result = reportService.getFileExtension(ReportType.EXCEL);
        assertThat(result).isEqualTo(".xlsx");
    }

    @Test
    @DisplayName("generateProfitabilityReport — calcula totales correctamente")
    void generateProfitabilityReport_calculatesCorrectTotals() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(saleRepository.sumTotalByStatusAndDateRange(any(), any(), any(), eq(BUSINESS_ID)))
                .thenReturn(BigDecimal.valueOf(10000));
        when(saleRepository.countByStatusAndDateRange(any(), any(), any(), eq(BUSINESS_ID)))
                .thenReturn(50L);
        when(saleRepository.sumItemsSoldByStatusAndDateRange(any(), any(), any(), eq(BUSINESS_ID)))
                .thenReturn(200L);
        when(saleRepository.getProductProfitabilityBreakdown(any(), any(), any(), eq(BUSINESS_ID)))
                .thenReturn(List.of());

        ProfitabilityReport report = reportService.generateProfitabilityReport(startDate, endDate);

        assertThat(report.totalSales()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(report.salesCount()).isEqualTo(50);
        assertThat(report.itemsSold()).isEqualTo(200);
        assertThat(report.startDate()).isEqualTo(startDate);
        assertThat(report.endDate()).isEqualTo(endDate);
    }

    @Test
    @DisplayName("generateProfitabilityReport — con productos calcula ganancia por producto")
    void generateProfitabilityReport_withProducts() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 31);

        Object[] productRow = new Object[]{
                1L, "Producto A", "SKU-001", 10,
                BigDecimal.valueOf(1000), BigDecimal.valueOf(50)
        };

        List<Object[]> productRows = new ArrayList<>();
        productRows.add(productRow);

        when(saleRepository.sumTotalByStatusAndDateRange(any(), any(), any(), eq(BUSINESS_ID)))
                .thenReturn(BigDecimal.valueOf(1000));
        when(saleRepository.countByStatusAndDateRange(any(), any(), any(), eq(BUSINESS_ID)))
                .thenReturn(5L);
        when(saleRepository.sumItemsSoldByStatusAndDateRange(any(), any(), any(), eq(BUSINESS_ID)))
                .thenReturn(10L);
        when(saleRepository.getProductProfitabilityBreakdown(any(), any(), any(), eq(BUSINESS_ID)))
                .thenReturn(productRows);

        ProfitabilityReport report = reportService.generateProfitabilityReport(startDate, endDate);

        assertThat(report.productBreakdown()).hasSize(1);
        ProfitabilityReport.ProductProfitability prod = report.productBreakdown().get(0);
        assertThat(prod.productName()).isEqualTo("Producto A");
        assertThat(prod.quantitySold()).isEqualTo(10);
        // revenue = 1000, cost = 50 * 10 = 500, profit = 500
        assertThat(prod.revenue()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}
