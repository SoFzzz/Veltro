import React from 'react';
import type { SaleRow } from '../../api/dashboard';

interface LatestSalesTableProps {
  sales: SaleRow[];
  isLoading: boolean;
}

/**
 * LatestSalesTable - Display recent sales transactions
 */
export const LatestSalesTable: React.FC<LatestSalesTableProps> = ({
  sales,
  isLoading,
}) => {
  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64" aria-live="polite" aria-label="Loading sales data">
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

  if (sales.length === 0) {
    return (
      <div
        className="text-center py-8"
        style={{ color: '#6B7280' }}
        aria-label="No sales recorded"
      >
        No sales recorded yet today
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full" aria-label="Recent sales transactions">
        <thead>
          <tr style={{ backgroundColor: '#F9F7F2' }}>
            <th
              className="px-4 py-3 text-left text-sm font-semibold"
              style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
              scope="col"
            >
              Sale #
            </th>
            <th
              className="px-4 py-3 text-left text-sm font-semibold"
              style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
              scope="col"
            >
              Cashier
            </th>
            <th
              className="px-4 py-3 text-right text-sm font-semibold"
              style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
              scope="col"
            >
              Items
            </th>
            <th
              className="px-4 py-3 text-right text-sm font-semibold"
              style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
              scope="col"
            >
              Total
            </th>
            <th
              className="px-4 py-3 text-left text-sm font-semibold"
              style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
              scope="col"
            >
              Time
            </th>
          </tr>
        </thead>
        <tbody>
          {sales.map((sale, idx) => (
            <tr
              key={sale.saleId}
              style={{
                backgroundColor: idx % 2 === 0 ? '#FFFFFF' : '#F9F7F2',
                borderBottom: '1px solid #E8E3DB',
              }}
              className="hover:bg-opacity-75 transition"
            >
              <td
                className="px-4 py-3 text-sm font-medium"
                style={{ color: '#1F2937' }}
              >
                {sale.saleNumber}
              </td>
              <td
                className="px-4 py-3 text-sm"
                style={{ color: '#6B7280' }}
              >
                Cajero #{sale.cashierId}
              </td>
              <td
                className="px-4 py-3 text-sm text-right"
                style={{ color: '#6B7280' }}
                aria-label={`${sale.itemCount} items`}
              >
                {sale.itemCount}
              </td>
              <td
                className="px-4 py-3 text-sm text-right font-semibold"
                style={{
                  color: '#038E57',
                  fontVariantNumeric: 'tabular-nums',
                }}
                aria-label={`Total: $${sale.total}`}
              >
                ${typeof sale.total === 'number' ? sale.total.toFixed(2) : sale.total}
              </td>
              <td
                className="px-4 py-3 text-sm"
                style={{ color: '#6B7280' }}
              >
                {sale.completedAt ? new Date(sale.completedAt).toLocaleTimeString() : 'N/A'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
