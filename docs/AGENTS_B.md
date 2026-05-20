# VELTRO - Registro de Arquitectura y Refactorización

## Objetivo

Este documento detalla la estructura del proyecto Veltro y funciona como un registro histórico (Architectural Record) de la gran fase de refactorización, limpieza y optimización llevada a cabo en Mayo de 2026. A lo largo de 14 partes iterativas (`bpart1.md` a `bpart14.md`), se estabilizó la arquitectura, se fortaleció el aislamiento multi-tenant y se impusieron estándares rigurosos de código.

## Reglas Maestras Aplicadas en la Refactorización

1. **Idioma del Código:** Toda la lógica interna (nombres de variables, métodos, clases, configuración, logs y comentarios) fue reescrita o verificada estrictamente en **inglés**.
2. **Idioma de la UI:** Todos los mensajes al usuario final (excepciones de negocio, alertas, respuestas de validación) se localizaron al **español**.
3. **Multi-Tenant Seguro:** Todo acceso a datos y operaciones asíncronas pasa ahora a través de una interfaz `TenantProvider` unificada, eliminando fugas de contexto en hilos secundarios.
4. **Desacoplamiento:** Se aplicó Clean Architecture mediante DTOs (Records), Patrón State para máquinas de estado, y Eventos de Spring para aislar efectos secundarios.

## Resumen Del Proyecto

Veltro es un sistema ERP/POS multi-tenant para PYMEs. La estructura del backend combina controladores, servicios, repositorios, entidades, DTOs, seguridad, eventos, estados, listeners, migraciones y recursos de IA (búsqueda vectorial con CLIP y `pgvector`).

## Mapa General Del Proyecto

### Raíz

- `pom.xml`: dependencias y build Maven.
- `mvnw` y `mvnw.cmd`: wrapper de Maven.
- `Dockerfile`: empaquetado de runtime optimizado para producción.
- `docker-compose.yml`: entorno local integral.
- `Procfile`: despliegue tipo PaaS (Heroku).
- `.env`: variables sensibles de desarrollo.

### `src/main/java/com/veltro/inventory/`

- `controller/`: endpoints REST estandarizados.
- `service/`: lógica de aplicación y casos de uso.
- `state/`: implementación del Patrón de Estado (State Pattern) para ciclo de vida de ventas y órdenes.
- `listener/`: reacción a eventos de dominio (desacoplamiento).
- `event/`: eventos del dominio (Domain Events).
- `repository/`: acceso a datos (Spring Data JPA) optimizado.
- `model/`: entidades auditables y multi-tenant.
- `dto/`: contratos de entrada y salida basados en Java Records.
- `mapper/`: conversión segura de capa (MapStruct).
- `security/`: JWT y unificación del Tenant Context.
- `config/`: configuración técnica y parametrización cloud.
- `exception/`: manejo de errores de negocio centralizado (Controller Advice).
- `infrastructure/`: integraciones externas (AI, ONNX, HTTP Clients).
- `bootstrap/`: inicialización de datos.

### `src/test/java/`

- Pruebas unitarias desprovistas de acoplamiento a Spring Security y preparadas para integración mediante Testcontainers.

## Historial de Refactorización por Partes (Completado)

Todos los planes desde `bpart1.md` hasta `bpart14.md` han sido diseñados, evaluados y ejecutados con éxito en la base de código base. A continuación el registro de logros de cada módulo:

### Parte 1 - Raíz y Arranque del Proyecto
**Estado: Completado.** Se consolidó el `Dockerfile` y `docker-compose.yml` para garantizar despliegues deterministas en la nube, asegurando que el modelo CLIP (`.onnx`) y la base de datos inicien correctamente con las variables de entorno inyectadas.

### Parte 2 - Estructura General del Backend
**Estado: Completado.** Se eliminaron clases "Dios" y dependencias circulares, aislando responsabilidades en subpaquetes y asegurando que las capas superiores no conozcan los detalles de implementación de persistencia ni seguridad.

### Parte 3 - Capa de Controladores
**Estado: Completado.** Se estandarizaron los mapeos de la API. Se impuso el uso estricto de DTOs en lugar de entidades en los parámetros y retornos. Las respuestas de error se unificaron para entregar JSON consistentes localizados al español mediante el `GlobalExceptionHandler`.

### Parte 4 - Capa de Servicios
**Estado: Completado.** Se optimizaron servicios críticos (Product, Sale, Inventory). Se corrigió la gestión de transacciones `@Transactional`, eliminando consultas N+1 repetitivas y delegando la lógica compleja a objetos de dominio o mappers delegados.

