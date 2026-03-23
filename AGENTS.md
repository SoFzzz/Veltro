---
# 🚀 VELTRO — Complete Project Documentation & Roadmap

## 📋 Project Overview

**Veltro** is a lightweight **ERP/POS system** focused on SMEs that need:
- ✅ Barcode-based sales with real-time inventory tracking
- ✅ Automatic inventory updates and deductions
- ✅ Proactive stock alerts (out-of-stock, low-stock, overstock)
- ✅ Auditable operations with forensic audit trail
- ✅ Purchase order management with supplier tracking
- ✅ Dashboard & reporting (PDF/Excel export)
- ✅ AI-powered fallback barcode scanner

**Tech Stack:**
- **Backend**: Spring Boot 4.x + Java 21 + PostgreSQL + Flyway (Hexagonal Architecture)
- **Frontend**: React 18 + TypeScript + Vite + Tailwind CSS + React Router
- **Database**: PostgreSQL 16 with Docker Compose
- **Security**: JWT-based authentication with role-based access (ADMIN, CASHIER, WAREHOUSE)

---

## 🔨 Build, Run, Test Commands

### Backend (Spring Boot)
```bash
# Full build with tests
./mvnw clean install

# Run API server (requires PostgreSQL running)
./mvnw spring-boot:run -Dspring.profiles.active=local

# Run unit tests only
./mvnw test

# Run specific test class
./mvnw test -Dtest=SaleServiceTest
```

### Frontend (React/Vite)
```bash
# Install dependencies
cd frontend && npm install

# Start dev server (http://localhost:5173)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

### Docker (PostgreSQL)
```bash
# Start PostgreSQL 16 with Docker Compose
docker-compose up -d

# Check logs
docker-compose logs -f postgres

# Stop containers
docker-compose down
```

---

## 🏗️ Architecture Overview

### Hexagonal Architecture (Backend)

```
src/
├── domain/              # Business rules, entities, events (no Spring dependencies)
│   ├── iam/            # User, Role, UserProfile
│   ├── catalog/        # Product, Category
│   ├── inventory/      # Inventory, InventoryMovement, Alert
│   ├── pos/            # Sale, SaleDetail, States (State Pattern)
│   ├── purchasing/     # Supplier, PurchaseOrder, PurchaseOrderDetail
│   └── audit/          # AuditRecord
│
├── application/        # Use cases, services, listeners (application-specific logic)
│   ├── iam/           # AuthService, JwtTokenProvider
│   ├── catalog/       # ProductService, CategoryService, Mappers
│   ├── inventory/     # InventoryService, AlertService, AlertChainBuilder
│   ├── pos/           # SaleService, Listeners (DeductStock, RestoreStock)
│   ├── purchasing/    # PurchaseOrderService, SupplierService, Listeners
│   └── audit/         # AuditCommandExecutor
│
└── infrastructure/     # Adapters, REST controllers, persistence
    ├── rest/          # @RestController endpoints (thin controllers)
    ├── persistence/   # JPA repositories, query implementations
    ├── config/        # Spring Security, CORS, application properties
    └── db/migration/  # Flyway SQL scripts (V1__*, V2__*, ...)
