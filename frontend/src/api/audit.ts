import apiClient from './client';
import type { PageResponse } from './dashboard';

export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'VOID';
export type AuditEntity = 'SALE' | 'INVENTORY' | 'ORDER' | 'PRODUCT' | 'SUPPLIER';

export interface AuditRecord {
  id: string;
  action: AuditAction;
  entityType: AuditEntity;
  entityId: string;
  previousData: Record<string, unknown>;
  newData: Record<string, unknown>;
  userId: string;
  username: string;
  timestamp: string; // ISO DateTime
  reason?: string;
}

export interface AuditFilters {
  page?: number;
  pageSize?: number;
  entityType?: AuditEntity;
  action?: AuditAction;
  startDate?: string; // ISO date
  endDate?: string; // ISO date
  entityId?: string;
}

/**
 * Get audit records with optional filters
 * GET /api/v1/audit
 */
export const getAuditRecords = (filters: AuditFilters = {}) => {
  const params = {
    page: filters.page || 0,
    size: filters.pageSize || 20,
    ...(filters.entityType && { entityType: filters.entityType }),
    ...(filters.action && { action: filters.action }),
    ...(filters.startDate && { startDate: filters.startDate }),
    ...(filters.endDate && { endDate: filters.endDate }),
    ...(filters.entityId && { entityId: filters.entityId }),
  };

  return apiClient.get<PageResponse<AuditRecord>>('/audit', { params });
};

/**
 * Get single audit record detail
 * GET /api/v1/audit/{auditId}
 */
export const getAuditRecordDetail = (auditId: string) => {
  return apiClient.get<AuditRecord>(`/audit/${auditId}`);
};

/**
 * Export audit trail as CSV
 * NOTE: Backend does not have a /audit/export endpoint.
 * This is a client-side stub that will fail if called.
 * TODO: Implement CSV export on backend or remove this.
 */
export const exportAuditCsv = async (_filters: AuditFilters = {}) => {
  throw new Error('Audit CSV export is not yet implemented on the backend.');
};