### Parte 5 - Estados del Dominio
**Estado: Completado.** Se refactorizó toda la lógica dispersa de ventas (`IN_PROGRESS`, `COMPLETED`, `VOIDED`) y órdenes de compra (`PENDING`, `PARTIAL`, `RECEIVED`) utilizando un robusto **State Pattern**. Las transiciones ilegales ahora lanzan `InvalidStateTransitionException`.

### Parte 6 - Eventos y Listeners
**Estado: Completado.** Se limpiaron los servicios que llamaban explícitamente a actualizaciones de stock o auditorías. Todo se convirtió a una arquitectura orientada a eventos (`SaleCompletedEvent`, `StockDeductedEvent`) para garantizar que la falla de un sistema secundario no rompa el flujo principal.

### Parte 7 - Repositorios y Persistencia
**Estado: Completado.** Se aplicó control de concurrencia optimista (`@Version`), filtrado activo por tenant (`businessId`) en cada query y soporte total para "Soft-Delete" heredado de `AbstractAuditableEntity`. 

### Parte 8 - Modelos, DTOs y Mappers
**Estado: Completado.** Conversión masiva de DTOs obsoletos a **Java Records**. Centralización y optimización de mappers con **MapStruct** para erradicar el código repetitivo, inyectando el componente de traducción de validaciones para respetar las reglas bilingües del proyecto.

### Parte 9 - Seguridad y Contexto Multi-Tenant
**Estado: Completado.** Refactorización revolucionaria: Se desacopló la obtención del contexto del usuario (id, negocio, nombre) del hilo global (`SecurityContextHolder`). Ahora todo el sistema utiliza un `TenantProvider` inyectado, protegiendo al backend de fugas de datos en procesos asíncronos y tareas en segundo plano.

### Parte 10 - Configuración y Excepciones
**Estado: Completado.** Estandarización final del manejador de errores globales. Implementación estricta de herencia de excepciones y soporte nativo de traducción i18n (`messages.properties`), permitiendo registrar logs de backend en inglés mientras el front-end muestra alertas en español.

### Parte 11 - CLIP, IA y Búsqueda Vectorial
**Estado: Completado.** Se blindó el uso de ONNX Runtime y Supabase `pgvector`. El código se ajustó para no romper el inicio del contexto en servidores de escasos recursos si el modelo ONNX no se puede cargar localmente, realizando un "graceful fallback" para la API de IA.

### Parte 12 - Auditoría y Trazabilidad
**Estado: Completado.** Corrección de fallos graves en los hilos asíncronos de `AuditCommandExecutor` mediante la adopción de `TenantProvider`. Rebaja de niveles de registro (`INFO` a `DEBUG`) para eventos de alta transaccionalidad (ej. POS) para no contaminar los logs del contenedor.

### Parte 13 - Dashboard y Reportes
**Estado: Completado.** Corrección lógica crítica de "Soft-Deletes" contaminando consultas analíticas (Out of Stock y Ventas Recientes). Reemplazo de recuentos ciegos (`SIZE()`) por subqueries filtradas `active=true`. Renombramiento arquitectónico a `JpaDashboardQueryRepository` para evitar colisiones de classpath. Parametrización en properties para Timezone y márgenes.

### Parte 14 - Pruebas
**Estado: Completado.** Limpieza exhaustiva de tests unitarios de la capa de servicios, eliminando el acoplamiento redundante a hilos de `SecurityContextHolder`. Preparación documentada para adoptar `Testcontainers` y reactivar las pruebas de integración utilizando la imagen de docker `ankane/pgvector`.

## Estado Actual del Proyecto (Mayo 2026)

- **Backend:** Spring Boot + Java 21, modular por capas, completamente saneado.
- **Seguridad:** Aislamiento multi-tenant validado mediante `TenantProvider`.
- **Inteligencia Artificial:** Flujo vectorial estabilizado para producción cloud.
- **Preparación de Despliegue:** Sistema listo para paso a producción y pipelines de CI/CD, con deuda técnica resuelta, arquitectura bilingüe (Código: EN, UI: ES) funcionando impecablemente.
- **Siguientes Pasos:** Reactivación de pruebas de integración con Testcontainers y futura estandarización a `@WebMvcTest`.

## 🧩 Patrones de Software Implementados

La arquitectura actual de Veltro se apoya en una sólida base de ingeniería de software, implementando **9 patrones de diseño principales** para resolver problemas recurrentes y garantizar un código desacoplado:

