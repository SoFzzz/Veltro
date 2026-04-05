# Frontend Handoff - Veltro

Actualizado contra el backend vigente del repositorio al 2026-04-05.

## 1. Estado general

El frontend original no esta presente actualmente en el repo, pero el backend y la documentacion confirman que el frontend esperado era una app React + TypeScript + Vite con auth JWT, multi-tenant por `businessId`, Zustand, React Router, React Hook Form y Zod.

Tomar como fuente principal:
- `docs/AGENTS.md`
- codigo backend bajo `src/main/java/com/veltro/inventory/`

No usar como fuente operativa principal documentos historicos que contradigan el backend actual.

## 2. Stack frontend esperado

- React 19
- TypeScript 5.9
- Vite 8
- React Router 7
- Zustand
- React Query
- React Hook Form
- Zod
- Tailwind CSS 4
- Vitest
- Testing Library
- MSW

## 3. Estructura frontend esperada

Estructura minima esperada segun `docs/AGENTS.md`:

- `frontend/src/api/client.ts`
- `frontend/src/api/auth.ts`
- `frontend/src/stores/authStore.ts`
- `frontend/src/types/index.ts`
- `frontend/src/pages/auth/LoginPage.tsx`
- `frontend/src/pages/auth/RegisterPage.tsx`
- `frontend/src/pages/settings/WorkersPage.tsx`
- `frontend/src/components/auth/AuthGuard.tsx`
- `frontend/src/components/auth/RoleGuard.tsx`
- `frontend/src/components/layout/MainLayout.tsx`
- `frontend/src/App.tsx`

Si los nombres finales cambian, respetar el contrato funcional equivalente.

## 4. Fix de UI requerido

Aplicar `maxLength={20}` en UI a estos campos:

### Login

Archivo objetivo principal:
- `frontend/src/pages/auth/LoginPage.tsx`

Cambios:
- input `username`: `maxLength={20}`
- input `password`: `maxLength={20}`

Notas:
- El login sigue haciendo `POST /api/v1/auth/login` con `{ username, password }`
- No tocar logica JWT
- No tocar `authStore.ts`
- No cambiar DTOs backend

### Alta de usuarios

Replicar el mismo limite de UI en formularios de creacion de usuarios:

- `frontend/src/pages/auth/RegisterPage.tsx`
- `frontend/src/pages/settings/WorkersPage.tsx`

Cambios:
- input `username`: `maxLength={20}`
- input `password`: `maxLength={20}`

No aplicar este limite extra a:
- `email`
- `businessName`
- `role`

## 5. Contrato auth actual del backend

Base URL:
- `http://localhost:8080/api/v1`

### POST `/auth/login`

Request:

```json
{
  "username": "admin2",
  "password": "admin123"
}
```

Response:

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

Importante:
- La respuesta es plana
- No existe `user` anidado
- El frontend debe construir manualmente el objeto `User`

### POST `/auth/register`

Registro de nuevo negocio.

Request:

```json
{
  "username": "owner",
  "email": "owner@test.com",
  "password": "pass12345",
  "businessName": "Mi Tienda"
}
```

Comportamiento:
- crea `Business`
- crea `User` con rol `ADMIN`
- vincula el usuario al nuevo `businessId`

### POST `/auth/workers`

Solo ADMIN autenticado.

Request:

```json
{
  "username": "cashier1",
  "email": "cashier@test.com",
  "password": "pass12345",
  "role": "CASHIER"
}
```

Comportamiento:
- crea usuario en el mismo `businessId` del ADMIN logueado
- roles validos: `CASHIER`, `WAREHOUSE`
- no usar este endpoint para crear ADMIN

### POST `/auth/refresh`

Request:

```json
{
  "refreshToken": "..."
}
```

Response:
- mismo shape plano de login
- devuelve nuevo `accessToken`
- conserva `refreshToken`

### POST `/auth/logout`

- backend stateless
- el cliente debe descartar tokens localmente

### PUT `/auth/change-password`

Request:

```json
{
  "currentPassword": "oldpass123",
  "newPassword": "newpass123"
}
```

## 6. Reglas de validacion relevantes para frontend

### LoginRequest

Backend:
- `username`: requerido, max 50
- `password`: requerido, min 6, max 100

UI requerida adicional:
- `username`: max 20
- `password`: max 20

### RegisterRequest

Backend:
- `username`: requerido, min 3, max 50
- `email`: requerido, email valido
- `password`: requerido, min 8, max 20, sin espacios
- `role`: string
- `businessName`: requerido para registro de negocio, ignorado al crear worker

