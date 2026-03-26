import React from 'react';
import type { AuditAction, AuditEntity, AuditFilters as AuditFiltersType } from '../../api/audit';

interface AuditFiltersProps {
  filters: AuditFiltersType;
  onFiltersChange: (filters: AuditFiltersType) => void;
  onReset: () => void;
}

const ACTIONS: AuditAction[] = ['CREATE', 'UPDATE', 'DELETE', 'VOID'];
const ENTITIES: AuditEntity[] = ['SALE', 'INVENTORY', 'ORDER', 'PRODUCT', 'SUPPLIER'];

/**
 * AuditFilters - Component for filtering audit records
 */
export const AuditFilters: React.FC<AuditFiltersProps> = ({
  filters,
  onFiltersChange,
  onReset,
}) => {
  const handleActionChange = (action: AuditAction) => {
    onFiltersChange({
      ...filters,
      action: filters.action === action ? undefined : action,
      page: 0,
    });
  };

  const handleEntityChange = (entity: AuditEntity) => {
    onFiltersChange({
      ...filters,
      entityType: filters.entityType === entity ? undefined : entity,
      page: 0,
    });
  };

  const handleStartDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onFiltersChange({
      ...filters,
      startDate: e.target.value || undefined,
      page: 0,
    });
  };

  const handleEndDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onFiltersChange({
      ...filters,
      endDate: e.target.value || undefined,
      page: 0,
    });
  };

  const handleEntityIdChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onFiltersChange({
      ...filters,
      entityId: e.target.value || undefined,
      page: 0,
    });
  };

  return (
    <div className="rounded-lg p-6 space-y-4" style={{ backgroundColor: '#FFFFFF', border: '1px solid #E8E3DB' }}>
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-semibold" style={{ color: '#1F2937' }}>Filters</h3>
         <button
            onClick={onReset}
            className="text-sm px-3 py-1 rounded-md font-medium transition-colors"
            style={{
              backgroundColor: '#6B7280',
              color: '#FFFFFF',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.opacity = '0.9';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.opacity = '1';
            }}
          >
            Reset All
          </button>
      </div>

      {/* Action Filter */}
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: '#1F2937' }}>
          Action
        </label>
        <div className="flex flex-wrap gap-2">
          {ACTIONS.map((action) => (
            <button
              key={action}
              onClick={() => handleActionChange(action)}
              className="px-3 py-1 rounded-full text-sm font-medium transition-colors"
              style={{
                backgroundColor: filters.action === action ? '#038E57' : '#F3F4F6',
                color: filters.action === action ? '#FFFFFF' : '#1F2937',
              }}
            >
              {action}
            </button>
          ))}
        </div>
      </div>

      {/* Entity Type Filter */}
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: '#1F2937' }}>
          Entity Type
        </label>
        <div className="flex flex-wrap gap-2">
          {ENTITIES.map((entity) => (
            <button
              key={entity}
              onClick={() => handleEntityChange(entity)}
              className="px-3 py-1 rounded-full text-sm font-medium transition-colors"
              style={{
                backgroundColor: filters.entityType === entity ? '#038E57' : '#F3F4F6',
                color: filters.entityType === entity ? '#FFFFFF' : '#1F2937',
              }}
            >
              {entity}
            </button>
          ))}
        </div>
      </div>

      {/* Date Range Filter */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium mb-2" style={{ color: '#1F2937' }}>
            Start Date
          </label>
          <input
             type="date"
             value={filters.startDate || ''}
             onChange={handleStartDateChange}
             className="w-full text-sm px-3 py-2 rounded-md focus:outline-none"
             style={{
               border: '1px solid #E8E3DB',
               color: '#1F2937',
               backgroundColor: '#FFFFFF',
             }}
             onFocus={(e) => {
               e.currentTarget.style.borderColor = '#038E57';
             }}
             onBlur={(e) => {
               e.currentTarget.style.borderColor = '#E8E3DB';
             }}
           />
        </div>
        <div>
          <label className="block text-sm font-medium mb-2" style={{ color: '#1F2937' }}>
            End Date
          </label>
          <input
             type="date"
             value={filters.endDate || ''}
             onChange={handleEndDateChange}
             className="w-full text-sm px-3 py-2 rounded-md focus:outline-none"
             style={{
               border: '1px solid #E8E3DB',
               color: '#1F2937',
               backgroundColor: '#FFFFFF',
             }}
             onFocus={(e) => {
               e.currentTarget.style.borderColor = '#038E57';
             }}
             onBlur={(e) => {
               e.currentTarget.style.borderColor = '#E8E3DB';
             }}
           />
        </div>
      </div>

      {/* Entity ID Search */}
      <div>
        <label className="block text-sm font-medium mb-2" style={{ color: '#1F2937' }}>
          Entity ID (Search)
        </label>
         <input
           type="text"
           placeholder="Enter entity ID..."
           value={filters.entityId || ''}
           onChange={handleEntityIdChange}
           className="w-full text-sm px-3 py-2 rounded-md focus:outline-none"
           style={{
             border: '1px solid #E8E3DB',
             color: '#1F2937',
             backgroundColor: '#FFFFFF',
           }}
           onFocus={(e) => {
             e.currentTarget.style.borderColor = '#038E57';
           }}
           onBlur={(e) => {
             e.currentTarget.style.borderColor = '#E8E3DB';
           }}
         />
      </div>
    </div>
  );
};
