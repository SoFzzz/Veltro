import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ScannerContainer, CartTable, ConfirmModal, SaleReceipt } from '../../components/pos';
import { useCartStore } from '../../stores/cartStore';
import { confirmSale, type SaleResponse, type CreateSaleRequest } from '../../api/pos';

export const POSPage: React.FC = () => {
  const navigate = useNavigate();
  const { items, clear, getTotal, getItemCount } = useCartStore();

  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [saleResponse, setSaleResponse] = useState<SaleResponse | null>(null);
  const [showReceipt, setShowReceipt] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleConfirmSale = async (saleData: CreateSaleRequest) => {
    try {
      setIsProcessing(true);
      setError(null);

      const response = await confirmSale(saleData);
      setSaleResponse(response.data);
      setShowReceipt(true);
      setShowConfirmModal(false);
      clear();
    } catch (err) {
      const errorMsg =
        err instanceof Error ? err.message : 'Error al confirmar la venta';
      setError(errorMsg);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleClearCart = () => {
    if (confirm('¿Estás seguro de que deseas vaciar el carrito?')) {
      clear();
      setError(null);
    }
  };

  const handleReceiptClose = () => {
    setShowReceipt(false);
    setSaleResponse(null);
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">Punto de Venta (POS)</h1>
          <p className="mt-1 text-[var(--text-secondary)] text-sm">
            Escanea o busca productos para agregar al carrito
          </p>
        </div>
        <button
          onClick={() => navigate('/')}
          className="btn-secondary"
        >
          ← Volver
        </button>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-600 shadow-sm">
          <p className="font-semibold text-sm">Error al procesar venta</p>
          <p className="text-sm mt-0.5">{error}</p>
        </div>
      )}

      {/* Main Content */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* Scanner Section (2/3) */}
        <div className="lg:col-span-2">
          <div className="card p-6 h-full">
            <h2 className="text-lg font-bold text-[var(--text-primary)] mb-6 flex items-center gap-2">
              <span className="text-[var(--primary-base)]">&#128269;</span> Agregar Productos
            </h2>
            <ScannerContainer />
          </div>
        </div>

        {/* Cart Section (1/3) */}
        <div className="lg:col-span-1">
          <div className="card overflow-hidden flex flex-col h-full">
            <div className="bg-[var(--surface-tertiary)] border-b border-[var(--border-light)] px-6 py-4">
              <h2 className="text-lg font-bold text-[var(--text-primary)] flex items-center justify-between">
                <span>🛒 Carrito</span>
                <span className="bg-[var(--primary-base)] text-white text-xs px-2.5 py-1 rounded-full">
                  {getItemCount()} items
                </span>
              </h2>
            </div>

            <div className="flex-1 flex flex-col overflow-hidden p-4">
              <CartTable />
            </div>

            {/* Action Buttons */}
            <div className="bg-[var(--surface-inset)] p-5 space-y-3 border-t border-[var(--border-light)]">
              <div className="flex justify-between items-center mb-4 pb-4 border-b border-[var(--border-light)]">
                <span className="text-[var(--text-secondary)] font-medium">Total</span>
                <span className="text-2xl font-bold tabular-data text-[var(--primary-base)]">
                  S/ {parseFloat(getTotal()).toFixed(2)}
                </span>
              </div>
              <button
                onClick={() => setShowConfirmModal(true)}
                disabled={items.length === 0 || isProcessing}
                className="btn-primary w-full py-3 text-base"
              >
                ✓ Confirmar Venta
              </button>

              <button
                onClick={handleClearCart}
                disabled={items.length === 0 || isProcessing}
                className="btn-secondary w-full text-red-600 hover:text-red-700 hover:bg-red-50 border-transparent hover:border-red-200"
              >
                ✕ Vaciar Carrito
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Modals */}
      <ConfirmModal
        isOpen={showConfirmModal}
        onClose={() => setShowConfirmModal(false)}
        onConfirm={handleConfirmSale}
        isLoading={isProcessing}
      />

      <SaleReceipt
        isOpen={showReceipt}
        saleData={saleResponse}
        onClose={handleReceiptClose}
      />
    </div>
  );
};
