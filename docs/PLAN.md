# Plan Para Matching De Sugerencias IA Usando `ProductMatchingService`

## Resumen
- Mantener `OpenAiVisionClient` enfocado en llamada/parsing de IA y mover la lógica de matching contra catálogo a un servicio dedicado.
- Crear `ProductMatchingService` con una única responsabilidad: encontrar el mejor producto activo del negocio actual a partir del nombre sugerido por la IA.
- Enriquecer `ProductSuggestionResponse` sin cambiar su shape: completar `productId`, `barcode` y `suggestedPrice` solo cuando exista un match claro; si no, devolver la sugerencia textual como hoy.

## Cambios De Implementación
- Crear `ProductMatchingService` en capa `service` con método público:
  - `Optional<ProductEntity> findMatch(String suggestedName, Long businessId)`
- Modificar [OpenAiVisionClient.java](D:\Escritorio\trabajo\UCC\cuarto\Veltro\src\main\java\com\veltro\inventory\service\OpenAiVisionClient.java):
  - inyectar `ProductMatchingService`
  - mantener la llamada a IA y el parseo donde están hoy
  - en `parseInventoryItem(...)`, después de construir `fullName`, invocar `productMatchingService.findMatch(fullName.toString(), TenantContext.getBusinessId())`
  - si hay match:
    - `productId = entity.getId()`
    - `barcode = entity.getBarcode()`
    - `suggestedPrice = entity.getSalePrice()`
  - si no hay match:
    - conservar `null` en esos campos
- Extender [ProductRepository.java](D:\Escritorio\trabajo\UCC\cuarto\Veltro\src\main\java\com\veltro\inventory\repository\ProductRepository.java) con una búsqueda textual tenant-aware y active-only.
- Implementar el algoritmo de matching dentro de `ProductMatchingService`, no dentro del cliente de IA.

## Matching Y Query
- Query base en repositorio:
  - recuperar candidatos activos del negocio actual por nombre case-insensitive usando una keyword significativa
- Método recomendado:
  - `List<ProductEntity> findTop10ByActiveTrueAndBusinessIdAndNameContainingIgnoreCase(Long businessId, String keyword);`
- Extracción de keyword en `ProductMatchingService`:
  - normalizar `suggestedName` a minúsculas
  - quitar espacios duplicados y puntuación simple
  - tokenizar
  - descartar tokens vacíos y unidades genéricas si aparecen aisladas
  - usar la primera palabra significativa como keyword principal
- Ejemplo:
  - `"Sprite Sabor Lima Limon"` -> keyword `sprite`
- Ranking de candidatos en Java:
  - prioridad 1: nombre completo normalizado igual
  - prioridad 2: nombre del catálogo contiene toda la sugerencia normalizada o tokens clave fuertes
  - prioridad 3: match por marca + volumen si la sugerencia contiene volumen
- Regla de seguridad:
  - solo devolver match automático cuando haya un mejor candidato claro
  - si hay ambigüedad real entre variantes, devolver `Optional.empty()`

## Múltiples Matches Y Sin Match
- Múltiples matches:
  - ejemplo: `Sprite 500ml` y `Sprite 1.5L`
  - si la sugerencia IA no trae volumen o dato suficiente para desempatar, no asignar `productId`
  - si sí trae volumen y un candidato coincide claramente, elegir ese
- Sin match:
  - no romper nada
  - devolver la sugerencia IA con:
    - `productName` y `confidence`
    - `productId = null`
    - `barcode = null`
    - `suggestedPrice = null`

## Archivos A Tocar
- [OpenAiVisionClient.java](D:\Escritorio\trabajo\UCC\cuarto\Veltro\src\main\java\com\veltro\inventory\service\OpenAiVisionClient.java)
  - inyectar `ProductMatchingService` y usarlo para enriquecer cada sugerencia parseada
- [ProductMatchingService.java](D:\Escritorio\trabajo\UCC\cuarto\Veltro\src\main\java\com\veltro\inventory\service\ProductMatchingService.java)
  - nuevo servicio con normalización, extracción de keyword, búsqueda de candidatos y selección del mejor match
- [ProductRepository.java](D:\Escritorio\trabajo\UCC\cuarto\Veltro\src\main\java\com\veltro\inventory\repository\ProductRepository.java)
  - agregar método de búsqueda por nombre tenant-aware y active-only
- [OpenAiVisionClientTest.java](D:\Escritorio\trabajo\UCC\cuarto\Veltro\src\test\java\com\veltro\inventory\service\OpenAiVisionClientTest.java)
  - cubrir enriquecimiento con match y fallback sin match
- [ProductMatchingServiceTest.java](D:\Escritorio\trabajo\UCC\cuarto\Veltro\src\test\java\com\veltro\inventory\service\ProductMatchingServiceTest.java)
  - nuevo test unitario para match único, ambigüedad y ausencia de coincidencias

## Test Plan
- Sugerencia IA `"Sprite Sabor Lima Limon"` con un único producto compatible en el negocio:
  - debe completar `productId`, `barcode`, `suggestedPrice`
- Sugerencia IA `"Sprite"` con varias variantes activas:
  - debe dejar `productId = null`
- Sugerencia IA de producto inexistente:
  - debe devolver sugerencia textual sin error
- Productos de otro `businessId`:
  - nunca deben matchear
- Productos inactivos:
  - nunca deben matchear
- El shape de `ProductSuggestionResponse` no cambia

## Supuestos
- El matching inicial será conservador para evitar falsos positivos.
- No se implementará fuzzy matching avanzado en esta primera iteración.
- `OpenAiVisionClient` seguirá resolviendo el `businessId` con `TenantContext` al momento de enriquecer sugerencias.