UI requerida adicional:
- `username`: max 20
- `password`: max 20

### ChangePasswordRequest

Backend:
- `currentPassword`: requerido
- `newPassword`: requerido, min 8, max 20, sin espacios

## 7. Auth store esperado

`authStore.ts` debe persistir al menos:
- `accessToken`
- `refreshToken`
- `username`
- `role`
- `businessId`

Tambien debe exponer algo equivalente a:
- `login()`
- `logout()`
- `setTokens()`
- `getBusinessId()`

No mandar `businessId` en payloads de negocio.
El backend obtiene el tenant desde JWT.

## 8. User type esperado

El frontend debe tener un tipo `User` o equivalente con al menos:

```ts
type Role = 'ADMIN' | 'CASHIER' | 'WAREHOUSE'

type User = {
  username: string
  role: Role
  businessId: number | null
}
```

Y un tipo para login/refresh equivalente a:

```ts
type LoginResponse = {
  accessToken: string
  refreshToken: string
  tokenType: 'Bearer'
  expiresIn: number
  username: string
  role: Role
  businessId: number | null
}
```

## 9. Axios client esperado

`frontend/src/api/client.ts` debe:
- usar `Authorization: Bearer <token>`
- refrescar token silenciosamente ante 401 cuando corresponda
- redirigir a login o limpiar sesion si refresh falla
- soportar `responseType: 'blob'` para exportes
- soportar `multipart/form-data` para escaner IA

## 10. Roles y acceso de pantallas

### Todos autenticados

- dashboard
- lectura de productos
- lectura de categorias
- lectura de inventario
- POS segun rol permitido por backend

### ADMIN

- auditoria
- reportes
- gestion de proveedores
- crear trabajadores
- void de ventas
- navegacion "Empleados"

### ADMIN o WAREHOUSE

- crear/editar productos
- crear/editar categorias
- operaciones de inventario
- ordenes de compra
- alertas activas y configuracion

### ADMIN o CASHIER

- ventas/POS

## 11. Endpoints clave que el frontend debe consumir

### Productos

- `GET /products`
- `GET /products/{id}`
- `GET /products/barcode/{barcode}`
- `POST /products`
- `PUT /products/{id}`
- `PUT /products/{id}/deactivate`
- `PUT /products/{id}/activate`

Notas:
- `GET /products` devuelve `Page<ProductResponse>`

### Categorias

- `GET /categories`
- `GET /categories/{id}`
- `POST /categories`
- `PUT /categories/{id}`
- `PUT /categories/{id}/deactivate`
- `PUT /categories/{id}/activate`

### Inventario

- `GET /inventory`
- `GET /inventory/{productId}`
- `GET /inventory/{productId}/movements`
- `POST /inventory/{productId}/entry`
- `POST /inventory/{productId}/exit`
- `POST /inventory/{productId}/adjustment`
- `PUT /inventory/{productId}/limits`

Notas:
- `GET /inventory` y movimientos devuelven `Page<>`

### Alertas

- `GET /alerts`
- `GET /alerts/unread/count`
- `PUT /alerts/{id}/read`
- `PUT /alerts/{id}/resolve`
- `GET /alerts/configuration/{productId}`
- `PUT /alerts/configuration/{productId}`

### Ventas

- `POST /sales/start`
- `POST /sales/quick`
- `GET /sales/{id}`
- `POST /sales/{id}/items`
- `PUT /sales/{id}/items/{itemId}`
- `DELETE /sales/{id}/items/{itemId}`
- `POST /sales/{id}/confirm`
- `POST /sales/{id}/void`

### Proveedores

- `GET /suppliers`
- `GET /suppliers/{id}`
- `GET /suppliers/tax-id/{taxId}`
- `POST /suppliers`
- `PUT /suppliers/{id}`
- `PUT /suppliers/{id}/activate`
- `PUT /suppliers/{id}/deactivate`
- `DELETE /suppliers/{id}`

### Ordenes de compra

- `GET /purchase-orders`
- `GET /purchase-orders?supplierId=...`
- `GET /purchase-orders/{id}`
- `GET /purchase-orders/number/{orderNumber}`
- `POST /purchase-orders`
- `POST /purchase-orders/{orderId}/items`
- `POST /purchase-orders/{sourceOrderId}/clone`
- `PUT /purchase-orders/{orderId}/receive`
- `PUT /purchase-orders/{orderId}/void`
- `DELETE /purchase-orders/{orderId}/items/{detailId}`

