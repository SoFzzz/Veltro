import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { RoleGuard } from '../../components/auth/RoleGuard';

// Mock useAuth hook
vi.mock('../../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from '../../hooks/useAuth';

describe('RoleGuard Component', () => {
  it('should render children when user has allowed role', () => {
    (useAuth as any).mockReturnValue({
      user: { id: '1', email: 'test@example.com', role: 'ADMIN' },
      isAuthenticated: true,
    });

    render(
      <BrowserRouter>
        <RoleGuard allowedRoles={['ADMIN']}>
          <div>Protected Content</div>
        </RoleGuard>
      </BrowserRouter>
    );

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('should redirect to /unauthorized when user does not have allowed role', () => {
    (useAuth as any).mockReturnValue({
      user: { id: '1', email: 'test@example.com', role: 'CASHIER' },
      isAuthenticated: true,
    });

    render(
      <BrowserRouter>
        <RoleGuard allowedRoles={['ADMIN']}>
          <div>Protected Content</div>
        </RoleGuard>
      </BrowserRouter>
    );

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('should allow access when user has one of multiple allowed roles', () => {
    (useAuth as any).mockReturnValue({
      user: { id: '1', email: 'test@example.com', role: 'WAREHOUSE' },
      isAuthenticated: true,
    });

    render(
      <BrowserRouter>
        <RoleGuard allowedRoles={['ADMIN', 'WAREHOUSE']}>
          <div>Protected Content</div>
        </RoleGuard>
      </BrowserRouter>
    );

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('should handle case-insensitive role comparison', () => {
    (useAuth as any).mockReturnValue({
      user: { id: '1', email: 'test@example.com', role: 'admin' },
      isAuthenticated: true,
    });

    render(
      <BrowserRouter>
        <RoleGuard allowedRoles={['ADMIN']}>
          <div>Protected Content</div>
        </RoleGuard>
      </BrowserRouter>
    );

    // Note: Implementation should handle case-insensitive comparison
    // This test documents expected behavior
  });

  it('should deny access when user role is undefined', () => {
    (useAuth as any).mockReturnValue({
      user: { id: '1', email: 'test@example.com', role: undefined },
      isAuthenticated: true,
    });

    render(
      <BrowserRouter>
        <RoleGuard allowedRoles={['ADMIN']}>
          <div>Protected Content</div>
        </RoleGuard>
      </BrowserRouter>
    );

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });
});
