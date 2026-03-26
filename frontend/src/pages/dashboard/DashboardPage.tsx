import React, { useState, useEffect } from 'react';
import { getDashboard } from '../../api/dashboard';
import type { DashboardResponse } from '../../api/dashboard';
import {
  KPICard,
  LatestSalesTable,
  ExportButtons,
} from '../../components/dashboard';

/**
 * DashboardPage - Main dashboard displaying KPIs and sales overview
 * Shows today's sales, average ticket, stock status, and profit estimation
 */
export const DashboardPage: React.FC = () => {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDashboard = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await getDashboard();
        setData(response.data);
      } catch (err) {
        setError(
          err instanceof Error
            ? err.message
            : 'Failed to load dashboard data'
        );
      } finally {
        setIsLoading(false);
      }
    };

    fetchDashboard();
  }, []);

  return (
    <div className="w-full">
      {/* Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-gray-900">
            Panel de Control
          </h1>
          <p className="mt-1 text-sm text-gray-500 font-medium">
            Resumen de rendimiento y estado del sistema
          </p>
        </div>
        
        {/* Optional Actions here */}
      </div>

      {/* Error */}
      {error && (
        <div
          className="mb-6 p-4 rounded-md border"
          style={{
            backgroundColor: 'rgba(255, 46, 33, 0.05)',
            borderColor: 'rgba(255, 46, 33, 0.2)',
          }}
        >
          <div className="flex items-center gap-2">
            <span className="text-red-500">⚠</span>
            <p style={{ color: '#FF2E21', fontSize: '0.875rem', fontWeight: 500 }}>{error}</p>
          </div>
        </div>
      )}

      {/* Loading */}
      {isLoading && (
        <div className="flex justify-center items-center h-64 border rounded-lg bg-white" style={{ borderColor: 'rgba(31,41,55,0.08)' }}>
          <div className="flex flex-col items-center gap-3">
            <div
              className="animate-spin rounded-full h-8 w-8 border-2"
              style={{
                borderColor: 'rgba(3, 142, 87, 0.2)',
                borderTopColor: '#038E57',
              }}
            ></div>
            <span className="text-sm font-medium text-gray-500 tracking-wide">SINCRONIZANDO DATOS...</span>
          </div>
        </div>
      )}

      {/* Content */}
      {data && (
        <div className="space-y-6">
          {/* Section: Financials */}
          <div className="space-y-3">
            <h2 className="text-xs font-bold uppercase tracking-widest text-gray-400">Métricas Financieras</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <KPICard
                title="Ventas de Hoy"
                value={`$${parseFloat(data.todaySales).toFixed(2)}`}
                icon="💰"
                variant="success"
              />
              <KPICard
                title="Ticket Promedio"
                value={`$${parseFloat(data.averageTicket).toFixed(2)}`}
                icon="🎫"
                variant="default"
              />
              <KPICard
                title="Ganancia Est. Mensual"
                value={`$${parseFloat(data.estimatedMonthlyProfit).toFixed(2)}`}
                icon="📈"
                variant="default"
              />
              <KPICard
                title="Ventas del Dia"
                value={data.todaySalesCount}
                icon="📋"
                variant="default"
              />
            </div>
          </div>

          {/* Section: Inventory Status */}
          <div className="space-y-3">
            <h2 className="text-xs font-bold uppercase tracking-widest text-gray-400">Estado de Inventario</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <KPICard
                title="Productos Sin Stock"
                value={data.outOfStockProducts}
                icon="📦"
                variant="critical"
              />
              <KPICard
                title="Productos Stock Bajo"
                value={data.lowStockAlertCount}
                icon="⚠️"
                variant="warning"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
            {/* Recent Sales Section */}
            <div className="xl:col-span-2 space-y-3">
              <h2 className="text-xs font-bold uppercase tracking-widest text-gray-400">Últimas Transacciones</h2>
              <div className="card flex-1 min-h-0">
                <LatestSalesTable
                  sales={data.recentSales}
                  isLoading={isLoading}
                />
              </div>
            </div>

            {/* Export Section */}
            <div className="space-y-3">
              <h2 className="text-xs font-bold uppercase tracking-widest text-gray-400">Reportes</h2>
              <div className="card p-6">
                <div className="w-10 h-10 rounded-lg flex items-center justify-center mb-4" style={{ backgroundColor: 'rgba(3,142,87,0.1)' }}>
                  <span className="text-xl" style={{ color: '#038E57' }}>📊</span>
                </div>
                <h3 className="text-base font-bold text-gray-900 mb-1">Exportar Datos</h3>
                <p className="text-sm text-gray-500 mb-6">
                  Descarga informes de rentabilidad e inventario en el formato de tu preferencia.
                </p>
                <div className="border-t pt-4" style={{ borderColor: 'rgba(31,41,55,0.08)' }}>
                  <ExportButtons />
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
