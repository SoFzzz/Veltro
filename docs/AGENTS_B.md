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
"mvnw" clean package -DskipTests
java -jar target/Veltro-0.0.1-SNAPSHOT.jar > target/backend.log 2>&1 &

# Matar Java antes de rebuild (Windows)
taskkill //F //IM java.exe

# TypeScript check (si hay frontend)
cd frontend && npx tsc --noEmit
```

### Rutas API Base
- Backend: `http://localhost:8080/api/v1/`
- Frontend: `http://localhost:5173` (si existe)

---

## Arquitectura Backend

### Estructura de Paquetes (MVC + Capas de Dominio)

```
src/main/java/com/veltro/inventory/
├── VeltroApplication.java
│
├── controller/              # REST controllers (endpoints HTTP)
│   ├── AlertController.java
│   ├── AuditController.java
│   ├── AuthController.java
│   ├── CategoryController.java
│   ├── DashboardController.java
│   ├── InventoryController.java
│   ├── ProductController.java
│   ├── PurchaseOrderController.java
│   ├── ReportController.java
│   ├── SaleController.java
│   ├── ScannerController.java
│   └── SupplierController.java
│
├── service/                  # Lógica de aplicación y servicios
│   ├── SaleService.java      # Gestión de ventas POS
│   ├── ProductService.java
│   ├── CategoryService.java
│   ├── InventoryService.java
│   ├── PurchaseOrderService.java
│   ├── SupplierService.java
│   ├── AlertService.java
│   ├── DashboardService.java
│   ├── AuthService.java
│   ├── ForensicAuditService.java
│   ├── ReportService.java
│   ├── ProductRecognitionService.java
│   ├── ProductMatchingService.java
│   │   # --- Estrategias de Escaneo (Strategy Pattern) ---
│   ├── ScannerStrategy.java       # Interfaz del patrón
│   ├── BarcodeRecognitionStrategy.java
│   ├── AiVisionStrategy.java      # Integración con GPT-4o-mini
│   │   # --- Chain of Responsibility para Alertas ---
│   ├── AlertHandler.java          # Interfaz base
│   ├── OutOfStockHandler.java
│   ├── LowStockHandler.java
│   ├── OverstockHandler.java
│   ├── AlertChainBuilder.java     # Configura la cadena
│   ├── StockAlertEvaluationContext.java
│   │   # --- Auditoría Forense ---
│   ├── AuditCommandExecutor.java  # Command Pattern
│   ├── AuditableCommand.java
│   ├── RequestAuditContext.java
│   │   # --- Exportación de Reportes (Factory Method) ---
│   ├── ReportExporter.java        # Interfaz
│   ├── PdfReportExporter.java
│   ├── ExcelReportExporter.java
│   ├── ReportService.java
│   │   # --- Helpers y Configuración ---
│   ├── AlertConfigurationService.java
│   └── DashboardQueryRepository.java
│
├── state/                    # State Pattern para ciclos de vida
│   ├── SaleState.java            # Interfaz
│   ├── SaleInProgressState.java
│   ├── SaleCompletedState.java
│   ├── SaleVoidedState.java
│   ├── PurchaseOrderState.java
│   ├── PurchaseOrderPendingState.java
│   ├── PurchaseOrderPartialState.java
│   ├── PurchaseOrderReceivedState.java
│   └── PurchaseOrderVoidedState.java
│
├── listener/                # Observer Pattern para eventos
│   ├── DeductStockSaleListener.java    # SaleCompletedEvent → deduce stock
│   ├── RestoreStockSaleListener.java   # SaleVoidedEvent → restaura stock
│   ├── IncrementStockOrderListener.java # OrderReceivedEvent → incrementa stock
│   └── EvaluateStockAlertsListener.java # StockChangedEvent → evalúa alertas
│
├── event/                   # Domain events
│   ├── SaleCompletedEvent.java
│   ├── SaleVoidedEvent.java
│   ├── OrderReceivedEvent.java
│   ├── StockChangedEvent.java
│   ├── SaleItemInfo.java
│   └── ReceivedItemInfo.java
│
├── repository/              # Spring Data JPA (patrón DAO)
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   ├── InventoryRepository.java
│   ├── SaleRepository.java
│   ├── PurchaseOrderRepository.java
│   ├── SupplierRepository.java
│   ├── AlertRepository.java
│   ├── AlertConfigurationRepository.java
│   ├── AuditRecordRepository.java
│   ├── InventoryMovementRepository.java
│   ├── BusinessRepository.java
│   └── UserRepository.java
│
├── model/                   # Entidades JPA y enums
│   ├── ProductEntity.java
│   ├── CategoryEntity.java
│   ├── InventoryEntity.java
│   ├── InventoryMovementEntity.java
│   ├── SaleEntity.java         # Tiene estado SaleState
│   ├── SaleDetailEntity.java
│   ├── PurchaseOrderEntity.java # Tiene estado PurchaseOrderState
│   ├── PurchaseOrderDetailEntity.java
│   ├── SupplierEntity.java
│   ├── AlertEntity.java
│   ├── AlertConfigurationEntity.java
│   ├── AuditRecordEntity.java
│   ├── BusinessEntity.java     # Raíz del multi-tenant
│   ├── UserEntity.java
│   ├── AbstractAuditableEntity.java # createdAt, updatedAt, createdBy
│   └── enums/: SaleStatus, PurchaseOrderStatus, PaymentMethod,
│              MovementType, AlertType, AlertSeverity, AuditAction, AuditEntityType, Role
│
├── dto/                     # Request/Response DTOs
│   ├── auth/: LoginRequest, LoginResponse, RegisterRequest, ChangePasswordRequest, WorkerResponse
│   ├── catalog/: ProductResponse, CreateProductRequest, UpdateProductRequest,
│   │            CategoryResponse, CreateCategoryRequest, UpdateCategoryRequest
│   ├── pos/: SaleResponse, SaleDetailResponse, AddItemRequest, ModifyItemRequest,
│   │        ConfirmSaleRequest, QuickSaleRequest
│   ├── inventory/: InventoryResponse, StockEntryRequest, StockExitRequest,
│   │              StockAdjustmentRequest, AlertResponse, AlertConfigurationResponse,
│   │              InventoryMovementResponse, UpdateStockLimitsRequest, UpdateAlertConfigurationRequest
│   ├── purchasing/: PurchaseOrderResponse, CreatePurchaseOrderRequest,
│   │               SupplierResponse, CreateSupplierRequest, UpdateSupplierRequest,
│   │               AddOrderItemRequest, PurchaseOrderDetailResponse
│   ├── scanner/: ProductSuggestionResponse
│   ├── dashboard/: DashboardResponse
│   ├── audit/: AuditRecordResponse, AuditFilterRequest, AuditInfo
│   ├── report/: ReportType, ProfitabilityReport
│   └── common/: ErrorResponse, PageResponse
│
├── mapper/                  # MapStruct (type-safe mapping)
│   ├── ProductMapper.java, CategoryMapper.java, SaleMapper.java,
│   ├── PurchaseOrderMapper.java, InventoryMapper.java, SupplierMapper.java, etc.
│
├── security/               # JWT + Multi-Tenant
│   ├── JwtTokenProvider.java      # Genera/valida tokens con claims uid, bid, role
│   ├── JwtAuthenticationFilter.java # Filtro que reconstruye VeltroUserDetails
│   ├── VeltroUserDetails.java      # UserDetails extendido con userId + businessId
│   ├── TenantContext.java          # Utilidad estática para obtener tenant actual
│   ├── RequestContextHolder.java
│   └── CustomUserDetailsService.java
│
├── config/                 # Configuración de Spring
│   ├── SecurityConfig.java        # CORS, JWT filter, autorización por roles
│   ├── AuditConfig.java           # Auditoría con VeltroAuditorAware
│   ├── ClientIpContextFilter.java
│   ├── LoginRateLimitFilter.java
│   ├── RateLimitConfig.java
│   ├── JwtProperties.java
│   ├── RestTemplateConfig.java
│   └── VeltroAuditorAware.java
│
├── exception/              # Excepciones de negocio
│   ├── GlobalExceptionHandler.java # @RestControllerAdvice
│   ├── NotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── InvalidStateTransitionException.java
│   ├── InsufficientStockException.java
│   ├── InvalidPaymentException.java
│   ├── InvalidPriceException.java
│   ├── MaxStockExceededException.java
│   └── InactiveResourceExistsException.java
│
├── infrastructure/        # Detalles de infraestructura
│   └── ai/
│       ├── VisionApiConfig.java
│       └── VisionClient.java
│
├── bootstrap/
│   └── DevDataInitializer.java     # Datos de desarrollo
│
└── resources/
    └── db/migration/               # Flyway migrations
        ├── V1__complete_schema.sql
        ├── V2__seed_data.sql
        ├── V3__multi_tenant.sql
        └── V4__seed_dev_users.sql
```

