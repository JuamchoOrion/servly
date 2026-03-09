# 📋 EJEMPLOS LADO A LADO: ANTES vs DESPUÉS

## 🎯 COMPARACIÓN DE CASOS DE USO

---

## CASO 1: POLLO (7 días de expiración)

### ❌ ANTES - Tenías que calcular manualmente

**Proceso:**
1. Sé que hoy es 8 de Marzo 2026
2. Pollo tiene 7 días de expiración
3. Calculo: 8 + 7 = 15 de Marzo
4. Envío la fecha calculada

```http
POST /api/stock-batch
Content-Type: application/json

{
  "itemStockId": 2,
  "quantity": 50,
  "supplierId": 1,
  "batchNumber": "LOTE-POLLO-001",
  "expiryDate": "2026-03-15"  ← CALCULASTE TÚ
}
```

**Respuesta:**
```json
{
  "id": 1,
  "batchNumber": "LOTE-POLLO-001",
  "quantity": 50,
  "expiryDate": "2026-03-15",
  "status": "VIGENTE",
  "daysUntilExpiry": 7
}
```

### ✅ DESPUÉS - El backend lo calcula automáticamente

**Proceso:**
1. Solo envío los datos básicos
2. Backend obtiene expirationDays del item (7 días)
3. Backend calcula: hoy + 7 = 15 de Marzo
4. Listo!

```http
POST /api/stock-batch
Content-Type: application/json

{
  "itemStockId": 2,
  "quantity": 50,
  "supplierId": 1,
  "batchNumber": "LOTE-POLLO-001"
  // ✅ NO ENVÍAS expiryDate - SE CALCULA AUTOMÁTICAMENTE
}
```

**Respuesta:**
```json
{
  "id": 1,
  "batchNumber": "LOTE-POLLO-001",
  "quantity": 50,
  "expiryDate": "2026-03-15",  ← BACKEND CALCULÓ ESTO
  "status": "VIGENTE",
  "daysUntilExpiry": 7
}
```

**Logs del servidor:**
```
INFO [StockBatchService] Fecha de expiración calculada automáticamente: 2026-03-15 (7 días desde hoy)
INFO [StockBatchService] Lote creado exitosamente con ID: 1 - Vence: 2026-03-15
```

---

## CASO 2: ARROZ (365 días - 1 año)

### ❌ ANTES

**Tenías que hacer este cálculo mental:**
- Hoy: 8 de Marzo 2026
- +365 días
- = 8 de Marzo 2027

```http
POST /api/stock-batch
{
  "itemStockId": 1,
  "quantity": 100,
  "supplierId": 1,
  "batchNumber": "LOTE-ARROZ-001",
  "expiryDate": "2027-03-08"  ← TÚ CALCULASTE
}
```

### ✅ DESPUÉS

```http
POST /api/stock-batch
{
  "itemStockId": 1,
  "quantity": 100,
  "supplierId": 1,
  "batchNumber": "LOTE-ARROZ-001"
  // Backend automáticamente hace: 8/Mar/2026 + 365 = 8/Mar/2027
}
```

---

## CASO 3: LECHE (14 días)

### ❌ ANTES

```http
POST /api/stock-batch
{
  "itemStockId": 3,
  "quantity": 50,
  "supplierId": 5,
  "batchNumber": "LOTE-LECHE-001",
  "expiryDate": "2026-03-22"  ← 8 + 14 = 22
}
```

### ✅ DESPUÉS

```http
POST /api/stock-batch
{
  "itemStockId": 3,
  "quantity": 50,
  "supplierId": 5,
  "batchNumber": "LOTE-LECHE-001"
  // Backend: 8/Mar + 14 = 22/Mar (automático)
}
```

---

## CASO 4: CASO ESPECIAL - Fecha diferente a los días normales

### Escenario
Necesitas una fecha distinta a la calculada automáticamente (ejemplo: llegó un lote vencido más rápido)

### ❌ ANTES
```http
POST /api/stock-batch
{
  "itemStockId": 3,
  "quantity": 40,
  "supplierId": 5,
  "batchNumber": "LOTE-LECHE-ESPECIAL",
  "expiryDate": "2026-03-20"  ← DIFERENTE a los 14 días normales
}
```

### ✅ DESPUÉS - SIGUE FUNCIONANDO (Retrocompatible)
```http
POST /api/stock-batch
{
  "itemStockId": 3,
  "quantity": 40,
  "supplierId": 5,
  "batchNumber": "LOTE-LECHE-ESPECIAL",
  "expiryDate": "2026-03-20"  ← ✅ SIGUE SIENDO VÁLIDO
}
```

El backend verifica: `if (expiryDate == null)` 
- Si proporcionas expiryDate → usa esa
- Si NO proporcionas → calcula automáticamente

---

## 📊 COMPARACIÓN LADO A LADO - JSON

