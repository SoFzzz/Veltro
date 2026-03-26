# 📋 OpenAI Vision API Integration Specification (B3-01)

## 1️⃣ PROMPT EXACTO PARA OPENAI VISION

### Prompt del Sistema (System Role)

```
Eres un experto en análisis de imágenes de productos para un sistema ERP/POS.
Tu tarea es analizar imágenes de productos y proporcionar sugerencias de 
identificación de manera estructurada y precisa.

REGLAS CRÍTICAS:
1. Responde SOLO en JSON válido, sin texto adicional
2. Analiza TODO lo visible en la imagen (colores, forma, marca, tamaño relativo)
3. Si no estás seguro, reduce la confianza, nunca inventes datos
4. Detecta códigos de barras si están visibles
5. Estima precio según mercado actual (si es visible el producto)
6. Prioriza sugerencias por relevancia (índice 0 = más probable)
```

### Prompt del Usuario (User Role) - DINÁMICO

```
Analiza esta imagen de producto para un sistema de inventario.

Proporciona análisis detallado en JSON con esta estructura exacta:

{
  "status": "success|error",
  "analysis": {
    "productName": "string - nombre exacto del producto",
    "category": "string - categoría principal (ej: Bebidas, Electrónica, etc)",
    "subcategory": "string - subcategoría más específica",
    "confidence": "number 0.0-1.0 - confianza en la identificación",
    "description": "string - descripción breve del producto",
    "features": ["array de características visibles"],
    "estimatedPrice": "number - precio estimado en USD",
    "priceRange": {
      "min": "number",
      "max": "number",
      "currency": "USD"
    },
    "brand": "string - marca detectada (null si no visible)",
    "barcode": {
      "detected": "boolean - ¿se detectó código de barras?",
      "value": "string - valor del código si está visible",
      "format": "string - EAN-13, UPC-A, etc (null si no detectable)"
    },
    "images": {
      "hasLogo": "boolean",
      "hasBarcode": "boolean",
      "hasExpiration": "boolean",
      "colors": ["array de colores principales"],
      "packaging": "string - tipo de empaque (botella, caja, bolsa, etc)"
    },
    "manufacturerInfo": {
      "manufacturer": "string - fabricante si es visible",
      "madeIn": "string - país de origen si es visible",
      "weight": "string - peso si está en la imagen"
    },
    "similarProducts": [
      {
        "name": "string",
        "relevance": "number 0-100"
      }
    ]
  },
  "suggestions": [
    {
      "productId": null,
      "productName": "string",
      "category": "string",
      "subcategory": "string",
      "estimatedPrice": "number",
      "confidence": "number 0.0-1.0",
      "description": "string - por qué esta sugerencia",
      "barcode": "string o null",
      "barcodeFormat": "string o null",
      "features": ["array"],
      "brand": "string o null"
    }
  ],
  "warnings": ["array de advertencias si las hay"],
  "metadata": {
    "analysisTime": "number - ms",
    "imageQuality": "excellent|good|fair|poor",
    "ambiguity": "number 0.0-1.0 - qué tan ambigua es la imagen",
    "analysisDepth": "standard"
  }
}
```

