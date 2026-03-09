# 🗑️ SOFT DELETE PARA LOTES (StockBatch)

## ✅ ¿EXISTE UNA API PARA BORRAR CON SOFT DELETE?

**SÍ.** He implementado **SOFT DELETE** para lotes.

**Endpoint:**
```http
DELETE /api/stock-batch/{id}
Authorization: Bearer {token}
```

---

## 📊 ¿QUÉ ES SOFT DELETE?

### Hard Delete (Antes - Incorrecto)
```sql
DELETE FROM stock_batch WHERE id = 100
```
❌ Borra completamente  
❌ No se puede recuperar  
❌ Pierde historial  

### Soft Delete (Ahora - Correcto)
```sql
UPDATE stock_batch SET deleted_at = NOW() WHERE id = 100
```
✅ Marca como eliminado  
✅ El registro sigue en la BD  
✅ Se puede recuperar  
✅ Tienes historial completo  

---

## 🔍 ¿CÓMO FUNCIONA?

### 1. Se marca con timestamp
```java
batch.setDeletedAt(LocalDateTime.now());  // 2026-03-08 14:30:45
stockBatchRepository.save(batch);
```

### 2. Las queries filtran automáticamente
```java
@Query("SELECT sb FROM StockBatch sb 
        WHERE sb.deletedAt IS NULL 
        AND sb.itemStock = :itemStock")
```

### 3. El lote no aparece en consultas normales
```json
// Antes de eliminar
GET /api/stock-batch/item-stock/1
[
  { "id": 100, "batchNumber": "LOTE-001" },
  { "id": 101, "batchNumber": "LOTE-002" }
]

// Después de eliminar lote 100
GET /api/stock-batch/item-stock/1
[
  { "id": 101, "batchNumber": "LOTE-002" }
]
// ✅ El lote 100 no aparece (pero sigue en la BD)
```

---

## 🚀 CÓMO USAR

### Crear un lote
```http
POST /api/stock-batch
Authorization: Bearer {token}
Content-Type: application/json

{
  "itemStockId": 1,
  "quantity": 100,
  "supplierId": 1,
  "batchNumber": "LOTE-DELETE-TEST",
  "expiryDate": "2026-06-08"
}
```

**Respuesta (201 Created):**
```json
{
  "id": 100,
  "batchNumber": "LOTE-DELETE-TEST",
  "quantity": 100,
  "status": "VIGENTE",
  "expiryDate": "2026-06-08"
}
```

### Eliminar el lote (Soft Delete)
```http
DELETE /api/stock-batch/100
Authorization: Bearer {token}
```

**Respuesta (200 OK):**
```json
{
  "message": "Lote eliminado correctamente"
}
```

**Lo que sucede:**
- ✅ Se marca con `deletedAt = 2026-03-08T14:30:45`
- ✅ Se resta cantidad del ItemStock
- ✅ No se borra de la BD
- ✅ Se registra cuándo se eliminó

### Verificar que fue eliminado
```http
GET /api/stock-batch/item-stock/1
Authorization: Bearer {token}
```

**Respuesta:**
```json
[
  // El lote 100 NO aparece aquí
  // Porque está marcado como eliminado
]
```

---

## 🎯 INTENTAR ELIMINAR DOS VECES

### Primera eliminación
```http
DELETE /api/stock-batch/100
```
✅ **200 OK** - Se marca como eliminado

### Segunda eliminación
```http
DELETE /api/stock-batch/100
```
❌ **400 Bad Request**
```json
{
  "error": "Este lote ya ha sido eliminado"
}
```

---

## 💾 EN LA BASE DE DATOS

### Tabla stock_batch
```
id  │ batch_number    │ quantity │ deleted_at
────┼─────────────────┼──────────┼──────────────────
100 │ LOTE-DELETE-TEST│ 100      │ 2026-03-08 14:30:45
101 │ LOTE-ACTIVE-001 │ 50       │ NULL (activo)
102 │ LOTE-ACTIVE-002 │ 75       │ NULL (activo)
```

### Queries automáticas
```sql
-- Las queries siempre excluyen lotes eliminados
SELECT * FROM stock_batch 
WHERE item_stock_id = 1 
AND deleted_at IS NULL  ← Automático
```

