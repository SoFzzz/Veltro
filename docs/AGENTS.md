# VELTRO — Documentación del Proyecto

## Descripción General

**Veltro** es un sistema ERP/POS **multi-tenant** ligero para PYMEs que incluye:
- **Multi-tenant**: cada negocio tiene datos aislados (productos, inventario, ventas, etc.)
- Ventas con escaneo de código de barras (cámara + USB)
- Identificación de productos por IA (GPT-4o-mini vía GitHub Models API)
- Inventario con alertas proactivas (sin stock, stock bajo, sobre-stock)
- Órdenes de compra con gestión de proveedores
- Dashboard con KPIs y exportación PDF/Excel
- Auditoría forense completa
- Roles: ADMIN, CASHIER, WAREHOUSE
- Registro de nuevos negocios + creación de trabajadores por ADMIN

**Stack Tecnológico:**
- **Backend**: Spring Boot 4.x + Java 21 + PostgreSQL 18 + Flyway
- **Frontend**: React 19 + TypeScript + Vite + Tailwind CSS v4 + React Router
- **IA**: GPT-4o-mini vía GitHub Models API (`https://models.inference.ai.azure.com`)
- **Seguridad**: JWT con roles (ADMIN, CASHIER, WAREHOUSE) + tenant isolation por businessId

---

## Comandos de Build y Ejecución

### Entorno Local

```bash
# PostgreSQL 18 local
# DB: veltro_db | Usuario: postgres | Password: lolxd777
# Login app: admin2/admin123 (rol ADMIN, businessId=1)
# Segundo negocio: owner_test/test123 (ADMIN, businessId=2), cashier_test/test123 (CASHIER)

# Backend (puerto 8080)
"C:/Users/dino2/OneDrive/Escritorio/Veltro/mvnw" clean package -DskipTests
nohup java -jar target/Veltro-0.0.1-SNAPSHOT.jar > target/backend.log 2>&1 &

# Frontend (puerto 5173)
cd frontend && nohup npm run dev > ../target/frontend.log 2>&1 &

# Matar Java antes de rebuild (Windows)
taskkill //F //IM java.exe

# TypeScript check
cd frontend && npx tsc --noEmit
```

### Rutas API Base
- Backend: `http://localhost:8080/api/v1/`
- Frontend: `http://localhost:5173`

---

## Arquitectura

### Backend — Hexagonal

```
src/
├── domain/              # Entidades, eventos, reglas de negocio (sin Spring)
│   ├── iam/            # User, Role
│   ├── catalog/        # Product, Category
│   ├── inventory/      # Inventory, InventoryMovement, Alert
│   ├── pos/            # Sale, SaleDetail, State Pattern
│   ├── purchasing/     # Supplier, PurchaseOrder, PurchaseOrderDetail
│   └── audit/          # AuditRecord
│
├── application/        # Servicios, listeners, strategies
│   ├── iam/           # AuthService, JwtTokenProvider
│   ├── catalog/       # ProductService, CategoryService
│   ├── inventory/     # InventoryService, AlertService, AlertChainBuilder
│   ├── pos/           # SaleService, DeductStockListener, RestoreStockListener
│   ├── purchasing/    # PurchaseOrderService, SupplierService
│   ├── scanner/       # ScannerService, AiVisionStrategy, OpenAiVisionClient
│   └── audit/         # AuditCommandExecutor
│
└── infrastructure/     # Controllers REST, JPA repos, config
    ├── rest/          # @RestController (controladores delgados)
    ├── persistence/   # Repositorios JPA
    ├── config/        # Security, CORS, application.yaml
    └── db/migration/  # Flyway SQL (V1__* a V6__*)
```

### Frontend

