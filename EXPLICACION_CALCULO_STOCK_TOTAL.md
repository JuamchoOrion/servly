# 📊 ¿CÓMO SE CALCULA EL STOCK TOTAL DE UN ITEM?

## 🎯 RESPUESTA RÁPIDA

**El stock total de un item es la SUMA de las cantidades de todos sus lotes (StockBatch).**

```
Stock Total = Suma de todas las cantidades de StockBatch
```

---

## 📋 ESTRUCTURA ACTUAL

```
Item: Arroz
│
└─ ItemStock
   ├─ quantity: 240 kg  ← ESTO ES LA SUMA TOTAL
   │
   └─ Batches (lotes):
      ├─ StockBatch #1: 100 kg ─┐
      ├─ StockBatch #2: 80 kg  ├─ SUMA = 240 kg
      └─ StockBatch #3: 60 kg  ─┘
```

---

## 🔢 CÓMO SE CALCULA EN LA PRÁCTICA

### EJEMPLO 1: Crear un Lote (ADD)

**Paso 1: Estado Inicial**
```
ItemStock.quantity = 0 kg
StockBatch = [] (lista vacía)
```

**Paso 2: Crear Lote de 100kg**
```
POST /api/stock-batch
{
  "itemStockId": 1,
  "quantity": 100,
  "batchNumber": "LOTE-ARROZ-001",
  "expiryDate": "2026-06-01"
}
```

**Paso 3: Fórmula en el Backend**
```
ItemStock.quantity = ItemStock.quantity + request.quantity
ItemStock.quantity = 0 + 100
ItemStock.quantity = 100 kg ✅
```

**Paso 4: Estado Después**
```
ItemStock.quantity = 100 kg
StockBatch = [LOTE-001: 100kg]
```

---

### EJEMPLO 2: Múltiples Lotes

**Escenario:** Llega más arroz del mismo proveedor

**Paso 1: Crear Lote 2 de 80kg**
```
POST /api/stock-batch
{
  "itemStockId": 1,
  "quantity": 80,
  "batchNumber": "LOTE-ARROZ-002",
  "expiryDate": "2026-06-15"
}
```

**Fórmula:**
```
ItemStock.quantity = ItemStock.quantity + 80
ItemStock.quantity = 100 + 80
ItemStock.quantity = 180 kg ✅
```

**Estado Después:**
```
ItemStock.quantity = 180 kg
StockBatch = [
  LOTE-001: 100kg,
  LOTE-002: 80kg
]
```

**Paso 2: Crear Lote 3 de 60kg**
```
POST /api/stock-batch
{
  "itemStockId": 1,
  "quantity": 60,
  "batchNumber": "LOTE-ARROZ-003",
  "expiryDate": "2026-06-20"
}
```

**Fórmula:**
```
ItemStock.quantity = ItemStock.quantity + 60
ItemStock.quantity = 180 + 60
ItemStock.quantity = 240 kg ✅
```

**Estado Final:**
```
ItemStock.quantity = 240 kg
StockBatch = [
  LOTE-001: 100kg,
  LOTE-002: 80kg,
  LOTE-003: 60kg
]
```

---

## 📉 CÓMO SE CALCULA AL CONSUMIR (FIFO)

### EJEMPLO: Consumir 50kg (FIFO Automático)

**Estado Inicial:**
```
ItemStock.quantity = 240 kg
StockBatch = [
  LOTE-001: 100kg (vence 01/Jun),
  LOTE-002: 80kg  (vence 15/Jun),
  LOTE-003: 60kg  (vence 20/Jun)
]
```

**Request:**
```
PUT /api/stock-batch/item-stock/1/decrease?quantity=50
```

**Proceso FIFO:**
```
1. Obtener lotes ordenados por fecha (FIFO)
   ↓
2. LOTE-001 vence primero (01/Jun), cantidad: 100kg
   ↓
3. Consumir 50kg del LOTE-001
   LOTE-001: 100kg - 50kg = 50kg
   ↓
4. Actualizar ItemStock.quantity
   ItemStock.quantity = 240kg - 50kg = 190kg ✅
```

**Estado Después:**
```
ItemStock.quantity = 190 kg
StockBatch = [
  LOTE-001: 50kg  (vence 01/Jun),  ← Reducido
  LOTE-002: 80kg  (vence 15/Jun),
  LOTE-003: 60kg  (vence 20/Jun)
]
```

**Verificación:**
```
50 + 80 + 60 = 190 kg ✅
```

---

## 🗑️ CÓMO SE CALCULA AL ELIMINAR UN LOTE

**Estado Inicial:**
```
ItemStock.quantity = 190 kg
StockBatch = [
  LOTE-001: 50kg,
  LOTE-002: 80kg,
  LOTE-003: 60kg
]
```

**Request: Eliminar LOTE-001**
```
DELETE /api/stock-batch/1
```

**Proceso:**
```
1. Obtener cantidad del lote a eliminar: 50kg
   ↓
2. Restar del ItemStock.quantity
   ItemStock.quantity = 190kg - 50kg = 140kg ✅
   ↓
3. Eliminar el lote de la BD
```

**Estado Después:**
```
ItemStock.quantity = 140 kg
StockBatch = [
  LOTE-002: 80kg,  ← LOTE-001 eliminado
  LOTE-003: 60kg
]
```

**Verificación:**
```
80 + 60 = 140 kg ✅
```

---

## 💾 DÓNDE SE ALMACENA

### En la Base de Datos:

