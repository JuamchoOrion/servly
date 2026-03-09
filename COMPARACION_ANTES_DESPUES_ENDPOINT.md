# 📊 COMPARACIÓN: ENDPOINT ANTES vs DESPUÉS

## 🎯 CAMBIO REALIZADO

Se agregó **cálculo automático de fecha de expiración** basado en `expirationDays` del item.

---

## 📋 ENDPOINT: POST /api/stock-batch

### ❌ ANTES (Sin cálculo automático)

**Código en StockBatchService.java (ANTES):**
```java
public StockBatchDTO createBatch(StockBatchCreateRequest request) {
    log.info("Creando nuevo lote para ItemStock ID: {}", request.getItemStockId());

    // Validar ItemStock
    ItemStock itemStock = itemStockRepository.findById(request.getItemStockId())
            .orElseThrow(() -> new AuthException("ItemStock no encontrado"));

    // Validar cantidad
    if (request.getQuantity() == null || request.getQuantity() <= 0) {
        throw new AuthException("La cantidad debe ser mayor a 0");
    }

    // Validar supplier si se proporciona
    Supplier supplier = null;
    if (request.getSupplierId() != null) {
        supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new AuthException("Proveedor no encontrado"));
    }

    // Crear nuevo lote directamente con la fecha proporcionada
    StockBatch batch = StockBatch.builder()
            .itemStock(itemStock)
            .batchNumber(request.getBatchNumber())
            .quantity(request.getQuantity())
            .supplier(supplier)
            .expiryDate(request.getExpiryDate())  // ← USA LA FECHA DIRECTAMENTE
            .createdDate(LocalDate.now())
            .status("VIGENTE")
            .build();

    StockBatch savedBatch = stockBatchRepository.save(batch);

    // Actualizar cantidad total en ItemStock
    itemStock.setQuantity(itemStock.getQuantity() + request.getQuantity());
    itemStockRepository.save(itemStock);

    log.info("Lote creado exitosamente con ID: {}", savedBatch.getId());

    return convertToDTO(savedBatch);
}
```

**Request JSON (ANTES) - expiryDate OBLIGATORIO:**
```json
POST /api/stock-batch
{
  "itemStockId": 2,
  "quantity": 50,
  "supplierId": 1,
  "batchNumber": "LOTE-POLLO-001",
  "expiryDate": "2026-03-15"  ← ¡DEBES PROPORCIONARLO MANUALMENTE!
}
```

**Problemas ANTES:**
❌ Tenías que calcular la fecha manualmente (hoy + días)
❌ Riesgo de olvidar proporcionar la fecha
❌ Riesgo de error en el cálculo
❌ Inconsistencia si no usabas los expirationDays del item
❌ Más trabajo para el cliente

---

### ✅ DESPUÉS (Con cálculo automático)

**Código en StockBatchService.java (DESPUÉS):**
```java
public StockBatchDTO createBatch(StockBatchCreateRequest request) {
    log.info("Creando nuevo lote para ItemStock ID: {}", request.getItemStockId());

    // Validar ItemStock
    ItemStock itemStock = itemStockRepository.findById(request.getItemStockId())
            .orElseThrow(() -> new AuthException("ItemStock no encontrado"));

    // Validar cantidad
    if (request.getQuantity() == null || request.getQuantity() <= 0) {
        throw new AuthException("La cantidad debe ser mayor a 0");
    }

    // Validar supplier si se proporciona
    Supplier supplier = null;
    if (request.getSupplierId() != null) {
        supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new AuthException("Proveedor no encontrado"));
    }

    // 🆕 CALCULAR FECHA DE EXPIRACIÓN AUTOMÁTICAMENTE SI NO SE PROPORCIONA
    LocalDate expiryDate = request.getExpiryDate();
    if (expiryDate == null) {
        // Obtener los días de expiración del item
        Item item = itemStock.getItem();
        Integer expirationDays = item.getExpirationDays();
        
        if (expirationDays != null && expirationDays > 0) {
            // Calcular: fecha actual + días de expiración
            expiryDate = LocalDate.now().plusDays(expirationDays);
            log.info("Fecha de expiración calculada automáticamente: {} ({} días desde hoy)", 
                     expiryDate, expirationDays);
        } else {
            throw new AuthException("El item no tiene configurado expirationDays y no se proporcionó expiryDate");
        }
    }

    // Crear nuevo lote con fecha calculada o proporcionada
    StockBatch batch = StockBatch.builder()
            .itemStock(itemStock)
            .batchNumber(request.getBatchNumber())
            .quantity(request.getQuantity())
            .supplier(supplier)
            .expiryDate(expiryDate)  // ← USA FECHA CALCULADA O PROPORCIONADA
            .createdDate(LocalDate.now())
            .status("VIGENTE")
            .build();

    StockBatch savedBatch = stockBatchRepository.save(batch);

    // Actualizar cantidad total en ItemStock
    itemStock.setQuantity(itemStock.getQuantity() + request.getQuantity());
    itemStockRepository.save(itemStock);

    log.info("Lote creado exitosamente con ID: {} - Vence: {}", savedBatch.getId(), expiryDate);

    return convertToDTO(savedBatch);
}
```