---

## Lógica del Funcionamiento

### 1. Flujo de Autenticación y Multi-Tenant

```
Request con JWT
    ↓
JwtAuthenticationFilter (extrae token del header Authorization)
    ↓
JwtTokenProvider.extractClaims() → { sub, uid, bid, role }
    ↓
VeltroUserDetails(username, userId, businessId, authorities)
    ↓
SecurityContextHolder.setAuthentication(UsernamePasswordAuthenticationToken)
    ↓
TenantContext.getBusinessId() / TenantContext.getUserId() (usable en cualquier servicio)
```

- El `businessId` se inyecta en todas las queries para garantizar aislamiento total
- Cada negocio tiene su propio namespace de: usuarios, productos, ventas, órdenes, etc.
- Un mismo username puede existir en negocios diferentes (constraint unique por businessId)

### 2. Ciclo de Vida de una Venta (State Pattern)

```
IN_PROGRESS (estado inicial)
    ├── addItem() → permite agregar/modificar/eliminar items del carrito
    ├── confirm() → valida pago y transiciona a COMPLETED
    │                  → publica SaleCompletedEvent
    │                  → AuditCommandExecutor registra auditoría before/after
    └── removeItem() → soft delete del item (active=false)

COMPLETED (estado terminal normal)
    └── voidSale() → transiciona a VOIDED
                       → publica SaleVoidedEvent
                       → restaura stock (RestoreStockSaleListener)
                       → registra auditoría

VOIDED (estado terminal)
    └── todas las operaciones lanzan InvalidStateTransitionException
```

