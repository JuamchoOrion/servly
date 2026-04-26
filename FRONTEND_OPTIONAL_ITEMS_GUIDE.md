# Guía para Frontend: Mostrar Items Opcionales en Órdenes

## 📱 Cómo el Frontend Debe Trabajar con Items Opcionales

### 1. Parsear el JSON

```typescript
interface OptionalItem {
  itemId: number;
  itemName: string;
  quantity: number;
}

// Parsear la respuesta del backend
const order = await getOrder(orderId);
const detail = order.items[0];

// optional_items viene como string JSON
const optionalItems: OptionalItem[] = detail.optional_items 
  ? JSON.parse(detail.optional_items) 
  : [];

console.log(optionalItems);
// [
//   { itemId: 5, itemName: "Queso extra", quantity: 2 },
//   { itemId: 6, itemName: "Salsa especial", quantity: 1 }
// ]
```

### 2. Mostrar en la UI

#### Vista de Cliente (Menu)
```
┌─────────────────────────────────────────┐
│ 🍔 Hamburguesa Clásica x2               │
│                                         │
│ Anotaciones: Sin cebolla en una         │
│                                         │
│ 📝 Extras agregados:                    │
│   • 2x Queso extra                      │
│   • 1x Bacon                            │
│                                         │
│ Subtotal: $50,000                       │
└─────────────────────────────────────────┘
```

#### Vista de Cocina (Pedidos)
```
┌─────────────────────────────────────────┐
│ [PENDING] Orden #1 - Mesa 3             │
│                                         │
│ 🍔 Hamburguesa x2                       │
│    └─ Sin cebolla en una                │
│    └─ +Queso extra x2                   │
│    └─ +Bacon x1                         │
│                                         │
│ [SERVIR] [CANCELAR]                     │
└─────────────────────────────────────────┘
```

### 3. Código React Ejemplo

```tsx
interface OrderDetail {
  id: number;
  item_name: string;
  quantity: number;
  annotations?: string;
  optional_items?: string;
  unit_price: number;
}

const OrderItemCard: React.FC<{ item: OrderDetail }> = ({ item }) => {
  // Parsear items opcionales
  const optionalItems = item.optional_items 
    ? JSON.parse(item.optional_items)
    : [];

  return (
    <div className="order-item">
      <div className="header">
        <h3>{item.item_name} x{item.quantity}</h3>
        <span className="price">${item.unit_price * item.quantity}</span>
      </div>

      {item.annotations && (
        <div className="annotations">
          📝 {item.annotations}
        </div>
      )}

      {optionalItems.length > 0 && (
        <div className="optional-items">
          <p className="label">Extras:</p>
          <ul>
            {optionalItems.map((opt) => (
              <li key={opt.itemId}>
                {opt.quantity}x {opt.itemName}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};
```

### 4. Validación de Estructura

```typescript
// Validar que el JSON es correcto antes de parsear
function parseOptionalItems(jsonString: string | null): OptionalItem[] {
  if (!jsonString) return [];
  
  try {
    const items = JSON.parse(jsonString);
    
    // Validar estructura
    if (!Array.isArray(items)) return [];
    
    return items.filter(item => 
      item.itemId && 
      item.itemName && 
      typeof item.quantity === 'number'
    );
  } catch (error) {
    console.error('Error parsing optional items:', error);
    return [];
  }
}
```

### 5. Enviar Items Opcionales (Crear Orden)

```typescript
// Client envía items opcionales
interface CreateOrderRequest {
  tableNumber: number;
  products: ProductOrder[];
}

interface ProductOrder {
  productId: number;
  quantity: number;
  annotations?: string;
  itemQuantityOverrides?: { [itemId: number]: number };
}

// Ejemplo: Cliente selecciona extras
const createOrder = async () => {
  const request: CreateOrderRequest = {
    tableNumber: 3,
    products: [
      {
        productId: 10,
        quantity: 2,
        annotations: "Sin cebolla en una",
        itemQuantityOverrides: {
          5: 2,   // 2x Queso extra
          8: 1    // 1x Bacon
        }
      }
    ]
  };

  const response = await fetch('/api/table-orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  });

  const order = await response.json();
  console.log(order.items[0].optional_items);
  // "[{\"itemId\":5,\"itemName\":\"Queso extra\",\"quantity\":2},...]"
};
```

### 6. CSS para Mostrar Items Opcionales

```css
.optional-items {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eee;
  font-size: 0.9em;
  color: #666;
}

.optional-items .label {
  font-weight: bold;
  margin: 0 0 4px 0;
  font-size: 0.8em;
}

.optional-items ul {
  list-style: none;
  padding-left: 16px;
  margin: 0;
}

.optional-items li {
  padding: 2px 0;
}

.optional-items li:before {
  content: "+ ";
  color: #4CAF50;
  font-weight: bold;
  margin-right: 4px;
}
```

## 📌 Puntos Importantes

1. **Siempre parsear con try-catch**
   - El JSON puede venir corrupto
   - Validar estructura antes de usar

2. **Manejar null/undefined**
   - Las órdenes sin extras tendrán `optional_items: null`
   - Usar fallback a array vacío

3. **Mostrar claramente**
   - Diferencia visual entre item base y extras
   - Usar iconos o colores para destacar

4. **Persistencia**
   - Los datos se guardan en BD
   - Próximas consultas tendrán los mismos extras

## 🧪 Ejemplo de Flujo Completo

```
1. Cliente en menú:
   - Selecciona Hamburguesa x2
   - Marca "Queso extra x2"
   - Marca "Bacon x1"
   - Escribe anotación "Sin cebolla en una"
   - Envía orden

2. Backend recibe:
   itemQuantityOverrides: {5: 2, 8: 1}
   annotations: "Sin cebolla en una"

3. Backend procesa:
   optionalItems: "[{itemId:5,itemName:Queso,qty:2},{itemId:8,itemName:Bacon,qty:1}]"
   Guarda en BD

4. Frontend muestra en orden activa:
   🍔 Hamburguesa x2
   📝 Sin cebolla en una
   ✏️ Extras:
     + 2x Queso extra
     + 1x Bacon

5. Mesero ve en cocina:
   [PENDING] Orden #1 - Mesa 3
   🍔 Hamburguesa x2
      └─ +Queso extra x2
      └─ +Bacon x1
```

