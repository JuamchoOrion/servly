# 📱 Guía de Integración Frontend - Servly Sistema de Mesas

## 🎯 Descripción General

Este documento describe todos los endpoints necesarios para integrar el frontend con el sistema de gestión de mesas y órdenes de Servly.

### Estados de Órdenes (Máquina de Estados)
```
PENDING → IN_PREPARATION → SERVED → PAID
         └────────────────────────────────┘
              (Cancelable en cualquier momento)
```

---

## 🔐 Autenticación

### Session Token (Cliente - Mesa)
- **Cómo obtenerlo**: GET `/api/client/session?table={numero}`
- **Tipo**: Cookie HttpOnly (automática)
- **Duración**: 4 horas
- **Uso**: Se envía automáticamente en todas las peticiones del cliente

### Admin/Staff Token (JWT Bearer)
- **Cómo obtenerlo**: Endpoint de login
- **Tipo**: Bearer Token en header `Authorization: Bearer {token}`
- **Duración**: 24 horas
- **Roles**: ADMIN, STAFF, CASHIER

---

## 📋 ENDPOINTS CLIENTE (Con sessionToken en Cookie)

### 1. Abrir Sesión de Mesa (Escanear QR)
```http
GET /api/client/session?table=10

Response (200):
{
  "message": "Sesión iniciada para mesa 10",
  "tableNumber": 10,
  "sessionToken": "...",
  "expiresAt": "2026-04-03T22:16:06"
}
```

### 2. Ver Menú (Productos Activos)
```http
GET /api/menu/products?page=0&size=10

Response (200):
{
  "content": [
    {
      "id": 1,
      "name": "Hamburguesa",
      "description": "Hamburguesa clásica",
      "price": 15.99,
      "category": "Platos",
      "imageUrl": "...",
      "active": true
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

### 3. Ver Detalle de Producto
```http
GET /api/products/1

Response (200):
{
  "id": 1,
  "name": "Hamburguesa",
  "price": 15.99,
  "description": "Hamburguesa clásica con queso y lechuga",
  "category": "Platos",
  "imageUrl": "...",
  "active": true,
  "stock": 50
}
```

### 4. Crear Orden
```http
POST /api/client/orders
Content-Type: application/json

{
  "products": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ]
}

Response (201):
{
  "id": 5,
  "tableNumber": 10,
  "status": "PENDING",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "item_name": "Hamburguesa",
      "quantity": 2,
      "unit_price": 15.99,
      "subtotal": 31.98
    }
  ],
  "total": 31.98,
  "created_at": "2026-04-03T18:22:00"
}
```

### 5. Ver Mis Órdenes
```http
GET /api/client/orders

Response (200):
[
  {
    "id": 5,
    "tableNumber": 10,
    "status": "PENDING",
    "total": 31.98,
    "created_at": "2026-04-03T18:22:00"
  },
  {
    "id": 6,
    "tableNumber": 10,
    "status": "IN_PREPARATION",
    "total": 45.50,
    "created_at": "2026-04-03T18:25:00"
  }
]
```

### 6. Ver Detalle de Orden
```http
GET /api/client/orders/5

Response (200):
{
  "id": 5,
  "tableNumber": 10,
  "status": "SERVED",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "item_name": "Hamburguesa",
      "quantity": 2,
      "unit_price": 15.99,
      "subtotal": 31.98,
      "tax_percent": 0.08
    }
  ],
  "total": 31.98,
  "created_at": "2026-04-03T18:22:00",
  "order_type": "TABLE"
}
```

### 7. Solicitar Ayuda (Llamar al Mesero)
```http
POST /api/client/orders/5/request-help

Response (200):
{
  "message": "Ayuda solicitada",
  "tableNumber": 10,
  "orderId": 5
}
```

### 8. Confirmar Entrega
```http
GET /api/client/orders/5/confirm-delivery

Response (200):
{
  "message": "Entrega confirmada",
  "orderId": 5,
  "status": "SERVED"
}
```

---

## 👨‍💼 ENDPOINTS ADMIN/STAFF (Con Authorization: Bearer {token})

### 1. Ver Órdenes Pendientes
```http
GET /api/staff/orders/pending
Authorization: Bearer {adminToken}

Response (200):
[
  {
    "id": 5,
    "tableNumber": 10,
    "status": "PENDING",
    "total": 31.98,
    "created_at": "2026-04-03T18:22:00",
    "orderType": "TABLE"
  }
]
```

### 2. Ver Órdenes en Preparación
```http
GET /api/staff/orders/in-preparation
Authorization: Bearer {adminToken}