Las transiciones合法性 las maneja cada estado (`SaleInProgressState`, `SaleCompletedState`, etc.)

### 3. Escaneo de Productos (Strategy Pattern)

```
ScannerController.POST /scanner/ai (multipart image)
    ↓
ProductRecognitionService.process(image)
    ↓
AiVisionStrategy (delega a OpenAiVisionClient)
    ├── llama GPT-4o-mini (GitHub Models API)
    ├── parsea respuesta JSON
    └── por cada sugerencia: ProductMatchingService.enrich(sugerencia)
        └── busca productos activos del tenant por nombre
            └── si match claro → productId populated
               si ambiguo o sin match → productId = null
    ↓
ProductSuggestionResponse (lista de sugerencias)
```

Matching conservativo: solo asigna `productId` si hay certeza, nunca自作主张.

### 4. Alertas de Inventario (Chain of Responsibility)

```
StockChangedEvent publicado por InventoryService
    ↓
EvaluateStockAlertsListener recibe el evento
    ↓
AlertHandler alertHandlerChain (configurado por AlertChainBuilder)
    ├── OutOfStockHandler (tipo=OUT_OF_STOCK, severidad=CRITICAL)
    │   └── si stock == 0 → crea alert
    ├── LowStockHandler (tipo=LOW_STOCK, severidad=WARNING)
    │   └── si stock <= warning threshold → crea alert
    └── OverstockHandler (tipo=OVERSTOCK, severidad=INFO)
        └── si stock > max threshold → crea alert
```

Cada handler puede crear una alerta o pasar al siguiente. Umbrales configurables por producto.

### 5. Órdenes de Compra (State Pattern)

```
PENDING (estado inicial)
    ├── addItem() → permite agregar items a la orden
    ├── submit() → transiciona a PARTIAL
    ├── confirm() → no permitido en PENDING
    └── removeItem() → soft delete

PARTIAL (orden enviada al proveedor)
    ├── receiveItem() → incrementa stock, puede dejar la orden en PARTIAL o RECEIVED
    │                    → publica OrderReceivedEvent → IncrementStockOrderListener
    └── voidOrder() → transiciona a VOIDED (restaura stock si aplica)

RECEIVED (completada)
    └── todas las operaciones de modificación lanzan excepción

VOIDED (cancelada)
    └── solo permite operaciones de consulta
```

### 6. Eventos de Dominio → Listeners (Observer Pattern)

| Evento | Listener | Acción |
|--------|----------|--------|
| `SaleCompletedEvent` | `DeductStockSaleListener` | `inventoryService.recordExit()` por cada item |
| `SaleVoidedEvent` | `RestoreStockSaleListener` | `inventoryService.recordEntry()` por cada item |
| `OrderReceivedEvent` | `IncrementStockOrderListener` | `inventoryService.recordEntry()` por cada item |
| `StockChangedEvent` | `EvaluateStockAlertsListener` | evalúa cadena de handlers de alertas |