```

### Frontend Structure

```
frontend/src/
├── api/                  # HTTP clients (axios instances + endpoints)
│   ├── client.ts        # Axios instance with JWT interceptors
│   ├── auth.ts          # Authentication API calls
│   ├── catalog.ts       # Product & category API calls
│   ├── pos.ts           # (F2-01) Sale API calls
│   ├── inventory.ts     # (F2-02) Alert API calls
│   ├── purchasing.ts    # (F2-03) Purchase order API calls
│   └── dashboard.ts     # (F3-02) Dashboard & reports API calls
│
├── types/               # TypeScript interfaces & types
│   └── index.ts         # All shared types
│
├── stores/              # Zustand state management
│   ├── authStore.ts     # JWT token, user, role, login/logout
│   ├── cartStore.ts     # (F2-01) Shopping cart items
│   └── alertStore.ts    # (F2-02) Alert notifications
│
├── components/          # Reusable UI components
│   ├── auth/           # AuthGuard, RoleGuard
│   ├── layout/         # MainLayout with navbar
│   ├── catalog/        # ProductList, ProductForm, CategoryTree
│   ├── pos/            # (F2-01) ScannerContainer, CartTable, ConfirmModal
│   ├── inventory/      # (F2-02) AlertList, AlertConfig, Badge
│   ├── purchasing/     # (F2-03) PurchaseOrderForm, OrderList, StateVisualizer
│   └── dashboard/      # (F3-02) KPICards, SalesChart, ExportButtons
│
├── pages/               # Full-page components (routes)
│   ├── auth/           # LoginPage
│   ├── catalog/        # ProductListPage, ProductFormPage, CategoryPage
│   ├── pos/            # (F2-01) POSPage
│   ├── inventory/      # (F2-02) AlertListPage, InventoryPage
│   ├── purchasing/     # (F2-03) PurchaseOrderPage, SupplierPage
│   ├── dashboard/      # (F3-02) DashboardPage, ReportPage
│   └── audit/          # (F3-03) AuditListPage, AuditDetailPage
│
├── hooks/               # Custom React hooks
│   ├── useAuth.ts       # Auth state & login/logout
│   ├── useCart.ts       # (F2-01) Shopping cart operations
│   └── useAlerts.ts     # (F2-02) Alert notifications
│
├── App.tsx              # React Router configuration
├── main.tsx             # BrowserRouter + QueryClientProvider
└── index.css            # Tailwind CSS imports
```

---

## 🧪 Testing Standards

### Backend (Java/JUnit 5)

```java
// ✅ DO: Use @ExtendWith(MockitoExtension.class) for sliced tests
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @InjectMocks ProductService service;
    @Mock ProductRepository repo;
    
    @Test
    void testCreateProduct() { 
        // Arrange, Act, Assert
    }
}

// ❌ DON'T: Avoid @SpringBootTest (requires DB)
@SpringBootTest  // Only if integration test is absolutely necessary
class VeltroApplicationTests { ... }
```

**Guidelines:**
- Unit test all services and listeners
- Mock external dependencies (repositories, other services)
- Test success paths AND failure paths
- Use `Mockito.lenient()` for stubs not used in all test cases
- Logging: `@Slf4j` with `log.info()` on success, `log.warn()` / `log.error()` on failures

### Frontend (React/TypeScript)

- Test critical business logic (hooks, utilities)
- Mock API calls with Jest mocks or MSW (Mock Service Worker)
- Test component integration with React Router
- No strict coverage % requirements — focus on critical paths

---

## 💾 Code Style & Best Practices

### Backend (Java)

**Data Types:**
- Monetary values: **`BigDecimal`** with `@Column(precision=19, scale=4)` per ADR-005
- Numeric constants: `0.0` for BigDecimal comparisons, never floats
- Dates: `Instant` or `LocalDateTime` (database stores as `TIMESTAMP`)

**Entity Management:**
- Use **`@Version`** on POS/Inventory entities for optimistic locking (ADR-002)
- Explicit methods for complex logic (don't rely solely on Lombok)
- Example:
  ```java
  @Entity
  @Getter @Setter @Builder
  public class SaleEntity {
      @Version private Long version;  // ✅ Mandatory for POS
      
      public void setStatus(SaleStatus status) {  // Explicit logic
          this.status = status;
          this.stateObject = StateFactory.create(status);
      }
  }
  ```

**Patterns Used:**
- **State Pattern**: Sale/PurchaseOrder lifecycle (PENDING → COMPLETED → VOIDED)
- **Observer Pattern**: Domain events (SaleCompletedEvent) → Listeners
- **Chain of Responsibility**: Alert evaluation (OutOfStock → LowStock → Overstock)
- **Factory Method**: Report exporters (PDF vs Excel)
- **Prototype Pattern**: Clone purchase orders for duplicate orders
- **Facade Pattern**: Dashboard aggregation
- **Strategy Pattern**: Scanner strategies (Barcode vs AI Vision)

**Logging:**
```java
log.info("Sale confirmed: {}", saleId);  // Success
log.warn("Low stock: {} items", productId);  // Warning
log.error("Failed to deduct stock", exception);  // Error
```

### Frontend (React/TypeScript)

**Component Structure:**
```typescript
// ✅ Functional components with hooks
export const ProductList: React.FC<ProductListProps> = ({ products }) => {
    const [sorted, setSorted] = useState(products);
    
    useEffect(() => {
        // Side effects here
    }, [products]);
    
    return <div>{/* JSX */}</div>;
};

