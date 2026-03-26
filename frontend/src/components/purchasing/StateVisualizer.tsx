import React from 'react';
import type { POStatus } from '../../api/purchasing';

interface StateVisualizerProps {
  status: POStatus;
  size?: 'sm' | 'md' | 'lg';
}

/**
 * StateVisualizer - Visual indicator for purchase order status
 * - PENDING: Blue (🔵)
 * - PARTIAL: Yellow (🟡)
 * - RECEIVED: Green (🟢)
 * - VOIDED: Gray (⚫)
 */
export const StateVisualizer: React.FC<StateVisualizerProps> = ({
  status,
  size = 'md',
}) => {
  const statusConfig: Record<
    POStatus,
    { bg: string; text: string; icon: string; label: string; dotColor: string }
  > = {
    PENDING: {
      bg: '#FFF9E6',
      text: '#FF9500',
      icon: '🔵',
      label: 'Pending',
      dotColor: '#FFAC00',
    },
    PARTIAL: {
      bg: '#FFF4E6',
      text: '#FF9500',
      icon: '🟡',
      label: 'Partial',
      dotColor: '#FF9500',
    },
    RECEIVED: {
      bg: '#E8F4F0',
      text: '#038E57',
      icon: '🟢',
      label: 'Received',
      dotColor: '#038E57',
    },
    VOIDED: {
      bg: '#F3F4F6',
      text: '#6B7280',
      icon: '⚫',
      label: 'Voided',
      dotColor: '#6B7280',
    },
  };

  const config = statusConfig[status];

  const sizeStyles = {
    sm: { padding: '0.5rem 0.5rem', fontSize: '0.75rem' },
    md: { padding: '0.375rem 0.75rem', fontSize: '0.875rem' },
    lg: { padding: '0.5rem 1rem', fontSize: '1rem' },
  };

  return (
    <span
      className="inline-flex items-center gap-2 rounded-full font-medium"
      style={{
        backgroundColor: config.bg,
        color: config.text,
        ...sizeStyles[size],
      }}
    >
      <span
        className="rounded-full"
        style={{
          width: '0.5rem',
          height: '0.5rem',
          backgroundColor: config.dotColor,
        }}
      ></span>
      <span>{config.icon}</span>
      {config.label}
    </span>
  );
};
