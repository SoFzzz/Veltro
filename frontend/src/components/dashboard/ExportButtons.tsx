import React, { useState } from 'react';
import {
  exportProfitabilityReportPdf,
  exportProfitabilityReportExcel,
} from '../../api/dashboard';

/**
 * ExportButtons - Download reports in PDF or Excel format
 */
export const ExportButtons: React.FC = () => {
  const [exportingPdf, setExportingPdf] = useState(false);
  const [exportingExcel, setExportingExcel] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleExportPdf = async () => {
    setExportingPdf(true);
    setError(null);

    try {
      const response = await exportProfitabilityReportPdf();
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `profitability-report-${new Date().toISOString().split('T')[0]}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError('Failed to export PDF report');
      console.error('PDF export error:', err);
    } finally {
      setExportingPdf(false);
    }
  };

  const handleExportExcel = async () => {
    setExportingExcel(true);
    setError(null);

    try {
      const response = await exportProfitabilityReportExcel();
      const url = window.URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `profitability-report-${new Date().toISOString().split('T')[0]}.xlsx`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError('Failed to export Excel report');
      console.error('Excel export error:', err);
    } finally {
      setExportingExcel(false);
    }
  };

  return (
    <div className="space-y-2">
      {error && (
        <div
          className="p-3 rounded-lg border-2"
          style={{
            backgroundColor: 'rgba(255, 46, 33, 0.1)',
            borderColor: '#FF2E21',
          }}
        >
          <p style={{ color: '#FF2E21', fontSize: '0.875rem' }}>{error}</p>
        </div>
      )}

      <div className="flex gap-3 flex-col sm:flex-row">
        <button
          onClick={handleExportPdf}
          disabled={exportingPdf || exportingExcel}
          className="flex items-center gap-2 px-4 py-2 rounded font-semibold transition flex-1 sm:flex-initial"
          style={{
            backgroundColor: '#FF2E21',
            color: '#FFFFFF',
            opacity: exportingPdf || exportingExcel ? 0.6 : 1,
            cursor: exportingPdf || exportingExcel ? 'not-allowed' : 'pointer',
          }}
          onMouseEnter={(e) => {
            if (!exportingPdf && !exportingExcel) {
              (e.currentTarget as HTMLButtonElement).style.filter = 'brightness(0.9)';
            }
          }}
          onMouseLeave={(e) => {
            (e.currentTarget as HTMLButtonElement).style.filter = 'brightness(1)';
          }}
          aria-label="Export dashboard profitability report as PDF file"
          title="Download PDF report"
        >
          📄
          {exportingPdf ? 'Exporting...' : 'Export PDF'}
        </button>

        <button
          onClick={handleExportExcel}
          disabled={exportingExcel || exportingPdf}
          className="flex items-center gap-2 px-4 py-2 rounded font-semibold transition flex-1 sm:flex-initial border-2"
          style={{
            backgroundColor: '#FFF4E6',
            borderColor: '#FFAC00',
            color: '#038E57',
            opacity: exportingExcel || exportingPdf ? 0.6 : 1,
            cursor: exportingExcel || exportingPdf ? 'not-allowed' : 'pointer',
          }}
          onMouseEnter={(e) => {
            if (!exportingExcel && !exportingPdf) {
              (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#FFAC00';
              (e.currentTarget as HTMLButtonElement).style.color = '#1F2937';
            }
          }}
          onMouseLeave={(e) => {
            (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#FFF4E6';
            (e.currentTarget as HTMLButtonElement).style.color = '#038E57';
          }}
          aria-label="Export dashboard profitability report as Excel spreadsheet"
          title="Download Excel report"
        >
          📊
          {exportingExcel ? 'Exporting...' : 'Export Excel'}
        </button>
      </div>
    </div>
  );
};