Response (200):
[
  {
    "id": 5,
    "tableNumber": 10,
    "status": "IN_PREPARATION",
    "total": 31.98
  }
]
```

### 3. Ver Órdenes de una Mesa
```http
GET /api/staff/tables/10/orders
Authorization: Bearer {adminToken}

Response (200):
[
  {
    "id": 5,
    "tableNumber": 10,
    "status": "PENDING",
    "total": 31.98
  },
  {
    "id": 6,
    "tableNumber": 10,
    "status": "SERVED",
    "total": 45.50
  }
]
```

### 4. Cambiar Estado de Orden
```http
POST /api/staff/orders/5/status
Authorization: Bearer {adminToken}
Content-Type: application/json

{
  "status": "IN_PREPARATION"
}

Response (200):
{
  "id": 5,
  "status": "IN_PREPARATION",
  "message": "Estado actualizado correctamente"
}
```

**Transiciones válidas**:
- PENDING → IN_PREPARATION
- IN_PREPARATION → SERVED
- SERVED → PAID
- Cualquier estado → CANCELLED

### 5. Generar Factura
```http
GET /api/staff/orders/5/invoice
Authorization: Bearer {adminToken}

Response (200):
{
  "orderId": 5,
  "tableNumber": 10,
  "items": [
    {
      "name": "Hamburguesa",
      "quantity": 2,
      "unitPrice": 15.99,
      "subtotal": 31.98
    }
  ],
  "subtotal": 31.98,
  "tax": 2.56,
  "total": 34.54,
  "createdAt": "2026-04-03T18:22:00"
}
```

### 6. Confirmar Pago (Cambia a PAID automáticamente)
```http
POST /api/staff/orders/5/confirm-payment
Authorization: Bearer {adminToken}

Response (200):
{
  "message": "Pago confirmado. Estado: PAID"
}
```

⚠️ **IMPORTANTE**: Este endpoint:
- Valida que la orden esté en estado SERVED
- Cambia automáticamente el estado a PAID
- NO se puede ejecutar dos veces (validación de estado)

### 7. Cancelar Orden
```http
POST /api/staff/orders/5/cancel
Authorization: Bearer {adminToken}