**Tabla: item_stock**
```sql
id  | item_id | quantity | supplier_id | inventory_id
1   | 1       | 240      | NULL        | 1
2   | 2       | 100      | NULL        | 1
3   | 3       | 90       | NULL        | 1
...
```

**Tabla: stock_batch**
```sql
id  | item_stock_id | batch_number      | quantity | supplier_id | expiry_date
1   | 1             | LOTE-ARROZ-001    | 100      | 1           | 2026-06-01
2   | 1             | LOTE-ARROZ-002    | 80       | 2           | 2026-06-15
3   | 1             | LOTE-ARROZ-003    | 60       | 1           | 2026-06-20
4   | 2             | LOTE-POLLO-001    | 40       | 3           | 2026-03-15
...
```

---

## 🔍 EN EL CÓDIGO

### Crear Lote (Línea 71-72 de StockBatchService.java):
```java
// Actualizar cantidad total en ItemStock
itemStock.setQuantity(itemStock.getQuantity() + request.getQuantity());
itemStockRepository.save(itemStock);
```

**Explicación:**
- Obtiene la cantidad actual: `itemStock.getQuantity()`
- Suma la nueva cantidad: `+ request.getQuantity()`
- Guarda el nuevo total: `itemStockRepository.save(itemStock)`

### Consumir Lote (StockBatchService.java línea ~170):
```java
// Actualizar cantidad total en ItemStock
itemStock.setQuantity(itemStock.getQuantity() - quantity);
itemStockRepository.save(itemStock);
```

**Explicación:**
- Obtiene la cantidad actual: `itemStock.getQuantity()`
- Resta la cantidad consumida: `- quantity`
- Guarda el nuevo total: `itemStockRepository.save(itemStock)`

### Eliminar Lote (StockBatchService.java línea ~205):
```java
// Restar cantidad del ItemStock
ItemStock itemStock = batch.getItemStock();
itemStock.setQuantity(itemStock.getQuantity() - batch.getQuantity());
itemStockRepository.save(itemStock);
```

**Explicación:**
- Obtiene el ItemStock del lote
- Resta la cantidad del lote: `- batch.getQuantity()`
- Guarda el nuevo total: `itemStockRepository.save(itemStock)`

---

## 📐 FÓRMULA MATEMÁTICA

```
ItemStock.quantity = Σ (StockBatch.quantity)

Donde:
  Σ = Sumatoria
  StockBatch.quantity = cantidad de cada lote
```

**Ejemplo con números:**
```
ItemStock.quantity = LOTE-001.quantity + LOTE-002.quantity + LOTE-003.quantity
ItemStock.quantity = 100 + 80 + 60
ItemStock.quantity = 240 kg
```

---

## ✅ VALIDACIONES

Para asegurar que el cálculo es correcto:

### 1. Al Crear un Lote:
```
✅ quantity > 0 (debe ser positivo)
✅ itemStockId existe
✅ supplierId existe
✅ expiryDate es válido
```

### 2. Al Consumir:
```
✅ itemStockId existe
✅ quantity > 0
✅ quantity <= ItemStock.quantity (no puede exceder)
✅ Usar FIFO (lote que vence primero)
```

### 3. Al Eliminar:
```
✅ batchId existe
✅ Restar correctamente del total
```

---

## 🔄 CICLO COMPLETO

```
PASO 1: Crear ItemStock (quantity = 0)
        ↓
PASO 2: Crear Lote 1 (+100kg)
        ItemStock = 100kg
        ↓
PASO 3: Crear Lote 2 (+80kg)
        ItemStock = 180kg
        ↓
PASO 4: Crear Lote 3 (+60kg)
        ItemStock = 240kg
        ↓
PASO 5: Consumir 50kg (FIFO)
        ItemStock = 190kg
        ↓
PASO 6: Eliminar Lote 1 (-50kg)
        ItemStock = 140kg
        ↓
PASO 7: Estado Final
        ItemStock = 140kg (= 80kg + 60kg)
```

---

## 💡 PUNTOS CLAVE

✅ **No se calcula dinámicamente** - Se actualiza en cada cambio
✅ **Se guarda en ItemStock.quantity** - Un solo número por item
✅ **FIFO automático** - Consume del lote que vence primero
✅ **Transaccional** - Se actualiza atomáticamente
✅ **Verificable** - La suma de lotes siempre coincide con el total

---

## 📊 EJEMPLO REAL CON DATASET

**Inventario Inicial:**
```
Arroz:      0 kg (sin lotes)
Pollo:      0 kg (sin lotes)
Leche:      0 kg (sin lotes)
```

**Después de crear lotes (con dataset):**
```
Arroz:      240 kg = 100 + 80 + 60 (3 lotes)
Pollo:      100 kg = 40 + 35 + 25  (3 lotes)
Leche:       90 L  = 50 + 40       (2 lotes)
Tomate:     105 kg = 60 + 45       (2 lotes)
Cebolla:    120 kg = 70 + 50       (2 lotes)
```

---

## 🎯 RESUMEN

| Operación | Fórmula | Ejemplo |
|-----------|---------|---------|
| Crear Lote | Total = Total + Lote | 100 = 0 + 100 |
| Más Lotes | Total = Total + Lote | 180 = 100 + 80 |
| Consumir | Total = Total - Consumo | 190 = 240 - 50 |
| Eliminar Lote | Total = Total - Lote | 140 = 190 - 50 |

**En conclusión:** El stock total siempre es la **suma de todos los lotes activos**.

¡Es tan simple como sumar! 🧮

