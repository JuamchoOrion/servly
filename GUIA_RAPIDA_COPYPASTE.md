# ⚡ GUÍA RÁPIDA - COPY-PASTE PARA FRONTEND

## 🚀 SI TIENES PRISA: OPCIÓN RÁPIDA

### PASO 1: Copiar Servicio (2 minutos)

**Archivo:** `CODIGO_ANGULAR_STOCK_BATCH.md`
**Sección:** "1. SERVICIO: stock-batch.service.ts"

Copia TODO el código del servicio y pégalo en:
```
src/app/services/stock-batch.service.ts
```

---

### PASO 2: Copiar Componente de Alertas (3 minutos)

**Archivo:** `CODIGO_ANGULAR_STOCK_BATCH.md`
**Sección:** "2. COMPONENTE: Alertas de Vencimiento"

Copia los 3 archivos:
- `stock-batch-alerts.component.ts` → `src/app/components/stock-batch-alerts/`
- `stock-batch-alerts.component.html` → `src/app/components/stock-batch-alerts/`
- `stock-batch-alerts.component.css` → `src/app/components/stock-batch-alerts/`

---

### PASO 3: Importar en Módulo (1 minuto)

**Archivo:** `CODIGO_ANGULAR_STOCK_BATCH.md`
**Sección:** "5. IMPORTAR EN MÓDULO"

Copia el código de importación y agrégalo a `app.module.ts`

---

### PASO 4: Usar en Dashboard (1 minuto)

**Archivo:** `CODIGO_ANGULAR_STOCK_BATCH.md`
**Sección:** "4. DASHBOARD PRINCIPAL"

Copia el ejemplo y pégalo en tu dashboard:
```html
<app-stock-batch-alerts></app-stock-batch-alerts>
```

---

## ✅ TOTAL: 7 MINUTOS

¡Listo! El dashboard mostrará alertas de vencimiento de lotes.

---

## 📊 ARCHIVOS POR ORDEN DE IMPORTANCIA

### Para el Frontend:

1. **RESUMEN_DASHBOARD_REFACTOR.md** ← LEE ESTO PRIMERO (5 min)
   - Explicación general
   - Qué cambió
   - Cómo funciona

2. **PROMPT_REFACTOR_FRONTEND_DASHBOARD.md** ← SI USAS IA
   - Copia y pega en ChatGPT/Claude
   - Dejar que IA genere componentes
   - (Opción más rápida si tienes experiencia)

3. **CODIGO_ANGULAR_STOCK_BATCH.md** ← SI HACES MANUAL
   - Código listo para copiar
   - Paso a paso
   - (Opción más segura)

4. **test-stock-batch-opcion-a.http** ← PARA PROBAR
   - Abre en cliente HTTP (JetBrains IDE)
   - Ejecuta requests
   - Verifica que el backend funciona

---

## 🎯 CHECKLIST RÁPIDO

- [ ] Leí `RESUMEN_DASHBOARD_REFACTOR.md`
- [ ] Copié `StockBatchService` a `src/app/services/`
- [ ] Copié componente de alertas a `src/app/components/`
- [ ] Actualicé `app.module.ts` con imports
- [ ] Agregué `<app-stock-batch-alerts></app-stock-batch-alerts>` a dashboard
- [ ] Probé con `test-stock-batch-opcion-a.http`
- [ ] Dashboard muestra alertas ✅

---

## 🔗 ESTRUCTURA DE CARPETAS ESPERADA

```
src/
├── app/
│   ├── services/
│   │   └── stock-batch.service.ts          ← COPIAR AQUÍ
│   ├── components/
│   │   └── stock-batch-alerts/
│   │       ├── stock-batch-alerts.component.ts
│   │       ├── stock-batch-alerts.component.html
│   │       └── stock-batch-alerts.component.css
│   ├── app.module.ts                       ← ACTUALIZAR AQUÍ
│   └── dashboard/
│       └── dashboard.component.html        ← AGREGAR COMPONENTE AQUÍ
```

---

## 🚨 ERRORES COMUNES

### Error: "Cannot find module 'stock-batch.service'"
**Solución:** Verifica la ruta del import en el componente
```typescript
// Correcto
import { StockBatchService } from '../services/stock-batch.service';
```

