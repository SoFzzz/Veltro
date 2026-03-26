import React from 'react';
import type { AuditRecord } from '../../api/audit';

interface AuditTableProps {
  records: AuditRecord[];
  isLoading: boolean;
  totalPages: number;
  currentPage: number;
  onPageChange: (page: number) => void;
  onRecordClick: (record: AuditRecord) => void;
}

/**
 * AuditTable - Display paginated audit records with action buttons
 */
export const AuditTable: React.FC<AuditTableProps> = ({
  records,
  isLoading,
  totalPages,
  currentPage,
  onPageChange,
  onRecordClick,
}) => {
  const actionColors: Record<string, { bg: string; text: string }> = {
    CREATE: { bg: '#E8F4F0', text: '#038E57' },
    UPDATE: { bg: '#E0E7FF', text: '#4F46E5' },
    DELETE: { bg: 'rgba(255,46,33,0.1)', text: '#FF2E21' },
    VOID: { bg: '#F3F4F6', text: '#6B7280' },
  };

  const entityColors = {
    SALE: '#7C3AED',
    INVENTORY: '#4F46E5',
    ORDER: '#06B6D4',
    PRODUCT: '#FF9500',
    SUPPLIER: '#EC4899',
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div
          className="animate-spin rounded-full h-8 w-8 border-b-2"
          style={{ borderBottomColor: '#038E57' }}
        ></div>
      </div>
    );
  }

  if (records.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-lg" style={{ color: '#6B7280' }}>No audit records found</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead style={{ backgroundColor: '#F9F7F2' }}>
            <tr>
              <th className="px-6 py-3 text-left text-sm font-semibold" style={{ color: '#1F2937' }}>
                Action
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold" style={{ color: '#1F2937' }}>
                Entity
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold" style={{ color: '#1F2937' }}>
                Entity ID
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold" style={{ color: '#1F2937' }}>
                User
              </th>
              <th className="px-6 py-3 text-left text-sm font-semibold" style={{ color: '#1F2937' }}>
                Timestamp
              </th>
              <th className="px-6 py-3 text-center text-sm font-semibold" style={{ color: '#1F2937' }}>
                Actions
              </th>
            </tr>
          </thead>
          <tbody style={{ borderTop: '1px solid #E8E3DB' }}>
            {records.map((record, idx) => (
              <tr
                key={record.id}
                style={{
                  backgroundColor: idx % 2 === 0 ? '#FFFFFF' : '#F9F7F2',
                  borderBottom: '1px solid #E8E3DB',
                }}
              >
                <td className="px-6 py-3 text-sm">
                  <span
                    className="inline-block px-3 py-1 rounded-full text-xs font-medium"
                    style={{
                      backgroundColor: actionColors[record.action].bg,
                      color: actionColors[record.action].text,
                    }}
                  >
                    {record.action}
                  </span>
                </td>
                <td className="px-6 py-3 text-sm font-medium" style={{ color: entityColors[record.entityType] }}>
                  {record.entityType}
                </td>
                <td className="px-6 py-3 text-sm font-mono" style={{ color: '#6B7280' }}>
                  {record.entityId}
                </td>
                <td className="px-6 py-3 text-sm" style={{ color: '#1F2937' }}>
                  {record.username}
                </td>
                <td className="px-6 py-3 text-sm" style={{ color: '#6B7280' }}>
                  {new Date(record.timestamp).toLocaleString()}
                </td>
                <td className="px-6 py-3 text-center text-sm">
                  <button
                     onClick={() => onRecordClick(record)}
                     className="px-3 py-1 rounded-md font-medium transition-colors text-sm"
                     style={{
                       backgroundColor: '#FFAC00',
                       color: '#FFFFFF',
                     }}
                     onMouseEnter={(e) => {
                       e.currentTarget.style.opacity = '0.9';
                     }}
                     onMouseLeave={(e) => {
                       e.currentTarget.style.opacity = '1';
                     }}
                   >
                     View Details
                   </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center items-center gap-2 mt-6">
           <button
             onClick={() => onPageChange(Math.max(0, currentPage - 1))}
             disabled={currentPage === 0}
             className="px-3 py-1 rounded-md font-medium transition-colors text-sm"
             style={{
               backgroundColor: '#FFAC00',
               color: '#FFFFFF',
               opacity: currentPage === 0 ? 0.5 : 1,
               cursor: currentPage === 0 ? 'not-allowed' : 'pointer',
             }}
           >
             Previous
           </button>

          <div className="flex items-center gap-1">
            {Array.from({ length: Math.min(5, totalPages) }).map((_, i) => {
              const pageNum = i + Math.max(0, currentPage - 2);
              if (pageNum >= totalPages) return null;

              return (
                <button
                  key={pageNum}
                  onClick={() => onPageChange(pageNum)}
                  className="px-3 py-1 rounded-md text-sm font-medium"
                  style={{
                    backgroundColor: pageNum === currentPage ? '#038E57' : '#FFFFFF',
                    color: pageNum === currentPage ? '#FFFFFF' : '#1F2937',
                    border: pageNum === currentPage ? 'none' : '1px solid #E8E3DB',
                  }}
                >
                  {pageNum + 1}
                </button>
              );
            })}
          </div>

           <button
             onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
             disabled={currentPage === totalPages - 1}
             className="px-3 py-1 rounded-md font-medium transition-colors text-sm"
             style={{
               backgroundColor: '#FFAC00',
               color: '#FFFFFF',
               opacity: currentPage === totalPages - 1 ? 0.5 : 1,
               cursor: currentPage === totalPages - 1 ? 'not-allowed' : 'pointer',
             }}
           >
             Next
           </button>
        </div>
      )}
    </div>
  );
};
