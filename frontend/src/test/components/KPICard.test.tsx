import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { KPICard } from '../../components/dashboard/KPICard';

describe('KPICard Component', () => {
  it('should render title and value', () => {
    render(
      <KPICard
        title="Today's Sales"
        value="$1,234.56"
        icon="💰"
      />
    );

    expect(screen.getByText("Today's Sales")).toBeInTheDocument();
    expect(screen.getByText('$1,234.56')).toBeInTheDocument();
    expect(screen.getByText('💰')).toBeInTheDocument();
  });

  it('should render with correct color class', () => {
    const { container } = render(
      <KPICard
        title="Test"
        value="100"
        icon="📊"
        color="green"
      />
    );

    const card = container.querySelector('[class*="bg-green-50"]');
    expect(card).toBeInTheDocument();
  });

  it('should render trend information when provided', () => {
    render(
      <KPICard
        title="Revenue"
        value="$5,000"
        icon="📈"
        trend="up"
        trendValue="+15%"
      />
    );

    expect(screen.getByText('+15%')).toBeInTheDocument();
    expect(screen.getByText('↑')).toBeInTheDocument();
  });

  it('should display correct trend icons', () => {
    const { rerender } = render(
      <KPICard
        title="Test"
        value="100"
        icon="📊"
        trend="up"
        trendValue="+10%"
      />
    );

    expect(screen.getByText('↑')).toBeInTheDocument();

    rerender(
      <KPICard
        title="Test"
        value="100"
        icon="📊"
        trend="down"
        trendValue="-5%"
      />
    );

    expect(screen.getByText('↓')).toBeInTheDocument();

    rerender(
      <KPICard
        title="Test"
        value="100"
        icon="📊"
        trend="neutral"
        trendValue="0%"
      />
    );

    expect(screen.getByText('→')).toBeInTheDocument();
  });

  it('should use default color when not provided', () => {
    const { container } = render(
      <KPICard
        title="Test"
        value="100"
        icon="📊"
      />
    );

    const card = container.querySelector('[class*="bg-blue-50"]');
    expect(card).toBeInTheDocument();
  });

  it('should handle numeric values', () => {
    render(
      <KPICard
        title="Items Sold"
        value={42}
        icon="📦"
      />
    );

    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('should not render trend when trendValue is missing', () => {
    const { container } = render(
      <KPICard
        title="Test"
        value="100"
        icon="📊"
        trend="up"
      />
    );

    expect(container.querySelector('[class*="mt-2"]')).not.toBeInTheDocument();
  });
});