### 7. Auditoría Forense (Command Pattern)

```java
auditCommandExecutor.execute(
    AuditEntityType.SALE,
    saleId,
    AuditAction.CONFIRM,
    () -> beforeSnapshot,    // supplier antes del cambio
    () -> sale,              // entidad actual (snapshot se construye via toString())
    (result) -> afterSnapshot, // construido desde saved
    RequestAuditContext.empty()
);
```

Captura before/after para cualquier operación CRUD. Usa `TenantContext` para asociar el registro al negocio actual.

---

## Migraciones de Base de Datos (Flyway)

| Migración | Contenido |
|-----------|-----------|
| `V1__complete_schema.sql` | Schema completo: todas las tablas, constraints, secuencias |
| `V2__seed_data.sql` | Datos iniciales de categorías y productos de ejemplo |
| `V3__multi_tenant.sql` | Tabla `business`, columna `business_id` en todas las tablas principales, constraints únicos por tenant |
| `V4__seed_dev_users.sql` | Usuarios de desarrollo: admin2, owner_test, cashier_test |
| `V5__add_pgvector_and_clip.sql` | Extensión vector, tabla `product_embeddings` con índice HNSW para búsqueda semántica CLIP |

### Búsqueda Semántica con CLIP (pgvector)

```
Tabla: product_embeddings
- id (PK)
- product_id (FK → products.id)
- embedding vector(512)  -- vector CLIP de 512 dimensiones
- created_at

Índice: idx_product_embeddings_hnsw (HNSW con cosine distance)
```

Repository: `ProductEmbeddingRepository` permite:
- `findMostSimilarProduct(float[] embedding)` → búsqueda de producto más similar por similitud coseno
- `insertEmbedding(Long productId, float[] embedding)` →插入embedding

Servicio: `ClipInferenceService` carga modelo ONNX desde `src/main/resources/models/clip-image-vit-32.onnx`

---

## Patrones de Diseño Implementados

| Patrón | Ubicación | Propósito |
|--------|-----------|-----------|
| **Multi-Tenant (Shared DB)** | `TenantContext`, `business_id` en todas las tablas | Aislamiento de datos por negocio |
| **State Pattern** | `state/Sale*.java`, `state/PurchaseOrder*.java` | Ciclo de vida de ventas y órdenes |
| **Observer Pattern** | `event/*.java`, `listener/*.java` | Reacción a cambios de dominio |
| **Chain of Responsibility** | `AlertChainBuilder`, `AlertHandler` | Evaluación de alertas de inventario |
| **Strategy Pattern** | `ScannerStrategy`, `AiVisionStrategy`, `BarcodeRecognitionStrategy` | Escaneo por código vs. IA |
| **Factory Method** | `ReportExporter`, `PdfReportExporter`, `ExcelReportExporter` | Creación de reportes |
| **Command Pattern (Functional)** | `AuditCommandExecutor` | Auditoría forense con snapshots |

---

## Configuración de IA

### Vision API (GPT-4o-mini vía GitHub Models)

```yaml
# application.yaml (o .env)
spring.config.import: optional:file:./.env[.properties]

veltro:
  ai:
    openai:
      enabled: true
      api-key: ${OPENAI_API_KEY}
      api-endpoint: https://models.inference.ai.azure.com/chat/completions
      model: gpt-4o-mini
      max-tokens: 1000
      timeout-seconds: 30
      max-image-size-mb: 10
      max-retries: 3
```

### CLIP (Modelo ONNX para embeddings visuales)

```
Modelo: clip-image-vit-32.onnx
Ubicación: src/main/resources/models/
Dimensión embedding: 512
Infraestructura: onnxruntime-java (carga en ClipInferenceService)
```

El backend soporta múltiples proveedores de IA (GitHub Models, OpenRouter, Gemini) configurados en `VisionApiConfig`.

---

## Endpoints Principales

### Auth
- `POST /api/v1/auth/login` → `{ accessToken, refreshToken, username, role, businessId }` (respuesta plana)
- `POST /api/v1/auth/register` → crea business + admin user
- `POST /api/v1/auth/workers` → crea CASHIER/WAREHOUSE (solo ADMIN)
- `PUT /api/v1/auth/change-password`

