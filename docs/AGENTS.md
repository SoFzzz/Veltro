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

### Backend — MVC Clásico

La estructura backend ya no sigue el layout hexagonal anterior. El proyecto fue refactorizado hacia una organización clásica de Spring Boot con capas técnicas más directas:

```
src/main/java/com/veltro/inventory/
├── controller/         # REST controllers
├── service/            # Servicios, listeners, strategies y facades
├── repository/         # Spring Data JPA repositories
├── model/              # Entidades JPA, enums y estados de dominio
├── dto/                # Request/response DTOs
├── mapper/             # MapStruct mappers
├── security/           # JWT, TenantContext, VeltroUserDetails, filtros
├── config/             # Security, CORS, auditoría y beans
├── exception/          # Excepciones de negocio e infraestructura
├── event/              # Eventos de dominio/aplicación
└── VeltroApplication.java
```

**Implicaciones de esta refactorización:**
- La documentación histórica que mencionaba `domain/`, `application/` e `infrastructure/` debe considerarse obsoleta.
- Los controladores exponen endpoints REST y delegan directamente en servicios Spring.
- Los servicios concentran la lógica de aplicación, la orquestación multi-tenant, la auditoría y la publicación de eventos.
- Los repositorios exponen métodos tenant-aware con sufijo `AndBusinessId(...)`.
- `security/` dejó de ser un detalle periférico y ahora es una pieza central del flujo de negocio por la dependencia de `TenantContext`.

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
7. Retorna `ProductSuggestionResponse` con dos modos:
   - match de catalogo: `productId != null`, `barcode` poblado, `suggested* = null`
   - sugerencia para creacion: `productId = null`, `suggestedName/suggestedBarcode/suggestedPrice` poblados si la IA los detecta
8. Usuario selecciona sugerencia:
   - si `productId != null` → busca por `productId` o barcode/nombre y agrega al carrito
   - si `productId == null` → el frontend debe ofrecer CTA "Crear producto" y navegar al formulario de catalogo con prefill

### Flujo de IA en Catálogo (ProductFormPage)

1. En la página de crear/editar producto, hay un botón "Escanear producto con cámara o IA"
2. Al presionar, se abre el `ProductScanner` con cámara
3. Si se detecta código de barras → verifica si el producto ya existe en la BD
   - Si existe: muestra advertencia con el nombre del producto
   - Si no existe: llena el campo de código de barras
4. Si no se detecta código → botón "Identificar con IA" (mismo flujo 3s)
5. IA retorna sugerencias:
   - si `productId != null`, la sugerencia representa un producto ya existente
   - si `productId == null`, la sugerencia representa un posible producto nuevo y el frontend debe usar `suggested*`
6. Usuario selecciona una sugerencia sin match → auto-completa campos:
   - Nombre desde `suggestedName`
   - Código de barras desde `suggestedBarcode`
   - Precio de venta desde `suggestedPrice`
7. Usuario revisa, completa datos faltantes, y guarda

### Tipos Frontend (pos.ts)

