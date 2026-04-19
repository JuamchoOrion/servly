# Ejemplos de Uso - Control de Mesas (Frontend)

## 1. Abrir Sesión de Mesa

### Caso de Éxito (Mesa Disponible)

**Request:**
```http
GET http://localhost:8081/api/client/session?table=5
```

**Response (200 OK):**
```json
{
  "sessionToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "tableNumber": 5,
  "sessionId": 42,
  "expiresAt": "2026-04-20T12:30:45",
  "tokenType": "Bearer"
}
```

**En el Frontend:**
```javascript
// Guardar el token en sessionStorage
sessionStorage.setItem('sessionToken', response.sessionToken);
sessionStorage.setItem('tableNumber', response.tableNumber);

// Usar en requests posteriores
const headers = {
  'Authorization': `Bearer ${sessionStorage.getItem('sessionToken')}`
};
```

---

### Caso de Error (Mesa Ocupada)

**Request:**
```http
GET http://localhost:8081/api/client/session?table=5
```

**Response (409 Conflict):**
```json
{
  "message": "La mesa número 5 no está disponible (estado: OCCUPIED). No se puede abrir una sesión."
}
```

**En el Frontend:**
```javascript
async function openTableSession(tableNumber) {
  try {
    const response = await fetch(`/api/client/session?table=${tableNumber}`);
    
    if (response.status === 409) {
      // Mesa ocupada
      const error = await response.json();
      alert(`Error: ${error.message}`);
      console.error('Mesa no disponible');
      return null;
    }
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const session = await response.json();
    sessionStorage.setItem('sessionToken', session.sessionToken);
    sessionStorage.setItem('tableNumber', session.tableNumber);
    
    return session;
  } catch (error) {
    console.error('Error al abrir sesión:', error);
    return null;
  }
}
```

---

## 2. Realizar Órdenes

### Crear Orden de Mesa

**Request:**
```http
POST http://localhost:8081/api/orders/table
Authorization: Bearer <sessionToken>
Content-Type: application/json

{
  "tableNumber": 5,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "itemQuantityOverrides": {}
    },
    {
      "productId": 3,
      "quantity": 1,
      "itemQuantityOverrides": {
        "4": 3  // Item 4 agregado 3 veces en lugar de 2
      }
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": 123,
  "date": "2026-04-19",
  "total": 156.50,
  "orderType": "TABLE",
  "status": "PENDING",
  "tableNumber": 5,
  "orderDetails": [
    {
      "id": 456,
      "itemId": 1,
      "itemName": "Hamburguesa",
      "quantity": 2,
      "unitPrice": 25.00,
      "subtotal": 50.00
    },
    {
      "id": 457,
      "itemId": 3,
      "itemName": "Pasta Carbonara",
      "quantity": 1,
      "unitPrice": 14.99,
      "subtotal": 14.99
    }
  ]
}
```

---

## 3. Obtener Órdenes de Mesa

### Obtener Órdenes Activas

**Request:**
```http
GET http://localhost:8081/api/client/orders
Authorization: Bearer <sessionToken>
```

**Response (200 OK):**
```json
[
  {
    "id": 123,
    "date": "2026-04-19",
    "total": 156.50,
    "orderType": "TABLE",
    "status": "PENDING",
    "tableNumber": 5,
    "orderDetails": [...]
  },
  {
    "id": 124,
    "date": "2026-04-19",
    "total": 45.99,
    "orderType": "TABLE",
    "status": "SERVED",
    "tableNumber": 5,
    "orderDetails": [...]
  }
]
```

---

## 4. Pagar Orden (Libera la Mesa)

### Pagar Orden

**Request:**
```http
PATCH http://localhost:8081/api/orders/123/status
Authorization: Bearer <adminToken>
Content-Type: application/json

{
  "status": "PAID"
}
```

**Response (200 OK):**
```json
{
  "id": 123,
  "date": "2026-04-19",
  "total": 156.50,
  "orderType": "TABLE",
  "status": "PAID",
  "tableNumber": 5,
  "orderDetails": [...]
}
```

**Lo que pasa automáticamente:**
- ✅ Orden cambia a PAID
- ✅ Mesa #5 cambia a AVAILABLE automáticamente
- ✅ Nueva sesión se puede abrir en mesa #5

