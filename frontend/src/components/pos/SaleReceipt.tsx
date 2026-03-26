import React from 'react';
import type { SaleResponse } from '../../api/pos';

interface SaleReceiptProps {
  isOpen: boolean;
  saleData: SaleResponse | null;
  onClose: () => void;
}

export const SaleReceipt: React.FC<SaleReceiptProps> = ({
  isOpen,
  saleData,
  onClose,
}) => {
  if (!isOpen || !saleData) return null;

  const formatDate = (dateString: string | null) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString('es-ES', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const paymentMethodLabel: Record<string, string> = {
    CASH: 'Efectivo',
    CARD: 'Tarjeta',
    YAPE: 'Yape',
    PLIN: 'Plin',
  };

  const itemCount = saleData.details?.reduce((sum, d) => sum + d.quantity, 0) ?? 0;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4 overflow-y-auto max-h-96">
        {/* Header */}
        <div className="bg-green-600 text-white px-6 py-4 text-center">
          <div className="text-3xl mb-2">&#10003;</div>
          <h2 className="text-xl font-bold">Venta Confirmada!</h2>
        </div>

        {/* Receipt Content */}
        <div className="p-6 space-y-4 text-sm">
          {/* Sale Number */}
          <div className="text-center pb-3 border-b">
            <p className="text-2xl font-bold text-gray-900">
              {saleData.saleNumber}
            </p>
            <p className="text-xs text-gray-500 mt-1">
              {formatDate(saleData.completedAt)}
            </p>
          </div>

          {/* Items */}
          <div className="space-y-2">
            <h3 className="font-bold text-gray-900">Articulos:</h3>
            {saleData.details?.map((item, index) => (
              <div key={index} className="flex justify-between text-xs">
                <div>
                  <p className="font-medium">{item.productName}</p>
                  <p className="text-gray-500">
                    {item.quantity}x ${parseFloat(item.unitPrice).toFixed(2)}
                  </p>
                </div>
                <p className="font-semibold">
                  ${parseFloat(item.subtotal).toFixed(2)}
                </p>
              </div>
            ))}
          </div>

          {/* Summary */}
          <div className="border-t pt-3 space-y-2">
            <div className="flex justify-between">
              <span className="text-gray-600">Cantidad:</span>
              <span className="font-semibold">{itemCount} items</span>
            </div>
            <div className="flex justify-between text-base font-bold text-green-600 border-t pt-2">
              <span>Total:</span>
              <span>${parseFloat(saleData.total).toFixed(2)}</span>
            </div>
            {saleData.amountReceived && (
              <>
                <div className="flex justify-between text-xs">
                  <span className="text-gray-600">Recibido:</span>
                  <span>${parseFloat(saleData.amountReceived).toFixed(2)}</span>
                </div>
                {saleData.change && (
                  <div className="flex justify-between text-xs">
                    <span className="text-gray-600">Cambio:</span>
                    <span>${parseFloat(saleData.change).toFixed(2)}</span>
                  </div>
                )}
              </>
            )}
          </div>

          {/* Payment Method */}
          <div className="bg-blue-50 p-3 rounded text-center">
            <p className="text-xs text-gray-600 mb-1">Metodo de pago:</p>
            <p className="font-semibold text-gray-900">
              {saleData.paymentMethod ? paymentMethodLabel[saleData.paymentMethod] || saleData.paymentMethod : 'N/A'}
            </p>
          </div>

          {/* Footer Message */}
          <div className="bg-yellow-50 p-3 rounded text-center text-xs text-gray-700 border border-yellow-200">
            <p>
              Conserve este recibo para referencias futuras y devoluciones
            </p>
          </div>
        </div>

        {/* Footer */}
        <div className="bg-gray-50 px-6 py-4">
          <button
            onClick={onClose}
            className="w-full px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition font-medium"
          >
            Cerrar Recibo
          </button>
        </div>
      </div>
    </div>
  );
};
