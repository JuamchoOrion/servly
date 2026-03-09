# 📊 PROMPT PARA REFACTORIZAR DASHBOARD - MÉTRICAS DE EXPIRACIÓN CON STOCK BATCH

Usa este prompt con tu IA del frontend (ChatGPT, Claude, etc.) para refactorizar el dashboard:

---

## 🎯 PROMPT PARA IA DEL FRONTEND

```
Necesito que refactorices el dashboard de inventario para cambiar de mostrar métricas de ItemStock 
a mostrar métricas de StockBatch (lotes individuales).

## CAMBIOS PRINCIPALES:

### 1. ESTRUCTURA DE DATOS - ANTES vs DESPUÉS

**ANTES (ItemStock):**
```typescript
interface ItemStock {
  itemStockId: Long;
  name: string;
  quantity: Integer;
  supplierName: string;
  expirationDays: Integer;
  idealStock: Integer;
}
```

**DESPUÉS (StockBatch):**
```typescript
interface StockBatch {
  id: Long;
  batchNumber: string;
  quantity: Integer;
  supplierName: string;
  createdDate: LocalDate;
  expiryDate: LocalDate;
  status: 'VIGENTE' | 'PROXIMO_A_EXPIRAR' | 'EXPIRADO' | 'AGOTADO';
  daysUntilExpiry: Integer;
}
```

### 2. ENDPOINTS A USAR

Reemplaza los endpoints antiguos con estos nuevos:

**Obtener lotes próximos a expirar (ALERTA - 7 días):**
```
GET /api/stock-batch/close-to-expire
Headers: Authorization: Bearer <token>
```
Retorna: Array de StockBatch que vencen en menos de 7 días

**Obtener lotes expirados:**
```
GET /api/stock-batch/expired
Headers: Authorization: Bearer <token>
```
Retorna: Array de StockBatch que ya vencieron

**Obtener todos los lotes de un ItemStock:**
```
GET /api/stock-batch/item-stock/{itemStockId}
Headers: Authorization: Bearer <token>
```
Retorna: Array de StockBatch ordenados por fecha de vencimiento (FIFO)

**Obtener lote próximo a expirar de un ItemStock:**
```
GET /api/stock-batch/item-stock/{itemStockId}/next-to-expire
Headers: Authorization: Bearer <token>
```
Retorna: Un StockBatch - el que vence primero

**Consumir stock (FIFO automático):**
```
PUT /api/stock-batch/item-stock/{itemStockId}/decrease?quantity=50
Headers: Authorization: Bearer <token>
```
Body: (vacío)
Retorna: StockBatch actualizado

### 3. COMPONENTES A REFACTORIZAR

#### A) WIDGET DE ALERTAS DE VENCIMIENTO

**ANTES:**
- Mostraba Items próximos a expirar basado en expirationDays
- Datos: nombre del item, días restantes estimados

**DESPUÉS:**
- Mostrar StockBatch próximos a expirar (menos de 7 días)
- Datos: batchNumber, nombre del item, proveedor, expiryDate, daysUntilExpiry
- Colorear en ROJO si vence en 3 días o menos
- Colorear en AMARILLO si vence entre 4-7 días

**Ejemplo de Card:**
```
┌─────────────────────────────────┐
│ ⚠️  LOTE PRÓXIMO A EXPIRAR      │
├─────────────────────────────────┤
│ Lote: LOTE-ARROZ-2026-001      │
│ Producto: Arroz                │
│ Proveedor: Proveedor A         │
│ Cantidad: 50kg                 │
│ Vence: 01/Jun/2026            │
│ Faltan: 3 días ⏰              │
│ [Consumir Ahora] [Descartar]   │
└─────────────────────────────────┘
```

#### B) TABLA DE INVENTARIO DETALLADO

**ANTES:**
Columnas:
- Item | Cantidad | Supplier | Días Exp. | Ideal Stock | Acciones

**DESPUÉS:**
Columnas:
- # Lote | Item | Proveedor | Cantidad | Fecha Entrada | Vence | Faltan | Estado | Acciones

Con expansible para ver todos los lotes de un item:
```
▼ Arroz
  ├─ LOTE-001 | Prov A | 50kg | 01/Mar | 01/Jun | 85 días | VIGENTE | [Ver][Consumir][Descartar]
  ├─ LOTE-002 | Prov B | 30kg | 05/Mar | 05/Jun | 89 días | VIGENTE | [Ver][Consumir][Descartar]
  └─ LOTE-003 | Prov A | 20kg | 10/Mar | 10/Jun | 94 días | VIGENTE | [Ver][Consumir][Descartar]
