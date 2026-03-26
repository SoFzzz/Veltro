import { Link, useNavigate, Outlet, useLocation } from 'react-router-dom';
import { useState } from 'react';
import { useAuthStore } from '../../stores/authStore';
import { AlertBadge } from '../inventory';

export function MainLayout() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const getRoleLabel = (role: string): string => {
    switch (role) {
      case 'ADMIN': return 'Administrador';
      case 'CASHIER': return 'Cajero';
      case 'WAREHOUSE': return 'Almacén';
      default: return role;
    }
  };

  const getNavLinks = () => {
    switch (user?.role) {
      case 'ADMIN':
        return [
          { to: '/dashboard', label: 'Panel Principal', icon: '📊' },
          { to: '/pos', label: 'Terminal POS', icon: '💳' },
          { to: '/catalog/products', label: 'Productos', icon: '📦' },
          { to: '/catalog/categories', label: 'Categorías', icon: '📁' },
          { to: '/inventory', label: 'Inventario', icon: '📋' },
          { to: '/alerts', label: 'Alertas', icon: '⚠️' },
          { to: '/purchasing', label: 'Compras', icon: '🛒' },
          { to: '/purchasing/suppliers', label: 'Proveedores', icon: '🏢' },
          { to: '/settings/workers', label: 'Empleados', icon: '👥' },
        ];
      case 'WAREHOUSE':
        return [
          { to: '/catalog/products', label: 'Productos', icon: '📦' },
          { to: '/catalog/categories', label: 'Categorías', icon: '📁' },
          { to: '/inventory', label: 'Inventario', icon: '📋' },
          { to: '/alerts', label: 'Alertas', icon: '⚠️' },
          { to: '/purchasing', label: 'Órdenes de Compra', icon: '🛒' },
        ];
      case 'CASHIER':
        return [
          { to: '/pos', label: 'Terminal POS', icon: '💳' },
          { to: '/catalog/products', label: 'Productos', icon: '📦' },
        ];
      default:
        return [];
    }
  };

  const navLinks = getNavLinks();

  return (
    <div className="min-h-screen flex flex-col lg:flex-row" style={{ backgroundColor: 'var(--surface-primary)' }}>
      {/* Mobile Header */}
      <div className="lg:hidden fixed top-0 left-0 right-0 h-16 flex items-center px-4 z-40" 
           style={{ backgroundColor: 'var(--surface-secondary)', borderBottom: 'var(--border-light)' }}>
        <button
          onClick={() => setSidebarOpen(!sidebarOpen)}
          className="p-2 rounded-md hover:bg-gray-100 transition"
          style={{ color: 'var(--primary-base)' }}
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
        <div className="ml-4 font-bold text-lg" style={{ color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
          Veltro <span style={{ color: 'var(--primary-base)' }}>POS</span>
        </div>
      </div>

      {/* Desktop Sidebar (The Ledger Bind) + Mobile Sidebar Support */}
      <aside 
        className={`${sidebarOpen ? 'translate-x-0' : '-translate-x-full'} lg:translate-x-0 fixed top-0 left-0 bottom-0 z-50 w-[260px] flex flex-col transition-transform duration-300 ease-in-out`}
        style={{ 
          backgroundColor: '#0F172A', // Very dark slate to ground the application
          borderRight: '1px solid rgba(255,255,255,0.1)'
        }}
      >
        {/* Mobile close button */}
        <button 
          onClick={() => setSidebarOpen(false)}
          className="lg:hidden absolute top-4 right-4 text-gray-400 hover:text-white"
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
             <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        {/* Logo Area */}
        <div className="h-16 flex items-center px-6" style={{ borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
          <div className="w-8 h-8 rounded flex items-center justify-center font-bold text-white mr-3" 
               style={{ backgroundColor: 'var(--primary-base)' }}>
            V
          </div>
          <span className="font-semibold text-white tracking-wide text-sm">VELTRO SYSTEM</span>
        </div>

        {/* Navigation */}
        <nav className="flex-1 py-6 px-3 space-y-1 overflow-y-auto">
          <div className="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-gray-500">
            Módulos
          </div>
          {navLinks.map((link) => {
            const isActive = location.pathname.startsWith(link.to);
            return (
              <Link
                key={link.to}
                to={link.to}
                onClick={() => setSidebarOpen(false)}
                className="flex items-center px-3 py-2.5 rounded-md text-sm font-medium transition-all duration-200"
                style={{
                  backgroundColor: isActive ? 'var(--primary-base)' : 'transparent',
                  color: isActive ? '#FFFFFF' : '#9CA3AF',
                  boxShadow: isActive ? '0 1px 3px rgba(0,0,0,0.2)' : 'none',
                }}
                onMouseEnter={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.05)';
                    e.currentTarget.style.color = '#FFFFFF';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!isActive) {
                    e.currentTarget.style.backgroundColor = 'transparent';
                    e.currentTarget.style.color = '#9CA3AF';
                  }
                }}
              >
                <span className="mr-3 text-base opacity-80">{link.icon}</span>
                {link.label}
              </Link>
            );
          })}
        </nav>

        {/* User Context Footer */}
        <div className="p-4" style={{ borderTop: '1px solid rgba(255,255,255,0.1)', backgroundColor: 'rgba(0,0,0,0.2)' }}>
          <div className="flex items-center justify-between mb-3">
            <div>
              <p className="text-sm font-medium text-white">{user?.username}</p>
              <p className="text-xs text-gray-400 mt-0.5">{getRoleLabel(user?.role || '')}</p>
            </div>
            <div className="w-2 h-2 rounded-full" style={{ backgroundColor: 'var(--primary-base)', boxShadow: '0 0 8px var(--primary-base)' }} />
          </div>
          <button
            onClick={handleLogout}
            className="w-full py-2 px-3 rounded text-xs font-semibold text-gray-300 transition-colors"
            style={{ border: '1px solid rgba(255,255,255,0.1)' }}
            onMouseEnter={e => {
              e.currentTarget.style.backgroundColor = 'rgba(255,46,33,0.1)';
              e.currentTarget.style.color = '#FF2E21';
              e.currentTarget.style.borderColor = 'rgba(255,46,33,0.3)';
            }}
            onMouseLeave={e => {
              e.currentTarget.style.backgroundColor = 'transparent';
              e.currentTarget.style.color = '#D1D5DB';
              e.currentTarget.style.borderColor = 'rgba(255,255,255,0.1)';
            }}
          >
            Cerrar Sesión
          </button>
        </div>
      </aside>

      {/* Main Content Wrapper */}
      <div className="flex-1 flex flex-col min-h-screen lg:ml-[260px]">
        {/* Top Desktop Header */}
        <header className="hidden lg:flex items-center justify-between h-16 px-8 z-10 sticky top-0" 
                style={{ 
                  backgroundColor: 'var(--surface-primary)', 
                  borderBottom: 'var(--border-light)',
                  backdropFilter: 'blur(8px)'
                }}>
          <div className="flex items-center text-sm font-medium text-gray-500">
            {/* Breadcrumb structural hint */}
            <span className="opacity-60">Veltro</span>
            <span className="mx-2 opacity-40">/</span>
            <span style={{ color: 'var(--text-primary)' }}>
              {navLinks.find(l => location.pathname.startsWith(l.to))?.label || 'Panel'}
            </span>
          </div>
          
          <div className="flex items-center gap-6">
            {(user?.role === 'ADMIN' || user?.role === 'WAREHOUSE') && (
              <div className="flex items-center gap-2 text-sm">
                <span className="text-gray-500">Sistema:</span>
                <AlertBadge />
              </div>
            )}
            <div className="h-6 w-px bg-gray-200" />
            <div className="text-xs tabular-data font-medium text-gray-500">
              {new Date().toLocaleDateString('es-ES', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
            </div>
          </div>
        </header>

        {/* Content Canvas */}
        <main className="flex-1 p-4 lg:p-8 overflow-auto lg:pt-8 pt-20">
          <div className="max-w-7xl mx-auto">
            <Outlet />
          </div>
        </main>
      </div>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 bg-black/60 z-40 lg:hidden backdrop-blur-sm" 
             onClick={() => setSidebarOpen(false)} />
      )}
    </div>
  );
}
