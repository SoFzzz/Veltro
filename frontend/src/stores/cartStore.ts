import { create } from 'zustand';
import type { Product } from '../api/pos';

export interface CartItem {
  productId: string;
  product: Product;
  quantity: number;
}

interface CartStore {
  items: CartItem[];
  add: (product: Product, quantity: number) => void;
  remove: (productId: string) => void;
  updateQty: (productId: string, quantity: number) => void;
  clear: () => void;
  getTotal: () => string;
  getItemCount: () => number;
  getSubtotal: (item: CartItem) => string;
}

export const useCartStore = create<CartStore>((set, get) => ({
  items: [],

  add: (product, quantity) => {
    set((state) => {
      const pid = String(product.id);
      const existingItem = state.items.find((item) => item.productId === pid);

      if (existingItem) {
        return {
          items: state.items.map((item) =>
            item.productId === pid
              ? { ...item, quantity: item.quantity + quantity }
              : item
          ),
        };
      }

      return {
        items: [
          ...state.items,
          {
            productId: pid,
            product,
            quantity,
          },
        ],
      };
    });
  },

  remove: (productId) => {
    set((state) => ({
      items: state.items.filter((item) => item.productId !== productId),
    }));
  },

  updateQty: (productId, quantity) => {
    if (quantity <= 0) {
      get().remove(productId);
      return;
    }

    set((state) => ({
      items: state.items.map((item) =>
        item.productId === productId ? { ...item, quantity } : item
      ),
    }));
  },

  clear: () => {
    set({ items: [] });
  },

  getSubtotal: (item) => {
    const price = parseFloat(item.product.salePrice);
    const subtotal = price * item.quantity;
    return subtotal.toFixed(4);
  },

  getTotal: () => {
    const { items } = get();
    const total = items.reduce((sum, item) => {
      const price = parseFloat(item.product.salePrice);
      const subtotal = price * item.quantity;
      return sum + subtotal;
    }, 0);

    return total.toFixed(4);
  },

  getItemCount: () => {
    const { items } = get();
    return items.reduce((count, item) => count + item.quantity, 0);
  },
}));
