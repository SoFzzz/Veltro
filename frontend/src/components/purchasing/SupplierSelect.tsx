import React, { useState, useEffect } from 'react';
import type { Supplier } from '../../api/purchasing';
import { getSuppliers } from '../../api/purchasing';

interface SupplierSelectProps {
  value: string;
  onChange: (supplierId: string) => void;
  disabled?: boolean;
}

/**
 * SupplierSelect - Dropdown for selecting a supplier
 * Fetches suppliers from API on mount
 */
export const SupplierSelect: React.FC<SupplierSelectProps> = ({
  value,
  onChange,
  disabled = false,
}) => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSuppliers = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const response = await getSuppliers();
        setSuppliers(response.data);
      } catch (err) {
        setError('Failed to load suppliers');
        console.error('Error loading suppliers:', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchSuppliers();
  }, []);

  return (
    <div>
      <label
        htmlFor="supplier"
        className="block text-sm font-medium mb-1"
        style={{ color: '#1F2937' }}
      >
        Supplier
      </label>
      <select
        id="supplier"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled || isLoading}
        className="block w-full px-3 py-2 rounded-md focus:outline-none"
        style={{
          border: '1px solid #E8E3DB',
          backgroundColor: '#FFFFFF',
          color: '#1F2937',
          opacity: disabled || isLoading ? 0.5 : 1,
          cursor: disabled || isLoading ? 'not-allowed' : 'pointer',
        }}
        onFocus={(e) => {
          e.currentTarget.style.borderColor = '#038E57';
        }}
        onBlur={(e) => {
          e.currentTarget.style.borderColor = '#E8E3DB';
        }}
      >
        <option value="">
          {isLoading ? 'Loading suppliers...' : 'Select a supplier'}
        </option>
        {suppliers.map((supplier) => (
          <option key={supplier.id} value={supplier.id}>
            {supplier.name} ({supplier.taxId})
          </option>
        ))}
      </select>
      {error && <p className="mt-1 text-sm" style={{ color: '#FF2E21' }}>{error}</p>}
    </div>
  );
};
