import apiClient from './client';

export interface DashboardResponse {
  todaySales: string; // BigDecimal as string
  todaySalesCount: number;
  averageTicket: string; // BigDecimal as string
  outOfStockProducts: number;
  outOfStockProductList: string[];
  estimatedMonthlyProfit: string; // BigDecimal as string
  lowStockAlertCount: number;
  recentSales: SaleRow[];
}

export interface SaleRow {
  saleId: number;
  saleNumber: string;
  total: number;
  itemCount: number;
  cashierId: number;
  completedAt: string;
}

/**
 * Spring Page<T> serialization format.
 * Spring uses `number` for current page (0-indexed) and `size` for page size.
 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;    // current page (0-indexed) — Spring's field name
  size: number;      // page size — Spring's field name
}

/**
 * Get dashboard data
 * GET /api/v1/dashboard
 */
export const getDashboard = () => {
  return apiClient.get<DashboardResponse>('/dashboard');
};

/**
 * Export profitability report as PDF
 * GET /api/v1/reports/export/PDF
 */
export const exportProfitabilityReportPdf = () => {
  const end = new Date();
  const start = new Date();
  start.setDate(1); // First day of current month
  
  const startDateStr = start.toISOString().split('T')[0];
  const endDateStr = end.toISOString().split('T')[0];

  return apiClient.get<Blob>(
    `/reports/export/PDF?startDate=${startDateStr}&endDate=${endDateStr}`,
    {
      responseType: 'blob',
    }
  );
};

/**
 * Export profitability report as Excel
 * GET /api/v1/reports/export/EXCEL
 */
export const exportProfitabilityReportExcel = () => {
  const end = new Date();
  const start = new Date();
  start.setDate(1); // First day of current month
  
  const startDateStr = start.toISOString().split('T')[0];
  const endDateStr = end.toISOString().split('T')[0];

  return apiClient.get<Blob>(
    `/reports/export/EXCEL?startDate=${startDateStr}&endDate=${endDateStr}`,
    {
      responseType: 'blob',
    }
  );
};