```

#### C) GRÁFICOS/ESTADÍSTICAS

**CAMBIOS:**
- De: Mostrar Items por días de expiración
- A: Mostrar lotes por estado (Vigente, Próximo a Expirar, Expirado, Agotado)

**Métricas nuevas:**
1. **Total de lotes activos:** Suma de lotes en estado VIGENTE
2. **Lotes próximos a expirar:** Count de lotes con daysUntilExpiry <= 7
3. **Lotes expirados:** Count de lotes con status = EXPIRADO
4. **Pérdida estimada:** Suma de cantidad de lotes expirados
5. **Por proveedor:** Cuántos lotes tiene cada proveedor

#### D) ACCIONES/BOTONES

**Nuevas acciones para cada StockBatch:**
1. **[Consumir cantidad]** → PUT /api/stock-batch/item-stock/{id}/decrease?quantity=X
   - Abre modal para ingresar cantidad
   - Usa FIFO automático
2. **[Cambiar estado]** → PUT /api/stock-batch/{id}/status?status=EXPIRADO|AGOTADO
3. **[Descartar lote]** → DELETE /api/stock-batch/{id}
4. **[Ver detalles]** → Modal con info completa del lote

### 4. CAMBIOS EN LLAMADAS A API

**ANTES - Obtener inventario:**
```typescript
// Obtener todos los items
GET /api/items/paginated?page=0&size=10

// En componente, mostrar cantidad de cada item
items.forEach(item => {
  console.log(`${item.name}: ${item.quantity}kg`);
});
```

**DESPUÉS - Obtener lotes:**
```typescript
// Obtener lotes próximos a expirar (para dashboard principal)
GET /api/stock-batch/close-to-expire

// Obtener lotes expirados (para reportes)
GET /api/stock-batch/expired

// Obtener todos los lotes de un item específico
GET /api/stock-batch/item-stock/{itemStockId}

// En componente, mostrar cada lote con su vencimiento
batches.forEach(batch => {
  console.log(`${batch.batchNumber}: ${batch.quantity}kg - Vence: ${batch.expiryDate}`);
});
```

### 5. EJEMPLO DE RESPUESTA DE API

```json
{
  "id": 1,
  "batchNumber": "LOTE-ARROZ-2026-001",
  "quantity": 100,
  "supplierName": "Proveedor A",
  "createdDate": "2026-03-01",
  "expiryDate": "2026-06-01",
  "status": "VIGENTE",
  "daysUntilExpiry": 85
}
```

### 6. COLORES Y ESTADOS

Usa este esquema de colores:

```typescript
const statusColors = {
  'VIGENTE': '#4CAF50',           // Verde
  'PROXIMO_A_EXPIRAR': '#FFC107', // Amarillo
  'EXPIRADO': '#F44336',          // Rojo
  'AGOTADO': '#9E9E9E'            // Gris
};

const urgencyColors = {
  lessThan3Days: '#F44336',       // Rojo urgente
  between4to7Days: '#FFC107',     // Amarillo alerta
  moreThan7Days: '#4CAF50'        // Verde ok
};
```

### 7. COMPONENTES TYPESCRIPT/JAVASCRIPT A CREAR

Crea estos servicios/componentes:

**1. StockBatchService (servicio HTTP)**
```typescript
@Injectable()
export class StockBatchService {
  constructor(private http: HttpClient) {}
  
