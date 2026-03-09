# 🔍 CÓMO VER LOTES EXPIRADOS Y POR QUÉ ESTÁN COMO "VIGENTE"

## 🎯 TU PREGUNTA

Viste esto:
```json
{
  "id": 15,
  "batchNumber": "LOTE-EXP-001",
  "quantity": 50,
  "expiryDate": "2026-01-01",
  "status": "VIGENTE",           ← ❌ ¿Por qué vigente si expiró?
  "daysUntilExpiry": -66         ← Hace 66 días que expiró
}
```

---

## ✅ RESPUESTA: PROBLEMA ARREGLADO

He actualizado el código para que **el `status` se calcule DINÁMICAMENTE** basándose en `daysUntilExpiry`.

Ahora el mismo lote mostrará:
```json
{
  "id": 15,
  "batchNumber": "LOTE-EXP-001",
  "quantity": 50,
  "expiryDate": "2026-01-01",
  "status": "EXPIRADO",          ← ✅ AHORA CORRECTO
  "daysUntilExpiry": -66
}
```

---

## 🔍 **¿CÓMO VER LOTES EXPIRADOS?**

### **FORMA 1: Ver TODOS los expirados (RECOMENDADO)**

```http
GET http://localhost:8081/api/stock-batch/expired
Authorization: Bearer {token}
```

**Respuesta:**
```json
[
  {
    "id": 15,
    "batchNumber": "LOTE-EXP-001",
    "quantity": 50,
    "expiryDate": "2026-01-01",
    "status": "EXPIRADO",        ← Calculado dinámicamente
    "daysUntilExpiry": -66
  },
  {
    "id": 16,
    "batchNumber": "LOTE-EXP-002",
    "quantity": 75,
    "expiryDate": "2026-01-15",
    "status": "EXPIRADO",
    "daysUntilExpiry": -52
  }
  // ... más lotes ...
]
```

✅ **Ventaja:** Ya filtra automáticamente solo los expirados

---

### **FORMA 2: Ver todos los lotes de un ItemStock**

```http
GET http://localhost:8081/api/stock-batch/item-stock/{itemStockId}
Authorization: Bearer {token}
```

**Respuesta:**
```json
[
  {
    "id": 15,
    "batchNumber": "LOTE-EXP-001",
    "status": "EXPIRADO",        ← Dinámicamente
    "daysUntilExpiry": -66
  },
  {
    "id": 16,
    "batchNumber": "LOTE-VIGENTE",
    "status": "VIGENTE",         ← Dinámicamente
    "daysUntilExpiry": 45
  }
]
```

✅ **Ventaja:** Ves todos juntos, expirados y vigentes

---

### **FORMA 3: Ver solo lotes próximos a expirar (7 días)**

```http
GET http://localhost:8081/api/stock-batch/close-to-expire
Authorization: Bearer {token}
```

**Respuesta:**
```json
[
  {
    "id": 20,
    "batchNumber": "LOTE-PRONTO",
    "status": "PROXIMO_A_EXPIRAR", ← Calculado dinámicamente
    "daysUntilExpiry": 3
  }
]
```

✅ **Ventaja:** Alertas de vencimiento cercano

---

## ❓ **¿POR QUÉ ESTABAN COMO "VIGENTE"?**

### **ANTES (Problema):**

```java
// En convertToDTO (ANTES):
return StockBatchDTO.builder()
    .status(batch.getStatus())  // ← USA EL STATUS GUARDADO EN BD
    .build();
```

- Se guardaba `status = "VIGENTE"` en la BD al crear el lote
- **Nunca se actualizaba automáticamente**
- Aunque venciera, la BD seguía diciendo "VIGENTE"
- Había inconsistencia entre la fecha de vencimiento y el estado

---

### **AHORA (Solución):**

```java
// En convertToDTO (AHORA):
String dynamicStatus;

if (batch.getQuantity() == 0) {
    dynamicStatus = "AGOTADO";
} else if (daysUntilExpiry < 0) {
    dynamicStatus = "EXPIRADO";           // ← CALCULA DINÁMICAMENTE
} else if (daysUntilExpiry >= 0 && daysUntilExpiry <= 7) {
    dynamicStatus = "PROXIMO_A_EXPIRAR";  // ← CALCULA DINÁMICAMENTE
} else {
    dynamicStatus = "VIGENTE";            // ← CALCULA DINÁMICAMENTE
}

return StockBatchDTO.builder()
    .status(dynamicStatus)  // ← USA EL STATUS CALCULADO
    .build();
```

