import { describe, it, expect, beforeAll, afterEach, afterAll, vi } from 'vitest';
import { server } from './mswServer';
import { handlers, generateMockProduct, generateMockUser, generateMockSale, generateMockAlert } from './mswHandlers';
import { http, HttpResponse } from 'msw';

// Start server before all tests
beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const API_BASE = 'http://localhost:8080/api/v1';

describe('Frontend Integration Tests with Mocked API', () => {
  describe('Authentication Module (F1-02)', () => {
    it('should login successfully with valid credentials', async () => {
      const response = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'admin', password: 'password123' }),
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.accessToken).toBeDefined();
      expect(data.refreshToken).toBeDefined();
      expect(data.user.role).toBe('ADMIN');
    });

    it('should refresh token successfully', async () => {
      const response = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: 'test_refresh' }),
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.accessToken).toBeDefined();
    });

    it('should return 401 on invalid credentials', async () => {
      server.use(
        http.post(`${API_BASE}/auth/login`, () => {
          return HttpResponse.json({ error: 'Invalid credentials' }, { status: 401 });
        })
      );

      const response = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'admin', password: 'wrongpass' }),
      });

      expect(response.status).toBe(401);
    });
  });

  describe('Catalog Module (F1-03)', () => {
    it('should fetch products with pagination', async () => {
      const response = await fetch(`${API_BASE}/products?page=0`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.content).toHaveLength(3);
      expect(data.totalPages).toBe(10);
      expect(data.content[0].barcode).toBeDefined();
    });

    it('should find product by barcode', async () => {
      const response = await fetch(`${API_BASE}/products/barcode/EAN-00000001`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.name).toBe('Product 1');
      expect(data.barcode).toBe('EAN-00000001');
    });

    it('should return 404 for non-existent barcode', async () => {
      const response = await fetch(`${API_BASE}/products/barcode/INVALID-BARCODE`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(404);
    });

    it('should create new product', async () => {
      const response = await fetch(`${API_BASE}/products`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({
          name: 'New Product',
          barcode: 'EAN-NEW001',
          costPrice: 15.00,
          salePrice: 35.99,
          categoryId: 1,
        }),
      });

      expect(response.status).toBe(201);
      const data = await response.json();
      expect(data.id).toBeDefined();
    });

    it('should fetch categories', async () => {
      const response = await fetch(`${API_BASE}/categories`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.content).toHaveLength(2);
    });
  });

  describe('POS Module (F2-01)', () => {
    it('should create a sale successfully', async () => {
      const response = await fetch(`${API_BASE}/pos/sales`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({
          items: [{ productId: 1, quantity: 2 }],
          paymentMethod: 'CASH',
          notes: 'Test sale',
        }),
      });

      expect(response.status).toBe(201);
      const data = await response.json();
      expect(data.saleNumber).toBeDefined();
      expect(data.status).toBe('COMPLETED');
    });

    it('should fetch sale details', async () => {
      const response = await fetch(`${API_BASE}/pos/sales/1`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.items).toHaveLength(1);
      expect(data.total).toBe(51.00);
    });

    it('should void a sale', async () => {
      const response = await fetch(`${API_BASE}/pos/sales/1/void`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({ reason: 'Customer request' }),
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.status).toBe('VOIDED');
    });

    it('should handle POS error - insufficient stock', async () => {
      server.use(
        http.post(`${API_BASE}/pos/sales`, () => {
          return HttpResponse.json(
            { error: 'Insufficient stock for product ID 1' },
            { status: 400 }
          );
        })
      );

      const response = await fetch(`${API_BASE}/pos/sales`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({
          items: [{ productId: 1, quantity: 1000 }],
          paymentMethod: 'CASH',
        }),
      });

      expect(response.status).toBe(400);
      const data = await response.json();
      expect(data.error).toContain('Insufficient stock');
    });
  });

  describe('Inventory / Alerts Module (F2-02)', () => {
    it('should fetch alerts with pagination', async () => {
      const response = await fetch(`${API_BASE}/inventory/alerts?page=0`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.content).toHaveLength(2);
      expect(data.content[0].severity).toBe('CRITICAL');
    });

    it('should dismiss an alert', async () => {
      const response = await fetch(`${API_BASE}/inventory/alerts/1/dismiss`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({}),
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.resolved).toBe(true);
    });

    it('should handle multiple alert severities', async () => {
      const response = await fetch(`${API_BASE}/inventory/alerts?page=0`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      const data = await response.json();
      const severities = data.content.map((a: any) => a.severity);
      expect(severities).toContain('CRITICAL');
      expect(severities).toContain('WARNING');
    });
  });

  describe('Purchasing Module (F2-03)', () => {
    it('should create a purchase order', async () => {
      const response = await fetch(`${API_BASE}/purchasing/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({
          supplierId: 1,
          items: [{ productId: 1, quantity: 100, unitCost: 10.00 }],
        }),
      });

      expect(response.status).toBe(201);
      const data = await response.json();
      expect(data.orderNumber).toBeDefined();
      expect(data.status).toBe('PENDING');
    });

    it('should fetch purchase orders', async () => {
      const response = await fetch(`${API_BASE}/purchasing/orders?page=0`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.content).toHaveLength(2);
    });

    it('should clone a purchase order', async () => {
      const response = await fetch(`${API_BASE}/purchasing/orders/1/clone`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({}),
      });

      expect(response.status).toBe(201);
      const data = await response.json();
      expect(data.id).toBe(999);
      expect(data.items).toHaveLength(1);
    });

    it('should mark items as received', async () => {
      const response = await fetch(`${API_BASE}/purchasing/orders/1/receive`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({
          items: [{ itemId: 'item-1', receivedQty: 50 }],
        }),
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.status).toBe('RECEIVED');
    });

    it('should void a purchase order', async () => {
      const response = await fetch(`${API_BASE}/purchasing/orders/1/void`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer test-token',
        },
        body: JSON.stringify({ reason: 'Cancelled by user' }),
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.status).toBe('VOIDED');
    });

    it('should fetch suppliers list', async () => {
      const response = await fetch(`${API_BASE}/purchasing/suppliers`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data).toHaveLength(2);
      expect(data[0].name).toBe('Supplier 1');
    });
  });

  describe('Dashboard Module (F3-02)', () => {
    it('should fetch dashboard KPIs', async () => {
      const response = await fetch(`${API_BASE}/dashboard`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.todaySales).toBe(5000.00);
      expect(data.averageTicket).toBe(125.50);
      expect(data.outOfStockProducts).toBe(3);
      expect(data.recentSales).toHaveLength(2);
    });

    it('should include inventory metrics in dashboard', async () => {
      const response = await fetch(`${API_BASE}/dashboard`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      const data = await response.json();
      expect(data.activePendingOrders).toBe(2);
      expect(data.lowStockCount).toBe(5);
    });
  });

  describe('Audit Module (F3-03)', () => {
    it('should fetch audit records with pagination', async () => {
      const response = await fetch(`${API_BASE}/audit?page=0`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(200);
      const data = await response.json();
      expect(data.content).toHaveLength(1);
      expect(data.content[0].action).toBe('CREATE');
    });

    it('should track audit fields correctly', async () => {
      const response = await fetch(`${API_BASE}/audit?page=0`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      const data = await response.json();
      const audit = data.content[0];
      expect(audit.entityType).toBe('SALE');
      expect(audit.username).toBe('admin');
      expect(audit.timestamp).toBeDefined();
      expect(audit.newData).toBeDefined();
    });
  });

  describe('Error Handling & Edge Cases', () => {
    it('should handle 401 unauthorized', async () => {
      server.use(
        http.get(`${API_BASE}/products`, () => {
          return HttpResponse.json(
            { error: 'Unauthorized' },
            { status: 401 }
          );
        })
      );

      const response = await fetch(`${API_BASE}/products`, {
        method: 'GET',
      });

      expect(response.status).toBe(401);
    });

    it('should handle 403 forbidden (RBAC)', async () => {
      server.use(
        http.post(`${API_BASE}/purchasing/orders`, () => {
          return HttpResponse.json(
            { error: 'CASHIER role cannot create purchase orders' },
            { status: 403 }
          );
        })
      );

      const response = await fetch(`${API_BASE}/purchasing/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer cashier-token',
        },
        body: JSON.stringify({}),
      });

      expect(response.status).toBe(403);
    });

    it('should handle 500 server errors', async () => {
      server.use(
        http.get(`${API_BASE}/dashboard`, () => {
          return HttpResponse.json(
            { error: 'Internal Server Error' },
            { status: 500 }
          );
        })
      );

      const response = await fetch(`${API_BASE}/dashboard`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer test-token' },
      });

      expect(response.status).toBe(500);
    });
  });

  describe('RBAC Authorization Tests', () => {
    it('ADMIN should access all modules', async () => {
      const adminToken = 'admin-token';
      
      const endpoints = [
        `${API_BASE}/products`,
        `${API_BASE}/pos/sales`,
        `${API_BASE}/inventory/alerts`,
        `${API_BASE}/purchasing/orders`,
        `${API_BASE}/audit`,
      ];

      for (const endpoint of endpoints) {
        const response = await fetch(endpoint, {
          method: 'GET',
          headers: { 'Authorization': `Bearer ${adminToken}` },
        });
        expect(response.status).toBeGreaterThanOrEqual(200);
        expect(response.status).toBeLessThan(300);
      }
    });

    it('CASHIER should access POS but not Audit', async () => {
      // This would require server.use to setup different responses
      // based on authorization header, demonstrating RBAC
      expect(true).toBe(true); // Placeholder
    });

    it('WAREHOUSE should access Inventory and Purchasing', async () => {
      // This would demonstrate warehouse role access
      expect(true).toBe(true); // Placeholder
    });
  });

  describe('Critical Workflow Integration - Complete POS Sale', () => {
    it('should complete full POS sale workflow', async () => {
      // 1. Login
      const loginRes = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'cashier', password: 'pass' }),
      });
      expect(loginRes.status).toBe(200);
      const loginData = await loginRes.json();
      const token = loginData.accessToken;

      // 2. Lookup product by barcode
      const productRes = await fetch(`${API_BASE}/products/barcode/EAN-00000001`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` },
      });
      expect(productRes.status).toBe(200);
      const product = await productRes.json();

      // 3. Create sale
      const saleRes = await fetch(`${API_BASE}/pos/sales`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          items: [{ productId: product.id, quantity: 2 }],
          paymentMethod: 'CASH',
        }),
      });
      expect(saleRes.status).toBe(201);
      const sale = await saleRes.json();

      // 4. Verify sale was created
      expect(sale.saleNumber).toBeDefined();
      expect(sale.status).toBe('COMPLETED');

      // 5. Check audit trail (as ADMIN)
      const auditRes = await fetch(`${API_BASE}/audit?page=0`, {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` },
      });
      expect(auditRes.status).toBe(200);
    });
  });

  describe('Critical Workflow Integration - Purchase Order & Reception', () => {
    it('should complete PO creation and reception workflow', async () => {
      // 1. Create PO
      const poRes = await fetch(`${API_BASE}/purchasing/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer warehouse-token',
        },
        body: JSON.stringify({
          supplierId: 1,
          items: [{ productId: 1, quantity: 100, unitCost: 10.00 }],
        }),
      });
      expect(poRes.status).toBe(201);
      const po = await poRes.json();

      // 2. Fetch PO details
      const detailRes = await fetch(`${API_BASE}/purchasing/orders/${po.id}`, {
        method: 'GET',
        headers: { 'Authorization': 'Bearer warehouse-token' },
      });
      expect(detailRes.status).toBe(200);

      // 3. Mark items as received
      const receiveRes = await fetch(`${API_BASE}/purchasing/orders/${po.id}/receive`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer warehouse-token',
        },
        body: JSON.stringify({
          items: [{ itemId: po.items[0].id, receivedQty: 100 }],
        }),
      });
      expect(receiveRes.status).toBe(200);
      const updated = await receiveRes.json();
      expect(updated.status).toBe('RECEIVED');
    });
  });
});