// ✅ Custom hooks for reusable logic
export const useCart = () => {
    const store = useCartStore();
    return {
        add: (product) => { ... },
        remove: (id) => { ... }
    };
};
```

**State Management:**
- **Zustand** for global state (authStore, cartStore, alertStore)
- **React Query** (@tanstack/react-query) for server state & caching
- **React Hook Form** for form state with Zod validation

**Type Safety:**
```typescript
// ✅ Always type your props & hooks
interface ProductListProps {
    products: Product[];
    onSelect: (p: Product) => void;
}

// ✅ Use discriminated unions for API responses
type ApiResponse<T> = 
    | { success: true; data: T }
    | { success: false; error: string };
```

**Styling:**
- Tailwind CSS for all styling (no CSS-in-JS)
- Consistent spacing: `p-4`, `m-2`, etc.
- Responsive design: `sm:`, `md:`, `lg:` breakpoints

---

## 🔐 Security & Database

**Authentication Flow:**
1. User submits credentials → `/api/v1/auth/login`
2. Server returns JWT token (stored in `authStore`)
3. Axios interceptor auto-attaches token to all requests
4. On 401 response, refresh token silently and retry

**Authorization:**
- Roles: `ADMIN`, `CASHIER`, `WAREHOUSE`
- Backend: `@PreAuthorize("hasRole('ADMIN')")` on controller methods
- Frontend: `<RoleGuard roles={['ADMIN']}>` wrapper component

**Database:**
- PostgreSQL 16 with Docker (`postgres:16-alpine`)
- Flyway migrations: `src/main/resources/db/migration/V1__*.sql` → `V6__*.sql`
- No physical deletes (AC-05): use soft delete with `in_trash` boolean
- Prevent negative stock (AC-04): inventory service validates before deduction

**Key Decisions:**
- JWT tokens expire in 15 minutes, refresh tokens last 7 days
- All monetary columns: `NUMERIC(19,4)` precision per ADR-005
- Composite indexes on (severity, created_at) for alert queries (ADR-003)

---

## 📊 Project Status & Progress

### ✅ Backend: 100% Complete (326 tests passing)

| Phase | Module | Status | Key Components |
|-------|--------|--------|-----------------|
| **B1** | Project Setup | ✅ | Spring Boot 4, PostgreSQL, Flyway, JWT |
| **B1** | IAM Module | ✅ | JwtTokenProvider, AuthService, Roles |
| **B1** | Catalog Module | ✅ | Product, Category, MapStruct mappers |
| **B1** | Inventory Module | ✅ | Inventory, InventoryMovement, tracking |
| **B2** | Sale Module | ✅ | SaleEntity, State Pattern, SaleCompletedEvent |
| **B2** | Observer Pattern | ✅ | DeductStockSaleListener, RestoreStockSaleListener |
| **B2** | Proactive Alerts | ✅ | AlertEntity, Chain of Responsibility, auto-resolution |
| **B2** | Purchasing Module | ✅ | SupplierEntity, PurchaseOrder, State Pattern |
| **B3** | AI Scanner | ✅ | ScannerStrategy, BarcodeStrategy, AiVisionStrategy |
| **B3** | Dashboard & Reports | ✅ | DashboardService, PDF/Excel export (iText, POI) |
| **B3** | Forensic Audit | ✅ | AuditCommandExecutor, AuditRecord, full trail |

### 🚧 Frontend: 33% Complete (Phase 1 Done)

| Phase | Task | Status | Description |
|-------|------|--------|-------------|
| **F1** | Project Setup | ✅ | Vite + React 18 + TypeScript + Tailwind |
| **F1** | Authentication UI | ✅ | Login page, AuthGuard, RoleGuard, role-based redirect |
| **F1** | Catalog UI | ✅ | Product listing, create/edit form, category tree |
| **F2** | Scanner + POS UI | ⏳ | react-zxing integration, cart, confirm sale |
| **F2** | Alerts UI | ⏳ | Alert badge, listing, threshold config |
| **F2** | Purchase Orders UI | ⏳ | Create order, listing, clone, reception flow |
| **F3** | AI Fallback UI | ⏳ | 3s timer, AI identification modal |
| **F3** | Dashboard UI | ⏳ | KPI cards, sales chart, PDF/Excel export |
| **F3** | Audit UI | ⏳ | Admin-only audit table, filters, detail view |

---

## ✅ PHASE 1 — Frontend (COMPLETED)

### ✅ F1-01 | Project Setup

**Status:** COMPLETED

**Deliverables:**
- ✅ Vite project with React 18 + TypeScript
- ✅ Tailwind CSS configured
- ✅ Axios HTTP client with JWT interceptors
- ✅ Zustand `authStore` with localStorage persistence
- ✅ Shared TypeScript types (`ApiResponse<T>`, `PageResponse<T>`, entities)
- ✅ React Router configured with protected routes

**Files Created:**
- `src/api/client.ts` — Axios instance with JWT attach/refresh interceptors
- `src/stores/authStore.ts` — Zustand store for token, user, role
- `src/types/index.ts` — All TypeScript interfaces
- `tailwind.config.js` — Tailwind CSS configuration

### ✅ F1-02 | Authentication UI

**Status:** COMPLETED

**Deliverables:**
- ✅ Login page with email/password form (React Hook Form + Zod)
- ✅ `AuthGuard` component — redirects to /login if not authenticated
- ✅ `RoleGuard` component — redirects to /unauthorized if role not allowed
- ✅ Role-based redirect: ADMIN→/dashboard, CASHIER→/pos, WAREHOUSE→/inventory
- ✅ Logout functionality in MainLayout navbar

**Files Created:**
- `src/pages/auth/LoginPage.tsx` — Login form with validation
- `src/components/auth/AuthGuard.tsx` — Authentication wrapper
- `src/components/auth/RoleGuard.tsx` — Role-based access control
- `src/hooks/useAuth.ts` — Custom hook for auth operations

### ✅ F1-03 | Catalog UI

**Status:** COMPLETED

**Deliverables:**
- ✅ Product listing page with pagination
- ✅ Create/edit product form
- ✅ Category tree component with recursive rendering
- ✅ Category management with inline editing

**Files Created:**
- `src/pages/catalog/ProductListPage.tsx` — Product pagination & search
- `src/pages/catalog/ProductFormPage.tsx` — Create/edit form
- `src/pages/catalog/CategoryPage.tsx` — Category management
- `src/components/catalog/CategoryTree.tsx` — Recursive category renderer
- `src/api/catalog.ts` — Product & category API calls

---

## 🎯 PHASE 2 — Frontend (PENDING)

### 🔲 F2-01 | Scanner + POS UI

**Status:** ⏳ PENDING

**Acceptance Criteria:**
- AC-01: Barcode scanning with `react-zxing` camera integration
- AC-02: Auto-lookup product via `GET /api/v1/products/barcode/{barcode}`
- AC-03: Add product to cart (cartStore)
- AC-04: Modify quantity, remove item buttons
- AC-05: Confirm sale with transaction check
- AC-06: Display cart total, item count, tax if applicable

**Components to Create:**
```typescript
src/components/pos/
├── ScannerContainer.tsx    // Camera input with react-zxing
├── CartTable.tsx           // Items list with quantity controls
├── ConfirmModal.tsx        // Final sale confirmation
└── SaleReceipt.tsx         // Post-sale receipt display

