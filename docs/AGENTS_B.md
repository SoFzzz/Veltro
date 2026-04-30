# VELTRO - Guia De Analisis Por Partes

## Objetivo

Este documento sirve para analizar el proyecto por secciones, identificar oportunidades de optimizacion y revisar la estructura sin tocar la implementacion del backend.

## Alcance

- Solo documentacion.
- No modifica archivos de `src/`.
- No propone cambios directos en el codigo aqui; solo organiza el analisis.

## Resumen Del Proyecto

Veltro es un sistema ERP/POS multi-tenant para PYMEs. La estructura del backend combina controladores, servicios, repositorios, entidades, DTOs, seguridad, eventos, estados, listeners, migraciones y recursos de IA.

## Mapa General Del Proyecto

### Raiz

- `pom.xml`: dependencias y build Maven.
- `mvnw` y `mvnw.cmd`: wrapper de Maven.
- `Dockerfile`: empaquetado de runtime.
- `docker-compose.yml`: entorno local.
- `Procfile`: despliegue tipo PaaS.
- `.env`: variables sensibles de desarrollo.

### `src/main/java/`

- `controller/`: endpoints REST.
- `service/`: logica de aplicacion y casos de uso.
- `state/`: ciclo de vida de ventas y ordenes.
- `listener/`: reaccion a eventos de dominio.
- `event/`: eventos del dominio.
- `repository/`: acceso a datos.
- `model/`: entidades y enums.
- `dto/`: contratos de entrada y salida.
- `mapper/`: conversion entre entidades y DTOs.
- `security/`: JWT, tenant context y autenticacion.
- `config/`: configuracion tecnica.
- `exception/`: manejo de errores de negocio.
- `infrastructure/`: integraciones externas.
- `bootstrap/`: inicializacion de datos.

### `src/main/resources/`

- `application.yaml`: configuracion base.
- `application-dev-with-ai.yml`: perfil de desarrollo con IA.
- `db/migration/`: scripts Flyway.
- `models/clip-image-vit-32.onnx`: modelo ONNX usado por CLIP.

### `src/test/java/`

- `controller/`: pruebas de endpoints.
- `service/`: pruebas de logica de negocio.
- `state/`: pruebas de estados.
- `listener/`: pruebas de eventos.
- `security/`: pruebas de JWT y seguridad.
- `exception/`: pruebas del manejador global.
- `infrastructure/`: pruebas de integracion tecnica.
- `model/`: pruebas de entidades.

### `docs/`

- Documentacion funcional, tecnica y operativa del proyecto.

## Analisis Del Codigo Por Partes

### Parte 1 - Raiz Y Arranque Del Proyecto

Archivos clave:

- `pom.xml`
- `Dockerfile`
- `docker-compose.yml`
- `Procfile`

Que revisar:

- Dependencias principales.
- Estrategia de build y despliegue.
- Configuracion para entorno local.
- Separacion entre ejecucion, empaquetado y variables de entorno.

### Parte 2 - Estructura General Del Backend

Carpeta:

- `src/main/java/`

Subcarpetas:

- `controller/`
- `service/`
- `repository/`
- `model/`
- `dto/`
- `mapper/`
- `security/`
- `config/`
- `exception/`
- `listener/`
- `event/`
- `state/`
- `infrastructure/`
- `bootstrap/`

Que revisar:

- Si cada carpeta mantiene una sola responsabilidad.
- Si hay servicios muy grandes.
- Si la seguridad esta mezclada con logica de negocio.
- Si los DTOs estan bien separados por caso de uso.

### Parte 3 - Capa De Controladores

Carpeta:

- `src/main/java/com/veltro/inventory/controller/`

Que revisar:

- Consistencia de endpoints.
- Validacion de entrada.
- Delegacion real hacia servicios.
- Posible exceso de logica en controladores.

### Parte 4 - Capa De Servicios

Carpeta:

- `src/main/java/com/veltro/inventory/service/`

Subzonas relevantes:

- Catalogo: productos, categorias y proveedores.
- Inventario y alertas.
- POS y ventas.
- Ordenes de compra.
- Auditoria.
- Reportes.
- Seguridad y autenticacion.
- IA, scanner y busqueda vectorial.

Que revisar:

- Servicios que orquestan demasiadas tareas.
- Logica repetida entre modulos.
- Casos de uso que puedan aislarse mejor.
- Dependencias cruzadas entre servicios.

### Parte 5 - Estados Del Dominio

Carpeta:

- `src/main/java/com/veltro/inventory/state/`

Que revisar:

- Flujo de vida de ventas.
- Flujo de vida de ordenes de compra.
- Consistencia entre transiciones.
- Regla de negocio en cada estado.

### Parte 6 - Eventos Y Listeners

Carpetas:

- `src/main/java/com/veltro/inventory/event/`
- `src/main/java/com/veltro/inventory/listener/`

Que revisar:

- Que evento dispara cada listener.
- Si la reaccion a eventos esta bien desacoplada.
- Si hay dependencias fuertes entre listeners y servicios.
- Si se puede aislar mejor la orquestacion de eventos.

### Parte 7 - Repositorios Y Persistencia

Carpeta:

- `src/main/java/com/veltro/inventory/repository/`

Que revisar:

