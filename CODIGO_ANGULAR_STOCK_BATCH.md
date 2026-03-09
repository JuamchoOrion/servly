# 🚀 CÓDIGO ANGULAR LISTO - REFACTOR DASHBOARD STOCK BATCH

## 1. SERVICIO: stock-batch.service.ts

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface StockBatch {
  id: number;
  batchNumber: string;
  quantity: number;
  supplierName: string;
  createdDate: string;
  expiryDate: string;
  status: 'VIGENTE' | 'PROXIMO_A_EXPIRAR' | 'EXPIRADO' | 'AGOTADO';
  daysUntilExpiry: number;
}

@Injectable({
  providedIn: 'root'
})
export class StockBatchService {
  private apiUrl = 'http://localhost:8081/api/stock-batch';

  constructor(private http: HttpClient) { }

  /**
   * Obtiene todos los lotes próximos a expirar (menos de 7 días)
   */
  getBatchesCloseTExpiry(): Observable<StockBatch[]> {
    return this.http.get<StockBatch[]>(`${this.apiUrl}/close-to-expire`);
  }

  /**
   * Obtiene todos los lotes expirados
   */
  getExpiredBatches(): Observable<StockBatch[]> {
    return this.http.get<StockBatch[]>(`${this.apiUrl}/expired`);
  }

  /**
   * Obtiene todos los lotes de un ItemStock específico
   * Ordenados por fecha de vencimiento (FIFO)
   */
  getBatchesByItemStock(itemStockId: number): Observable<StockBatch[]> {
    return this.http.get<StockBatch[]>(`${this.apiUrl}/item-stock/${itemStockId}`);
  }

  /**
   * Obtiene el lote próximo a expirar de un ItemStock (para FIFO)
   */
  getNextToExpireBatch(itemStockId: number): Observable<StockBatch> {
    return this.http.get<StockBatch>(`${this.apiUrl}/item-stock/${itemStockId}/next-to-expire`);
  }

  /**
   * Consume cantidad de stock usando FIFO automático
   * Automáticamente consume del lote que vence primero
   */
  decreaseQuantity(itemStockId: number, quantity: number): Observable<StockBatch> {
    return this.http.put<StockBatch>(
      `${this.apiUrl}/item-stock/${itemStockId}/decrease?quantity=${quantity}`,
      {}
    );
  }

  /**
   * Actualiza el estado de un lote
   */
  updateBatchStatus(batchId: number, status: string): Observable<StockBatch> {
    return this.http.put<StockBatch>(
      `${this.apiUrl}/${batchId}/status?status=${status}`,
      {}
    );
  }