Response (200):
{
  "message": "Orden cancelada",
  "orderId": 5,
  "status": "CANCELLED"
}
```

---

## 🔄 FLUJOS COMPLETOS

### Flujo Cliente (Desde Mesa)

#### Paso 1: Escanear QR (Abrir Mesa)
```javascript
const response = await fetch('http://localhost:8081/api/client/session?table=10', {
  credentials: 'include'
});
```

#### Paso 2: Ver Menú
```javascript
const menu = await fetch('http://localhost:8081/api/menu/products?page=0&size=10', {
  credentials: 'include'
});
```

#### Paso 3: Crear Orden
```javascript
const order = await fetch('http://localhost:8081/api/client/orders', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    products: [
      { productId: 1, quantity: 2 },
      { productId: 2, quantity: 1 }
    ]
  })
});
```

#### Paso 4: Monitorear Estado
```javascript
// Polling cada 5 segundos
setInterval(async () => {
  const order = await fetch('http://localhost:8081/api/client/orders/5', {
    credentials: 'include'
  });
  const data = await order.json();
  
  console.log('Estado:', data.status);
  // PENDING → IN_PREPARATION → SERVED → PAID
}, 5000);
```

#### Paso 5: Confirmar Entrega (cuando llega la comida)
```javascript
await fetch('http://localhost:8081/api/client/orders/5/confirm-delivery', {
  credentials: 'include'
});
```

#### Paso 6: Ver Total a Pagar
```javascript
const invoice = await fetch('http://localhost:8081/api/staff/orders/5/invoice', {
  credentials: 'include' // Para obtener detalles
});
```

---

### Flujo Admin/Staff (Cocina y Caja)

#### Paso 1: Ver Órdenes Pendientes
```javascript
const pending = await fetch('http://localhost:8081/api/staff/orders/pending', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

#### Paso 2: Marcar como En Preparación
```javascript
await fetch('http://localhost:8081/api/staff/orders/5/status', {
  method: 'POST',
  headers: { 
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ status: 'IN_PREPARATION' })
});
```

#### Paso 3: Marcar como Servida (Comida lista)
```javascript
await fetch('http://localhost:8081/api/staff/orders/5/status', {
  method: 'POST',
  headers: { 
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ status: 'SERVED' })
});
```

#### Paso 4: Generar Factura
```javascript
const invoice = await fetch('http://localhost:8081/api/staff/orders/5/invoice', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

#### Paso 5: Confirmar Pago (Cambia a PAID)
```javascript
await fetch('http://localhost:8081/api/staff/orders/5/confirm-payment', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` }
});
// Estado cambia automáticamente a PAID
```

---

## 📊 Estados de Orden

| Estado | Descripción | Quién lo establece | Siguiente estado |
|--------|-------------|-------------------|------------------|
| **PENDING** | Orden creada, esperando en cocina | Cliente | IN_PREPARATION |
| **IN_PREPARATION** | Cocina preparando | Staff/Cocina | SERVED |
| **SERVED** | Comida entregada, esperando pago | Staff/Mesero | PAID |
| **PAID** | Orden pagada, completada | Admin/Caja (confirm-payment) | ✓ Fin |
| **CANCELLED** | Orden cancelada | Admin/Staff | ✓ Fin |

---

## 🛡️ Permisos y Roles

### ROLE_CLIENT (sessionToken de mesa)
- ✅ Ver menú
- ✅ Crear orden
- ✅ Ver mis órdenes
- ✅ Solicitar ayuda
- ✅ Confirmar entrega
- ❌ Cambiar estado
- ❌ Ver órdenes de otros
- ❌ Confirmar pago

### ROLE_ADMIN
- ✅ Ver todas las órdenes
- ✅ Cambiar estado
- ✅ Confirmar pago
- ✅ Generar factura
- ✅ Cancelar orden

### ROLE_STAFF / ROLE_CASHIER
- ✅ Ver órdenes por mesa
- ✅ Ver órdenes por estado
- ✅ Cambiar estado
- ✅ Generar factura
- ✅ Confirmar pago
- ✅ Cancelar orden

---

## ⚠️ Errores Comunes

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "No tienes permiso para acceder a este recurso"
}
```
**Solución**: Verifica que el token sea válido y esté en el header correcto.

### 403 Forbidden
```json
{
  "error": "Access Denied",
  "message": "Tu rol no tiene permiso para esta acción"
}
```
**Solución**: Verifica que el usuario tenga el rol correcto (ADMIN, STAFF, etc).

### 404 Not Found
```json
{
  "error": "NotFoundException",
  "message": "Orden no encontrada: 999"
}
```
**Solución**: Verifica que el ID de la orden sea correcto.

### 422 Invalid State Transition
```json
{
  "error": "ValidationException",
  "message": "No se puede cambiar de PENDING a PAID"
}
```
**Solución**: Solo se permiten estas transiciones:
- PENDING → IN_PREPARATION
- IN_PREPARATION → SERVED
- SERVED → PAID

---

## 🧪 Ejemplo de Integración Completa (JavaScript)

```javascript
class ServlyClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl;
    this.token = null;
  }

  // CLIENTE: Abrir sesión
  async abrirMesa(numeroMesa) {
    const response = await fetch(
      `${this.baseUrl}/api/client/session?table=${numeroMesa}`,
      { credentials: 'include' }
    );
    return response.json();
  }

  // CLIENTE: Ver menú
  async verMenu(page = 0, size = 10) {
    const response = await fetch(
      `${this.baseUrl}/api/menu/products?page=${page}&size=${size}`,
      { credentials: 'include' }
    );
    return response.json();
  }

  // CLIENTE: Crear orden
  async crearOrden(productos) {
    const response = await fetch(`${this.baseUrl}/api/client/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ products: productos })
    });
    return response.json();
  }

  // CLIENTE: Ver mis órdenes
  async verMisOrdenes() {
    const response = await fetch(`${this.baseUrl}/api/client/orders`, {
      credentials: 'include'
    });
    return response.json();
  }

  // ADMIN: Confirmar pago
  async confirmarPago(orderId) {
    const response = await fetch(
      `${this.baseUrl}/api/staff/orders/${orderId}/confirm-payment`,
      {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${this.token}` }
      }
    );
    return response.json();
  }

  // ADMIN: Cambiar estado
  async cambiarEstado(orderId, nuevoEstado) {
    const response = await fetch(
      `${this.baseUrl}/api/staff/orders/${orderId}/status`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ status: nuevoEstado })
      }
    );
    return response.json();
  }
}

// Uso
const servly = new ServlyClient('http://localhost:8081');

// Cliente
await servly.abrirMesa(10);
const menu = await servly.verMenu();
const orden = await servly.crearOrden([
  { productId: 1, quantity: 2 }
]);

// Admin
servly.token = 'admin_token_aqui';
await servly.confirmarPago(orden.id); // Cambia a PAID automáticamente
```

---

## 📞 Soporte

Para preguntas sobre la integración, consulta:
- Los logs del servidor: `server.log`
- El archivo de pruebas: `test-mesas-pago-completo.http`
- La documentación técnica: `CAMBIOS_ESTADO_PAID.md`