```
frontend/src/
├── api/                  # Clientes HTTP (Axios + endpoints)
│   ├── client.ts        # Axios con interceptores JWT
│   ├── auth.ts          # Login, registro, cambio contraseña, crear trabajador
│   ├── catalog.ts       # Productos y categorías CRUD
│   ├── pos.ts           # Ventas, escáner IA, búsqueda productos
│   ├── inventory.ts     # Alertas de inventario
│   ├── purchasing.ts    # Órdenes de compra
│   ├── dashboard.ts     # KPIs y reportes
│   └── audit.ts         # Registros de auditoría
│
├── stores/              # Zustand
│   ├── authStore.ts     # Token JWT, usuario, rol, businessId, getBusinessId()
│   ├── cartStore.ts     # Carrito POS (Product, salePrice)
│   └── alertStore.ts    # Alertas de inventario
│
├── components/
│   ├── auth/           # AuthGuard, RoleGuard
│   ├── layout/         # MainLayout (sidebar responsive, "Empleados" nav para ADMIN)
│   ├── catalog/        # CategoryTree, ProductScanner (cámara + IA)
│   ├── pos/            # ScannerContainer, CartTable, ConfirmModal, SaleReceipt, AiIdentificationModal
│   ├── inventory/      # AlertList, AlertConfigForm
│   ├── purchasing/     # PurchaseOrderForm, OrderList, ReceptionFlow
│   ├── dashboard/      # KPICards, LatestSalesTable, ExportButtons
│   └── audit/          # AuditTable, AuditFilters, DiffViewer
│
├── pages/
│   ├── auth/           # LoginPage, RegisterPage
│   ├── catalog/        # ProductListPage, ProductFormPage, CategoryPage
│   ├── pos/            # POSPage
│   ├── inventory/      # InventoryPage, AlertListPage (placeholder)
│   ├── purchasing/     # PurchaseOrderPage
│   ├── dashboard/      # DashboardPage
│   ├── audit/          # AuditListPage
│   └── settings/       # WorkersPage (crear CASHIER/WAREHOUSE)
│
├── hooks/              # useFocusTrap, useAuth, useAlerts
├── types/              # TypeScript interfaces compartidos
├── App.tsx             # React Router + lazy loading
└── index.css           # Tailwind CSS v4 imports
```

---

## Arquitectura Multi-Tenant

### Modelo de Aislamiento

Cada negocio (business) tiene datos completamente aislados. El aislamiento se implementa mediante `business_id` en todas las tablas principales.

**Tablas con `business_id`:** users, categories, products, inventory, inventory_movements, alert_configuration, alert, supplier, purchase_order, sale, audit_record

**Tablas SIN `business_id`:** purchase_order_detail, sale_detail (heredan tenant de su padre PO/sale), business (tabla raíz)

**Constraints únicos per-tenant:**
- `(username, business_id)` — mismo username puede existir en distintos negocios
- `(barcode, business_id)`, `(sku, business_id)` — productos únicos por negocio
- `(tax_id, business_id)` — proveedores únicos por negocio
- `(order_number, business_id)`, `(sale_number, business_id)` — numeración por negocio
- `email` — globalmente único (para recuperación de contraseña)

### Auth Stack (JWT + Tenant)

```
Request → JwtAuthenticationFilter
  ├── Extrae JWT del header Authorization
  ├── JwtTokenProvider.extractClaims() → { sub, uid, bid, role }
  ├── Construye VeltroUserDetails(username, userId, businessId, authorities)
  └── SecurityContextHolder.setAuthentication(UsernamePasswordAuthenticationToken)

Service Layer → TenantContext (static utility)
  ├── TenantContext.getBusinessId() → Long
  ├── TenantContext.getUserId() → Long
  └── TenantContext.getUsername() → String
```

**Archivos clave:**
- `VeltroUserDetails.java` — extiende Spring `User` con `userId` + `businessId`
- `TenantContext.java` — utilidad estática que extrae tenant del SecurityContext
- `JwtTokenProvider.java` — embeds `bid` (businessId) y `uid` (userId) en access tokens
- `JwtAuthenticationFilter.java` — reconstruye `VeltroUserDetails` desde JWT claims

### JWT Token Claims

