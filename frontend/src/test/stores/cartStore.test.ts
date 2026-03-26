import { describe, it, expect, beforeEach } from 'vitest';
import { useCartStore } from '../../stores/cartStore';

// Helper to create test products
const createTestProduct = (id: string, name: string, price: string = '10.00') => ({
  id,
  name,
  price,
  barcode: `BARCODE${id}`,
  sku: `SKU${id}`,
  costPrice: (parseFloat(price) / 2).toFixed(2),
  category: { id: 'cat1', name: 'Test Category' },
});

describe('cartStore', () => {
  let store: any;

  beforeEach(() => {
    // Reset store before each test
    useCartStore.setState({ items: [] });
  });

  describe('add', () => {
    it('should add a new product to the cart', () => {
      const product = {
        id: '1',
        name: 'Test Product',
        price: '10.00',
        barcode: 'TEST123',
        sku: 'SKU001',
        costPrice: '5.00',
        category: { id: 'cat1', name: 'Category 1' },
      };

      useCartStore.getState().add(product, 1);
      store = useCartStore.getState();

      expect(store.items).toHaveLength(1);
      expect(store.items[0].productId).toBe('1');
      expect(store.items[0].quantity).toBe(1);
    });

    it('should increment quantity if product already exists', () => {
      const product = createTestProduct('1', 'Test Product');

      useCartStore.getState().add(product, 2);
      useCartStore.getState().add(product, 3);
      store = useCartStore.getState();

      expect(store.items).toHaveLength(1);
      expect(store.items[0].quantity).toBe(5);
    });

    it('should handle multiple different products', () => {
      const product1 = createTestProduct('1', 'Product 1', '10.00');
      const product2 = createTestProduct('2', 'Product 2', '20.00');

      useCartStore.getState().add(product1, 1);
      useCartStore.getState().add(product2, 2);
      store = useCartStore.getState();

      expect(store.items).toHaveLength(2);
      expect(store.getItemCount()).toBe(3);
    });
  });

  describe('remove', () => {
    it('should remove a product from the cart', () => {
      const product = createTestProduct('1', 'Test Product');

      useCartStore.getState().add(product, 1);
      store = useCartStore.getState();
      expect(store.items).toHaveLength(1);

      store.remove('1');
      store = useCartStore.getState();
      expect(store.items).toHaveLength(0);
    });

    it('should not throw if product not found', () => {
      expect(() => useCartStore.getState().remove('nonexistent')).not.toThrow();
    });
  });

  describe('updateQty', () => {
    it('should update product quantity', () => {
      const product = createTestProduct('1', 'Test Product');

      useCartStore.getState().add(product, 1);
      useCartStore.getState().updateQty('1', 5);
      store = useCartStore.getState();

      expect(store.items[0].quantity).toBe(5);
    });

    it('should remove product if quantity is 0 or less', () => {
      const product = createTestProduct('1', 'Test Product');

      useCartStore.getState().add(product, 5);
      useCartStore.getState().updateQty('1', 0);
      store = useCartStore.getState();

      expect(store.items).toHaveLength(0);
    });
  });

  describe('getTotal', () => {
    it('should calculate correct total for single item', () => {
      const product = createTestProduct('1', 'Test Product', '10.50');

      useCartStore.getState().add(product, 2);
      const total = useCartStore.getState().getTotal();

      expect(parseFloat(total)).toBe(21.00);
    });

    it('should calculate correct total for multiple items', () => {
      const product1 = createTestProduct('1', 'Product 1', '10.00');
      const product2 = createTestProduct('2', 'Product 2', '20.00');

      useCartStore.getState().add(product1, 1);
      useCartStore.getState().add(product2, 1);
      const total = useCartStore.getState().getTotal();

      expect(parseFloat(total)).toBe(30.00);
    });

    it('should return 0 for empty cart', () => {
      const total = useCartStore.getState().getTotal();

      expect(total).toBe('0.0000');
    });
  });

  describe('clear', () => {
    it('should clear all items from the cart', () => {
      const product = createTestProduct('1', 'Test Product');

      useCartStore.getState().add(product, 5);
      store = useCartStore.getState();
      expect(store.items).toHaveLength(1);

      store.clear();
      store = useCartStore.getState();
      expect(store.items).toHaveLength(0);
      expect(store.getTotal()).toBe('0.0000');
    });
  });

  describe('getItemCount', () => {
    it('should return correct item count', () => {
      const product1 = createTestProduct('1', 'Product 1', '10.00');
      const product2 = createTestProduct('2', 'Product 2', '20.00');

      useCartStore.getState().add(product1, 3);
      useCartStore.getState().add(product2, 2);

      expect(useCartStore.getState().getItemCount()).toBe(5);
    });
  });

  describe('getSubtotal', () => {
    it('should calculate correct subtotal for item', () => {
      const product = createTestProduct('1', 'Test Product', '15.50');

      useCartStore.getState().add(product, 3);
      store = useCartStore.getState();
      const subtotal = store.getSubtotal(store.items[0]);

      expect(parseFloat(subtotal)).toBe(46.50);
    });
  });
});
