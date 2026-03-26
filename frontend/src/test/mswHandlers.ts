import { http, HttpResponse } from 'msw';

const API_BASE = 'http://localhost:8080/api/v1';

// Mock JWT tokens
const MOCK_ACCESS_TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFkbWluIFVzZXIiLCJpYXQiOjE1MTYyMzkwMjIsInJvbGUiOiJBRE1JTiJ9.test';
const MOCK_REFRESH_TOKEN = 'refresh_token_test_1234567890';

// Mock data generators
export const generateMockUser = (role = 'ADMIN') => ({
  id: 1,
  username: 'admin',
  email: 'admin@veltro.com',
  role,
  active: true,
  createdAt: new Date().toISOString(),
});

export const generateMockProduct = (id = 1) => ({
  id,
  name: `Product ${id}`,
  barcode: `EAN-${String(id).padStart(8, '0')}`,
  sku: `SKU-${id}`,
  description: `Test product ${id}`,
  costPrice: 10.00,
  salePrice: 25.50,
  categoryId: 1,
  active: true,
  createdAt: new Date().toISOString(),
});

export const generateMockCategory = (id = 1) => ({
  id,
  name: `Category ${id}`,
  description: `Test category ${id}`,
  parentCategoryId: null,
  active: true,
  createdAt: new Date().toISOString(),
});

export const generateMockAlert = (id = 1) => ({
  id,
  productId: 1,
  type: 'OUT_OF_STOCK',
  severity: 'CRITICAL',
  message: 'Product out of stock',
  createdAt: new Date().toISOString(),
  resolved: false,
});

export const generateMockSale = (id = 1) => ({
  id,
  saleNumber: `SALE-${String(id).padStart(6, '0')}`,
  items: [
    {
      productId: 1,
      productName: 'Product 1',
      quantity: 2,
      unitPrice: 25.50,
      subtotal: 51.00,
    },
  ],
  total: 51.00,
  paymentMethod: 'CASH',
  status: 'COMPLETED',
  createdAt: new Date().toISOString(),
});

export const generateMockPurchaseOrder = (id = 1) => ({
  id,
  orderNumber: `PO-${String(id).padStart(6, '0')}`,
  supplierId: 1,
  items: [
    {
      id: `item-${id}`,
      productId: 1,
      productName: 'Product 1',
      quantity: 100,
      unitCost: 10.00,
      receivedQty: 0,
      subtotal: 1000.00,
    },
  ],
  status: 'PENDING',
  createdAt: new Date().toISOString(),
  totalCost: 1000.00,
});

