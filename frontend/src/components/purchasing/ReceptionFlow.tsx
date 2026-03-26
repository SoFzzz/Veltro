import React, { useState } from 'react';
import { markAsReceived } from '../../api/purchasing';

interface ReceptionFlowProps {
  orderId: number;
  onReceived?: () => void;
  onClose?: () => void;
}

/**
 * ReceptionFlow - Modal for receiving a purchase order.
 * Backend marks the entire order as received in one call (no per-item granularity).
 */
export const ReceptionFlow: React.FC<ReceptionFlowProps> = ({
  orderId,
  onReceived,
  onClose,
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    setIsSubmitting(true);
    setError(null);

    try {
      // Backend markAsReceived accepts no body — marks entire order
      await markAsReceived(orderId);
      onReceived?.();
      onClose?.();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : 'Failed to receive order'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 flex items-center justify-center z-50" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
      <div className="rounded-lg max-w-md w-full mx-4" style={{ backgroundColor: '#FFFFFF', border: '1px solid #E8E3DB' }}>
        {/* Header */}
        <div className="px-6 py-4" style={{ borderBottom: '1px solid #E8E3DB' }}>
          <h2 className="text-xl font-bold" style={{ color: '#1F2937' }}>
            Confirm Reception
          </h2>
        </div>

        {/* Content */}
        <div className="px-6 py-4">
          {error && (
            <div className="mb-4 p-3 rounded-md" style={{ backgroundColor: 'rgba(255,46,33,0.1)', border: '1px solid #FF2E21' }}>
              <p className="text-sm" style={{ color: '#FF2E21' }}>{error}</p>
            </div>
          )}

          <p className="text-sm" style={{ color: '#6B7280' }}>
            This will mark the entire purchase order as received and update inventory stock accordingly.
          </p>
        </div>

        {/* Footer */}
        <div className="px-6 py-4 flex justify-end gap-3" style={{ borderTop: '1px solid #E8E3DB' }}>
          <button
            onClick={onClose}
            disabled={isSubmitting}
            className="px-4 py-2 rounded-md font-medium transition-opacity"
            style={{
              backgroundColor: '#FFFFFF',
              color: '#1F2937',
              border: '1px solid #E8E3DB',
              opacity: isSubmitting ? 0.5 : 1,
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
            }}
            onMouseEnter={(e) => {
              if (!isSubmitting) e.currentTarget.style.backgroundColor = '#F9F7F2';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = '#FFFFFF';
            }}
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={isSubmitting}
            className="px-4 py-2 rounded-md font-medium text-white transition-opacity"
            style={{
              backgroundColor: '#038E57',
              opacity: isSubmitting ? 0.5 : 1,
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
            }}
            onMouseEnter={(e) => {
              if (!isSubmitting) e.currentTarget.style.opacity = '0.9';
            }}
            onMouseLeave={(e) => {
              if (!isSubmitting) e.currentTarget.style.opacity = '1';
            }}
          >
            {isSubmitting ? 'Receiving...' : 'Confirm Receipt'}
          </button>
        </div>
      </div>
    </div>
  );
};
