import React, { useState } from 'react';
import type { PurchaseOrder } from '../../api/purchasing';
import {
  clonePurchaseOrder,
  voidPurchaseOrder,
} from '../../api/purchasing';
import { StateVisualizer } from './StateVisualizer';
import { ReceptionFlow } from './ReceptionFlow';

interface OrderListProps {
  orders: PurchaseOrder[];
  isLoading: boolean;
  onOrderUpdated?: () => void;
}

/**
 * OrderList - Display list of purchase orders
 * Allows cloning, voiding, and receiving items
 */
export const OrderList: React.FC<OrderListProps> = ({
  orders,
  isLoading,
  onOrderUpdated,
}) => {
  const [expandedOrder, setExpandedOrder] = useState<number | null>(null);
  const [receivingOrder, setReceivingOrder] = useState<number | null>(null);
  const [voidingOrder, setVoidingOrder] = useState<number | null>(null);
  const [operatingOrderId, setOperatingOrderId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleClone = async (orderId: number) => {
    setOperatingOrderId(orderId);
    setError(null);
    try {
      await clonePurchaseOrder(orderId);
      onOrderUpdated?.();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || 'Error al clonar la orden.';
      setError(msg);
    } finally {
      setOperatingOrderId(null);
    }
  };

  const handleVoid = async (orderId: number) => {
    setOperatingOrderId(orderId);
    setError(null);
    try {
      // Backend voidOrder accepts no request body
      await voidPurchaseOrder(orderId);
      setVoidingOrder(null);
      onOrderUpdated?.();
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || 'Error al anular la orden.';
      setError(msg);
      setVoidingOrder(null); // Dismiss the confirmation dialog on error
    } finally {
      setOperatingOrderId(null);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div
          className="animate-spin rounded-full h-8 w-8 border-2"
          style={{
            borderColor: 'rgba(3, 142, 87, 0.2)',
            borderTopColor: '#038E57',
          }}
        ></div>
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="text-center py-12">
        <svg
          className="mx-auto h-12 w-12"
          style={{ color: '#9CA3AF' }}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
        <h3 className="mt-2 text-lg font-medium" style={{ color: '#1F2937' }}>
          No purchase orders
        </h3>
        <p className="mt-1 text-sm" style={{ color: '#6B7280' }}>
          Create your first purchase order to get started
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Error message */}
      {error && (
        <div
          className="p-3 rounded-lg border-2 flex items-center justify-between"
          style={{
            backgroundColor: 'rgba(255, 46, 33, 0.1)',
            borderColor: '#FF2E21',
          }}
        >
          <p style={{ color: '#FF2E21', fontSize: '0.875rem' }}>{error}</p>
          <button
            onClick={() => setError(null)}
            className="ml-2 shrink-0"
            style={{ color: '#FF2E21' }}
          >
            ✕
          </button>
        </div>
      )}

      {/* Order items */}
      <div
        className="border-2 rounded-lg"
        style={{ borderColor: '#E8E3DB' }}
      >
        {orders.map((order, idx) => (
          <div
            key={order.id}
            className="p-4"
            style={{
              backgroundColor: idx % 2 === 0 ? '#FFFFFF' : '#F9F7F2',
              borderBottom: idx < orders.length - 1 ? '1px solid #E8E3DB' : 'none',
            }}
          >
            {/* Header */}
            <div className="flex items-center justify-between mb-3">
              <div className="flex-1">
                <div className="flex items-center gap-3">
                  <h3 className="font-semibold" style={{ color: '#1F2937' }}>
                    {order.orderNumber}
                  </h3>
                  <StateVisualizer status={order.status} size="sm" />
                </div>
                <p className="text-sm mt-1" style={{ color: '#6B7280' }}>
                  Supplier: {order.supplierName}
                </p>
              </div>
              <div className="text-right">
                <p
                  className="font-semibold"
                  style={{
                    color: '#038E57',
                    fontVariantNumeric: 'tabular-nums',
                  }}
                >
                  ${order.total}
                </p>
                <p className="text-xs" style={{ color: '#9CA3AF' }}>
                  {order.auditInfo?.createdAt
                    ? new Date(order.auditInfo.createdAt).toLocaleDateString()
                    : ''}
                </p>
              </div>
            </div>

            {/* Action buttons */}
            <div className="flex flex-wrap gap-2 mb-3">
              <button
                onClick={() =>
                  setExpandedOrder(
                    expandedOrder === order.id ? null : order.id
                  )
                }
                className="text-sm px-3 py-1 rounded transition border-2"
                style={{
                  backgroundColor: '#FFF4E6',
                  borderColor: '#FFAC00',
                  color: '#038E57',
                }}
                onMouseEnter={(e) => {
                  (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#FFAC00';
                  (e.currentTarget as HTMLButtonElement).style.color = '#1F2937';
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#FFF4E6';
                  (e.currentTarget as HTMLButtonElement).style.color = '#038E57';
                }}
              >
                {expandedOrder === order.id ? 'Hide Items' : 'View Items'}
              </button>

              {order.status !== 'VOIDED' && (
                <>
                  {(order.status === 'PENDING' || order.status === 'PARTIAL') && (
                    <button
                      onClick={() => setReceivingOrder(order.id)}
                      disabled={operatingOrderId === order.id}
                      className="text-sm px-3 py-1 rounded transition"
                      style={{
                        backgroundColor: '#038E57',
                        color: '#FFFFFF',
                        opacity: operatingOrderId === order.id ? 0.6 : 1,
                        cursor: operatingOrderId === order.id ? 'not-allowed' : 'pointer',
                      }}
                      onMouseEnter={(e) => {
                        if (operatingOrderId !== order.id) {
                          (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#10A96D';
                        }
                      }}
                      onMouseLeave={(e) => {
                        (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#038E57';
                      }}
                    >
                      Receive
                    </button>
                  )}
                  <button
                    onClick={() => handleClone(order.id)}
                    disabled={operatingOrderId === order.id}
                    className="text-sm px-3 py-1 rounded transition"
                    style={{
                      backgroundColor: '#038E57',
                      color: '#FFFFFF',
                      opacity: operatingOrderId === order.id ? 0.6 : 1,
                      cursor: operatingOrderId === order.id ? 'not-allowed' : 'pointer',
                    }}
                    onMouseEnter={(e) => {
                      if (operatingOrderId !== order.id) {
                        (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#10A96D';
                      }
                    }}
                    onMouseLeave={(e) => {
                      (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#038E57';
                    }}
                  >
                    Clone
                  </button>
                  {/* Void only for PENDING and PARTIAL — RECEIVED orders cannot be voided */}
                  {(order.status === 'PENDING' || order.status === 'PARTIAL') && (
                    <button
                      onClick={() => setVoidingOrder(order.id)}
                      className="text-sm px-3 py-1 rounded transition"
                      style={{
                        backgroundColor: '#FF2E21',
                        color: '#FFFFFF',
                      }}
                      onMouseEnter={(e) => {
                        (e.currentTarget as HTMLButtonElement).style.filter = 'brightness(0.9)';
                      }}
                      onMouseLeave={(e) => {
                        (e.currentTarget as HTMLButtonElement).style.filter = 'brightness(1)';
                      }}
                    >
                      Void
                    </button>
                  )}
                </>
              )}
            </div>

            {/* Expanded items view — uses `details` not `items` */}
            {expandedOrder === order.id && (
              <div
                className="mt-3 pt-3 border-t overflow-x-auto"
                style={{ borderColor: '#E8E3DB' }}
              >
                <table className="min-w-full text-sm">
                  <thead style={{ backgroundColor: '#F9F7F2' }}>
                    <tr>
                      <th
                        className="px-3 py-2 text-left font-medium"
                        style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
                      >
                        Product
                      </th>
                      <th
                        className="px-3 py-2 text-right font-medium"
                        style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
                      >
                        Qty
                      </th>
                      <th
                        className="px-3 py-2 text-right font-medium"
                        style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
                      >
                        Unit Cost
                      </th>
                      <th
                        className="px-3 py-2 text-right font-medium"
                        style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
                      >
                        Received
                      </th>
                      <th
                        className="px-3 py-2 text-right font-medium"
                        style={{ color: '#4B5563', borderBottom: '1px solid #E8E3DB' }}
                      >
                        Subtotal
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {order.details.map((detail, detailIdx) => (
                      <tr
                        key={detail.id}
                        style={{
                          backgroundColor: detailIdx % 2 === 0 ? '#FFFFFF' : '#F9F7F2',
                          borderBottom: '1px solid #E8E3DB',
                        }}
                      >
                        <td className="px-3 py-2" style={{ color: '#1F2937' }}>
                          {detail.productName}
                        </td>
                        <td
                          className="px-3 py-2 text-right"
                          style={{
                            color: '#1F2937',
                            fontVariantNumeric: 'tabular-nums',
                          }}
                        >
                          {detail.requestedQuantity}
                        </td>
                        <td
                          className="px-3 py-2 text-right"
                          style={{
                            color: '#038E57',
                            fontVariantNumeric: 'tabular-nums',
                          }}
                        >
                          ${detail.unitCost}
                        </td>
                        <td
                          className="px-3 py-2 text-right"
                          style={{
                            color: '#1F2937',
                            fontVariantNumeric: 'tabular-nums',
                          }}
                        >
                          {detail.receivedQuantity}
                          {detail.receivedQuantity < detail.requestedQuantity && (
                            <span
                              className="ml-1 font-medium"
                              style={{ color: '#FFAC00' }}
                            >
                              (pending)
                            </span>
                          )}
                        </td>
                        <td
                          className="px-3 py-2 text-right font-medium"
                          style={{
                            color: '#038E57',
                            fontVariantNumeric: 'tabular-nums',
                          }}
                        >
                          ${detail.subtotal}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Void confirmation dialog */}
            {voidingOrder === order.id && (
              <div
                className="mt-3 p-3 rounded-lg border-2"
                style={{
                  backgroundColor: 'rgba(255, 46, 33, 0.1)',
                  borderColor: '#FF2E21',
                }}
              >
                <p
                  className="text-sm font-medium mb-3"
                  style={{ color: '#1F2937' }}
                >
                  Are you sure you want to void this order?
                </p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setVoidingOrder(null)}
                    className="text-sm px-3 py-1 rounded transition border-2"
                    style={{
                      backgroundColor: '#FFF4E6',
                      borderColor: '#FFAC00',
                      color: '#038E57',
                    }}
                    onMouseEnter={(e) => {
                      (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#FFAC00';
                      (e.currentTarget as HTMLButtonElement).style.color = '#1F2937';
                    }}
                    onMouseLeave={(e) => {
                      (e.currentTarget as HTMLButtonElement).style.backgroundColor = '#FFF4E6';
                      (e.currentTarget as HTMLButtonElement).style.color = '#038E57';
                    }}
                  >
                    Cancel
                  </button>
                  <button
                    onClick={() => handleVoid(order.id)}
                    disabled={operatingOrderId === order.id}
                    className="text-sm px-3 py-1 rounded transition"
                    style={{
                      backgroundColor: '#FF2E21',
                      color: '#FFFFFF',
                      opacity: operatingOrderId === order.id ? 0.6 : 1,
                      cursor: operatingOrderId === order.id ? 'not-allowed' : 'pointer',
                    }}
                    onMouseEnter={(e) => {
                      if (operatingOrderId !== order.id) {
                        (e.currentTarget as HTMLButtonElement).style.filter = 'brightness(0.9)';
                      }
                    }}
                    onMouseLeave={(e) => {
                      (e.currentTarget as HTMLButtonElement).style.filter = 'brightness(1)';
                    }}
                  >
                    Confirm Void
                  </button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Reception modal */}
      {receivingOrder && (
        <ReceptionFlow
          orderId={receivingOrder}
          onReceived={() => {
            setReceivingOrder(null);
            onOrderUpdated?.();
          }}
          onClose={() => setReceivingOrder(null)}
        />
      )}
    </div>
  );
};