### Error: "Template parse error"
**Solución:** Verifica que el componente esté importado en `declarations` del módulo
```typescript
@NgModule({
  declarations: [StockBatchAlertsComponent] // ← Agregar aquí
})
```

### Error: "Cannot read property 'get' of undefined"
**Solución:** Verifica que `HttpClientModule` esté importado
```typescript
import { HttpClientModule } from '@angular/common/http';

@NgModule({
  imports: [HttpClientModule] // ← Agregar aquí
})
```

---

## 📝 ENDPOINT PARA PROBAR

Una vez implementado, el servicio hará requests a:

```
GET http://localhost:8081/api/stock-batch/close-to-expire
```

Respuesta esperada:
```json
[
  {
    "id": 1,
    "batchNumber": "LOTE-ARROZ-2026-001",
    "quantity": 50,
    "supplierName": "Proveedor A",
    "expiryDate": "2026-06-01",
    "status": "VIGENTE",
    "daysUntilExpiry": 85
  }
]
```

---

## 🎨 ESTILOS PERSONALIZADOS

Si quieres cambiar los colores de las alertas:

```css
/* En stock-batch-alerts.component.css */

.urgency-badge {
  padding: 5px 10px;
  border-radius: 4px;
  color: white;
  font-weight: bold;
  font-size: 12px;
}

/* Cambiar colores aquí */
.urgency-badge.red { background-color: #F44336; }
.urgency-badge.yellow { background-color: #FFC107; }
.urgency-badge.green { background-color: #4CAF50; }
```

---

## 💾 GUARDAR DATOS LOCALES (Opcional)

Si quieres cachear datos para evitar requests continuos:

```typescript
// En stock-batch.service.ts
private batchCache: StockBatch[] = [];

getBatchesCloseTExpiry(): Observable<StockBatch[]> {
  if (this.batchCache.length > 0) {
    return of(this.batchCache);
  }
  
  return this.http.get<StockBatch[]>(`${this.apiUrl}/close-to-expire`)
    .pipe(
      tap(batches => this.batchCache = batches)
    );
}
```

---

## 🔄 AUTO-REFRESH (Recomendado)

El código ya incluye auto-refresh cada 5 minutos. Si quieres cambiarlo:

```typescript
// En stock-batch-alerts.component.ts

// Cambiar de 5 minutos a 1 minuto
interval(1 * 60 * 1000)  // 1 minuto
  .pipe(
    switchMap(() => this.stockBatchService.getBatchesCloseTExpiry()),
    takeUntil(this.destroy$)
  )
  .subscribe(batches => this.alertBatches = batches);
```

---

## 📱 RESPONSIVE (Mobile)

El CSS ya es responsive. Para ajustar el grid:

```css
/* stock-batch-alerts.component.css */

.alerts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 15px;
}

/* Para móvil, cambiar a 1 columna */
@media (max-width: 600px) {
  .alerts-grid {
    grid-template-columns: 1fr;
  }
}
```

---

## 🎯 PRÓXIMO NIVEL

Una vez que funcione el componente de alertas, agrega:

### Paso Extra 1: Tabla de Inventario
Copia el componente `StockBatchInventoryComponent` de `CODIGO_ANGULAR_STOCK_BATCH.md`

### Paso Extra 2: Gráficos
```bash
npm install ng2-charts
npm install chart.js
```

Ejemplo:
```typescript
import { Chart } from 'chart.js';

// Gráfico de lotes por estado
let ctx = document.getElementById('stateChart');
new Chart(ctx, {
  type: 'pie',
  data: {
    labels: ['VIGENTE', 'PRÓXIMO A EXPIRAR', 'EXPIRADO'],
    datasets: [{
      data: [100, 30, 5],
      backgroundColor: ['#4CAF50', '#FFC107', '#F44336']
    }]
  }
});
```

---

## ✨ FINALMENTE

Una vez implementado, tendrás:

✅ Dashboard que muestra alertas de vencimiento
✅ Actualización automática cada 5 minutos
✅ Colores según urgencia (rojo <3 días, amarillo 4-7 días)
✅ Botones para consumir y descartar lotes
✅ Información completa: lote, proveedor, cantidad, vencimiento

---

## 🎉 LISTO

**Tiempo total:** ~30 minutos
**Resultado:** Dashboard con métricas de expiración de lotes

**¡A copiar y pegar! 🚀**

