import React, { useState, useEffect } from 'react';
import { getAlerts } from '../../api/inventory';
import type { Alert } from '../../api/inventory';
import { AlertList } from '../../components/inventory';
import { useAlertStore } from '../../stores/alertStore';

/**
 * AlertListPage - Display and manage all alerts
 * Allows viewing alerts with pagination and dismissing them
 */
export const AlertListPage: React.FC = () => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [selectedSeverity, setSelectedSeverity] = useState<string | undefined>(undefined);
  const setAllerts = useAlertStore((state) => state.setAlerts);

  useEffect(() => {
    const fetchAlerts = async () => {
      setIsLoading(true);
      try {
        const response = await getAlerts(currentPage, selectedSeverity);
        setAlerts(response.data.content);
        setTotalPages(response.data.totalPages);
        setAllerts(response.data.content);
      } catch (error) {
        console.error('Failed to fetch alerts:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAlerts();
  }, [currentPage, selectedSeverity, setAllerts]);

  const handleSeverityChange = (severity: string | undefined) => {
    setSelectedSeverity(severity);
    setCurrentPage(0);
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">Alertas de Inventario</h1>
        <p className="mt-1 text-[var(--text-secondary)] text-sm">
          Monitorea y gestiona las alertas de stock
        </p>
      </div>

      {/* Filters */}
      <div className="card p-6">
        <label className="block text-sm font-semibold text-[var(--text-primary)] mb-4">
          Filtrar por Severidad
        </label>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => handleSeverityChange(undefined)}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all shadow-sm ${
              selectedSeverity === undefined
                ? 'bg-slate-800 text-white shadow-md'
                : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-50'
            }`}
          >
            Todas
          </button>
          <button
            onClick={() => handleSeverityChange('CRITICAL')}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all shadow-sm flex items-center gap-2 ${
              selectedSeverity === 'CRITICAL'
                ? 'bg-red-600 text-white shadow-md'
                : 'bg-white border border-red-200 text-red-700 hover:bg-red-50'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-current"></span> Críticas
          </button>
          <button
            onClick={() => handleSeverityChange('WARNING')}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all shadow-sm flex items-center gap-2 ${
              selectedSeverity === 'WARNING'
                ? 'bg-orange-500 text-white shadow-md'
                : 'bg-white border border-orange-200 text-orange-700 hover:bg-orange-50'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-current"></span> Advertencias
          </button>
          <button
            onClick={() => handleSeverityChange('INFO')}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-all shadow-sm flex items-center gap-2 ${
              selectedSeverity === 'INFO'
                ? 'bg-blue-600 text-white shadow-md'
                : 'bg-white border border-blue-200 text-blue-700 hover:bg-blue-50'
            }`}
          >
            <span className="w-2 h-2 rounded-full bg-current"></span> Información
          </button>
        </div>
      </div>

      {/* Alert List */}
      <div className="card overflow-hidden">
        <AlertList
          alerts={alerts}
          isLoading={isLoading}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </div>
    </div>
  );
};