```json
{
  "sub": "admin2",
  "uid": 3,
  "bid": 1,
  "role": "ADMIN",
  "type": "ACCESS",
  "iat": 1774537249,
  "exp": 1774538149
}
```

### Login Response (POST /auth/login)
```json
{
  "accessToken": "eyJhbG...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "admin2",
  "role": "ADMIN",
  "businessId": 1
}
```
**IMPORTANTE:** La respuesta es PLANA (no tiene objeto `user` anidado). El frontend construye el objeto `User` manualmente.

### Registro de Nuevo Negocio (POST /auth/register)
```json
{
  "username": "owner",
  "email": "owner@test.com",
  "password": "pass123",
  "businessName": "Mi Tienda"
}
```
Crea: 1) BusinessEntity, 2) UserEntity (ADMIN) vinculado al business.

### Crear Trabajador (POST /auth/workers) — Solo ADMIN
```json
{
  "username": "cashier1",
  "email": "cashier@test.com",
  "password": "pass123",
  "role": "CASHIER"
}
```
Crea un usuario en el mismo `businessId` del ADMIN autenticado. Roles válidos: CASHIER, WAREHOUSE.

### Patrón en Servicios

Todos los servicios siguen el mismo patrón para aislamiento de datos:

```java
// En cada método de servicio:
Long businessId = TenantContext.getBusinessId();

// Queries filtran por businessId:
productRepository.findAllByActiveTrueAndBusinessId(businessId, pageable);

// Al crear entidades, se asigna businessId:
entity.setBusinessId(businessId);
```

### Migración SQL

`V3__multi_tenant.sql` — Agrega:
- Tabla `business` (id, name, owner_id FK users, audit fields)
- Columna `business_id BIGINT NOT NULL` a todas las tablas principales
- Datos existentes asignados a business id=1 ("Negocio Principal")
- Constraints únicos actualizados a per-business

---

## Patrones de Diseño Implementados

| Patrón | Uso |
|--------|-----|
| **Multi-Tenant (Shared DB)** | Aislamiento por `business_id` en cada tabla, TenantContext estático |
| **State Pattern** | Ciclo de vida de Sale y PurchaseOrder (PENDING → COMPLETED → VOIDED) |
| **Observer Pattern** | Domain events → Listeners (SaleCompletedEvent → DeductStock) |
| **Chain of Responsibility** | Evaluación de alertas (OutOfStock → LowStock → Overstock) |
| **Strategy Pattern** | Scanner: BarcodeStrategy + AiVisionStrategy |
| **Factory Method** | Exportadores de reportes (PDF vs Excel) |
| **Command Pattern (Functional)** | AuditCommandExecutor con before/after snapshots |

---

## Integración IA — Escáner con Visión

### Configuración (application.yaml)

```yaml
veltro:
  ai:
    openai:
      enabled: true
      api-key: ${OPENAI_API_KEY}  # GitHub Models API PAT or OpenAI key
      api-endpoint: https://models.inference.ai.azure.com/chat/completions
      model: gpt-4o-mini
      max-tokens: 1000
      timeout-seconds: 30
      max-image-size-mb: 10
      max-retries: 3
```

### Endpoints del Scanner

| Endpoint | Método | Descripción |
|----------|--------|-------------|
| `/api/v1/scanner/ai` | POST (multipart) | Envía imagen, retorna sugerencias de producto |
| `/api/v1/scanner/status` | GET | Estado de estrategias (`{ BARCODE: true, AI_VISION: true }`) |
| `/api/v1/scanner/ai/available` | GET | `{ available: boolean }` |

### Flujo de IA en POS (ScannerContainer)

1. Se abre la cámara con `react-zxing` para escaneo de código de barras en tiempo real
2. Si se detecta un código → busca producto por barcode → agrega al carrito
3. Si pasan 3 segundos sin detectar código → aparece botón "Identificar con IA"
4. Al presionar → captura frame del video (`canvas.toBlob()`)
5. Envía imagen como `FormData` a `POST /scanner/ai`
6. Backend (AiVisionStrategy → OpenAiVisionClient → GPT-4o-mini) analiza la imagen
7. Retorna `ProductSuggestionResponse` con sugerencias, confianza, precio sugerido
8. Usuario selecciona sugerencia → busca por barcode/nombre → agrega al carrito

