import React, { useState, useEffect } from 'react';
import { getPurchaseOrders } from '../../api/purchasing';
import type { PurchaseOrder } from '../../api/purchasing';
import { OrderList, PurchaseOrderForm } from '../../components/purchasing';

/**
 * PurchaseOrderPage - Main purchase order management page
 * Allows creating new orders and managing existing ones
 */
export const PurchaseOrderPage: React.FC = () => {
  const [orders, setOrders] = useState<PurchaseOrder[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);

  const fetchOrders = async () => {
    setIsLoading(true);
    try {
      // Backend returns List<PurchaseOrderResponse> (not paginated)
      const response = await getPurchaseOrders();
      setOrders(response.data);
    } catch (error) {
      console.error('Failed to fetch purchase orders:', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleOrderCreated = () => {
    setShowForm(false);
    fetchOrders();
  };

  const handleOrderUpdated = () => {
    fetchOrders();
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">
            Ordenes de Compra
          </h1>
          <p className="mt-1 text-[var(--text-secondary)] text-sm">
            Gestiona los pedidos a proveedores y el ingreso de mercaderia
          </p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className={showForm ? 'btn-secondary' : 'btn-primary'}
        >
          {showForm ? 'Cancelar' : '+ Nueva Orden'}
        </button>
      </div>

      {/* Form section */}
      {showForm && (
        <div className="mb-8 card p-6">
          <PurchaseOrderForm onCreated={handleOrderCreated} />
        </div>
      )}

      {/* Orders list */}
      <div className="card overflow-x-auto">
        <OrderList
          orders={orders}
          isLoading={isLoading}
          onOrderUpdated={handleOrderUpdated}
        />
      </div>
    </div>
  );
};