```
ANTES                                    │ DESPUÉS
─────────────────────────────────────────┼─────────────────────────────────
{                                        │ {
  "itemStockId": 2,                      │   "itemStockId": 2,
  "quantity": 50,                        │   "quantity": 50,
  "supplierId": 1,                       │   "supplierId": 1,
  "batchNumber": "LOTE-001",             │   "batchNumber": "LOTE-001"
  "expiryDate": "2026-03-15"  ← NECESARIO│ }
}                                        │
                                         │ Backend calcula automáticamente:
                                         │ expiryDate = "2026-03-15"
```

---

## ⏱️ COMPARACIÓN DE TIEMPO

### ANTES: Crear 10 lotes de pollo

```
Lote 1: Pensar (30s) + calcular (10s) + escribir (20s) = 60s
Lote 2: 60s
Lote 3: 60s
...
Lote 10: 60s
────────────────────────────────────────────
TOTAL: 10 minutos
```

### DESPUÉS: Crear 10 lotes de pollo

```
Lote 1: Escribir (20s) = 20s
Lote 2: 20s
Lote 3: 20s
...
Lote 10: 20s
────────────────────────────────────────────
TOTAL: 3.3 minutos (17% del tiempo anterior)
```

**Ahorro:** 6.7 minutos (67% más rápido)

---

## 🔍 INTERIOR DEL CAMBIO - Paso a Paso

### FLUJO ANTES

```
Cliente envía request
       ↓
  ¿Tiene expiryDate?
       ↓
    SÍ (siempre)
       ↓
Backend lo guarda tal cual
```

### FLUJO DESPUÉS

```
Cliente envía request
       ↓
Backend verifica: ¿Tiene expiryDate?
       ↓
    ┌──┴──┐
   NO    SÍ
    │     │
    │     └─→ Usa esa fecha
    │
    └─→ Obtiene Item.expirationDays
         ↓
      Calcula: hoy + días
         ↓
      Usa fecha calculada
         ↓
      Guarda + valida
```

---

## 💡 PREGUNTAS FRECUENTES

### P: ¿Qué pasa si no envío expiryDate?

**ANTES:** Error 400 - Campo requerido
```
{
  "error": "Field 'expiryDate' is required"
}
```

**DESPUÉS:** Se calcula automáticamente
```
{
  "id": 1,
  "expiryDate": "2026-03-15",  ← Calculada
  "status": "VIGENTE"
}
```

---

### P: ¿Qué pasa si envío expiryDate?

**ANTES:** Se usa tal cual
```
Entrada: "expiryDate": "2026-03-15"
Salida:  "expiryDate": "2026-03-15"
```

**DESPUÉS:** Se usa tal cual (igual que antes)
```
Entrada: "expiryDate": "2026-03-15"
Salida:  "expiryDate": "2026-03-15"
```

---

### P: ¿Qué si el item no tiene expirationDays?

**ANTES:** Se guardaba con expiryDate indefinida (o nula)

**DESPUÉS:** Error claro
```
{
  "error": "El item no tiene configurado expirationDays y no se proporcionó expiryDate"
}
```

---

## 📱 EJEMPLO COMPLETO CON CURL

### ANTES

```bash
# Tenías que calcular primero
# Hoy: 8 de Marzo 2026
# Pollo: 7 días
# Cálculo: 8 + 7 = 15

curl -X POST http://localhost:8081/api/stock-batch \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "itemStockId": 2,
    "quantity": 50,
    "supplierId": 1,
    "batchNumber": "LOTE-POLLO-001",
    "expiryDate": "2026-03-15"
  }'
```

### DESPUÉS

```bash
# Solo envías los datos básicos
# Backend calcula la fecha automáticamente

curl -X POST http://localhost:8081/api/stock-batch \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "itemStockId": 2,
    "quantity": 50,
    "supplierId": 1,
    "batchNumber": "LOTE-POLLO-001"
  }'

# Backend retorna:
# "expiryDate": "2026-03-15"  (calculada automáticamente)
```

---

## 🎯 RESUMEN FINAL

| Aspecto | ANTES | DESPUÉS |
|---------|-------|---------|
| Envías expiryDate | ✅ Sí (obligatorio) | ⭕ Opcional |
| Campo "expiryDate" en request | ✅ Necesario | ⭕ Deseado pero no necesario |
| Cálculo de fecha | 🧮 Tú | 🤖 Backend |
| Riesgo de error | ⚠️ Alto | ✅ Bajo |
| Velocidad | 🐢 Lenta | 🚀 Rápida |
| Consistencia | 📊 Variable | 📊 Garantizada |
| Flexibilidad | 📦 Rígido | 📦 Flexible |

---

## ✨ CONCLUSIÓN

El cambio es **simple pero poderoso**:

- **Retrocompatible:** Si envías expiryDate, funciona igual
- **Inteligente:** Si no lo envías, lo calcula automáticamente
- **Consistente:** Siempre usa los expirationDays del item
- **Rápido:** Menos trabajo para el cliente
- **Seguro:** Menos errores posibles

Básicamente: El backend ahora es más "inteligente" y hace el trabajo pesado por ti. 🚀

