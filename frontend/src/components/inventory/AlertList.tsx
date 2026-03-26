import React, { useState } from 'react';
import type { Alert } from '../../api/inventory';
import { dismissAlert } from '../../api/inventory';
import { useAlertStore } from '../../stores/alertStore';
import { SeverityBadge } from './SeverityBadge';

interface AlertListProps {
  alerts: Alert[];
  isLoading: boolean;
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

/**
 * AlertList - Display paginated list of alerts with dismiss functionality
 */
export const AlertList: React.FC<AlertListProps> = ({
  alerts,
  isLoading,
  currentPage,
  totalPages,
  onPageChange,
}) => {
  const [dismissing, setDismissing] = useState<number | null>(null);
  const markAsRead = useAlertStore((state) => state.markAsRead);
  const removeAlert = useAlertStore((state) => state.removeAlert);

  const handleDismiss = async (alertId: number) => {
    setDismissing(alertId);
    try {
      await dismissAlert(alertId);
      markAsRead(alertId);
      removeAlert(alertId);
    } catch (error) {
      console.error('Failed to dismiss alert:', error);
    } finally {
      setDismissing(null);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div
          className="animate-spin rounded-full h-8 w-8 border-2"
          style={{
            borderColor: 'rgba(3, 142, 87, 0.2)',
            borderTopColor: '#038E57',
          }}
        ></div>
      </div>
    );
  }

  if (alerts.length === 0) {
    return (
      <div className="text-center py-12">
        <svg
          className="mx-auto h-12 w-12"
          style={{ color: '#9CA3AF' }}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
        <h3
          className="mt-2 text-lg font-medium"
          style={{ color: '#1F2937' }}
        >
          No alerts
        </h3>
        <p className="mt-1 text-sm" style={{ color: '#6B7280' }}>
          Your inventory is in good shape!
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Alert items */}
      <div
        className="border-2 rounded-lg"
        style={{
          borderColor: '#E8E3DB',
        }}
      >
        {alerts.map((alert, idx) => (
          <div
            key={alert.id}
            className="p-4 transition-colors"
            style={{
              backgroundColor: idx % 2 === 0 ? '#FFFFFF' : '#F9F7F2',
              borderBottom: idx < alerts.length - 1 ? '1px solid #E8E3DB' : 'none',
            }}
            onMouseEnter={(e) => {
              (e.currentTarget as HTMLDivElement).style.backgroundColor = '#F9F7F2';
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLDivElement).style.backgroundColor = idx % 2 === 0 ? '#FFFFFF' : '#F9F7F2';
            }}
          >
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-3 mb-2">
                  <SeverityBadge severity={alert.severity} />
                  <span className="text-xs" style={{ color: '#6B7280' }}>
                    {alert.type.replace(/_/g, ' ')}
                  </span>
                </div>
                <p
                  className="text-sm font-medium"
                  style={{ color: '#1F2937' }}
                >
                  {alert.productName}
                </p>
                <p className="text-sm mt-1" style={{ color: '#6B7280' }}>
                  {alert.message}
                </p>
                <p className="text-xs mt-2" style={{ color: '#9CA3AF' }}>
                  {new Date(alert.createdAt).toLocaleString()}
                </p>
              </div>
              <button
                onClick={() => handleDismiss(alert.id)}
                disabled={dismissing === alert.id}
                className="ml-4 px-3 py-1 text-sm font-medium rounded transition"
                style={{
                  color: '#038E57',
                  backgroundColor: 'transparent',
                  opacity: dismissing === alert.id ? 0.5 : 1,
                  cursor: dismissing === alert.id ? 'not-allowed' : 'pointer',
                }}
                onMouseEnter={(e) => {
                  if (dismissing !== alert.id) {
                    (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#E8F4F0';
                  }
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLButtonElement).style.backgroundColor = 'transparent';
                }}
              >
                {dismissing === alert.id ? 'Dismissing...' : 'Dismiss'}
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 0}
            className="px-3 py-2 text-sm font-medium rounded transition border-2"
            style={{
              color: '#1F2937',
              backgroundColor: '#FFFFFF',
              borderColor: '#E8E3DB',
              opacity: currentPage === 0 ? 0.5 : 1,
              cursor: currentPage === 0 ? 'not-allowed' : 'pointer',
            }}
            onMouseEnter={(e) => {
              if (currentPage > 0) {
                (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#F9F7F2';
              }
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#FFFFFF';
            }}
          >
            Previous
          </button>
          <span className="text-sm" style={{ color: '#6B7280' }}>
            Page {currentPage + 1} of {totalPages}
          </span>
          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage >= totalPages - 1}
            className="px-3 py-2 text-sm font-medium rounded transition border-2"
            style={{
              color: '#1F2937',
              backgroundColor: '#FFFFFF',
              borderColor: '#E8E3DB',
              opacity: currentPage >= totalPages - 1 ? 0.5 : 1,
              cursor: currentPage >= totalPages - 1 ? 'not-allowed' : 'pointer',
            }}
            onMouseEnter={(e) => {
              if (currentPage < totalPages - 1) {
                (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#F9F7F2';
              }
            }}
            onMouseLeave={(e) => {
              (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#FFFFFF';
            }}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};
