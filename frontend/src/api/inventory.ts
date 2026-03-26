import apiClient from './client';

export type AlertSeverity = 'CRITICAL' | 'WARNING' | 'INFO';
export type AlertType = 'OUT_OF_STOCK' | 'LOW_STOCK' | 'OVERSTOCK';

export interface Alert {
  id: number;
  productId: number;
  productName: string;
  type: AlertType;
  severity: AlertSeverity;
  message: string;
  read: boolean;
  resolved: boolean;
  createdAt: string;
}

export interface AlertConfig {
  productId: string;
  criticalStock: number;
  minStock: number;
  overstockThreshold: number;
}

export interface InventoryItem {
  id: number;
  productId: number;
  productName: string;
  currentStock: number;
  minStock: number;
  maxStock: number;
  active: boolean;
  version: number;
}

export interface InventoryMovement {
  id: number;
  inventoryId: number;
  movementType: 'ENTRY' | 'EXIT' | 'ADJUSTMENT' | 'SALE';
  quantity: number;
  previousStock: number;
  newStock: number;
  reason: string;
  createdAt: string;
  createdBy: string;
}

export interface StockEntryRequest {
  quantity: number;
  reason: string;
}

export interface StockExitRequest {
  quantity: number;
  reason: string;
}

export interface StockAdjustmentRequest {
  newStock: number;
  reason: string;
}

export interface UpdateStockLimitsRequest {
  minStock: number;
  maxStock: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;    // current page (0-indexed)
  size: number;      // page size
}

/**
 * Get all inventory items with pagination
 * GET /api/v1/inventory
 */
export const getInventory = (page: number = 0, size: number = 20) => {
  return apiClient.get<PageResponse<InventoryItem>>('/inventory', {
    params: { page, size },
  });
};

/**
 * Get inventory for a specific product
 * GET /api/v1/inventory/{productId}
 */
export const getInventoryByProductId = (productId: number) => {
  return apiClient.get<InventoryItem>(`/inventory/${productId}`);
};

/**
 * Get movement history for a product
 * GET /api/v1/inventory/{productId}/movements
 */
export const getInventoryMovements = (productId: number, page: number = 0) => {
  return apiClient.get<PageResponse<InventoryMovement>>(`/inventory/${productId}/movements`, {
    params: { page, size: 20 },
  });
};

/**
 * Record stock entry
 * POST /api/v1/inventory/{productId}/entry
 */
export const recordStockEntry = (productId: number, data: StockEntryRequest) => {
  return apiClient.post<InventoryItem>(`/inventory/${productId}/entry`, data);
};

/**
 * Record stock exit
 * POST /api/v1/inventory/{productId}/exit
 */
export const recordStockExit = (productId: number, data: StockExitRequest) => {
  return apiClient.post<InventoryItem>(`/inventory/${productId}/exit`, data);
};

/**
 * Record stock adjustment
 * POST /api/v1/inventory/{productId}/adjustment
 */
export const recordStockAdjustment = (productId: number, data: StockAdjustmentRequest) => {
  return apiClient.post<InventoryItem>(`/inventory/${productId}/adjustment`, data);
};

/**
 * Update stock limits
 * PUT /api/v1/inventory/{productId}/limits
 */
export const updateStockLimits = (productId: number, data: UpdateStockLimitsRequest) => {
  return apiClient.put<InventoryItem>(`/inventory/${productId}/limits`, data);
};

/**
 * Get paginated list of alerts
 * GET /api/v1/alerts
 * Backend returns Spring Page<AlertResponse>
 */
export const getAlerts = (page: number = 0, severity?: string) => {
  return apiClient.get<PageResponse<Alert>>('/alerts', {
    params: {
      page,
      size: 10,
      severity: severity || undefined,
    },
  });
};

/**
 * Resolve an alert
 * PUT /api/v1/alerts/{alertId}/resolve
 */
export const dismissAlert = (alertId: number) => {
  return apiClient.put(`/alerts/${alertId}/resolve`);
};

/**
 * Get alert configuration for a product
 * GET /api/v1/alerts/configuration/{productId}
 */
export const getAlertConfig = (productId: number) => {
  return apiClient.get<AlertConfig>(
    `/alerts/configuration/${productId}`
  );
};

/**
 * Update alert configuration for a product
 * PUT /api/v1/alerts/configuration/{productId}
 */
export const updateAlertConfig = (productId: number, config: Omit<AlertConfig, 'productId'>) => {
  return apiClient.put<AlertConfig>(
    `/alerts/configuration/${productId}`,
    config
  );
};
