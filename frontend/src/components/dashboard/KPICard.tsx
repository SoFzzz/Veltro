import React from 'react';

interface KPICardProps {
  title: string;
  value: string | number;
  icon: string;
  trend?: 'up' | 'down' | 'neutral';
  trendValue?: string;
  variant?: 'default' | 'success' | 'warning' | 'critical';
}

/**
 * KPICard - Displays a key performance indicator with value and optional trend
 * Uses Veltro design system: Emerald green (#038E57), Orange accent (#FFAC00), Red critical (#FF2E21)
 * "Precision Ledger" style: crisp 1px borders, tabular-nums, embedded look
 */
export const KPICard: React.FC<KPICardProps> = ({
  title,
  value,
  icon,
  trend,
  trendValue,
  variant = 'default',
}) => {
  const getVariantStyles = (v: string) => {
    switch (v) {
      case 'success':
        return {
          barColor: '#10B981',
          bgColor: '#FFFFFF',
          iconBg: 'rgba(16, 185, 129, 0.1)',
          iconColor: '#10B981',
        };
      case 'warning':
        return {
          barColor: '#FFAC00',
          bgColor: '#FFFFFF',
          iconBg: 'rgba(255, 172, 0, 0.1)',
          iconColor: '#FF9500',
        };
      case 'critical':
        return {
          barColor: '#FF2E21',
          bgColor: '#FFFFFF',
          iconBg: 'rgba(255, 46, 33, 0.1)',
          iconColor: '#FF2E21',
        };
      default:
        return {
          barColor: '#038E57',
          bgColor: '#FFFFFF',
          iconBg: 'rgba(3, 142, 87, 0.08)',
          iconColor: '#038E57',
        };
    }
  };

  const trendColors = {
    up: '#10B981',
    down: '#FF2E21',
    neutral: '#6B7280',
  };

  const styles = getVariantStyles(variant);

  return (
    <div
      className="relative p-5 rounded-md transition-all duration-200"
      style={{
        backgroundColor: styles.bgColor,
        border: '1px solid rgba(31, 41, 55, 0.1)',
        boxShadow: '0 1px 2px rgba(0,0,0,0.02)',
        overflow: 'hidden'
      }}
      onMouseEnter={e => {
        e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.05)';
        e.currentTarget.style.transform = 'translateY(-1px)';
        e.currentTarget.style.borderColor = 'rgba(31, 41, 55, 0.2)';
      }}
      onMouseLeave={e => {
        e.currentTarget.style.boxShadow = '0 1px 2px rgba(0,0,0,0.02)';
        e.currentTarget.style.transform = 'translateY(0)';
        e.currentTarget.style.borderColor = 'rgba(31, 41, 55, 0.1)';
      }}
    >
      {/* Precision Ledger Indicator Line */}
      <div 
        className="absolute top-0 left-0 bottom-0 w-1"
        style={{ backgroundColor: styles.barColor }}
      />
      
      <div className="flex items-start justify-between pl-2">
        <div className="flex-1">
          <p className="text-xs font-semibold uppercase tracking-wider mb-1" style={{ color: '#6B7280' }}>
            {title}
          </p>
          <p
            className="text-3xl font-bold tracking-tight mt-1"
            style={{
              color: '#111827',
              fontVariantNumeric: 'tabular-nums',
              letterSpacing: '-0.02em'
            }}
          >
            {value}
          </p>
          {trend && trendValue && (
            <div className="flex items-center mt-3 text-xs font-medium">
              <span 
                className="inline-flex items-center px-1.5 py-0.5 rounded"
                style={{ 
                  backgroundColor: `rgba(${
                    trend === 'up' ? '16, 185, 129' : 
                    trend === 'down' ? '255, 46, 33' : '107, 114, 128'
                  }, 0.1)`,
                  color: trendColors[trend] 
                }}
              >
                {trend === 'up' && '↑ '}
                {trend === 'down' && '↓ '}
                {trend === 'neutral' && '→ '}
                {trendValue}
              </span>
              <span className="ml-2 text-gray-500">vs mes anterior</span>
            </div>
          )}
        </div>
        <div
          className="w-10 h-10 rounded-md flex items-center justify-center text-xl"
          style={{ 
            backgroundColor: styles.iconBg,
            color: styles.iconColor 
          }}
          role="img"
          aria-hidden="true"
        >
          {icon}
        </div>
      </div>
    </div>
  );
};