### Flujo de IA en Catálogo (ProductFormPage)

1. En la página de crear/editar producto, hay un botón "Escanear producto con cámara o IA"
2. Al presionar, se abre el `ProductScanner` con cámara
3. Si se detecta código de barras → verifica si el producto ya existe en la BD
   - Si existe: muestra advertencia con el nombre del producto
   - Si no existe: llena el campo de código de barras
4. Si no se detecta código → botón "Identificar con IA" (mismo flujo 3s)
5. IA retorna sugerencias → usuario selecciona una → auto-completa campos:
   - Nombre, código de barras, precio de venta, descripción
6. Usuario revisa, completa datos faltantes, y guarda

### Tipos Frontend (pos.ts)

```typescript
interface SuggestedProduct {
  productId: number | null;
  productName: string;
  confidence: number;        // 0.0–1.0
  suggestedPrice: string | null;
  barcode: string | null;
}

interface ProductSuggestionResponse {
  suggestions: SuggestedProduct[];
  processingTimeMs: number;
  strategyUsed: string;
}
```

---

## Formato de Respuestas Backend

### ProductResponse (GET /products, GET /products/barcode/{code})
Campos planos (sin objetos anidados):
```json
{
  "id": 1,
  "name": "Producto X",
  "barcode": "7750000000000",
  "sku": "SKU-001",
  "description": "...",
  "costPrice": "10.0000",
  "salePrice": "15.0000",
  "categoryId": 1,
  "categoryName": "Categoría A",
  "active": true,
  "minStockInfo": 20,
  "minStockWarning": 10,
  "minStockCritical": 5
}
```

### Auth Login (POST /auth/login)
```json
{
  "accessToken": "eyJhbG...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "admin2",
  "role": "ADMIN",
  "businessId": 1
}
```
**Nota:** Respuesta plana — el frontend construye el objeto `User` manualmente.

### Inventory Adjustment (PUT /inventory/{id}/adjust)
```json
{ "newStock": 50, "reason": "Ajuste por conteo físico" }
```

---

## Bugs Resueltos (Historial)

### Backend
1. `App.tsx` InventoryPage duplicado → corregido con Suspense
2. Audit `findByFilters` query → fix CAST para enums null
3. POS API path + quick sale → endpoint `POST /sales/quick` con `QuickSaleRequest`
4. Jackson LocalDateTime → `.toString()` en snapshots
5. Payment methods → YAPE/PLIN en lugar de CHECK
6. Product/Category delete → cambiado a PUT deactivate (soft delete)
7. PO `requested_by` NOT NULL → `UserRepository` + `SecurityContextHolder`
8. DB constraint `ck_alert_type` → ALTER CHECK para incluir 6 tipos

### Frontend
9. `SaleReceipt` → reescrito para matchear backend
10. Dashboard types → campos correctos
11. Category `getTree` → path corregido
12. `purchasing.ts` → reescritura completa (endpoints, tipos, creación multi-paso)
13. `inventory.ts` → paths de alertas corregidos
14. `auth.ts` → PUT change-password, campo currentPassword
15. `audit.ts` → export lanza error descriptivo
16. `PageResponse` → formato Spring Page
17. Páginas de purchasing (PurchaseOrderPage, OrderList, ReceptionFlow, PurchaseOrderForm) → reescritas
18. AlertList, alertStore, AlertConfigForm → conversiones string→number
19. AuditListPage → currentPage→number, manejo de error en export
20. POS barcode/salePrice → tipo Product corregido, cartStore usa `salePrice`
21. CartTable, ConfirmModal → `salePrice` en lugar de `price`
22. PO Supplier dropdown → `setValue` en lugar de evento DOM
23. ProductListPage → `overflow-x-auto`