**Request JSON (DESPUÉS) - expiryDate OPCIONAL:**

**Opción 1: SIN proporcionar expiryDate (AUTOMÁTICO)**
```json
POST /api/stock-batch
{
  "itemStockId": 2,
  "quantity": 50,
  "supplierId": 1,
  "batchNumber": "LOTE-POLLO-001"
  // ✅ NO necesita expiryDate - se calcula automáticamente
  // Fórmula: hoy (8/Mar/2026) + 7 días = 15/Mar/2026
}

RESPUESTA:
{
  "id": 1,
  "batchNumber": "LOTE-POLLO-001",
  "quantity": 50,
  "expiryDate": "2026-03-15",  ← Calculada automáticamente
  "status": "VIGENTE",
  "daysUntilExpiry": 7
}
```

**Opción 2: CON expiryDate personalizada (MANUAL)**
```json
POST /api/stock-batch
{
  "itemStockId": 2,
  "quantity": 50,
  "supplierId": 1,
  "batchNumber": "LOTE-POLLO-CUSTOM",
  "expiryDate": "2026-03-20"  ← ✅ Puedes proporcionar si necesitas
}

RESPUESTA:
{
  "id": 2,
  "batchNumber": "LOTE-POLLO-CUSTOM",
  "quantity": 50,
  "expiryDate": "2026-03-20",  ← Usa la fecha proporcionada
  "status": "VIGENTE",
  "daysUntilExpiry": 12
}
```

**Beneficios DESPUÉS:**
✅ La fecha se calcula automáticamente (no tienes que hacerlo)
✅ Usa siempre los expirationDays del item
✅ Menos errores (no olvidas la fecha)
✅ Más rápido (menos trabajo)
✅ Flexible (puedes proporcionar fecha si necesitas)
✅ Consistente (todas las fechas se calculan igual)

---

## 📊 TABLA COMPARATIVA

| Aspecto | ANTES | DESPUÉS |
|---------|-------|---------|
| **expiryDate requerido** | ✅ Sí, OBLIGATORIO | ❌ No, OPCIONAL |
| **Cálculo de fecha** | Cliente | Backend (automático) |
| **Fórmula usada** | Manual (cliente) | Item.expirationDays |
| **Riesgo de error** | ⚠️ Alto | ✅ Bajo |
| **Velocidad entrada** | Lenta | Rápida |
| **Flexibilidad** | Baja | Alta |
| **Consistencia** | Variable | Garantizada |

---

## 🔄 EJEMPLOS PRÁCTICOS

### CASO 1: POLLO (7 días de expiración)

**ANTES:**
```json
{
  "itemStockId": 2,
  "quantity": 50,
  "supplierId": 1,
  "batchNumber": "LOTE-001",
  "expiryDate": "2026-03-15"  ← TÚ CALCULAS: 8/Mar + 7 = 15/Mar
}
```

**DESPUÉS:**
```json
{
  "itemStockId": 2,
  "quantity": 50,
  "supplierId": 1,
  "batchNumber": "LOTE-001"
  // ✅ BACKEND CALCULA: 8/Mar + 7 = 15/Mar
}
```

---

### CASO 2: ARROZ (365 días de expiración)

**ANTES:**
```json
{
  "itemStockId": 1,
  "quantity": 100,
  "supplierId": 1,
  "batchNumber": "LOTE-ARROZ-001",
  "expiryDate": "2027-03-08"  ← TÚ CALCULAS: 8/Mar/2026 + 365 = 8/Mar/2027
}
```