**Ahora:**
- El status se calcula **cada vez** que se retorna
- Usa la `expiryDate` actual para calcular `daysUntilExpiry`
- No depende de lo que está en la BD
- Es **siempre exacto en tiempo real**

---

## 📊 **LÓGICA DEL CÁLCULO DINÁMICO**

```
Si quantity == 0
  ↓
  status = "AGOTADO" 🔴

Si NO, entonces:
  ↓
  Si daysUntilExpiry < 0
    ↓
    status = "EXPIRADO" 🔴
  
  Si NO, entonces:
    ↓
    Si 0 <= daysUntilExpiry <= 7
      ↓
      status = "PROXIMO_A_EXPIRAR" 🟡
    
    Si NO (daysUntilExpiry > 7):
      ↓
      status = "VIGENTE" 🟢
```

---

## 🔢 **TABLA DE CONVERSIÓN**

| daysUntilExpiry | status | Color | Significado |
|---|---|---|---|
| < 0 | EXPIRADO | 🔴 Rojo | Ya pasó la fecha |
| 0 | EXPIRADO | 🔴 Rojo | Vence hoy |
| 1 | PROXIMO_A_EXPIRAR | 🟡 Amarillo | Vence mañana |
| 2-6 | PROXIMO_A_EXPIRAR | 🟡 Amarillo | Vence en 2-6 días |
| 7 | PROXIMO_A_EXPIRAR | 🟡 Amarillo | Vence en 7 días |
| 8+ | VIGENTE | 🟢 Verde | Más de 7 días |
| 0 (qty) | AGOTADO | ⚫ Gris | Sin cantidad |

---

## 💾 **¿DÓNDE SE GUARDA?**

### **En la Base de Datos:**
```sql
-- Tabla stock_batch
id │ batch_number │ quantity │ expiry_date │ status
───┼──────────────┼──────────┼─────────────┼────────
15 │ LOTE-001     │ 50       │ 2026-01-01  │ VIGENTE
16 │ LOTE-002     │ 75       │ 2026-01-15  │ VIGENTE
```

❌ La BD sigue guardando `status = "VIGENTE"`

### **En la Respuesta JSON:**
```json
{
  "id": 15,
  "status": "EXPIRADO"      ← CALCULADO DINÁMICAMENTE
}
```

✅ Pero la respuesta retorna el status **CORRECTO**

---

## ⚡ **VENTAJAS DEL CÁLCULO DINÁMICO**

✅ **Sin inconsistencias**
   - No hay que actualizar manualmente
   - Siempre está al día

✅ **Tiempo real**
   - Cambia automáticamente cada segundo
   - No depende de trabajos programados

✅ **Eficiente**
   - Se calcula en memoria, no en BD
   - Es una operación simple

✅ **Exacto**
   - Basado en los días exactos hasta expiración
   - Usa `LocalDate.now()` del servidor

---

## 📋 **ARCHIVO DE PRUEBA**

Abre: `test-ver-lotes-expirados.http`

Contiene 3 requests listos:
1. `GET /api/stock-batch/expired` - Ver todos los expirados
2. `GET /api/stock-batch/item-stock/{id}` - Ver por ItemStock
3. `GET /api/stock-batch/close-to-expire` - Ver próximos a expirar

---

## 🎯 **RESUMEN**

| Pregunta | Respuesta |
|----------|-----------|
| ¿Cómo ves lotes expirados? | `GET /api/stock-batch/expired` |
| ¿Por qué decía "VIGENTE"? | Se guardaba al crear, no se actualizaba |
| ¿Cómo se arregló? | Cálculo dinámico basado en `daysUntilExpiry` |
| ¿Se actualiza automáticamente? | Sí, en cada request (tiempo real) |
| ¿Dónde se guarda el nuevo valor? | En memoria, no en BD |
| ¿Qué pasa si se reinicia el servidor? | Nada, se recalcula automáticamente |

---

## 🚀 **PRÓXIMOS PASOS**

1. **Inicia el servidor** (si no está activo)
2. **Abre** `test-ver-lotes-expirados.http`
3. **Ejecuta** `GET /api/stock-batch/expired`
4. **Verifica** que el status sea `"EXPIRADO"` ✅
5. **Prueba en el dashboard** para ver colores correctos

¡Listo! Ahora los lotes expirados se mostrarán correctamente. 🎉