REGLAS DE RESPUESTA:
- `confidence`: 0.9-1.0 = muy seguro, 0.7-0.9 = seguro, 0.5-0.7 = moderado, <0.5 = incierto
- `suggestions`: máximo 3 sugerencias ordenadas por confianza DESC
- `barcode`: solo incluir si es claramente visible en la imagen
- `estimatedPrice`: basado en marcas/productos similares actuales
- Si la imagen es borrosa/oscura, establecer imageQuality = "poor" y confianzas más bajas
- `warnings`: incluir "Low image quality", "Barcode partially obscured", etc
```

---

## 2️⃣ FORMATO JSON ESPERADO

### Respuesta Exitosa (200 OK)

```json
{
  "status": "success",
  "analysis": {
    "productName": "Coca-Cola 500ml Botella Plástica",
    "category": "Bebidas",
    "subcategory": "Refrescos",
    "confidence": 0.95,
    "description": "Bebida refrescante carbonatada marca Coca-Cola en botella de plástico de 500ml",
    "features": [
      "Botella plástica transparente",
      "Etiqueta roja con logo blanco",
      "Volumen: 500ml",
      "Producto refrigerado"
    ],
    "estimatedPrice": 1.50,
    "priceRange": {
      "min": 1.25,
      "max": 2.00,
      "currency": "USD"
    },
    "brand": "Coca-Cola",
    "barcode": {
      "detected": true,
      "value": "5449000131497",
      "format": "EAN-13"
    },
    "images": {
      "hasLogo": true,
      "hasBarcode": true,
      "hasExpiration": false,
      "colors": ["Rojo", "Blanco", "Negro"],
      "packaging": "Botella plástica"
    },
    "manufacturerInfo": {
      "manufacturer": "The Coca-Cola Company",
      "madeIn": "Colombia",
      "weight": "500g"
    },
    "similarProducts": [
      {
        "name": "Coca-Cola 350ml",
        "relevance": 95
      },
      {
        "name": "Pepsi 500ml",
        "relevance": 60
      }
    ]
  },
  "suggestions": [
    {
      "productId": null,
      "productName": "Coca-Cola 500ml",
      "category": "Bebidas",
      "subcategory": "Refrescos",
      "estimatedPrice": 1.50,
      "confidence": 0.95,
      "description": "Identificación exacta por código de barras y características visuales",
      "barcode": "5449000131497",
      "barcodeFormat": "EAN-13",
      "features": ["Botella plástica", "500ml", "Coca-Cola"],
      "brand": "Coca-Cola"
    },
    {
      "productId": null,
      "productName": "Coca-Cola 500ml (Alternativa)",
      "category": "Bebidas",
      "subcategory": "Refrescos",
      "estimatedPrice": 1.50,
      "confidence": 0.85,
      "description": "Posible variante regional del mismo producto",
      "barcode": null,
      "barcodeFormat": null,
      "features": ["Botella plástica", "500ml"],
      "brand": "Coca-Cola"
    }
  ],
  "warnings": [],
  "metadata": {
    "analysisTime": 1250,
    "imageQuality": "excellent",
    "ambiguity": 0.05,
    "analysisDepth": "standard"
  }
}
```

### Respuesta con Errores (Imagen Borrosa)

```json
{
  "status": "success",
  "analysis": {
    "productName": "Producto indeterminado",
    "category": "Desconocida",
    "subcategory": null,
    "confidence": 0.35,
    "description": "Imagen muy borrosa, no se puede identificar con certeza",
    "features": ["Posiblemente botella", "Color oscuro"],
    "estimatedPrice": null,
    "priceRange": {
      "min": null,
      "max": null,
      "currency": "USD"
    },
    "brand": null,
    "barcode": {
      "detected": false,
      "value": null,
      "format": null
    },
    "images": {
      "hasLogo": false,
      "hasBarcode": false,
      "hasExpiration": false,
      "colors": ["Oscuro"],
      "packaging": "Desconocido"
    },
    "manufacturerInfo": {
      "manufacturer": null,
      "madeIn": null,
      "weight": null
    },
    "similarProducts": []
  },
  "suggestions": [],
  "warnings": [
    "Low image quality - image is blurry",
    "Unable to detect barcode - image quality too poor",
    "Confidence below 50% threshold - manual verification recommended"
  ],
  "metadata": {
    "analysisTime": 800,
    "imageQuality": "poor",
    "ambiguity": 0.95,
    "analysisDepth": "standard"
  }
}
```

### Respuesta de Error de API (500 Internal Server Error)

```json
{
  "status": "error",
  "error": {
    "code": "OPENAI_API_ERROR",
    "message": "OpenAI Vision API request failed",
    "details": "Rate limit exceeded. Try again in 60 seconds.",
    "retryAfter": 60
  },
  "suggestions": [],
  "warnings": ["API error occurred - please retry the scan"]
}
```

---

## 3️⃣ SEGURIDAD DE CREDENCIALES

### ✅ CERO EXPOSICIÓN EN GITHUB

**1. Environment Variables ONLY**
```properties
# .env (NUNCA commitear)
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxx

# application.yaml (usando variable)
veltro.ai.openai.api-key=${OPENAI_API_KEY:}
```

**2. .gitignore Configuration**
```
# Credenciales
.env
.env.local
.env.*.local
application-prod.yaml
application-*.properties

# Logs que pueden contener API keys
logs/
*.log

# IDE
.idea/
.vscode/
*.swp
```

**3. GitHub Secrets (CI/CD)**
```yaml
# .github/workflows/deploy.yml
env:
  OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
```

**4. application-prod.yaml (ejemplo seguro)**
```yaml
veltro:
  ai:
    openai:
      enabled: true
      api-key: ${OPENAI_API_KEY}  # NUNCA hardcoded
      model: gpt-4-vision-preview
      max-tokens: 1000
```

**5. Detección de Exposiciones**
- Script pre-commit para detectar patrones de API keys
- Renovación automática si hay exposición

---

## 4️⃣ HTTP REQUEST EXACTO A OPENAI

### Request Headers
```
POST https://api.openai.com/v1/chat/completions
Content-Type: application/json
Authorization: Bearer {OPENAI_API_KEY}
```

### Request Body
```json
{
  "model": "gpt-4-vision-preview",
  "messages": [
    {
      "role": "system",
      "content": "Eres un experto en análisis de imágenes de productos..."
    },
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "Analiza esta imagen de producto para un sistema de inventario..."
        },
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/jpeg;base64,{BASE64_IMAGE}",
            "detail": "low"  // "low", "high", "auto"
          }
        }
      ]
    }
  ],
  "max_tokens": 1000,
  "temperature": 0.3
}
```

### Response Status Codes
- **200 OK**: Análisis completado (incluye suggestions array)
- **400 Bad Request**: Imagen inválida, no enviada correctamente
- **401 Unauthorized**: API key inválida o expirada
- **429 Too Many Requests**: Rate limit alcanzado (incluye Retry-After)
- **500 Internal Server Error**: Error interno de OpenAI

---

## 5️⃣ RECOMENDACIONES DE IMPLEMENTACIÓN

### Validación Previa
- ✅ Tamaño: máx 10MB
- ✅ Formato: JPEG, PNG, WebP, GIF
- ✅ Dimensiones: mín 256x256px
- ✅ No URLS externas, solo base64 data

### Manejo de Errores
- Rate limiting: Retry con backoff exponencial (1s, 2s, 4s, 8s)
- Timeout: 30 segundos máximo
- Si falla: retornar `suggestions: []` + warning

### Caching
- No cachear respuestas (cada imagen es única)
- Cachear métadata de modelos (configuración)

### Logging
- Log entrada: nombre archivo, tamaño
- Log salida: confianza, sugerencias encontradas
- Log errores: código error, mensaje, timestamp
- NUNCA loguear API keys

---

## 6️⃣ FLUJO COMPLETO

```
1. Frontend envía imagen → POST /api/v1/scanner/ai
2. Backend valida imagen
3. Backend encoda a Base64
4. Backend llama OpenAI Vision API
5. OpenAI retorna análisis JSON
6. Backend parsea respuesta
7. Backend retorna ProductSuggestionResponse al frontend
8. Frontend muestra sugerencias al usuario
9. Usuario selecciona o escanea manualmente
```

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

- [ ] Prompt exacto en OpenAiVisionClient
- [ ] Estructura JSON documentada
- [ ] OpenAI API integration completa
- [ ] Respuesta parseada correctamente
- [ ] Manejo de errores implementado
- [ ] Variables de entorno configuradas
- [ ] .gitignore actualizado
- [ ] Tests unitarios con mocks
- [ ] Logs sin exponer credentials
- [ ] Documentación actualizada