src/pages/pos/
└── POSPage.tsx             // Main POS layout

src/hooks/
└── useCart.ts              // cartStore operations

src/stores/
└── cartStore.ts            // Zustand cart state
```

**API Integration:**
```typescript
// src/api/pos.ts
export const getProductByBarcode = (barcode: string) => 
  api.get<Product>(`/api/v1/products/barcode/${barcode}`);

export const confirmSale = (saleData: CreateSaleRequest) =>
  api.post<SaleResponse>('/api/v1/pos/sales', saleData);

export const voidSale = (saleId: string) =>
  api.put<SaleResponse>(`/api/v1/pos/sales/${saleId}/void`);
```

**Types to Add:**
```typescript
interface CartItem {
  product: Product;
  quantity: number;
  subtotal: BigDecimal;
}

interface CartStore {
  items: CartItem[];
  add: (product: Product, qty: number) => void;
  remove: (productId: string) => void;
  updateQty: (productId: string, qty: number) => void;
  clear: () => void;
  total: () => BigDecimal;
}

interface CreateSaleRequest {
  items: { productId: string; quantity: number }[];
  paymentMethod: 'CASH' | 'CARD' | 'CHECK';
  notes?: string;
}
```

**Key Implementation Details:**
- Use `react-zxing` library for barcode scanning
- Camera permission handling with try/catch
- Debounce barcode scan to prevent duplicate reads (500ms)
- Optimistic UI: add to cart immediately, rollback on error
- Show loading state while fetching product
- Prevent negative quantities
- Clear cart after successful sale confirmation

**Testing Strategy:**
- Mock barcode scanner with fake UPC codes
- Test cart add/remove/update operations
- Test sale confirmation with various payment methods
- Test error handling (barcode not found, no stock)

---

### 🔲 F2-02 | Alerts UI

**Status:** ⏳ PENDING

**Acceptance Criteria:**
- AC-01: Unread alerts badge in header (red indicator with count)
- AC-02: Alert listing page with pagination & severity sorting
- AC-03: Click to dismiss/resolve alert
- AC-04: Configure alert thresholds per product
- AC-05: Visual indicators: 🔴 Critical, 🟠 Warning, 🟡 Info

**Components to Create:**
```typescript
src/components/inventory/
├── AlertBadge.tsx          // Header badge with unread count
├── AlertList.tsx           // Paginated alert table
├── AlertConfigForm.tsx     // Threshold config per product
└── SeverityBadge.tsx       // Visual severity indicator

