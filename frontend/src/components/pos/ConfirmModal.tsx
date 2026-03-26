import React, { useState, useRef } from 'react';
import { useCartStore } from '../../stores/cartStore';
import type { CreateSaleRequest } from '../../api/pos';
import { useFocusTrap } from '../../hooks/useFocusTrap';

interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (saleData: any) => void;
  isLoading?: boolean;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  onClose,
  onConfirm,
  isLoading = false,
}) => {
  const modalRef = useRef<HTMLDivElement>(null);
  const { items, getTotal } = useCartStore();
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'CARD' | 'YAPE' | 'PLIN'>(
    'CASH'
  );
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  // Use focus trap hook for accessibility
  useFocusTrap(modalRef, onClose, isOpen);

  const handleConfirm = async () => {
    try {
      setError(null);

      if (items.length === 0) {
        setError('El carrito está vacío');
        return;
      }

      const saleData: CreateSaleRequest = {
        items: items.map((item) => ({
          productId: item.productId,
          quantity: item.quantity,
        })),
        paymentMethod,
        notes: notes || undefined,
      };

      await onConfirm(saleData);
    } catch (err) {
      const errorMsg =
        err instanceof Error ? err.message : 'Error al confirmar venta';
      setError(errorMsg);
    }
  };

  if (!isOpen) return null;

  const total = getTotal();

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" role="presentation">
      <div 
        ref={modalRef}
        className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 max-h-96 overflow-y-auto"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-modal-title"
        aria-describedby="confirm-modal-description"
        tabIndex={-1}
      >
        {/* Header */}
        <div className="bg-blue-600 text-white px-6 py-4">
          <h2 id="confirm-modal-title" className="text-xl font-bold">Confirmar Venta</h2>
        </div>

        {/* Content */}
        <div className="p-6 space-y-4" id="confirm-modal-description">
          {/* Items Summary */}
          <div className="bg-gray-50 p-4 rounded border">
            <h3 className="font-semibold text-sm mb-2">Items a vender:</h3>
            <ul className="space-y-1 text-sm text-gray-700" aria-label="Items in cart">
              {items.map((item) => (
                <li key={item.productId} className="flex justify-between">
                  <span>{item.product.name}</span>
                  <span className="font-medium" aria-label={`${item.quantity} items at $${parseFloat(item.product.salePrice).toFixed(2)} each`}>
                    {item.quantity}x ${parseFloat(item.product.salePrice).toFixed(2)}
                  </span>
                </li>
              ))}
            </ul>
          </div>

          {/* Total */}
          <div className="bg-green-50 p-4 rounded border-2 border-green-300">
            <div className="flex justify-between items-center">
              <span className="text-lg font-bold">Total:</span>
              <span className="text-2xl font-bold text-green-600" aria-label={`Sale total: $${parseFloat(total).toFixed(2)}`}>
                ${parseFloat(total).toFixed(2)}
              </span>
            </div>
          </div>

          {/* Payment Method */}
          <div>
            <label htmlFor="payment-method" className="block text-sm font-medium text-gray-700 mb-2">
              Método de pago
            </label>
            <select
              id="payment-method"
              value={paymentMethod}
              onChange={(e) =>
                setPaymentMethod(e.target.value as 'CASH' | 'CARD' | 'YAPE' | 'PLIN')
              }
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:border-blue-500"
              aria-label="Select payment method for transaction"
              title="Cash, Card, Yape, or Plin"
            >
              <option value="CASH">Efectivo</option>
              <option value="CARD">Tarjeta</option>
              <option value="YAPE">Yape</option>
              <option value="PLIN">Plin</option>
            </select>
          </div>

          {/* Notes */}
          <div>
            <label htmlFor="sale-notes" className="block text-sm font-medium text-gray-700 mb-2">
              Notas (opcional)
            </label>
            <textarea
              id="sale-notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Ej: Cliente solicitó embalaje especial"
              className="w-full px-3 py-2 border border-gray-300 rounded text-sm focus:outline-none focus:border-blue-500 resize-none"
              rows={2}
              aria-label="Add optional notes for this sale"
              title="Enter any special notes or instructions for this transaction"
            />
          </div>

          {/* Error Message */}
          {error && (
            <div className="p-3 bg-red-100 border border-red-400 rounded text-red-700 text-sm" role="alert" aria-live="assertive">
              {error}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="bg-gray-50 px-6 py-4 flex space-x-3">
          <button
            onClick={onClose}
            disabled={isLoading}
            className="flex-1 px-4 py-2 bg-gray-300 text-gray-800 rounded hover:bg-gray-400 transition font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label="Cancel sale confirmation and close dialog"
            title="Close this dialog (Escape key)"
          >
            Cancelar
          </button>
          <button
            onClick={handleConfirm}
            disabled={isLoading || items.length === 0}
            className="flex-1 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition font-medium disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label={`Confirm and process sale of $${parseFloat(total).toFixed(2)}`}
            title="Complete this transaction"
          >
            {isLoading ? 'Procesando...' : 'Confirmar'}
          </button>
        </div>
      </div>
    </div>
  );
};