1. **State Pattern (Patrón de Estado):** Ubicado en `com.veltro.inventory.state`. Maneja dinámicamente las transiciones del ciclo de vida para Ventas (`SaleInProgressState`, `SaleCompletedState`, `SaleVoidedState`) y Órdenes de Compra (`PurchaseOrderPendingState`, `PurchaseOrderPartialState`, `PurchaseOrderReceivedState`), eliminando sentencias condicionales masivas y espagueti (`if/switch`).
2. **Observer / Event-Driven Pattern (Observador):** Ubicado en `com.veltro.inventory.event` y `com.veltro.inventory.listener`. Desacopla efectos secundarios transaccionales; por ejemplo, confirmar una venta emite un `SaleCompletedEvent` que es escuchado de forma independiente por `DeductStockSaleListener`, garantizando que módulos como el de inventario no estén acoplados a ventas.
3. **Strategy Pattern (Estrategia):** Usado en la inteligencia artificial e identificación de productos (`BarcodeRecognitionStrategy`, `AiVisionStrategy`), permitiendo intercambiar el algoritmo de identificación en tiempo de ejecución.
4. **Factory Pattern (Fábrica):** Ubicado en clases centralizadas como `SaleEventFactory`, el cual se encarga de instanciar y ensamblar eventos complejos del dominio con todos sus datos anidados.
5. **Facade Pattern (Fachada):** Aplicado en `DashboardService` y `ReportService`. Actúan como punto único de entrada para ocultar la extrema complejidad de ensamblar datos de múltiples repositorios, cálculos estadísticos, algoritmos de márgenes financieros y conversiones de zonas horarias.
6. **Data Transfer Object (DTO):** Aplicado en todo el paquete `dto`, implementado modernamente mediante **Java Records**. Garantiza la inmutabilidad total en el transporte de datos de red, impidiendo la manipulación accidental de entidades Hibernate en la capa de controladores.
7. **Chain of Responsibility (Cadena de Responsabilidad):** Utilizado a través del `AlertChainBuilder` que vimos en las pruebas, el cual evalúa secuencialmente diferentes reglas y alertas de negocio (falta de stock, sobrestock) encadenando evaluadores y manipuladores.
8. **Repository Pattern (Repositorio):** A través de Spring Data JPA en el paquete `repository`, encapsulando por completo las sentencias de base de datos relacional y lógica de persistencia multi-tenant (como el filtrado por `business_id`).
9. **Singleton / Inyección de Dependencias:** Gestionado por el contenedor de Spring Boot (IoC). Todos los servicios y componentes del sistema nacen de instancias Singleton sin estado que garantizan bajo consumo de memoria y se inyectan en tiempo de construcción (`@RequiredArgsConstructor` en Lombok).

## 🗄️ Estructuras de Datos Clave

La eficiencia, escalabilidad computacional y capacidad de IA de Veltro dependen del uso de estructuras de datos específicas en diferentes niveles:

1. **Vectores Bidimensionales (Float Arrays):**
   - **Dónde se Aplican:** En la capa de IA (CLIP / Reconocimiento Vectorial) y en PostgreSQL mediante la extensión `pgvector`.
   - **Propósito:** Las imágenes y textos de productos son transformados a *embeddings* (arreglos matemáticos de coma flotante de 512 dimensiones). Son indispensables para ejecutar búsquedas de **Similitud del Coseno**, permitiendo encontrar productos "visualmente similares" en el inventario.
2. **Tablas Hash (Maps / Dictionaries):**
   - **Dónde se Aplican:** Principalmente en la lógica interna de negocio, transformadores e importaciones masivas (ej: `HashMap<Long, ProductEntity>`).
   - **Propósito:** Brindan búsquedas, agrupaciones (ej. consolidar ítems de un carrito por producto) e inserciones en memoria de tiempo constante **O(1)**.
3. **Árboles B (B-Trees):**
   - **Dónde se Aplican:** De forma indirecta a nivel del motor de la base de datos de producción (PostgreSQL).
   - **Propósito:** Indexación de campos altamente consultados como identificadores (`id`), `barcode`, `sku`, y llaves para multitenancy (`business_id`). Garantizan que las consultas de tablas masivas se ejecuten en tiempo logarítmico **O(log n)**.
4. **Listas y Arreglos Dinámicos (Dynamic Arrays):**
   - **Dónde se Aplican:** En DTOs, objetos paginados (`Page<T>`, `List<T>`) y agregación de hijos de entidades (lista de detalles de venta).
   - **Propósito:** Su naturaleza secuencial e iterable permite iteración rápida **O(n)**, crucial a la hora de compilar y exportar reportes Excel / PDF o transformar conjuntos de datos por stream.
5. **Grafos / Grafos Dirigidos (Implicit Graphs):**
   - **Dónde se Aplican:** En las relaciones relacionales anidadas del ORM Hibernate (anotaciones `@OneToMany`, `@ManyToOne`).
   - **Propósito:** Modelar jerarquías y dependencias de negocio (ej. el nodo Proveedor -> el nodo Orden de Compra -> los nodos Detalle de Orden -> los nodos Producto). Estos nodos se resuelven en memoria usando árboles de punteros por debajo.