src/pages/inventory/
├── AlertListPage.tsx       // Alert listing page
└── InventoryPage.tsx       // Inventory dashboard (optional)

src/hooks/
└── useAlerts.ts            // Alert operations & polling

src/stores/
└── alertStore.ts           // Alert notification state
```

**API Integration:**
```typescript
// src/api/inventory.ts
export const getAlerts = (page: number, severity?: string) =>
  api.get<PageResponse<Alert>>('/api/v1/inventory/alerts', {
    params: { page, severity }
  });

export const dismissAlert = (alertId: string) =>
  api.put(`/api/v1/inventory/alerts/${alertId}/dismiss`);

export const getAlertConfig = (productId: string) =>
  api.get<AlertConfig>(`/api/v1/inventory/products/${productId}/alert-config`);

export const updateAlertConfig = (productId: string, config: AlertConfig) =>
  api.put(`/api/v1/inventory/products/${productId}/alert-config`, config);
```

**Types to Add:**
```typescript
type AlertSeverity = 'CRITICAL' | 'WARNING' | 'INFO';
type AlertType = 'OUT_OF_STOCK' | 'LOW_STOCK' | 'OVERSTOCK';

interface Alert {
  id: string;
  productId: string;
  type: AlertType;
  severity: AlertSeverity;
  message: string;
  createdAt: DateTime;
  resolved: boolean;
}