// MSW Handlers
export const handlers = [
  // ========== AUTHENTICATION ==========
  http.post(`${API_BASE}/auth/login`, () => {
    return HttpResponse.json(
      {
        accessToken: MOCK_ACCESS_TOKEN,
        refreshToken: MOCK_REFRESH_TOKEN,
        user: generateMockUser('ADMIN'),
      },
      { status: 200 }
    );
  }),

  http.post(`${API_BASE}/auth/refresh`, () => {
    return HttpResponse.json(
      {
        accessToken: MOCK_ACCESS_TOKEN,
        refreshToken: MOCK_REFRESH_TOKEN,
      },
      { status: 200 }
    );
  }),

  // ========== CATALOG ==========
  http.get(`${API_BASE}/products`, ({ request }) => {
    const url = new URL(request.url);
    const page = url.searchParams.get('page') || '0';
    return HttpResponse.json(
      {
        content: [generateMockProduct(1), generateMockProduct(2), generateMockProduct(3)],
        totalElements: 100,
        totalPages: 10,
        currentPage: parseInt(page),
        pageSize: 10,
      },
      { status: 200 }
    );
  }),

  http.get(`${API_BASE}/products/barcode/:barcode`, ({ params }) => {
    if (params.barcode === 'EAN-00000001') {
      return HttpResponse.json(generateMockProduct(1), { status: 200 });
    }
    return HttpResponse.json(
      { error: 'Product not found' },
      { status: 404 }
    );
  }),

  http.post(`${API_BASE}/products`, async ({ request }) => {
    return HttpResponse.json(generateMockProduct(1), { status: 201 });
  }),

  http.put(`${API_BASE}/products/:id`, () => {
    return HttpResponse.json(generateMockProduct(1), { status: 200 });
  }),

  http.get(`${API_BASE}/categories`, () => {
    return HttpResponse.json(
      {
        content: [generateMockCategory(1), generateMockCategory(2)],
      },
      { status: 200 }
    );
  }),

  http.post(`${API_BASE}/categories`, () => {
    return HttpResponse.json(generateMockCategory(1), { status: 201 });
  }),

  // ========== POS / SALES ==========
  http.get(`${API_BASE}/pos/sales`, ({ request }) => {
    const url = new URL(request.url);
    const page = url.searchParams.get('page') || '0';
    return HttpResponse.json(
      {
        content: [generateMockSale(1), generateMockSale(2)],
        totalElements: 50,
        totalPages: 5,
        currentPage: parseInt(page),
        pageSize: 10,
      },
      { status: 200 }
    );
  }),

  http.post(`${API_BASE}/pos/sales`, async ({ request }) => {
    return HttpResponse.json(
      {
        id: 1,
        saleNumber: 'SALE-000001',
        total: 51.00,
        status: 'COMPLETED',
        createdAt: new Date().toISOString(),
      },
      { status: 201 }
    );
  }),

  http.get(`${API_BASE}/pos/sales/:id`, () => {
    return HttpResponse.json(generateMockSale(1), { status: 200 });
  }),

  http.put(`${API_BASE}/pos/sales/:id/void`, () => {
    return HttpResponse.json(
      { ...generateMockSale(1), status: 'VOIDED' },
      { status: 200 }
    );
  }),

  // ========== AI SCANNER ==========
  http.post(`${API_BASE}/scanner/ai-scan`, async ({ request }) => {
    const body = await request.json() as any;
    return HttpResponse.json(
      {
        suggestions: [
          {
            productId: '1',
            name: 'Product 1',
            category: 'Electronics',
            estimatedPrice: '29.99',
            confidence: 95,
          },
          {
            productId: '2',
            name: 'Product 2',
            category: 'Electronics',
            estimatedPrice: '19.99',
            confidence: 87,
          },
          {
            productId: '3',
            name: 'Product 3',
            category: 'Electronics',
            estimatedPrice: '49.99',
            confidence: 78,
          },
        ],
      },
      { status: 200 }
    );
  }),

  // ========== INVENTORY / ALERTS ==========
  http.get(`${API_BASE}/inventory/alerts`, ({ request }) => {
    const url = new URL(request.url);
    const page = url.searchParams.get('page') || '0';
    return HttpResponse.json(
      {
        content: [
          generateMockAlert(1),
          { ...generateMockAlert(2), type: 'LOW_STOCK', severity: 'WARNING' },
        ],
        totalElements: 50,
        totalPages: 5,
        currentPage: parseInt(page),
        pageSize: 10,
      },
      { status: 200 }
    );
  }),

  http.put(`${API_BASE}/inventory/alerts/:id/dismiss`, () => {
    return HttpResponse.json(
      { ...generateMockAlert(1), resolved: true },
      { status: 200 }
    );
  }),

  // ========== PURCHASING ==========
  http.post(`${API_BASE}/purchasing/orders`, () => {
    return HttpResponse.json(generateMockPurchaseOrder(1), { status: 201 });
  }),

  http.get(`${API_BASE}/purchasing/orders`, ({ request }) => {
    const url = new URL(request.url);
    const page = url.searchParams.get('page') || '0';
    return HttpResponse.json(
      {
        content: [generateMockPurchaseOrder(1), generateMockPurchaseOrder(2)],
        totalElements: 25,
        totalPages: 3,
        currentPage: parseInt(page),
        pageSize: 10,
      },
      { status: 200 }
    );
  }),

  http.get(`${API_BASE}/purchasing/orders/:id`, () => {
    return HttpResponse.json(generateMockPurchaseOrder(1), { status: 200 });
  }),

  http.put(`${API_BASE}/purchasing/orders/:id/receive`, () => {
    return HttpResponse.json(
      { ...generateMockPurchaseOrder(1), status: 'RECEIVED' },
      { status: 200 }
    );
  }),

  http.post(`${API_BASE}/purchasing/orders/:id/clone`, () => {
    return HttpResponse.json(generateMockPurchaseOrder(999), { status: 201 });
  }),

  http.put(`${API_BASE}/purchasing/orders/:id/void`, () => {
    return HttpResponse.json(
      { ...generateMockPurchaseOrder(1), status: 'VOIDED' },
      { status: 200 }
    );
  }),

  http.get(`${API_BASE}/purchasing/suppliers`, () => {
    return HttpResponse.json(
      [
        { id: 1, name: 'Supplier 1', email: 'supplier1@test.com', phone: '555-0001' },
        { id: 2, name: 'Supplier 2', email: 'supplier2@test.com', phone: '555-0002' },
      ],
      { status: 200 }
    );
  }),

  // ========== DASHBOARD ==========
  http.get(`${API_BASE}/dashboard`, () => {
    return HttpResponse.json(
      {
        todaySales: 5000.00,
        averageTicket: 125.50,
        outOfStockProducts: 3,
        estimatedMonthlyProfit: 45000.00,
        recentSales: [
          { id: 1, saleNumber: 'SALE-000001', total: 125.50, itemCount: 5, createdAt: new Date().toISOString(), cashier: 'John Doe' },
          { id: 2, saleNumber: 'SALE-000002', total: 89.99, itemCount: 3, createdAt: new Date().toISOString(), cashier: 'Jane Smith' },
        ],
        activePendingOrders: 2,
        lowStockCount: 5,
      },
      { status: 200 }
    );
  }),

  // ========== AUDIT ==========
  http.get(`${API_BASE}/audit`, ({ request }) => {
    const url = new URL(request.url);
    const page = url.searchParams.get('page') || '0';
    return HttpResponse.json(
      {
        content: [
          {
            id: 1,
            action: 'CREATE',
            entityType: 'SALE',
            entityId: '1',
            previousData: {},
            newData: { total: 125.50, status: 'COMPLETED' },
            userId: 1,
            username: 'admin',
            timestamp: new Date().toISOString(),
          },
        ],
        totalElements: 100,
        totalPages: 10,
        currentPage: parseInt(page),
        pageSize: 10,
      },
      { status: 200 }
    );
  }),
];
