# Veltro Project Documentation

Guia de navegacion para la documentacion vigente de Veltro.

## Start Here

### Documentos operativos
- **AGENTS.md**: fuente tecnica principal del proyecto. Describe stack actual, arquitectura por capas, multi-tenant, seeds de desarrollo y flujo operativo.
- **REPORT_TECHNOLOGIES_AND_CLASS_STRUCTURE.md**: fotografia tecnica del repo actual, validada contra `pom.xml`, `package.json` y la estructura real de `src/`.
- **Veltro.postman_collection.json**: coleccion lista para importar en Postman, con guardado automatico de tokens.

### Reportes de estado
- **EXECUTIVE_SUMMARY.md**: resumen corto del estado actual del proyecto para stakeholders.
- **PROJECT_COMPLETION_REPORT.md**: reporte tecnico-ejecutivo del estado actual y de los cambios estructurales mas relevantes.
- **SESSION_SUMMARY.md**: resumen historico de la auditoria/documentacion previa. Se conserva como contexto, no como fuente operativa principal.

### Documentos historicos o de especificacion
- **VeltroBase.md**: especificacion historica del sistema. No describe exactamente la estructura actual del repo.
- **B3-01_IMPLEMENTATION_COMPLETE.md**: reporte historico de la integracion AI Vision. Util para contexto, no como mapa actual de paquetes.
- **BUGASOS.txt**: registro historico de bugs y mejoras ya resueltos al 100%.

## Estado actual del proyecto

### Stack validado
- Backend: Spring Boot `4.0.3`, Java `21`, Spring Security, Spring Data JPA, Flyway, PostgreSQL, H2 local, MapStruct, Lombok, JJWT, iText, Apache POI.
- Frontend: React `19`, TypeScript `5.9`, Vite `8`, React Router `7`, Zustand, React Query, React Hook Form, Zod, Tailwind CSS `4`, Vitest, Testing Library, MSW.

### Arquitectura vigente
- Backend principal organizado por capas tecnicas bajo `src/main/java/com/veltro/inventory/`:
  - `bootstrap`, `config`, `controller`, `dto`, `event`, `exception`, `listener`, `mapper`, `model`, `repository`, `security`, `service`, `state`
- Tests organizados en espejo funcional bajo `src/test/java/com/veltro/inventory/`:
  - `controller`, `exception`, `listener`, `model`, `security`, `service`, `state`
- La documentacion antigua que habla de `domain/`, `application/` e `infrastructure/` debe leerse como historica.

### Contexto operativo
- Seeds de desarrollo actuales:
  - `admin2/admin123` -> `ADMIN`, `businessId=1`
  - `owner_test/test123` -> `ADMIN`, `businessId=2`
  - `cashier_test/test123` -> `CASHIER`, `businessId=2`
- La migracion `V4__seed_dev_users.sql` es la referencia actual para esos usuarios.
- `DevDataInitializer` no forma parte del flujo esperado del perfil `dev-with-ai`; ese perfil depende de Flyway.

## Navegacion rapida

### Si quieres entender el proyecto hoy
1. Lee `AGENTS.md`
2. Revisa `REPORT_TECHNOLOGIES_AND_CLASS_STRUCTURE.md`
3. Importa `Veltro.postman_collection.json`

### Si quieres contexto historico
1. Lee `SESSION_SUMMARY.md`
2. Consulta `PROJECT_COMPLETION_REPORT.md`
3. Usa `VeltroBase.md` y `B3-01_IMPLEMENTATION_COMPLETE.md` solo como referencia historica/especificacion

### Si quieres revisar issues ya cerrados
1. Abre `BUGASOS.txt`
2. Toma ese archivo como archivo historico, no como backlog activo

## Nota editorial

Cuando haya diferencias entre documentos antiguos y el codigo fuente actual, prevalece el estado real del repo.