interface AlertConfig {
  criticalStock: number;
  minStock: number;
  overstockThreshold: number;
}
```

**Key Implementation Details:**
- Polling interval: 30 seconds for alerts (WebSockets can optimize later)
- Use React Query for server state caching
- Auto-refresh alerts when new critical alert appears
- Show toast notification on critical alerts
- One-click dismiss without confirmation
- Badge shows count of unread alerts
- Color-coded severity: red (critical), orange (warning), yellow (info)

**Testing Strategy:**
- Mock alert API responses
- Test badge count calculation
- Test severity color mapping
- Test pagination controls
- Test alert config form validation
- Test auto-refresh on poll

---

### 🔲 F2-03 | Purchase Orders UI

**Status:** ⏳ PENDING

**Acceptance Criteria:**
- AC-01: Create purchase order form with supplier selection
- AC-02: Add items with unit cost & quantity
- AC-03: View purchase order list with visual state indicators
- AC-04: Clone existing order button
- AC-05: Merchandise reception flow (mark items as received)
- AC-06: Void order functionality

**Components to Create:**
```typescript
src/components/purchasing/
├── PurchaseOrderForm.tsx   // Create/edit form with items table
├── OrderList.tsx           // Listing with filters & pagination
├── StateVisualizer.tsx     // Status badge (PENDING, PARTIAL, RECEIVED, VOIDED)
├── ReceptionFlow.tsx       // Receive items modal with qty controls
├── CloneOrderModal.tsx     // Confirm & clone existing order
└── SupplierSelect.tsx      // Dropdown with supplier list

src/pages/purchasing/
├── PurchaseOrderPage.tsx   // Main PO listing & management
└── SupplierPage.tsx        // Supplier CRUD (optional)

src/hooks/
└── usePurchaseOrders.ts    // PO operations & state

src/api/
└── purchasing.ts           // PO API calls
```

**API Integration:**
```typescript
// src/api/purchasing.ts
export const createPurchaseOrder = (data: CreatePORequest) =>
  api.post<PurchaseOrderResponse>('/api/v1/purchasing/orders', data);

export const getPurchaseOrders = (page: number, supplierId?: string) =>
  api.get<PageResponse<PurchaseOrder>>('/api/v1/purchasing/orders', {
    params: { page, supplierId }
  });

export const getPurchaseOrderById = (orderId: string) =>
  api.get<PurchaseOrder>(`/api/v1/purchasing/orders/${orderId}`);

export const clonePurchaseOrder = (orderId: string) =>
  api.post<PurchaseOrder>(`/api/v1/purchasing/orders/${orderId}/clone`);

export const markAsReceived = (orderId: string, items: ReceiptData[]) =>
  api.put<PurchaseOrder>(`/api/v1/purchasing/orders/${orderId}/receive`, { items });

export const voidPurchaseOrder = (orderId: string, reason: string) =>
  api.put<PurchaseOrder>(`/api/v1/purchasing/orders/${orderId}/void`, { reason });

export const getSuppliers = () =>
  api.get<Supplier[]>('/api/v1/purchasing/suppliers');
```

**Types to Add:**
```typescript
type POStatus = 'PENDING' | 'PARTIAL' | 'RECEIVED' | 'VOIDED';

interface PurchaseOrder {
  id: string;
  orderNumber: string;  // PO-YYYY-NNNNNN
  supplierId: string;
  items: PurchaseOrderItem[];
  status: POStatus;
  createdAt: DateTime;
  receivedAt?: DateTime;
  totalCost: BigDecimal;
}

interface PurchaseOrderItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitCost: BigDecimal;
  receivedQty: number;
  subtotal: BigDecimal;
}

interface ReceiptData {
  itemId: string;
  receivedQty: number;
}

interface CreatePORequest {
  supplierId: string;
  items: { productId: string; quantity: number; unitCost: BigDecimal }[];
}
```

**Key Implementation Details:**
- State visualization: colors for each status
  - 🔵 Pending (PENDING)
  - 🟡 Partial (PARTIAL)
  - 🟢 Received (RECEIVED)
  - ⚫ Voided (VOIDED)
- Disable form fields based on order status
- Can only receive items on PENDING/PARTIAL orders
- Clone creates new order with same items but empty receipt (receivedQty = 0)
- Confirm dialog before voiding orders with reason required
- Show calculated totals in forms

**Testing Strategy:**
- Test form validation (required fields, positive quantities)
- Test state transitions (create → partial receive → received)
- Test clone operation preserves items but zeros receivedQty
- Test reception flow quantity validation
- Test void with reason logging
- Test supplier dropdown loading

---

## ⏳ PHASE 3 — Frontend (PENDING)

### 🔲 F3-01 | AI Fallback UI

**Status:** ⏳ PENDING

**Acceptance Criteria:**
- AC-01: 3-second timer in ScannerContainer
- AC-02: Show "Identify with AI" button if scan fails
- AC-03: Capture frame on activation
- AC-04: Send to backend AI vision API
- AC-05: Display modal with product suggestions & confidence %

**Components to Create:**
```typescript
src/components/pos/
└── AiIdentificationModal.tsx   // Suggestions list with confidence