**En el Frontend:**
```javascript
async function payOrder(orderId) {
  try {
    const response = await fetch(`/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${sessionStorage.getItem('adminToken')}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ status: 'PAID' })
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const order = await response.json();
    
    // Mostrar confirmación
    alert(`Orden #${order.id} pagada exitosamente. Mesa ${order.tableNumber} disponible.`);
    
    // Limpiar sesión de cliente
    sessionStorage.removeItem('sessionToken');
    sessionStorage.removeItem('tableNumber');
    
    return order;
  } catch (error) {
    console.error('Error al pagar orden:', error);
  }
}
```

---

## 5. Cierre de Sesión Manual (Camarero)

### Cerrar Sesión de Mesa

**Request:**
```http
DELETE http://localhost:8081/api/staff/tables/5/session
Authorization: Bearer <staffToken>
```

**Response (200 OK):**
```json
{
  "message": "Sesión de la mesa 5 cerrada correctamente"
}
```

**En el Frontend (Panel de Camarero):**
```javascript
async function closeTableSession(tableNumber) {
  try {
    const response = await fetch(`/api/staff/tables/${tableNumber}/session`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${sessionStorage.getItem('staffToken')}`
      }
    });
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    const message = await response.json();
    console.log(message.message); // "Sesión de la mesa 5 cerrada correctamente"
    alert(`Mesa ${tableNumber} cerrada y disponible para nuevos clientes.`);
    
    return true;
  } catch (error) {
    console.error('Error al cerrar sesión:', error);
  }
}
```

---

## 6. Flujo Completo en Frontend

### Aplicación Cliente (Vue/React)
```javascript
class TableClient {
  constructor(tableNumber) {
    this.tableNumber = tableNumber;
    this.sessionToken = null;
    this.orders = [];
  }

  // 1️⃣ Cliente escanea QR
  async joinTable() {
    const session = await this.openSession();
    if (!session) return false;
    
    this.sessionToken = session.sessionToken;
    console.log(`✅ Sesión abierta en mesa ${this.tableNumber}`);
    return true;
  }

  // 2️⃣ Realizar orden
  async createOrder(items) {
    const response = await fetch('/api/orders/table', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.sessionToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        tableNumber: this.tableNumber,
        items: items
      })
    });
    
    const order = await response.json();
    this.orders.push(order);
    console.log(`✅ Orden #${order.id} creada`);
    return order;
  }

  // 3️⃣ Ver órdenes actuales
  async checkOrders() {
    const response = await fetch('/api/client/orders', {
      headers: {
        'Authorization': `Bearer ${this.sessionToken}`
      }
    });
    
    this.orders = await response.json();
    console.log(`📋 Órdenes actuales:`, this.orders);
    return this.orders;
  }

  // 4️⃣ Solicitar ayuda
  async requestHelp() {
    const response = await fetch(`/api/orders/${this.orders[0].id}/help`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.sessionToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        description: "Necesito ayuda"
      })
    });
    
    console.log(`🆘 Ayuda solicitada`);
  }

  // 5️⃣ Cuando camarero paga (no es responsabilidad del cliente)
  // El camarero/cajero hace: PATCH /api/orders/{id}/status con PAID
  // Automáticamente la mesa se libera
}

// Uso
const client = new TableClient(5);
await client.joinTable();
await client.createOrder([{ productId: 1, quantity: 2 }]);
await client.checkOrders();
await client.requestHelp();
// ... camarero paga ...
// Mesa se libera automáticamente ✨
```

---

## 7. Panel de Control (Administrador/Camarero)

```javascript
class TableManagement {
  // Ver estado de todas las mesas
  async getTableStatus() {
    const response = await fetch('/api/admin/tables', {
      headers: {
        'Authorization': `Bearer ${adminToken}`
      }
    });
    return await response.json();
  }

  // Ver órdenes de mesa específica
  async getTableOrders(tableNumber) {
    const response = await fetch(`/api/orders/table/${tableNumber}`, {
      headers: {
        'Authorization': `Bearer ${adminToken}`
      }
    });
    return await response.json();
  }

  // Pagar orden (libera mesa automáticamente)
  async payOrder(orderId) {
    const response = await fetch(`/api/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${adminToken}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ status: 'PAID' })
    });
    return await response.json();
  }

  // Cerrar sesión (alternativa si hay problema)
  async forceCloseTable(tableNumber) {
    const response = await fetch(`/api/staff/tables/${tableNumber}/session`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${staffToken}`
      }
    });
    return await response.json();
  }
}
```

---

## Resumen de Estados y Transiciones

### Estados de Orden
```
PENDING → IN_PREPARATION → SERVED → PAID
   ↓           ↓             ↓        ↓
   └───────────CANCELLED────────────→
```

### Estados de Mesa
```
AVAILABLE → OCCUPIED → AVAILABLE
   ↑          ↓           ↑
   └──(Sesión abierta)──┘
      └──(Orden pagada)──┘
      └──(Cierre manual)──┘
```

---

## Códigos de Error

| HTTP | Descripción | Ejemplo |
|------|-------------|---------|
| 200 | Éxito | Orden creada, sesión abierta |
| 201 | Creado | Nueva orden creada |
| 400 | Bad Request | Validación fallida |
| 401 | Unauthorized | Token inválido |
| 404 | Not Found | Orden/Mesa no existe |
| 409 | Conflict | **Mesa ocupada** ← Nuevo |
| 500 | Error Servidor | Error interno |