### Responsive UI (Sesión 6)
24. PurchaseOrderPage → overflow-x-auto, header wrap
25. AuditListPage → overflow-x-auto, header wrap
26. OrderList → overflow-x-auto en items expandidos
27. ProductListPage → header wrap
28. POSPage → header wrap + md:grid-cols-2
29. CategoryPage → header wrap + lg:sticky
30. InventoryPage → flex-wrap, whitespace, bg-black/50 (Tailwind v4)
31. DashboardPage → md:grid-cols-2, flex-1 min-h-0
32. AuditFilters → grid-cols-1 sm:grid-cols-2
33. MainLayout → bg-black/60 (Tailwind v4)

### Integración IA/Cámara (Sesión 6-7)
34. `application.yaml` → GitHub AI API (gpt-4o-mini)
35. `pos.ts` → tipos correctos, FormData upload, checkAiAvailable()
36. `AiIdentificationModal.tsx` → reescrito (Blob input, español, colores Veltro)
37. `ScannerContainer.tsx` → reescrito con react-zxing, timer 3s IA, captura frame
38. `ProductScanner.tsx` → NUEVO componente para formulario de catálogo
39. `ProductFormPage.tsx` → integración con ProductScanner (auto-fill campos)

---

## Archivos Modificados — Referencia Rápida

### Backend
| Archivo | Cambio |
|---------|--------|
| `application.yaml` | GitHub AI API config (gpt-4o-mini) |
| `PurchaseOrderService.java` | Fix requestedBy NOT NULL |
| `AlertType.java` | 6 tipos de alerta |
| `V3__multi_tenant.sql` | Migración multi-tenant (business table, business_id columns) |
| `VeltroUserDetails.java` | UserDetails extendido con userId + businessId |
| `TenantContext.java` | Utilidad estática para extraer tenant del SecurityContext |
| `JwtTokenProvider.java` | Claims `bid` y `uid` en tokens JWT |
| `JwtAuthenticationFilter.java` | Reconstruye VeltroUserDetails desde JWT claims |
| `AuthService.java` | Register crea Business + ADMIN, createWorker(), login con businessId |
| `AuthController.java` | Endpoint POST /auth/workers |
| `AuditCommandExecutor.java` | businessId en audit records via TenantContext |
| Todos los Services | TenantContext.getBusinessId() para queries y creación de entidades |
| Todos los Repositories | Métodos *ByBusinessId para aislamiento de datos |

### Frontend — API
| Archivo | Descripción |
|---------|-------------|
| `api/pos.ts` | Ventas, escáner IA (FormData), búsqueda productos |
| `api/catalog.ts` | CRUD productos + categorías |
| `api/purchasing.ts` | Órdenes de compra (7 endpoints) |
| `api/inventory.ts` | Alertas de inventario |
| `api/auth.ts` | Login (flat response), registro, cambio contraseña, createWorker() |
| `api/audit.ts` | Registros de auditoría |
| `api/dashboard.ts` | KPIs, exportación |

### Frontend — Componentes POS
| Archivo | Descripción |
|---------|-------------|
| `components/pos/ScannerContainer.tsx` | Cámara react-zxing + manual + IA 3s timer |
| `components/pos/AiIdentificationModal.tsx` | Modal sugerencias IA (Blob, español) |
| `components/pos/CartTable.tsx` | Tabla carrito (salePrice) |
| `components/pos/ConfirmModal.tsx` | Confirmación de venta |
| `components/pos/SaleReceipt.tsx` | Recibo post-venta |

### Frontend — Componentes Catálogo
| Archivo | Descripción |
|---------|-------------|
| `components/catalog/ProductScanner.tsx` | Escáner cámara + IA para formulario productos |
| `components/catalog/CategoryTree.tsx` | Árbol de categorías |
| `pages/catalog/ProductFormPage.tsx` | Crear/editar producto con escáner integrado |
| `pages/catalog/ProductListPage.tsx` | Lista de productos con paginación |

