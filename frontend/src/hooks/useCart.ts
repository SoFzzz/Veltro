import { useCallback } from 'react';
import { useCartStore, type CartItem } from '../stores/cartStore';

export const useCart = () => {
  const store = useCartStore();

  const add = useCallback((product: any, quantity: number) => {
    if (quantity > 0) {
      store.add(product, quantity);
    }
  }, [store]);

  const remove = useCallback((productId: string) => {
    store.remove(productId);
  }, [store]);

  const updateQuantity = useCallback((productId: string, quantity: number) => {
    if (quantity > 0) {
      store.updateQty(productId, quantity);
    } else {
      remove(productId);
    }
  }, [store, remove]);

  const clear = useCallback(() => {
    store.clear();
  }, [store]);

  const getTotal = useCallback((): number => {
    return parseFloat(store.getTotal());
  }, [store]);

  const getSubtotal = useCallback((item: CartItem): number => {
    return parseFloat(store.getSubtotal(item));
  }, [store]);

  const getItemCount = useCallback((): number => {
    return store.getItemCount();
  }, [store]);

  return {
    items: store.items,
    add,
    remove,
    updateQuantity,
    clear,
    getTotal,
    getSubtotal,
    getItemCount,
  };
};
