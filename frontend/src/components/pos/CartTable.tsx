import React from 'react';
import { useCartStore, type CartItem } from '../../stores/cartStore';

interface CartTableProps {
  onQuantityChange?: (productId: string, quantity: number) => void;
}

export const CartTable: React.FC<CartTableProps> = ({ onQuantityChange }) => {
  const { items, remove, updateQty, getTotal, getSubtotal, getItemCount } =
    useCartStore();

  const handleQuantityChange = (productId: string, value: string) => {
    const qty = parseInt(value, 10);
    if (qty > 0) {
      updateQty(productId, qty);
      onQuantityChange?.(productId, qty);
    }
  };

  const handleRemove = (productId: string) => {
    remove(productId);
  };

  if (items.length === 0) {
    return (
      <div className="p-8 rounded-lg border-2 border-dashed text-center" style={{ backgroundColor: '#F9F7F2', borderColor: '#E8E3DB' }}>
        <p className="text-lg font-medium" style={{ color: '#1F2937' }}>
          Carrito vacío
        </p>
        <p className="text-sm mt-2" style={{ color: '#6B7280' }} aria-label="Instructions to add products">
          Escanea productos para agregarlos al carrito
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full rounded-lg border" style={{ backgroundColor: '#FFFFFF', borderColor: '#E8E3DB' }}>
      {/* Table */}
      <div className="flex-1 overflow-x-auto overflow-y-auto">
        <table className="w-full" aria-label="Shopping cart items" style={{ borderCollapse: 'collapse' }}>
          <thead className="sticky top-0" style={{ backgroundColor: '#F9F7F2', borderBottom: '1px solid #E8E3DB' }}>
            <tr>
              <th className="px-4 py-3 text-left text-sm font-semibold" scope="col" style={{ color: '#1F2937' }}>
                Producto
              </th>
              <th className="px-4 py-3 text-center text-sm font-semibold" scope="col" style={{ color: '#1F2937' }}>
                Cant.
              </th>
              <th className="px-4 py-3 text-right text-sm font-semibold" scope="col" style={{ color: '#1F2937' }}>
                Precio
              </th>
              <th className="px-4 py-3 text-right text-sm font-semibold" scope="col" style={{ color: '#1F2937' }}>
                Subtotal
              </th>
              <th className="px-4 py-3 text-center text-sm font-semibold" scope="col" style={{ color: '#1F2937' }}>
                Acción
              </th>
            </tr>
          </thead>
          <tbody>
            {items.map((item: CartItem) => (
              <tr 
                key={item.productId} 
                className="transition"
                style={{ borderBottom: '1px solid #E8E3DB' }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = '#F9F7F2';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'transparent';
                }}
              >
                <td className="px-4 py-3">
                  <div className="flex flex-col">
                    <span className="font-medium text-sm" style={{ color: '#1F2937' }}>
                      {item.product.name}
                    </span>
                    <span className="text-xs" style={{ color: '#9CA3AF' }} aria-label={`SKU: ${item.product.sku}`}>
                      SKU: {item.product.sku}
                    </span>
                  </div>
                </td>
                <td className="px-4 py-3">
                  <input
                    type="number"
                    min="1"
                    value={item.quantity}
                    onChange={(e) =>
                      handleQuantityChange(item.productId, e.target.value)
                    }
                    className="w-16 text-center text-sm rounded px-2 py-1 border"
                    style={{
                      backgroundColor: '#FFFFFF',
                      borderColor: '#E8E3DB',
                      color: '#1F2937',
                    }}
                    aria-label={`Quantity for ${item.product.name}`}
                    title={`Change quantity for ${item.product.name}`}
                    onFocus={(e) => {
                      e.currentTarget.style.borderColor = '#038E57';
                      e.currentTarget.style.boxShadow = 'inset 0 0 0 2px rgba(3, 142, 87, 0.1)';
                    }}
                    onBlur={(e) => {
                      e.currentTarget.style.borderColor = '#E8E3DB';
                      e.currentTarget.style.boxShadow = 'none';
                    }}
                  />
                </td>
                <td className="px-4 py-3 text-right text-sm font-medium" style={{ color: '#1F2937', fontVariantNumeric: 'tabular-nums' }} aria-label={`Unit price: $${parseFloat(item.product.salePrice).toFixed(2)}`}>
                  ${parseFloat(item.product.salePrice).toFixed(2)}
                </td>
                <td className="px-4 py-3 text-right font-semibold text-sm" style={{ color: '#038E57', fontVariantNumeric: 'tabular-nums' }} aria-label={`Subtotal: $${parseFloat(getSubtotal(item)).toFixed(2)}`}>
                  ${parseFloat(getSubtotal(item)).toFixed(2)}
                </td>
                <td className="px-4 py-3 text-center">
                  <button
                    onClick={() => handleRemove(item.productId)}
                    className="px-3 py-1 text-sm font-medium rounded transition"
                    style={{
                      backgroundColor: '#FF2E21',
                      color: '#FFFFFF',
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.filter = 'brightness(0.9)';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.filter = 'brightness(1)';
                    }}
                    aria-label={`Remove ${item.product.name} from cart`}
                    title={`Delete ${item.product.name}`}
                  >
                    ✕
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Summary */}
      <div className="border-t p-4 space-y-3" style={{ backgroundColor: '#F9F7F2', borderColor: '#E8E3DB' }}>
        <div className="flex justify-between text-sm">
          <span style={{ color: '#6B7280' }}>Items:</span>
          <span className="font-semibold" style={{ color: '#1F2937', fontVariantNumeric: 'tabular-nums' }} aria-label={`${getItemCount()} items in cart`}>
            {getItemCount()}
          </span>
        </div>
        <div className="flex justify-between pt-3 border-t" style={{ borderColor: '#E8E3DB' }}>
          <span className="font-semibold" style={{ color: '#1F2937' }}>
            Total:
          </span>
          <span className="text-lg font-bold" style={{ color: '#038E57', fontVariantNumeric: 'tabular-nums' }} aria-label={`Cart total: $${parseFloat(getTotal()).toFixed(2)}`}>
            ${parseFloat(getTotal()).toFixed(2)}
          </span>
        </div>
      </div>
    </div>
  );
};