  /**
   * Elimina un lote de stock
   */
  deleteBatch(batchId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${batchId}`);
  }
}
```

---

## 2. COMPONENTE: Alertas de Vencimiento

### stock-batch-alerts.component.ts

```typescript
import { Component, OnInit, OnDestroy } from '@angular/core';
import { StockBatchService, StockBatch } from '../services/stock-batch.service';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-stock-batch-alerts',
  templateUrl: './stock-batch-alerts.component.html',
  styleUrls: ['./stock-batch-alerts.component.css']
})
export class StockBatchAlertsComponent implements OnInit, OnDestroy {
  alertBatches: StockBatch[] = [];
  expiredBatches: StockBatch[] = [];
  isLoading = false;
  errorMessage = '';
  private destroy$ = new Subject<void>();

  // Configuración de colores por urgencia
  urgencyConfig = {
    lessThan3Days: { color: '#F44336', label: '🔴 URGENTE' },
    between4to7Days: { color: '#FFC107', label: '🟡 ALERTA' },
    moreThan7Days: { color: '#4CAF50', label: '🟢 OK' }
  };

  constructor(private stockBatchService: StockBatchService) { }

  ngOnInit() {
    this.loadAlerts();
    
    // Recargar cada 5 minutos
    interval(5 * 60 * 1000)
      .pipe(
        switchMap(() => this.stockBatchService.getBatchesCloseTExpiry()),
        takeUntil(this.destroy$)
      )
      .subscribe(batches => this.alertBatches = batches);
  }

  loadAlerts() {
    this.isLoading = true;
    
    // Cargar lotes próximos a expirar
    this.stockBatchService.getBatchesCloseTExpiry().subscribe({
      next: (batches) => {
        this.alertBatches = batches;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Error al cargar alertas';
        this.isLoading = false;
        console.error(err);
      }
    });

    // Cargar lotes expirados
    this.stockBatchService.getExpiredBatches().subscribe({
      next: (batches) => {
        this.expiredBatches = batches;
      },
      error: (err) => console.error(err)
    });
  }

  getUrgencyLevel(daysUntilExpiry: number) {
    if (daysUntilExpiry <= 3) return this.urgencyConfig.lessThan3Days;
    if (daysUntilExpiry <= 7) return this.urgencyConfig.between4to7Days;
    return this.urgencyConfig.moreThan7Days;
  }

  consumeBatch(itemStockId: number, quantity: number) {
    const confirmConsume = confirm(`¿Consumir ${quantity}kg de este lote?`);
    if (!confirmConsume) return;

    this.stockBatchService.decreaseQuantity(itemStockId, quantity).subscribe({
      next: (batch) => {
        alert(`✅ Se consumieron ${quantity}kg del lote ${batch.batchNumber}`);
        this.loadAlerts();
      },
      error: (err) => {
        alert('❌ Error al consumir stock');
        console.error(err);
      }
    });
  }

  discardBatch(batchId: number) {
    const confirmDiscard = confirm('¿Descartar este lote?');
    if (!confirmDiscard) return;

    this.stockBatchService.deleteBatch(batchId).subscribe({
      next: () => {
        alert('✅ Lote descartado correctamente');
        this.loadAlerts();
      },
      error: (err) => {
        alert('❌ Error al descartar lote');
        console.error(err);
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
```

### stock-batch-alerts.component.html

```html
<div class="alerts-container">
  <!-- ALERTAS DE VENCIMIENTO -->
  <div class="alerts-section">
    <h2>⚠️ Lotes Próximos a Expirar</h2>
    
    <div *ngIf="isLoading" class="loading">Cargando...</div>
    
    <div *ngIf="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <div *ngIf="alertBatches.length === 0 && !isLoading" class="no-data">
      ✅ Sin alertas - Todos los lotes están vigentes
    </div>

    <div class="alerts-grid">
      <div *ngFor="let batch of alertBatches" class="alert-card" 
           [style.border-left]="'5px solid ' + getUrgencyLevel(batch.daysUntilExpiry).color">
        
        <div class="card-header">
          <span class="urgency-badge" 
                [style.background-color]="getUrgencyLevel(batch.daysUntilExpiry).color">
            {{ getUrgencyLevel(batch.daysUntilExpiry).label }}
          </span>
          <span class="batch-number">{{ batch.batchNumber }}</span>
        </div>

        <div class="card-body">
          <div class="info-row">
            <span class="label">Cantidad:</span>
            <span class="value">{{ batch.quantity }}kg</span>
          </div>
          <div class="info-row">
            <span class="label">Proveedor:</span>
            <span class="value">{{ batch.supplierName }}</span>
          </div>
          <div class="info-row">
            <span class="label">Vencimiento:</span>
            <span class="value">{{ batch.expiryDate | date:'dd/MMM/yyyy' }}</span>
          </div>
          <div class="info-row">
            <span class="label">Faltan:</span>
            <span class="value countdown">{{ batch.daysUntilExpiry }} días ⏰</span>
          </div>
        </div>

        <div class="card-actions">
          <button class="btn btn-primary" (click)="consumeBatch(batch.id, batch.quantity)">
            Consumir Ahora
          </button>
          <button class="btn btn-danger" (click)="discardBatch(batch.id)">
            Descartar
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- LOTES EXPIRADOS -->
  <div class="expired-section" *ngIf="expiredBatches.length > 0">
    <h2>🔴 Lotes Expirados</h2>
    
    <div class="expired-grid">
      <div *ngFor="let batch of expiredBatches" class="expired-card">
        <span class="batch-number">{{ batch.batchNumber }}</span>
        <span class="quantity">{{ batch.quantity }}kg</span>
        <span class="date">{{ batch.expiryDate | date:'dd/MMM/yyyy' }}</span>
        <button class="btn btn-sm btn-danger" (click)="discardBatch(batch.id)">
          Eliminar
        </button>
      </div>
    </div>
  </div>
</div>
```

### stock-batch-alerts.component.css

```css
.alerts-container {
  padding: 20px;
  background-color: #f5f5f5;
}

.alerts-section {
  margin-bottom: 30px;
}

.alerts-section h2 {
  color: #333;
  margin-bottom: 20px;
  font-size: 20px;
}

.alerts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 15px;
}

.alert-card {
  background: white;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  transition: transform 0.2s;
}

.alert-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.urgency-badge {
  padding: 5px 10px;
  border-radius: 4px;
  color: white;
  font-weight: bold;
  font-size: 12px;
}

.batch-number {
  font-weight: bold;
  color: #333;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
}

.label {
  color: #666;
  font-weight: 500;
}

.value {
  color: #333;
  font-weight: 600;
}

.countdown {
  color: #F44336;
  font-weight: bold;
}

.card-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.btn {
  flex: 1;
  padding: 8px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: background-color 0.3s;
}

.btn-primary {
  background-color: #4CAF50;
  color: white;
}

.btn-primary:hover {
  background-color: #45a049;
}

.btn-danger {
  background-color: #F44336;
  color: white;
}

.btn-danger:hover {
  background-color: #da190b;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.error-message {
  background-color: #ffebee;
  color: #c62828;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.no-data {
  background-color: #e8f5e9;
  color: #2e7d32;
  padding: 20px;
  border-radius: 4px;
  text-align: center;
}

.expired-section {
  background-color: #ffebee;
  padding: 20px;
  border-radius: 8px;
  border-left: 4px solid #F44336;
}

.expired-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 10px;
}

.expired-card {
  background: white;
  padding: 15px;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-left: 3px solid #F44336;
}

.btn-sm {
  padding: 5px 10px;
  font-size: 12px;
}
```

---

## 3. COMPONENTE: Tabla de Inventario con Lotes Expandibles

### stock-batch-inventory.component.ts

```typescript
import { Component, OnInit } from '@angular/core';
import { StockBatchService, StockBatch } from '../services/stock-batch.service';

interface ItemWithBatches {
  itemStockId: number;
  itemName: string;
  totalQuantity: number;
  batches?: StockBatch[];
  expanded?: boolean;
}

@Component({
  selector: 'app-stock-batch-inventory',
  templateUrl: './stock-batch-inventory.component.html',
  styleUrls: ['./stock-batch-inventory.component.css']
})
export class StockBatchInventoryComponent implements OnInit {
  items: ItemWithBatches[] = [];
  isLoading = false;

  // Colores por estado
  statusColors = {
    'VIGENTE': '#4CAF50',
    'PROXIMO_A_EXPIRAR': '#FFC107',
    'EXPIRADO': '#F44336',
    'AGOTADO': '#9E9E9E'
  };

  constructor(private stockBatchService: StockBatchService) { }

  ngOnInit() {
    this.loadInventory();
  }

  loadInventory() {
    this.isLoading = true;
    // Aquí deberías cargar la lista de items desde tu servicio de inventario
    // Por ahora, es un placeholder
    this.isLoading = false;
  }

  toggleExpand(item: ItemWithBatches) {
    item.expanded = !item.expanded;
    
    if (item.expanded && !item.batches) {
      this.stockBatchService.getBatchesByItemStock(item.itemStockId).subscribe({
        next: (batches) => {
          item.batches = batches;
        },
        error: (err) => console.error(err)
      });
    }
  }

  getStatusColor(status: string): string {
    return this.statusColors[status] || '#999';
  }

  consumeBatch(itemStockId: number, quantity: number) {
    const confirmConsume = confirm(`¿Consumir ${quantity}kg?`);
    if (!confirmConsume) return;

    this.stockBatchService.decreaseQuantity(itemStockId, quantity).subscribe({
      next: () => {
        alert('✅ Consumo registrado');
        this.loadInventory();
      },
      error: (err) => alert('❌ Error al consumir')
    });
  }
}
```

### stock-batch-inventory.component.html

```html
<div class="inventory-container">
  <h2>📦 Inventario de Lotes</h2>

  <table class="inventory-table">
    <thead>
      <tr>
        <th>Item</th>
        <th>Stock Total</th>
        <th>Lotes</th>
        <th>Próximo a Vencer</th>
        <th>Acciones</th>
      </tr>
    </thead>
    <tbody>
      <tr *ngFor="let item of items" class="item-row">
        <td>{{ item.itemName }}</td>
        <td class="quantity">{{ item.totalQuantity }}kg</td>
        <td>
          <button class="btn-expand" (click)="toggleExpand(item)">
            {{ item.expanded ? '▼' : '▶' }} Ver lotes
          </button>
        </td>
        <td>-</td>
        <td>
          <button class="btn btn-sm btn-primary">Consumir</button>
        </td>
      </tr>

      <!-- Fila expandida con lotes -->
      <tr *ngIf="item.expanded" class="expand-row">
        <td colspan="5">
          <div class="batches-detail">
            <table class="batches-table">
              <thead>
                <tr>
                  <th>Lote</th>
                  <th>Cantidad</th>
                  <th>Proveedor</th>
                  <th>Vence</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let batch of item.batches" class="batch-row">
                  <td class="batch-number">{{ batch.batchNumber }}</td>
                  <td>{{ batch.quantity }}kg</td>
                  <td>{{ batch.supplierName }}</td>
                  <td>{{ batch.expiryDate | date:'dd/MMM/yyyy' }}</td>
                  <td>
                    <span class="status-badge" 
                          [style.background-color]="getStatusColor(batch.status)">
                      {{ batch.status }}
                    </span>
                  </td>
                  <td>
                    <button class="btn btn-xs btn-success" 
                            (click)="consumeBatch(item.itemStockId, batch.quantity)">
                      Consumir
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</div>
```

---

## 4. DASHBOARD PRINCIPAL - Ejemplo de Integración

```typescript
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  template: `
    <div class="dashboard">
      <h1>📊 Dashboard de Inventario</h1>
      
      <!-- Widget de Alertas -->
      <app-stock-batch-alerts></app-stock-batch-alerts>
      
      <!-- Tabla de Inventario -->
      <app-stock-batch-inventory></app-stock-batch-inventory>
    </div>
  `,
  styles: [`
    .dashboard {
      padding: 20px;
      background-color: #fafafa;
    }
    
    h1 {
      color: #333;
      margin-bottom: 30px;
    }
  `]
})
export class DashboardComponent implements OnInit {
  ngOnInit() {
    console.log('Dashboard cargado - Stock Batch implementado');
  }
}
```

---

## 5. IMPORTAR EN MÓDULO

```typescript
// app.module.ts
import { StockBatchService } from './services/stock-batch.service';
import { StockBatchAlertsComponent } from './components/stock-batch-alerts/stock-batch-alerts.component';
import { StockBatchInventoryComponent } from './components/stock-batch-inventory/stock-batch-inventory.component';

@NgModule({
  declarations: [
    StockBatchAlertsComponent,
    StockBatchInventoryComponent
  ],
  providers: [StockBatchService]
})
export class AppModule { }
```

---

## ✅ CHECKLIST DE IMPLEMENTACIÓN

- [ ] Copiar StockBatchService
- [ ] Copiar componentes de alertas
- [ ] Copiar componente de inventario
- [ ] Importar en módulo
- [ ] Actualizar estilos CSS
- [ ] Probar con servidor backend
- [ ] Integrar en dashboard principal

¡Código listo para copiar y pegar! 🚀