### Frontend — Stores
| Archivo | Descripción |
|---------|-------------|
| `stores/cartStore.ts` | Zustand cart (Product, salePrice) |
| `stores/authStore.ts` | JWT, usuario, rol, businessId, getBusinessId() |
| `stores/alertStore.ts` | Alertas de inventario |

### Frontend — Multi-Tenant
| Archivo | Descripción |
|---------|-------------|
| `types/index.ts` | LoginResponse plana, User con businessId, RegisterRequest con businessName |
| `pages/auth/LoginPage.tsx` | Construye User desde respuesta plana |
| `pages/auth/RegisterPage.tsx` | Envía businessName en registro |
| `hooks/useAuth.ts` | Construye User desde respuesta plana |
| `pages/settings/WorkersPage.tsx` | UI para crear CASHIER/WAREHOUSE (solo ADMIN) |
| `App.tsx` | Ruta /settings/workers |
| `components/layout/MainLayout.tsx` | Nav "Empleados" para ADMIN |

---

## Sobre el Hitbox del Escáner

El área de detección de la cámara (hitbox) se puede modificar en dos niveles:

### 1. Guía visual (overlay CSS)
En `ScannerContainer.tsx` y `ProductScanner.tsx`, el rectángulo blanco semi-transparente es solo una guía visual:
```tsx
<div className="w-3/5 h-1/3 border-2 border-white/40 rounded-lg" />
```
Modificar `w-3/5 h-1/3` cambia el tamaño visual del rectángulo guía.

### 2. Área de decodificación real (react-zxing)
`react-zxing` por defecto analiza toda el área del video. Para restringir el área de análisis, se usa `DecodeHintType` del hook `useZxing`:

```tsx
import { useZxing, DecodeHintType } from 'react-zxing';

const { ref } = useZxing({
  hints: new Map([
    [DecodeHintType.TRY_HARDER, true],  // Análisis más exhaustivo
  ]),
  timeBetweenDecodingAttempts: 300,  // ms entre intentos (default 300)
  onDecodeResult(result) { ... },
});
```

Para un crop real del área de análisis, se necesita capturar un sub-rectángulo del video antes de pasarlo al decoder, lo cual requiere implementación custom con canvas — actualmente no es necesario porque react-zxing funciona bien con toda el área.

---

**Última actualización:** 26 de Marzo, 2026
**Estado Backend:** 100% funcional — multi-tenant completo
**Estado Frontend:** 100% funcional — escáner cámara + IA + multi-tenant + registro + workers
**TypeScript:** 0 errores (`tsc --noEmit`)
**Despliegue:** Configurado para Heroku (backend) + Vercel (frontend)
**Nota:** `GET /api/v1/sales` (listar ventas) no existe como endpoint — el dashboard muestra ventas recientes

---

## Despliegue en Producción (Heroku + Vercel)

### Arquitectura de Despliegue

```
[Vercel - Frontend]  ←→  [Heroku - Backend API]  ←→  [Heroku PostgreSQL]
   React SPA                Spring Boot JAR              Managed DB
   vercel.json              Procfile + system.properties
```

### Backend — Heroku

#### Archivos de configuración

| Archivo | Propósito |
|---------|-----------|
| `Procfile` | Define el proceso web: `java -jar target/Veltro-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod` |
| `system.properties` | Especifica Java 21: `java.runtime.version=21` |
| `application-prod.yml` | Config de producción (gitignored, usa env vars) |

#### Variables de Entorno Requeridas en Heroku

```bash
# Base de datos (automáticas con Heroku PostgreSQL addon)
# JDBC_DATABASE_URL, DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD

# Seguridad
JWT_SECRET=<generar con: openssl rand -base64 64>

# CORS — URL del frontend en Vercel
CORS_ALLOWED_ORIGINS=https://tu-app.vercel.app

# Perfil Spring Boot
SPRING_PROFILES_ACTIVE=prod

# IA (opcional — si se desea AI vision en producción)
OPENAI_API_KEY=<tu GitHub PAT o OpenAI key>
OPENAI_API_ENDPOINT=https://models.inference.ai.azure.com/chat/completions
OPENAI_MODEL=gpt-4o-mini
```

