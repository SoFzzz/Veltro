import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { DiffViewer } from '../../components/audit/DiffViewer';

describe('DiffViewer Component', () => {
  it('should display "No changes detected" when objects are identical', () => {
    const data = { name: 'John', age: 30 };
    render(<DiffViewer previousData={data} newData={data} />);

    expect(screen.getByText('No changes detected')).toBeInTheDocument();
  });

  it('should display changed fields with yellow background', () => {
    const previousData = { name: 'John', age: 30 };
    const newData = { name: 'Jane', age: 31 };

    const { container } = render(
      <DiffViewer previousData={previousData} newData={newData} />
    );

    const rows = container.querySelectorAll('tbody tr');
    expect(rows.length).toBeGreaterThan(0);

    // Check for "Changed" badge
    expect(screen.getAllByText('Changed')).toBeDefined();
  });

  it('should display all keys from both objects', () => {
    const previousData = { name: 'John', city: 'NYC' };
    const newData = { name: 'John', age: 30 };

    const { container } = render(
      <DiffViewer previousData={previousData} newData={newData} />
    );

    const rows = container.querySelectorAll('tbody tr');
    expect(rows.length).toBe(3); // name, city, age
  });

  it('should display null values correctly', () => {
    const previousData = { name: null };
    const newData = { name: 'John' };

    render(<DiffViewer previousData={previousData} newData={newData} />);

    expect(screen.getByText('null')).toBeInTheDocument();
    expect(screen.getByText('John')).toBeInTheDocument();
  });

  it('should handle nested objects', () => {
    const previousData = {
      user: { name: 'John', age: 30 },
      active: true,
    };
    const newData = {
      user: { name: 'Jane', age: 31 },
      active: true,
    };

    render(<DiffViewer previousData={previousData} newData={newData} />);

    expect(screen.getByText('active')).toBeInTheDocument();
  });

  it('should show previous values in red background', () => {
    const previousData = { status: 'pending' };
    const newData = { status: 'completed' };

    const { container } = render(
      <DiffViewer previousData={previousData} newData={newData} />
    );

    const redBg = container.querySelector('[class*="bg-red-50"]');
    expect(redBg).toBeInTheDocument();
  });

  it('should show new values in green background', () => {
    const previousData = { status: 'pending' };
    const newData = { status: 'completed' };

    const { container } = render(
      <DiffViewer previousData={previousData} newData={newData} />
    );

    const greenBg = container.querySelector('[class*="bg-green-50"]');
    expect(greenBg).toBeInTheDocument();
  });

  it('should handle empty objects', () => {
    render(<DiffViewer previousData={{}} newData={{}} />);

    expect(screen.getByText('No changes detected')).toBeInTheDocument();
  });

  it('should sort keys alphabetically', () => {
    const previousData = { z: 'last', a: 'first', m: 'middle' };
    const newData = { z: 'last', a: 'first', m: 'middle' };

    const { container } = render(
      <DiffViewer previousData={previousData} newData={newData} />
    );

    const headers = container.querySelectorAll('tbody tr td:first-child');
    const keys = Array.from(headers).map(h => h.textContent?.trim());

    expect(keys[0]).toBe('a');
    expect(keys[1]).toBe('m');
    expect(keys[2]).toBe('z');
  });
});