```typescript
interface SuggestedProduct {
  productId: number | null;
  productName: string;
  confidence: number;        // 0.0–1.0
  barcode: string | null;
  suggestedName: string | null;
  suggestedBarcode: string | null;
  suggestedPrice: string | null;
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

### Estado del registro historico

`docs/BUGASOS.txt` se conserva solo por trazabilidad.
Todos los bugs y mejoras listados en ese archivo ya fueron resueltos al 100% y no representan backlog activo.

### Backend
1. `App.tsx` InventoryPage duplicado → corregido con Suspense
2. Audit `findByFilters` query → fix CAST para enums null
3. POS API path + quick sale → endpoint `POST /sales/quick` con `QuickSaleRequest`
4. Jackson LocalDateTime → `.toString()` en snapshots
5. Payment methods → YAPE/PLIN en lugar de CHECK
6. Product/Category delete → cambiado a PUT deactivate (soft delete)
7. PO `requested_by` NOT NULL → `UserRepository` + `SecurityContextHolder`
8. DB constraint `ck_alert_type` → ALTER CHECK para incluir 6 tipos
9. Refactorización backend → migración desde layout hexagonal a MVC clásico (`controller/service/repository/model/security/config`)
10. Tests de servicios multi-tenant → adopción del patrón con `VeltroUserDetails` en `SecurityContextHolder` para que `TenantContext` funcione en unit tests
11. `ForensicAuditServiceTest` y `DashboardServiceTest` → actualización de stubs a firmas tenant-aware con `businessId`
12. `SaleServiceTest` y `SupplierServiceTest` → corrección de `when(...)` incompletos y limpieza explícita de `SecurityContextHolder` en `tearDown`
13. `VeltroApplicationTests` → deshabilitado con `@Disabled("Requiere PostgreSQL real, no compatible con H2 en tests")` porque Flyway usa SQL específico de PostgreSQL

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
| `controller/*` | Controladores REST del MVC clásico |
| `service/*` | Servicios de aplicación, listeners, estrategias y facades |
| `repository/*` | Repositorios JPA tenant-aware |
| `model/*` | Entidades JPA, enums y estados de negocio |
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

### Backend — Testing Multi-Tenant
| Archivo | Descripción |
|---------|-------------|
| `src/test/java/.../AlertServiceTest.java` | Patrón base de test tenant-aware con `VeltroUserDetails` |
| `src/test/java/.../ForensicAuditServiceTest.java` | Contexto tenant-aware + stubs con `businessId` |
| `src/test/java/.../ProductServiceTest.java` | Contexto tenant-aware para `TenantContext` |
| `src/test/java/.../DashboardServiceTest.java` | Stubs con firmas nuevas `(..., businessId)` |
| `src/test/java/.../InventoryServiceTest.java` | Contexto tenant-aware para operaciones de inventario |
| `src/test/java/.../PurchaseOrderServiceTest.java` | Contexto tenant-aware con username real para `getCurrentUser()` |
| `src/test/java/.../SaleServiceTest.java` | Contexto tenant-aware + corrección de stubs incompletos |
| `src/test/java/.../SupplierServiceTest.java` | Contexto tenant-aware + corrección de stubs incompletos |
| `src/test/java/com/veltro/inventory/VeltroApplicationTests.java` | `@Disabled` por dependencia en PostgreSQL real/Flyway |

---

## Estrategia de Testing Actual

- Los unit tests de servicios que usen `TenantContext` deben autenticarse con `VeltroUserDetails`, no con `String`, `User` de Spring ni principals genéricos.
- El patrón recomendado es el de `AlertServiceTest`: autenticación en `@BeforeEach` y `SecurityContextHolder.clearContext()` en `@AfterEach`.
- Si un servicio usa repositorios tenant-aware, los `when(...)` deben coincidir exactamente con las firmas actuales que incluyen `businessId`.
- Los errores típicos después de cambios multi-tenant son:
- `IllegalStateException: No authenticated VeltroUserDetails found in SecurityContext`
- `PotentialStubbingProblem` por stubs con firmas viejas
- `UnfinishedStubbingException` por `when(...)` sin `thenReturn(...)`
- Estado actual de la suite Maven: todos los tests pasan y `VeltroApplicationTests` queda en `skipped` porque requiere PostgreSQL real y no H2.

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

## Seguridad de Credenciales

### Protección en Tránsito

El sistema envía credenciales de login (username/password) como JSON en texto plano al endpoint `POST /api/v1/auth/login`. Para proteger estas credenciales:

**HTTPS es OBLIGATORIO en producción:**
- Vercel enforza HTTPS automáticamente en todos los deploys de producción
- Heroku enforza HTTPS automáticamente en todas las apps (certificados SSL incluidos)
- Los navegadores modernos requieren HTTPS para APIs sensibles

**Verificaciones de Seguridad:**
- Las credenciales están protegidas por la capa de transporte TLS/SSL
- No se requiere encriptación adicional a nivel de aplicación cuando HTTPS está activo
- El frontend debe verificar que todas las requests se hacen sobre HTTPS en producción

### Consideraciones de Desarrollo

**Entorno Local (HTTP):**
- Las credenciales son visibles en DevTools Network tab
- Esto es aceptable solo en desarrollo local
- NUNCA usar HTTP en producción

**Entorno Producción (HTTPS):**
- Las credenciales están encriptadas end-to-end por TLS
- DevTools muestra el JSON pero el tráfico de red está protegido
- Los certificados SSL son manejados automáticamente por Vercel + Heroku

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
- [ ] Verificar HTTPS is enforced (Vercel + Heroku handle this automatically)
- [ ] Verificar que Flyway migraciones corren sin errores en producción
- [ ] Probar login, registro, y operaciones CRUD post-deploy
- [ ] Verificar que CORS funciona (browser console sin errores de preflight)

---

## Update 2026-04-09 - AI Catalog Matching + Create Product Prefill

### Estado actual del scanner IA

- `POST /api/v1/scanner/ai` ya no devuelve solo sugerencias textuales.
- Despues de parsear la respuesta del modelo, el backend intenta enriquecer cada sugerencia con datos del catalogo existente del negocio actual.
- Los campos que ahora puede devolver el backend son:
  - `productId`
  - `barcode`
  - `suggestedName`
  - `suggestedBarcode`
  - `suggestedPrice`

### Flujo backend real

1. `ScannerController` recibe la imagen multipart.
2. `ProductRecognitionService` delega en `AiVisionStrategy`.
3. `AiVisionStrategy` delega en `OpenAiVisionClient`.
4. `OpenAiVisionClient` llama al proveedor multimodal (GitHub Models, OpenRouter o Gemini segun configuracion).
5. `OpenAiVisionClient` parsea el JSON de respuesta a `ProductSuggestionResponse`.
6. Por cada sugerencia, `OpenAiVisionClient` invoca `ProductMatchingService`.
7. `ProductMatchingService` busca candidatos activos del mismo `businessId` usando `ProductRepository`.
8. Si hay un match claro, la sugerencia vuelve enriquecida con `productId` y `barcode`.
9. Si no hay match o el resultado es ambiguo, la sugerencia sigue siendo valida pero vuelve con `productId = null` y con `suggestedName`, `suggestedBarcode` y `suggestedPrice` cuando la IA los haya detectado.

### Reglas de matching

- El matching es conservador: prioriza no generar falsos positivos.
- Solo participan productos activos del tenant actual.
- La busqueda es por nombre, case-insensitive, usando una keyword significativa extraida del nombre sugerido por la IA.
- Si hay variantes ambiguas como `Sprite 500 ml` y `Sprite 1.5 L`, no se asigna `productId` automaticamente salvo que la sugerencia incluya volumen suficiente para desempatar.

### Archivos backend relevantes

- `src/main/java/com/veltro/inventory/service/OpenAiVisionClient.java`
  - parsea la respuesta IA y enriquece sugerencias con matching de catalogo
- `src/main/java/com/veltro/inventory/service/ProductMatchingService.java`
  - servicio dedicado para matching conservador contra el catalogo activo del tenant
- `src/main/java/com/veltro/inventory/repository/ProductRepository.java`
  - incluye `findTop10ByActiveTrueAndBusinessIdAndNameContainingIgnoreCase(...)`

### Contrato efectivo de `ProductSuggestionResponse`

`SuggestedProduct` ahora tiene este shape efectivo:

```typescript
interface SuggestedProduct {
  productId: number | null;
  productName: string;
  confidence: number;
  barcode: string | null;
  suggestedName: string | null;
  suggestedBarcode: string | null;
  suggestedPrice: string | null;
}
```

Semantica contractual:
- Si `productId != null`, la sugerencia matcheo un producto existente del catalogo:
  - usar datos del catalogo
  - `suggestedName`, `suggestedBarcode` y `suggestedPrice` vienen en `null`
- Si `productId == null`, la sugerencia representa un posible producto nuevo:
  - usar `suggestedName`, `suggestedBarcode` y `suggestedPrice` para pre-llenar el formulario
  - `barcode` viene en `null` porque no hay match de catalogo

### Trabajo pendiente para frontend

Un agente de frontend que implemente esta feature debe hacer lo siguiente:

1. En `AiIdentificationModal` o componente equivalente:
   - detectar cuando una sugerencia tenga `productId == null`
   - renderizar un boton o CTA visible como `Crear producto`

2. En el flujo POS (`ScannerContainer` / `AiIdentificationModal`):
   - si `productId != null`, mantener el flujo actual de agregar producto existente
   - si `productId == null`, navegar al formulario de catalogo

3. En la navegacion al formulario:
   - preferir `React Router state` para pasar el prefill temporal
   - payload recomendado:
     ```ts
     {
       aiPrefill: {
         name: suggestedName,
         barcode: suggestedBarcode,
         salePrice: suggestedPrice
       }
     }
     ```
   - query params solo como fallback si el equipo necesita deep-linking

4. En `ProductFormPage`:
   - leer `location.state.aiPrefill`
   - rellenar solo al entrar en modo crear, no en modo editar
   - mapear:
     - nombre ← `suggestedName`
     - barcode ← `suggestedBarcode`
     - precio de venta ← `suggestedPrice`
   - no inventar otros campos; categoria, descripcion, costo y stock siguen siendo manuales

5. Compatibilidad:
   - si la IA devuelve solo `suggestedName` y no barcode/precio, el formulario debe completar solo lo disponible
   - si el usuario llega al formulario sin `state`, el flujo normal de creacion no debe cambiar

### Testing agregado

- `OpenAiVisionClientTest`
  - cubre enriquecimiento con match y fallback sin match
- `ProductMatchingServiceTest`
  - cubre match unico, ambiguedad entre variantes y ausencia de candidatos

---

## Update 2026-04-20 - Carga de Variables de Entorno + ARREGLOS

### Carga de .env al Iniciar

El backend ahora carga variables de entorno desde un archivo `.env` en la raíz del proyecto al iniciar, sin hardcodear valores sensibles en configuración.

**Cómo funciona:**
- `application.yaml` importa el `.env` como configuración opcional:
  ```yaml
  spring:
    config:
      import: optional:file:./.env[.properties]
  ```
- Cualquier perfil puede usar placeholders como `${GEMINI_API_KEY}` y `${DB_PASSWORD}` que se resuelven desde el `.env`.
- El archivo `.env` debe estar en la raíz del proyecto (mismo nivel que `pom.xml`).

**Notas:**
- El working directory al ejecutar debe ser la raíz del proyecto para que `./.env` se resuelva correctamente.
- Si ejecutas desde el IDE, asegúrate que el directorio de trabajo sea la raíz.
- `.env` está en `.gitignore` — nunca hacer commit de este archivo.

### Métodos de Pago — Soporte para Alias

El enum `PaymentMethod` ahora acepta aliases de los valores que el frontend puede enviar:
- `NEQUI`, `DAVIPLATA`, `DAVI_PLATA` → `TRANSFER`
- `TARJETA` → `CARD`
- `EFECTIVO` → `CASH`
- `MIXTO` → `MIXED`

Esto permite que el frontend envíe los nombres visibles en español sin que el backend rechace.

**Archivo relevante:**
- `src/main/java/com/veltro/inventory/model/PaymentMethod.java`

### Validación de Stock en Ventas Rápidas

El endpoint `POST /api/v1/sales/quick` ahora valida stock disponible antes de procesar. Si el stock es insuficiente, devuelve `422 Unprocessable Entity` con mensaje claro incluyendo producto, disponible y solicitado.

**Validación preventiva en:**
- `src/main/java/com/veltro/inventory/service/SaleService.java`

### Crear/Editar Empleados — Parsing de Roles

La creación y edición de empleados ahora acepta roles en español:
- `CAJERO`, `Cajero` → `CASHIER`
- `ALMACEN`, `Almacén`, `BODEGA` → `WAREHOUSE`
- `ADMIN`, `ADMINISTRADOR` → `ADMIN` (solo para registro inicial, no para workers)

Esto corrige el bug donde el frontend envía "Cajero" o "Almacén" y el backend rechazaba con 400.

**Archivo relevante:**
- `src/main/java/com/veltro/inventory/service/AuthService.java`