#### Pasos de Despliegue — Heroku

```bash
# 1. Crear app en Heroku
heroku create veltro-api

# 2. Agregar PostgreSQL addon
heroku addons:create heroku-postgresql:essential-0

# 3. Configurar variables de entorno
heroku config:set JWT_SECRET="$(openssl rand -base64 64)"
heroku config:set CORS_ALLOWED_ORIGINS="https://tu-app.vercel.app"
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set OPENAI_API_KEY="tu-api-key"

# 4. Deploy (push al remote de Heroku)
git push heroku main

# 5. Verificar logs
heroku logs --tail
```

#### Notas Heroku

- Heroku auto-detecta Java por la presencia de `pom.xml`
- Ejecuta `./mvnw clean install -DskipTests` automáticamente
- `$PORT` se asigna dinámicamente — `application-prod.yml` usa `${PORT:8080}`
- PostgreSQL addon provee `JDBC_DATABASE_URL` en formato JDBC correcto
- Flyway ejecuta migraciones automáticamente al iniciar

### Frontend — Vercel

#### Archivos de configuración

| Archivo | Propósito |
|---------|-----------|
| `frontend/vercel.json` | Rewrites para SPA routing (todas las rutas → `index.html`) |
| `frontend/.env.example` | Template de variables de entorno |

#### Variables de Entorno en Vercel

```bash
VITE_API_BASE_URL=https://veltro-api.herokuapp.com/api/v1
```

#### Configuración en Vercel Dashboard

- **Framework Preset**: Vite
- **Root Directory**: `frontend`
- **Build Command**: `npm run build`
- **Output Directory**: `dist`
- **Install Command**: `npm install --legacy-peer-deps`

#### Pasos de Despliegue — Vercel

1. Conectar el repositorio Git en Vercel
2. Configurar "Root Directory" como `frontend`
3. Agregar variable de entorno `VITE_API_BASE_URL` con la URL del backend Heroku
4. Deploy automático con cada push

### CORS — Flujo Cross-Origin

El frontend en Vercel (`https://tu-app.vercel.app`) hace requests al backend en Heroku (`https://veltro-api.herokuapp.com`). CORS está configurado en `SecurityConfig.java` mediante:

- **Property**: `cors.allowed-origins` (leída con `@Value`)
- **Env var**: `CORS_ALLOWED_ORIGINS` (seteada en Heroku)
- **Default (dev)**: `http://localhost:5173,https://localhost:5173`
- **Métodos permitidos**: GET, POST, PUT, PATCH, DELETE, OPTIONS
- **Credentials**: habilitadas
- **Max age**: 3600s

### Frontend API Base URL

En `frontend/src/api/client.ts`:
```typescript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';
```

- **Desarrollo local**: `VITE_API_BASE_URL` vacío → usa `/api/v1` (Vite proxy lo redirige a `localhost:8080`)
- **Producción (Vercel)**: `VITE_API_BASE_URL=https://veltro-api.herokuapp.com/api/v1` → requests directos al backend

### Checklist Pre-Deploy

- [ ] `JWT_SECRET` generado y configurado en Heroku (mínimo 256 bits)
- [ ] `CORS_ALLOWED_ORIGINS` apunta al dominio de Vercel
- [ ] `VITE_API_BASE_URL` en Vercel apunta al backend Heroku (con `/api/v1`)
- [ ] PostgreSQL addon activo en Heroku
- [ ] `SPRING_PROFILES_ACTIVE=prod` en Heroku
- [ ] Verificar que Flyway migraciones corren sin errores en producción
- [ ] Probar login, registro, y operaciones CRUD post-deploy
- [ ] Verificar que CORS funciona (browser console sin errores de preflight)