### Catálogo
- `GET/POST /api/v1/products`
- `GET/PUT/DELETE /api/v1/products/{id}`
- `GET /api/v1/products/barcode/{code}`
- `GET/POST /api/v1/categories`
- `GET/PUT/DELETE /api/v1/categories/{id}`

### POS
- `POST /api/v1/sales/start`
- `POST /api/v1/sales/{id}/items`
- `PUT /api/v1/sales/{id}/items/{detailId}`
- `DELETE /api/v1/sales/{id}/items/{detailId}`
- `POST /api/v1/sales/{id}/confirm`
- `POST /api/v1/sales/quick` → venta rápida en una transacción
- `GET /api/v1/sales/{id}`

### Scanner IA
- `POST /api/v1/scanner/ai` (multipart) → sugerencias de productos
- `GET /api/v1/scanner/status` → `{ BARCODE: bool, AI_VISION: bool }`
- `GET /api/v1/scanner/ai/available`

### Inventario
- `GET /api/v1/inventory`
- `PUT /api/v1/inventory/{id}/entry` → stock entry
- `PUT /api/v1/inventory/{id}/exit` → stock exit
- `PUT /api/v1/inventory/{id}/adjust` → ajuste manual
- `GET /api/v1/inventory/alerts`
- `GET /api/v1/inventory/alerts/configuration`
- `PUT /api/v1/inventory/alerts/configuration`

### Órdenes de Compra
- `GET/POST /api/v1/purchase-orders`
- `GET/PUT /api/v1/purchase-orders/{id}`
- `POST /api/v1/purchase-orders/{id}/items`
- `POST /api/v1/purchase-orders/{id}/submit`
- `POST /api/v1/purchase-orders/{id}/receive`
- `POST /api/v1/purchase-orders/{id}/void`
- `GET/POST /api/v1/suppliers`
- `GET/PUT/DELETE /api/v1/suppliers/{id}`

### Dashboard
- `GET /api/v1/dashboard` → KPIs (total revenue, sales count, top products, low stock)
- `GET /api/v1/reports/profitability`
- `GET /api/v1/reports/export?type=PDF|EXCEL`

### Auditoría
- `GET /api/v1/audit` (con filtros por entityType, action, dateRange, userId)

---

## Testing

Los tests de servicios que usan `TenantContext` requieren autenticación con `VeltroUserDetails` en el `SecurityContextHolder`:

```java
@BeforeEach
void setUp() {
    VeltroUserDetails user = new VeltroUserDetails(
        "testuser", "", true, true, true, true,
        Collections.emptyList(),
        1L, 1L, "ADMIN"
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities())
    );
}

@AfterEach
void tearDown() {
    SecurityContextHolder.clearContext();
}
```

`VeltroApplicationTests` está deshabilitado con `@Disabled` porque requiere PostgreSQL real (Flyway no es compatible con H2 en tests).

---

## Despliegue

### Docker Compose (Desarrollo Local)

```yaml
services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: veltro-postgres
    environment:
      POSTGRES_DB: ${DB_NAME:-veltro}
      POSTGRES_USER: ${DB_USER:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: veltro-backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev-with-ai
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME:-veltro}
      SPRING_DATASOURCE_USERNAME: ${DB_USER:-postgres}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-postgres}
      JWT_SECRET: dev-secret-key-minimum-32-chars-long
      GEMINI_API_KEY: ${GEMINI_API_KEY:-dev-gemini-key}
      SERVER_PORT: 8080
    depends_on:
      - postgres

volumes:
  pgdata:
```

**Dockerfile** optimizado para CLIP con ONNX Runtime:
- Imagen base `eclipse-temurin:21-jdk` para build, `eclipse-temurin:21-jre` para runtime
- Instalación de `libstdc++6` y `libgomp1` (dependencias nativas de onnxruntime)
- Modelo ONNX desplegado en `src/main/resources/models/clip-image-vit-32.onnx`

### Heroku (Producción)

- **Backend**: Heroku con Procfile + system.properties (Java 21), PostgreSQL addon
- **Frontend**: Vercel (si existe carpeta `frontend/`)
- CORS configurado via `CORS_ALLOWED_ORIGINS` environment variable
- `.env` en raíz para variables sensibles (JWT_SECRET, OPENAI_API_KEY, etc.)

---

**Última actualización:** Mayo 2026 | CLIP + Docker integration