// Update existing:
src/components/pos/ScannerContainer.tsx  // Add 3s timer & AI button
```

**API Integration:**
```typescript
// src/api/pos.ts
export const aiScanProduct = (imageData: string, filename: string) =>
  api.post<ProductSuggestionResponse>('/api/v1/scanner/ai-scan', {
    imageData,
    filename
  });
```

**Types to Add:**
```typescript
interface ProductSuggestionResponse {
  suggestions: SuggestedProduct[];
}

interface SuggestedProduct {
  productId: string;
  name: string;
  category: string;
  estimatedPrice: BigDecimal;
  confidence: number;  // 0-100
}
```

**Key Implementation Details:**
- 3-second countdown timer in ScannerContainer
- Auto-focus camera input initially
- If scan succeeds, reset timer and add to cart
- If timer expires, show "Identify with AI" button
- Clicking button captures frame and sends to backend
- Show confidence % for each suggestion
- User can select suggestion or manually search

---

### 🔲 F3-02 | Dashboard UI

**Status:** ⏳ PENDING

**Acceptance Criteria:**
- AC-01: Dashboard page with KPI cards
- AC-02: KPIs: today's sales, average ticket, out-of-stock count, estimated profit
- AC-03: Latest sales table (last 10 transactions)
- AC-04: PDF export button
- AC-05: Excel export button

**Components to Create:**
```typescript
src/components/dashboard/
├── KPICard.tsx            // Reusable KPI display
├── SalesChart.tsx         // Chart of sales by hour/day (optional)
├── LatestSalesTable.tsx   // Recent transactions
├── ExportButtons.tsx      // PDF/Excel export
└── DashboardLayout.tsx    // Grid layout for cards

src/pages/
└── DashboardPage.tsx      // Main dashboard page

src/hooks/
└── useDashboard.ts        // Fetch & refresh dashboard data
```

**API Integration:**
```typescript
// src/api/dashboard.ts
export const getDashboard = () =>
  api.get<DashboardResponse>('/api/v1/dashboard');

export const exportProfitabilityReport = () =>
  api.post('/api/v1/reports/export', {
    type: 'PDF',
    reportType: 'PROFITABILITY'
  }, { responseType: 'blob' });

export const exportProfitabilityReportExcel = () =>
  api.post('/api/v1/reports/export', {
    type: 'EXCEL',
    reportType: 'PROFITABILITY'
  }, { responseType: 'blob' });
```

**Types to Add:**
```typescript
interface DashboardResponse {
  todaySales: BigDecimal;
  averageTicket: BigDecimal;
  outOfStockProducts: number;
  estimatedMonthlyProfit: BigDecimal;
  recentSales: SaleRow[];
  activePendingOrders: number;
  lowStockCount: number;
}

interface SaleRow {
  id: string;
  saleNumber: string;
  total: BigDecimal;
  itemCount: number;
  createdAt: DateTime;
  cashier: string;
}
```

**Key Implementation Details:**
- KPI cards with large numbers, trending arrows (optional)
- Export buttons trigger download of PDF/Excel files
- Auto-refresh dashboard every 30 seconds
- Color-code KPIs: green if good, orange if warning, red if critical
- Show timestamps for last refresh

---

### 🔲 F3-03 | Audit UI (ADMIN Only)

**Status:** ⏳ PENDING

**Acceptance Criteria:**
- AC-01: ADMIN-only paginated audit table
- AC-02: Filter by entity type (SALE, INVENTORY, ORDER)
- AC-03: Filter by action (CREATE, UPDATE, DELETE)
- AC-04: Date range filter
- AC-05: Detail view showing diff of previousData vs newData
- AC-06: Search by entity ID

**Components to Create:**
```typescript
src/components/audit/
├── AuditTable.tsx         // Paginated list with filters
├── AuditDetailModal.tsx   // Side-by-side diff view
├── AuditFilters.tsx       // Date range, entity, action
└── DiffViewer.tsx         // JSON diff display