### Dashboard

- `GET /dashboard`

### Reportes

- `GET /reports/profitability?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`
- `GET /reports/export/{type}?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`

Importante:
- export usa blob
- revisar `Content-Disposition`

### Scanner IA

- `POST /scanner/ai` con `multipart/form-data`
- `GET /scanner/status`
- `GET /scanner/ai/available`

### Auditoria

- `GET /audit`
- `GET /audit/{id}`
- `GET /audit/entity/{type}/{entityId}`

## 12. DTOs importantes para alinear tipos frontend

### ProductResponse

Incluye:
- `id`
- `name`
- `barcode`
- `sku`
- `description`
- `costPrice` como string
- `salePrice` como string
- `categoryId`
- `categoryName`
- `active`
- `minStockInfo`
- `minStockWarning`
- `minStockCritical`

Importante:
- `costPrice` y `salePrice` vienen serializados como string, no number

### InventoryResponse

Incluye:
- `id`
- `productId`
- `productName`
- `currentStock`
- `minStock`
- `maxStock`
- `active`
- `version`

### DashboardResponse

Incluye KPIs y listas anidadas:
- `todaySales`
- `todaySalesCount`
- `averageTicket`
- `outOfStockProducts`
- `outOfStockProductList`
- `estimatedMonthlyProfit`
- `lowStockAlertCount`
- `recentSales`

### AuditRecordResponse

Incluye:
- `previousData`
- `newData`

Importante:
- esos JSON llegan como string para que el frontend los parsee/renderice

### ProductSuggestionResponse

Incluye:
- `suggestions`
- `processingTimeMs`
- `strategyUsed`

## 13. Shape de errores backend

El frontend debe asumir este shape estandar:

```json
{
  "success": false,
  "error": "VALIDATION_ERROR",
  "message": "Username is required",
  "timestamp": "2026-04-05T...",
  "path": "/api/v1/auth/login"
}
```

Codigos importantes:
- `INVALID_CREDENTIALS`
- `ACCOUNT_DISABLED`
- `ACCOUNT_LOCKED`
- `ACCESS_DENIED`
- `VALIDATION_ERROR`
- `INVALID_ARGUMENT`
- `DUPLICATE_RESOURCE`
- `INACTIVE_RESOURCE_EXISTS`
- `NOT_FOUND`
- `INSUFFICIENT_STOCK`
- `MAX_STOCK_EXCEEDED`
- `INVALID_PRICE`
- `INVALID_PAYMENT`
- `CONCURRENCY_CONFLICT`
- `INTERNAL_ERROR`

Recomendacion:
- mostrar siempre `message`
- usar `error` para logica especifica de UI si hace falta

## 14. Seeds de desarrollo actuales

Credenciales vigentes:
- `admin2 / admin123` -> `ADMIN`, `businessId=1`
- `owner_test / test123` -> `ADMIN`, `businessId=2`
- `cashier_test / test123` -> `CASHIER`, `businessId=2`

Fuente:
- `src/main/resources/db/migration/V4__seed_dev_users.sql`

## 15. Checklist de reconstruccion para el agente frontend

1. Restaurar estructura base de `frontend/`
2. Rehacer `client.ts` con JWT + refresh
3. Rehacer `authStore.ts` con persistencia de `businessId`
4. Rehacer tipos de auth con respuesta plana
5. Rehacer `LoginPage.tsx`
6. Aplicar `maxLength={20}` en login
7. Rehacer `RegisterPage.tsx`
8. Aplicar `maxLength={20}` en username/password de registro
9. Rehacer `WorkersPage.tsx`
10. Aplicar `maxLength={20}` en username/password de creacion de worker
11. Rehacer guards por auth/rol
12. Conectar navegacion por rol
13. Ajustar tipos de productos, inventario, alertas, dashboard, auditoria
14. Soportar blob en exportes
15. Soportar multipart en scanner IA
16. Correr `npx tsc --noEmit`

## 16. Verificacion minima esperada

- login funciona con `admin2/admin123`
- el store guarda `businessId`
- refresh funciona con respuesta plana
- registro de negocio envia `businessName`
- creacion de worker usa `/auth/workers`
- username/password no aceptan mas de 20 caracteres en login/registro/workers
- exportes descargan archivos validos
- scanner IA envia form-data
- TypeScript compila con `npx tsc --noEmit`