- Repositorios demasiado especificos o demasiado genericos.
- Consultas repetidas.
- Separacion entre lectura y escritura.
- Si el acceso a datos refleja bien el dominio.

### Parte 8 - Modelos, DTOs Y Mappers

Carpetas:

- `src/main/java/com/veltro/inventory/model/`
- `src/main/java/com/veltro/inventory/dto/`
- `src/main/java/com/veltro/inventory/mapper/`

Que revisar:

- Consistencia entre entidad y contrato.
- Duplicacion de campos.
- Complejidad del mapeo.
- Si conviene dividir mas los DTOs por modulo.

### Parte 9 - Seguridad Y Contexto Multi-Tenant

Carpeta:

- `src/main/java/com/veltro/inventory/security/`

Que revisar:

- Flujo de autenticacion.
- Extraccion de claims y contexto del usuario.
- Aislamiento por negocio.
- Acoplamiento entre seguridad y tenant context.

### Parte 10 - Configuracion Y Excepciones

Carpetas:

- `src/main/java/com/veltro/inventory/config/`
- `src/main/java/com/veltro/inventory/exception/`

Que revisar:

- Configuracion tecnica agrupada por responsabilidad.
- Excepciones de negocio consistentes.
- Manejo global de errores.
- Si hay configuracion que podria moverse a una capa mas clara.

### Parte 11 - CLIP, IA Y Busqueda Vectorial

Carpetas y archivos:

- `src/main/java/com/veltro/inventory/infrastructure/ai/`
- `src/main/java/com/veltro/inventory/service/ProductRecognitionService.java`
- `src/main/java/com/veltro/inventory/service/ProductMatchingService.java`
- `src/main/java/com/veltro/inventory/service/VectorSearchService.java`
- `src/main/java/com/veltro/inventory/service/ClipVectorSearchService.java`
- `src/main/java/com/veltro/inventory/service/BatchIndexingService.java`
- `src/main/resources/models/clip-image-vit-32.onnx`
- `src/main/resources/db/migration/V3_1__add_pgvector_embeddings.sql`
- `src/main/resources/db/migration/V6__product_embedding_status_and_version.sql`

Que revisar:

- Como se integra CLIP con el flujo de reconocimiento.
- Como se generan y almacenan embeddings.
- Como se consulta similitud vectorial.
- Si reconocimiento, matching e indexado estan demasiado acoplados.
- Si la infraestructura de IA deberia separarse mas del dominio principal.

### Parte 12 - Auditoria Y Trazabilidad

Carpeta y archivos:

- `src/main/java/com/veltro/inventory/service/ForensicAuditService.java`
- `src/main/java/com/veltro/inventory/service/AuditCommandExecutor.java`
- `src/main/java/com/veltro/inventory/service/AuditSnapshotBuilder.java`

Que revisar:

- Como se arma el snapshot.
- Que tan uniforme es el flujo de auditoria.
- Si la trazabilidad mezcla responsabilidades tecnicas y de negocio.

### Parte 13 - Dashboard Y Reportes

Carpeta y archivos:

- `src/main/java/com/veltro/inventory/service/DashboardService.java`
- `src/main/java/com/veltro/inventory/service/DashboardQueryRepository.java`
- `src/main/java/com/veltro/inventory/service/ReportService.java`
- `src/main/java/com/veltro/inventory/service/ReportExporter.java`
- `src/main/java/com/veltro/inventory/service/PdfReportExporter.java`
- `src/main/java/com/veltro/inventory/service/ExcelReportExporter.java`

Que revisar:

- Separacion entre consulta, calculo y exportacion.
- Repeticion de agregaciones.
- Si conviene una capa de lectura mas explicita.

### Parte 14 - Pruebas

Carpeta:

- `src/test/java/`

Que revisar:

- Cobertura por modulo.
- Pruebas de integracion vs unitarias.
- Casos fragiles o muy acoplados a implementacion.
- Balance entre pruebas de estado, servicio y controlador.

## Orden Sugerido De Analisis

1. Raiz y arranque del proyecto.
2. Estructura general del backend.
3. Controladores.
4. Servicios.
5. Estados del dominio.
6. Eventos y listeners.
7. Repositorios y persistencia.
8. Modelos, DTOs y mappers.
9. Seguridad y contexto multi-tenant.
10. Configuracion y excepciones.
11. CLIP, IA y busqueda vectorial.
12. Auditoria y trazabilidad.
13. Dashboard y reportes.
14. Pruebas.

## Preguntas Guia Para Optimizar

- Que codigo se repite y puede consolidarse.
- Que servicios hacen demasiadas cosas.
- Que partes del flujo de IA pueden separarse mejor.
- Que componentes de CLIP y busqueda vectorial conviene aislar.
- Que endpoints o contratos estan duplicados.
- Que area necesita una capa de lectura mas clara.

## Nota Importante

Este documento no modifica el backend. La idea es usar esta division para revisar el codigo por bloques y decidir despues donde conviene optimizar.

## Estado Del Proyecto

- Backend: Spring Boot + Java.
- Estructura: modular por capas.
- Soporte de IA: flujo de reconocimiento, CLIP y busqueda vectorial.
- Proposito de este archivo: servir como mapa de analisis por partes.

**Ultima actualizacion:** Mayo 2026
