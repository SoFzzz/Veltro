# VELTRO - EXECUTIVE SUMMARY

## Estado general

Veltro es un ERP/POS multi-tenant para pequenas y medianas empresas. El repositorio actual contiene backend Spring Boot, frontend React y una base documental separada entre documentacion operativa actual, reportes de contexto y documentos historicos.

## Que incluye hoy el proyecto

### Backend
- Spring Boot `4.0.3` con Java `21`
- API REST con autenticacion JWT y aislamiento por `businessId`
- Catalogo, inventario, ventas POS, compras, dashboard, reportes, auditoria y escaner con IA
- Persistencia con Spring Data JPA + Flyway

### Frontend
- React `19` + TypeScript + Vite `8`
- Router, stores Zustand, React Query y formularios con React Hook Form + Zod
- Pantallas para autenticacion, catalogo, POS, inventario, compras, dashboard y auditoria

## Arquitectura actual

### Backend
La estructura vigente del backend es clasica por capas tecnicas bajo `src/main/java/com/veltro/inventory/`:

- `bootstrap`
- `config`
- `controller`
- `dto`
- `event`
- `exception`
- `listener`
- `mapper`
- `model`
- `repository`
- `security`
- `service`
- `state`

La documentacion que habla de arquitectura hexagonal o de carpetas `domain/application/infrastructure` debe tomarse como historica.

### Tests
Los tests siguen la misma organizacion general bajo `src/test/java/com/veltro/inventory/`:

- `controller`
- `exception`
- `listener`
- `model`
- `security`
- `service`
- `state`

## Contexto operativo actual

- Seeds vigentes en desarrollo:
  - `admin2/admin123`
  - `owner_test/test123`
  - `cashier_test/test123`
- Esos usuarios quedan cubiertos por `V4__seed_dev_users.sql`
- `DevDataInitializer` no es la referencia operativa para `dev-with-ai`
- Existe una coleccion Postman lista para importar en `docs/Veltro.postman_collection.json`

## Documentos recomendados

- `docs/AGENTS.md`: fuente tecnica principal
- `docs/REPORT_TECHNOLOGIES_AND_CLASS_STRUCTURE.md`: fotografia del stack y estructura actual
- `docs/PROJECT_COMPLETION_REPORT.md`: reporte ejecutivo-tecnico ampliado
- `docs/BUGASOS.txt`: registro historico de bugs y mejoras ya resueltos al 100%

## Nota

Cuando haya diferencias entre documentos antiguos y el codigo fuente actual, prevalece el estado real del repo.
