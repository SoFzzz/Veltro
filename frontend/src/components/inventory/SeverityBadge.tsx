import React from 'react';
import type { AlertSeverity } from '../../api/inventory';

interface SeverityBadgeProps {
  severity: AlertSeverity;
}

/**
 * SeverityBadge - Visual indicator for alert severity levels
 * - CRITICAL: Red background
 * - WARNING: Orange background
 * - INFO: Yellow background
 */
export const SeverityBadge: React.FC<SeverityBadgeProps> = ({ severity }) => {
  const severityConfig: Record<
    AlertSeverity,
    { bg: string; border: string; text: string; icon: string; label: string }
  > = {
    CRITICAL: {
      bg: 'rgba(255, 46, 33, 0.1)',
      border: '#FF2E21',
      text: '#FF2E21',
      icon: '🔴',
      label: 'Critical',
    },
    WARNING: {
      bg: '#FFF4E6',
      border: '#FFAC00',
      text: '#FF9500',
      icon: '🟠',
      label: 'Warning',
    },
    INFO: {
      bg: '#E8F4F0',
      border: '#10B981',
      text: '#038E57',
      icon: '🟡',
      label: 'Info',
    },
  };

  const config = severityConfig[severity];

  return (
    <span
      className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium border-2"
      style={{
        backgroundColor: config.bg,
        borderColor: config.border,
        color: config.text,
      }}
    >
      <span>{config.icon}</span>
      {config.label}
    </span>
  );
};