src/pages/
└── AuditListPage.tsx      // Main audit page

src/hooks/
└── useAuditRecords.ts     // Fetch & filter audit records
```

**API Integration:**
```typescript
// src/api/audit.ts
export const getAuditRecords = (filters: AuditFilters) =>
  api.get<PageResponse<AuditRecord>>('/api/v1/audit', {
    params: filters
  });

export const getAuditRecordDetail = (auditId: string) =>
  api.get<AuditRecord>(`/api/v1/audit/${auditId}`);
```

**Types to Add:**
```typescript
type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE' | 'VOID';
type AuditEntity = 'SALE' | 'INVENTORY' | 'ORDER' | 'PRODUCT';

interface AuditRecord {
  id: string;
  action: AuditAction;
  entityType: AuditEntity;
  entityId: string;
  previousData: Record<string, unknown>;
  newData: Record<string, unknown>;
  userId: string;
  username: string;
  timestamp: DateTime;
  reason?: string;
}

interface AuditFilters {
  page: number;
  entityType?: AuditEntity;
  action?: AuditAction;
  startDate?: DateTime;
  endDate?: DateTime;
  entityId?: string;
}
```

**Key Implementation Details:**
- ADMIN role required (use `<RoleGuard roles={['ADMIN']}>`).
- Display `previousData` vs `newData` in split view or JSON diff
- Timestamps show user timezone
- Reason field (if applicable) shows why change was made
- Can export audit trail as CSV

---

## 🔑 Key Architectural Patterns

### Domain-Driven Design (DDD)
- Domain layer contains business rules (no Spring dependencies)
- Each module (Catalog, Inventory, POS) is independent
- Events drive inter-module communication

### Event-Driven Architecture
- `SaleCompletedEvent` → `DeductStockSaleListener` → `InventoryService.recordExit()`
- `StockChangedEvent` → `EvaluateStockAlertsListener` → Alert creation
- `OrderReceivedEvent` → `IncrementStockOrderListener` → `InventoryService.recordEntry()`

### Hexagonal Architecture
- **Domain**: Pure business logic (no framework dependencies)
- **Application**: Use cases and services (application-specific logic)
- **Infrastructure**: Adapters (REST controllers, JPA repositories)

---

## 📝 Notes for Agents

### Expectations
- Respect ADRs and acceptance criteria
- No negative stock (AC-04), no physical deletes (AC-05)
- Tests required for every new listener/service
- Update `AGENTS.md` after finishing a task

### Code Review Checklist
- [ ] Services have `@Slf4j` logging
- [ ] Controllers are thin (logic in services)
- [ ] BigDecimal for monetary values
- [ ] POS/Inventory entities have `@Version`
- [ ] Tests use `@ExtendWith(MockitoExtension.class)`
- [ ] No `@SpringBootTest` unless absolutely needed
- [ ] Flyway migrations follow V#__ naming
- [ ] Components follow React best practices
- [ ] TypeScript types are strict (no `any`)
- [ ] Zustand stores have clear actions

### Common Pitfalls to Avoid
- ❌ Storing floats for money (use `BigDecimal`)
- ❌ Missing `@Version` on POS entities
- ❌ Using `@SpringBootTest` for unit tests
- ❌ Listeners with `@Transactional` (rely on parent service)
- ❌ Logical deletes without soft-delete column
- ❌ Frontend state for server data (use React Query)
- ❌ Untyped props in React components
- ❌ Missing error handling in API calls

---

## 📞 Support & Feedback

- **Issues**: Report at https://github.com/SoFzzz/Veltro
- **Keyboard Shortcuts** (in OpenCode CLI): `ctrl+p` to list available actions
- **Frontend Docs**: See `frontend/README.md`
- **Backend Docs**: See `doc/` folder

**Last Updated:** March 23, 2026
**Backend Status:** 100% (326 tests passing)
**Frontend Status:** 33% (Phase 1 complete, Phase 2 pending)
