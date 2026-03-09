# GESTIÓN DE VENCIMIENTO CON LOTES (OPCIÓN A) - DOCUMENTACIÓN

## 📦 Arquitectura del Sistema

```
Item (Producto: Arroz)
│
└─ ItemStock (Cantidad total = 180kg)
   │
   ├─ StockBatch #1
   │  ├─ Cantidad: 100kg
   │  ├─ Proveedor: A
   │  ├─ Número Lote: LOTE-ARROZ-001
   │  └─ Vence: 01/Jun/2026
   │
   ├─ StockBatch #2
   │  ├─ Cantidad: 30kg
   │  ├─ Proveedor: B
   │  ├─ Número Lote: LOTE-ARROZ-002
   │  └─ Vence: 05/Jun/2026
   │
   └─ StockBatch #3
      ├─ Cantidad: 50kg
      ├─ Proveedor: A
      ├─ Número Lote: LOTE-ARROZ-003
      └─ Vence: 10/Jun/2026
```

## 🎯 Conceptos Clave

### **Item (Producto)**
- Nombre del producto (Arroz, Pollo, Leche, etc.)
- Descripción, unidad de medida, categoría
- Días de expiración (referencial)

### **ItemStock (Inventario del Producto)**
- Cantidad **TOTAL** del producto en el inventario
- Se actualiza automáticamente cuando agregas/quitas lotes
- Es la suma de todas las cantidades de StockBatch

### **StockBatch (Lote Individual)**
- Representa una **entrada específica** del producto
- Tiene cantidad, proveedor y fecha de vencimiento propios
- Permite trazabilidad completa
- Estados: VIGENTE, PROXIMO_A_EXPIRAR, EXPIRADO, AGOTADO

---

## 🔄 Flujo de Uso

### **1. Llega un nuevo producto al almacén**

```
POST /api/stock-batch
{
  "itemStockId": 1,           // ItemStock existente
  "quantity": 100,             // 100kg
  "supplierId": 1,             // Proveedor A
  "batchNumber": "LOTE-001",   // Identificador único
  "expiryDate": "2026-06-01"   // Fecha de vencimiento
}
```

**Resultado:**
- Se crea un nuevo StockBatch
- ItemStock.quantity se aumenta automáticamente (+100kg)

---

### **2. Necesitas saber qué lote vence primero (FIFO)**

```
GET /api/stock-batch/item-stock/1/next-to-expire
```

**Respuesta:**
```json
{
  "id": 1,
  "batchNumber": "LOTE-ARROZ-001",
  "quantity": 100,
  "supplierName": "Proveedor A",
  "createdDate": "2026-03-01",
  "expiryDate": "2026-06-01",
  "status": "VIGENTE",
  "daysUntilExpiry": 85
}
```

---

### **3. Necesitas consumir producto (venta/cocina)**

```
PUT /api/stock-batch/item-stock/1/decrease?quantity=50
```

**Lo que pasa automáticamente:**
1. Busca el lote que vence primero (FIFO)
2. Disminuye su cantidad en 50kg
3. Si el lote se agota, pasa al siguiente
4. Actualiza ItemStock.quantity automáticamente
5. Retorna el lote que fue modificado

---

### **4. Alertas de vencimiento**

```
GET /api/stock-batch/close-to-expire
```

**Retorna:** Todos los lotes que vencen en menos de 7 días

---

### **5. Ver lotes vencidos**

```
GET /api/stock-batch/expired
```

**Retorna:** Todos los lotes cuya fecha de vencimiento ya pasó

---

## 📊 Endpoints Completos

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/stock-batch` | Crear nuevo lote |
| GET | `/api/stock-batch/item-stock/{id}` | Obtener todos los lotes de un ItemStock |
| GET | `/api/stock-batch/item-stock/{id}/next-to-expire` | Obtener lote próximo a expirar |
| GET | `/api/stock-batch/expired` | Obtener lotes expirados |
| GET | `/api/stock-batch/close-to-expire` | Obtener lotes próximos a expirar |
| PUT | `/api/stock-batch/item-stock/{id}/decrease?quantity=X` | Consumir cantidad (FIFO) |
| PUT | `/api/stock-batch/{id}/status?status=X` | Cambiar estado manualmente |
| DELETE | `/api/stock-batch/{id}` | Eliminar un lote |

---

## 🔍 Estados de un Lote

- **VIGENTE**: El lote está disponible para consumo
- **PROXIMO_A_EXPIRAR**: Faltan menos de 7 días para expirar
- **EXPIRADO**: La fecha de vencimiento ya pasó
- **AGOTADO**: La cantidad llegó a 0 (se consumió todo)

---

## 💡 Ventajas de esta Implementación

✅ **Control granular**: Cada lote tiene su proveedor y fecha de vencimiento  
✅ **FIFO automático**: Consumo automático del lote que vence primero  
✅ **Trazabilidad**: Saber exactamente de dónde vino cada producto  
✅ **Alertas precisas**: Saber cuándo vence cada lote  
✅ **Evita pérdidas**: No compres productos que vencerán sin usar  
✅ **Auditoría**: Historial completo de movimientos  

---

## 🚀 Ejemplo Completo de Flujo

### **Escenario: Gestión de Arroz en un Restaurante**

**Día 1 - Compra a Proveedor A:**
```
POST /api/stock-batch
{
  "itemStockId": 1,
  "quantity": 100,
  "supplierId": 1,
  "batchNumber": "LOTE-ARROZ-2026-001",
  "expiryDate": "2026-06-01"
}
```
ItemStock.quantity = 100kg

**Día 3 - Compra a Proveedor B:**
```
POST /api/stock-batch
{
  "itemStockId": 1,
  "quantity": 30,
  "supplierId": 2,
  "batchNumber": "LOTE-ARROZ-2026-002",
  "expiryDate": "2026-06-05"
}
```
ItemStock.quantity = 130kg

**Día 5 - Preparación de comida (necesitamos 50kg):**
```
PUT /api/stock-batch/item-stock/1/decrease?quantity=50
```
- LOTE-ARROZ-001 disminuye: 100kg → 50kg
- ItemStock.quantity = 80kg

**Día 15 - Chequeo de vencimientos:**
```
GET /api/stock-batch/close-to-expire
```
Alerta: LOTE-ARROZ-001 vence en 17 días, hay que usarlo pronto!

**Día 20 - Más consumo (80kg):**
```
PUT /api/stock-batch/item-stock/1/decrease?quantity=80
```
- LOTE-ARROZ-001 se agota (50kg consumidos)
- LOTE-ARROZ-002 se disminuye: 30kg → 0kg
- ItemStock.quantity = 0kg

---

## 📝 Notas Importantes

1. **ItemStockId vs ItemId**:
   - Usas `itemStockId` para crear lotes (es el inventario)
   - **No** usas `itemId` directamente

2. **Cantidad Total**:
   - ItemStock.quantity es la suma de todos los lotes
   - Se actualiza automáticamente

3. **FIFO es automático**:
   - Cuando usas `/decrease`, automáticamente consume del lote que vence primero
   - No necesitas especificar qué lote

4. **Token JWT requerido**:
   - Roles necesarios: ADMIN, STOREKEEPER

---

## 🔧 Próximas Mejoras Sugeridas

- [ ] Notificaciones automáticas cuando falta 1 día para vencer
- [ ] Reportes de pérdidas por vencimiento
- [ ] Costo de merma por lote
- [ ] Historial de consumo por lote
- [ ] Integración con punto de venta (cuando se vende algo, restar automáticamente)