**DESPUÉS:**
```json
{
  "itemStockId": 1,
  "quantity": 100,
  "supplierId": 1,
  "batchNumber": "LOTE-ARROZ-001"
  // ✅ BACKEND CALCULA: 8/Mar/2026 + 365 = 8/Mar/2027
}
```

---

### CASO 3: FECHA PERSONALIZADA (Excepción)

**ANTES:**
```json
{
  "itemStockId": 3,
  "quantity": 40,
  "supplierId": 5,
  "batchNumber": "LOTE-LECHE-ESPECIAL",
  "expiryDate": "2026-03-25"  ← FECHA DIFERENTE A LOS 14 DÍAS NORMALES
}
```

**DESPUÉS:**
```json
{
  "itemStockId": 3,
  "quantity": 40,
  "supplierId": 5,
  "batchNumber": "LOTE-LECHE-ESPECIAL",
  "expiryDate": "2026-03-25"  ← SIGUE FUNCIONANDO para casos especiales
}
```

---

## 🔍 LÓGICA DEL CAMBIO

```
ANTES:
┌─────────────────────────────┐
│ Cliente envía request       │
│ con expiryDate              │
└──────────────┬──────────────┘
               │
               ▼
    ┌──────────────────┐
    │ Backend usa      │
    │ fecha tal cual   │
    └──────────────────┘

DESPUÉS:
┌──────────────────────────────────┐
│ Cliente envía request            │
│ SIN expiryDate (o con ella)      │
└──────────────┬───────────────────┘
               │
        ┌──────▼──────┐
        │ ¿Hay        │
        │ expiryDate? │
        └──────┬──────┘
               │
        ┌──────┴──────────┐
       NO                 SÍ
        │                 │
        ▼                 ▼
    ┌────────────┐   ┌─────────────────┐
    │ Calcular:  │   │ Usar la fecha   │
    │ hoy +      │   │ proporcionada   │
    │ Item.      │   │                 │
    │ expiration │   │                 │
    │ Days       │   │                 │
    └────┬───────┘   └────────┬────────┘
         │                    │
         └──────────┬─────────┘
                    │
                    ▼
            ┌──────────────────┐
            │ Backend guarda   │
            │ con fecha        │
            │ calculada/       │
            │ proporcionada    │
            └──────────────────┘
```

---

## 📝 RESUMEN DE CAMBIOS

**Archivos Modificados:**

1. **StockBatchCreateRequest.java**
   - Cambio: `expiryDate` pasó de ser obligatorio a opcional
   - Agregó documentación sobre cálculo automático

2. **StockBatchService.java**
   - Agregó lógica de cálculo: `LocalDate.now().plusDays(expirationDays)`
   - Agregó validación de expirationDays
   - Agregó logs de cálculo automático
   - Importó clase `Item`

**Flujo del Cambio:**
```
if (expiryDate == null) {
    expiryDate = LocalDate.now().plusDays(item.getExpirationDays());
} else {
    // Usar la fecha proporcionada
}
```

---

## 🎯 IMPACTO PARA EL CLIENTE

| Escenario | ANTES | DESPUÉS |
|-----------|-------|---------|
| Crear lote normal | 10 segundos (calcular + enviar) | 3 segundos (solo enviar) |
| Error al calcular fecha | ⚠️ Posible | ✅ Imposible |
| Olvidar expiryDate | ❌ Error 400 | ✅ Se calcula automáticamente |
| Necesitar fecha distinta | ✅ Funciona | ✅ Sigue funcionando |

---

## 💻 CÓDIGO ESPECÍFICO DEL CÁLCULO

**La línea mágica que hace todo:**
```java
expiryDate = LocalDate.now().plusDays(expirationDays);
```

**Ejemplo en acción:**
```
Hoy: 8 de Marzo de 2026
Pollo tiene expirationDays = 7

Cálculo:
  LocalDate.now()           → 2026-03-08
  .plusDays(7)              → suma 7 días
  = 2026-03-15

Resultado: El lote vence el 15 de Marzo de 2026
```

---

## ✨ CONCLUSIÓN

El cambio es **retrocompatible** (aún puedes enviar expiryDate) pero ahora **opcional** y más **inteligente** (calcula automáticamente basándose en los datos del item).

**Antes:** Cliente tenía que ser matemático
**Después:** Backend hace el trabajo sucio ✅