  getBatchesCloseTExpiry() { }
  getExpiredBatches() { }
  getBatchesByItemStock(itemStockId) { }
  getNextToExpireBatch(itemStockId) { }
  decreaseQuantity(itemStockId, quantity) { }
  updateBatchStatus(batchId, status) { }
  deleteBatch(batchId) { }
}
```

**2. StockBatchAlertComponent**
- Mostrar lotes próximos a expirar
- Actualización automática cada 5 minutos

**3. StockBatchDetailComponent**
- Expandible para ver todos los lotes de un item
- Acciones de consumo y descarte

**4. StockBatchStatisticsComponent**
- Gráficos de estados
- Métricas de pérdida

### 8. ACTUALIZACIONES EN CONSUMO DE STOCK

**ANTES:**
```typescript
decreaseItemStock(itemStockId: number, quantity: number) {
  return this.http.put(
    `/api/item-stock/${itemStockId}/decrease`,
    { quantity }
  );
}
```

**DESPUÉS:**
```typescript
decreaseQuantity(itemStockId: number, quantity: number) {
  // Automáticamente consume del lote que vence primero (FIFO)
  return this.http.put(
    `/api/stock-batch/item-stock/${itemStockId}/decrease?quantity=${quantity}`,
    {}
  );
}
```

### 9. PUNTOS IMPORTANTES

✅ Los StockBatch están automáticamente ordenados por expiryDate (FIFO)
✅ El consumo es automático - siempre consume del lote que vence primero
✅ Cada lote tiene su proveedor específico - mejor trazabilidad
✅ El status se actualiza automáticamente según la expiryDate
✅ Puedes filtrar por estado (VIGENTE, EXPIRADO, etc.)
✅ Los daysUntilExpiry se calculan en el backend

### 10. MIGRACIONES EN VISTAS

**Dashboard Principal:**
- Cambiar widget de "Items próximos a expirar" a "Lotes próximos a expirar"
- Mostrar tabla con lotes en lugar de items

**Inventario Detallado:**
- Cambiar vista de items planos a árbol de items con lotes expandibles
- Agregar filtros por estado de lote

**Reportes:**
- Cambiar "Inventario por Item" a "Lotes por Item"
- Agregar reporte de "Lotes expirados" con cantidad y valor

---

## 📋 CHECKLIST DE CAMBIOS

- [ ] Crear StockBatchService con todos los métodos HTTP
- [ ] Reemplazar ItemStockService con StockBatchService
- [ ] Actualizar Dashboard Principal - mostrar lotes próximos a expirar
- [ ] Actualizar Tabla de Inventario - mostrar lotes con detalles
- [ ] Crear componente expandible de lotes por item
- [ ] Actualizar gráficos - cambiar a métricas de lotes
- [ ] Cambiar colores - usar esquema de estados
- [ ] Implementar consumo FIFO - usar nuevo endpoint
- [ ] Agregar filtros por estado de lote
- [ ] Actualizar reportes
- [ ] Pruebas completas

---

## 🔗 ENDPOINTS RESUMEN

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | /api/stock-batch/close-to-expire | Lotes próximos a expirar (7 días) |
| GET | /api/stock-batch/expired | Lotes ya vencidos |
| GET | /api/stock-batch/item-stock/{id} | Todos los lotes de un item |
| GET | /api/stock-batch/item-stock/{id}/next-to-expire | Lote que vence primero |
| PUT | /api/stock-batch/item-stock/{id}/decrease?quantity=X | Consumir FIFO |
| PUT | /api/stock-batch/{id}/status?status=X | Cambiar estado |
| DELETE | /api/stock-batch/{id} | Eliminar lote |

---

## 💡 EJEMPLO PRÁCTICO DE FLUJO EN FRONTEND

```typescript
// 1. Cargar lotes próximos a expirar al iniciar dashboard
ngOnInit() {
  this.stockBatchService.getBatchesCloseTExpiry().subscribe(batches => {
    this.alertBatches = batches; // Mostrar en widget de alertas
  });
}

// 2. Expandir item para ver todos sus lotes
expandItem(item) {
  this.stockBatchService.getBatchesByItemStock(item.id).subscribe(batches => {
    item.batches = batches; // Mostrar lotes expandidos
  });
}

// 3. Consumir cantidad (automáticamente usa FIFO)
consumeBatch(itemStockId, quantity) {
  this.stockBatchService.decreaseQuantity(itemStockId, quantity).subscribe(batch => {
    console.log(`Se consumieron ${quantity}kg del lote ${batch.batchNumber}`);
    this.refreshData(); // Refrescar tabla
  });
}

// 4. Descartar lote expirado
discardBatch(batchId) {
  this.stockBatchService.deleteBatch(batchId).subscribe(() => {
    console.log('Lote descartado');
    this.refreshData();
  });
}
```

---

Con estos cambios, tu dashboard tendrá:
✅ Control granular por lote
✅ Vencimientos precisos
✅ Alertas automáticas
✅ FIFO automático
✅ Mejor trazabilidad
✅ Reportes de pérdidas

¿Necesitas más detalles sobre algún componente específico?
```

---

## 🎯 CÓMO USAR ESTE PROMPT

1. Copia el contenido entre las backticks (```)
2. Pégalo en ChatGPT, Claude o tu IA favorita
3. Espera la respuesta con el código refactorizado
4. El código estará listo para copiar al frontend

## 📝 NOTAS IMPORTANTES

- El backend ya está implementado (endpoints listos)
- Los lotes se ordenan automáticamente por fecha (FIFO)
- El consumo es automático - usa el lote que vence primero
- Cada lote tiene su proveedor específico
- El status se actualiza automáticamente según expiryDate

¡El prompt está listo para que tu IA del frontend lo procese! 🚀