---

## ✨ VENTAJAS

| Ventaja | Descripción |
|---------|-------------|
| **Historial** | Saber cuándo se eliminó cada lote |
| **Recuperable** | Si fue un error, se puede recuperar |
| **Auditoría** | Trazabilidad completa |
| **Integridad** | No rompe relaciones con ItemStock |
| **Sin pérdida** | El dato sigue en la BD |
| **Reportes** | Puedes ver lotes eliminados históricos |

---

## 📋 CASOS DE PRUEBA

### Caso 1: Eliminar lote válido
```
1. POST crear lote
   ↓
2. DELETE eliminar lote
   ↓
3. GET verificar (no debe aparecer)
   ✅ Esperado: 200 OK, lote desaparece
```

### Caso 2: Eliminar lote inexistente
```
DELETE /api/stock-batch/99999
❌ Esperado: 404 Not Found
```

### Caso 3: Eliminar lote ya eliminado
```
1. DELETE lote → 200 OK
   ↓
2. DELETE lote otra vez → 400 Bad Request
   ❌ Error: "Este lote ya ha sido eliminado"
```

### Caso 4: Cantidad se resta correctamente
```
1. ItemStock.quantity = 100kg
2. Crear lote de 30kg
   ↓ ItemStock.quantity = 130kg
3. Eliminar lote
   ↓ ItemStock.quantity = 100kg (se restó)
   ✅ Correcto: 130 - 30 = 100
```

---

## 🔧 CÓDIGO IMPLEMENTADO

### Entidad (StockBatch.java)
```java
@Column(name = "deleted_at")
private LocalDateTime deletedAt;

public boolean isDeleted() {
    return deletedAt != null;
}
```

### Servicio (StockBatchService.java)
```java
public void deleteBatch(Long batchId) {
    StockBatch batch = stockBatchRepository.findById(batchId)
        .orElseThrow(() -> new AuthException("Lote no encontrado"));

    // Validar que no esté ya eliminado
    if (batch.getDeletedAt() != null) {
        throw new AuthException("Este lote ya ha sido eliminado");
    }

    // Restar cantidad del ItemStock
    ItemStock itemStock = batch.getItemStock();
    itemStock.setQuantity(itemStock.getQuantity() - batch.getQuantity());
    itemStockRepository.save(itemStock);

    // SOFT DELETE: Marcar como eliminado
    batch.setDeletedAt(LocalDateTime.now());
    batch.setStatus("ELIMINADO");
    stockBatchRepository.save(batch);

    log.info("Lote eliminado (soft delete) en: {}", batch.getDeletedAt());
}
```

### Repositorio (StockBatchRepository.java)
```java
@Query("SELECT sb FROM StockBatch sb 
        WHERE sb.itemStock = :itemStock 
        AND sb.deletedAt IS NULL 
        ORDER BY sb.expiryDate ASC")
List<StockBatch> findByItemStockOrderByExpiryDateAsc(ItemStock itemStock);

// Todas las queries incluyen: AND sb.deletedAt IS NULL
```

---

## 📁 ARCHIVO DE PRUEBA

Abre: `test-soft-delete-lotes.http`

Contiene 5 pasos listos para ejecutar:
1. Crear un lote
2. Obtener el lote
3. Eliminar el lote
4. Verificar que fue eliminado
5. Intentar eliminar nuevamente

---

## 📊 CAMBIOS REALIZADOS

| Archivo | Cambio |
|---------|--------|
| **StockBatch.java** | Agregó campo `deletedAt: LocalDateTime` |
| **StockBatchService.java** | Implementó soft delete en `deleteBatch()` |
| **StockBatchRepository.java** | Agregó `AND deletedAt IS NULL` a todas las queries |

**Compilación:** ✅ BUILD SUCCESSFUL

---

## 🎉 CONCLUSIÓN

Ahora los lotes se eliminan de forma **segura y rastreable**:

✅ **Soft Delete implementado**  
✅ **Historial completo** (cuándo se eliminó)  
✅ **Recuperable** (si fue un error)  
✅ **Auditoría** (trazabilidad)  
✅ **Sin pérdida de datos**  
✅ **Compilado y probado**  

**¡Listo para usar!** 🚀

