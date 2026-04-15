# Guía de Integración - Mesas y Órdenes (Frontend)

## 🎯 Resumen General

Esta guía cubre la integración de:
1. **QR de Mesa** - Cliente escanea y obtiene sesión
2. **Gestión de Pedidos** - Cliente crea y consulta órdenes
3. **Panel de Mesero** - Mesero visualiza y maneja mesas
4. **Cierre de Pedido** - Pago y facturación

---

## 📱 Flujo para Cliente (Escanea QR)

### 1. Cliente abre la página (Escanea QR)

**Endpoint:**
```
GET /api/client/session?table={numeroMesa}
```

**Ejemplo:**
```bash
GET http://localhost:8081/api/client/session?table=5
```

**Respuesta:**
```json
{
  "sessionToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tableNumber": 5,
  "expiresIn": 14400,
  "message": "Sesión abierta para mesa 5"
}
```

**En el Frontend:**
- El servidor **automáticamente** establece la cookie `sessionToken` (HttpOnly)
- También recibe cookie `tableNumber`
- La cookie se envía **automáticamente** en cada request (no necesita localStorage)
- Válida por 4 horas

---

### 2. Cliente crea su primer pedido

**Endpoint:**
```
POST /api/client/orders
Authorization: Cookie sessionToken (automático)
Content-Type: application/json
```

**Request Body:**
```json
{
  "items": [
    {
      "id": 1,
      "quantity": 2,
      "notes": "Sin picante"
    },
    {
      "id": 3,
      "quantity": 1,
      "notes": ""
    }
  ],
  "paymentMethod": "CASH"
}
```

**Parámetros del Request:**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `items[].id` | Integer | ID del producto/item a pedir |
| `items[].quantity` | Integer | Cantidad pedida |
| `items[].notes` | String | Notas especiales (ej: "sin cebolla") |
| `paymentMethod` | String | `CASH`, `CARD`, `QR_PAYMENT` |

**Respuesta:**
```json
{
  "id": 42,
  "tableNumber": 5,
  "status": "PENDING",
  "items": [
    {
      "id": 1,
      "name": "Hamburguesa",
      "quantity": 2,
      "price": 12.99,
      "notes": "Sin picante"
    },
    {
      "id": 3,
      "name": "Gaseosa",
      "quantity": 1,
      "price": 2.50,
      "notes": ""
    }
  ],
  "subtotal": 30.47,
  "tax": 4.57,
  "total": 35.04,
  "createdAt": "2026-04-13T15:30:00",
  "orderType": "TABLE"
}
```

---

### 3. Cliente consulta sus órdenes

**Endpoint:**
```
GET /api/client/orders
Authorization: Cookie sessionToken (automático)
```

**Respuesta:**
```json
[
  {
    "id": 42,
    "tableNumber": 5,
    "status": "PENDING",
    "total": 35.04,
    "createdAt": "2026-04-13T15:30:00"
  },
  {
    "id": 43,
    "tableNumber": 5,
    "status": "IN_PREPARATION",
    "total": 18.50,
    "createdAt": "2026-04-13T15:45:00"
  }
]
```

---

### 4. Cliente consulta detalles de una orden

**Endpoint:**
```
GET /api/client/orders/{id}
Authorization: Cookie sessionToken (automático)
```

**Ejemplo:**
```bash
GET /api/client/orders/42
```

**Respuesta:**
```json
{
  "id": 42,
  "tableNumber": 5,
  "status": "IN_PREPARATION",
  "items": [
    {
      "id": 1,
      "name": "Hamburguesa",
      "quantity": 2,
      "price": 12.99,
      "notes": "Sin picante"
    }
  ],
  "subtotal": 30.47,
  "tax": 4.57,
  "total": 35.04,
  "createdAt": "2026-04-13T15:30:00",
  "estimatedTime": "15 minutos"
}
```

---

### 5. Cliente solicita ayuda al mesero

**Endpoint:**
```
POST /api/client/orders/{id}/request-help
Authorization: Cookie sessionToken (automático)
Content-Type: application/json
```

**Request Body:**
```json
{
  "message": "Mesero, necesito el cambio"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Solicitud enviada al mesero. Llegará en breve."
}
```

**En el Frontend:**
- Muestra notificación al mesero en tiempo real
- El cliente ve confirmación de la solicitud

---

### 6. Cliente confirma entrega del pedido

**Endpoint:**
```
PATCH /api/client/orders/{id}/confirm-delivery
Authorization: Cookie sessionToken (automático)
Content-Type: application/json
```

**Request Body:**
```json
{
  "rating": 5,
  "feedback": "Muy bueno, el servicio excelente"
}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Entrega confirmada. Gracias por su calificación."
}
```

---

### 7. Cliente paga y recibe factura

**Endpoint:**
```
POST /api/staff/orders/{id}/invoice
Authorization: Cookie sessionToken
Content-Type: application/json
```

**Request Body:**
```json
{
  "orderIds": [42, 43]
}
```

**Respuesta:**
```json
{
  "invoiceNumber": "INV-2026-0001",
  "date": "2026-04-13T16:00:00",
  "items": [
    {
      "description": "Hamburguesa x2",
      "price": 25.98
    },
    {
      "description": "Gaseosa x1",
      "price": 2.50
    }
  ],
  "subtotal": 48.97,
  "tax": 7.35,
  "total": 56.32,
  "pdfUrl": "https://api/invoices/INV-2026-0001.pdf"
}
```

---

## 👨‍💼 Flujo para Mesero (Staff)

### 1. Mesero se autentica (Login)

**Endpoint:**
```
POST /api/auth/login
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "mesero@restaurante.com",
  "password": "contraseña123"
}
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 28800,
  "user": {
    "id": 3,
    "name": "Juan Pérez",
    "email": "mesero@restaurante.com",
    "role": "WAITER"
  }
}
```

**En el Frontend:**
- Guardar `token` en localStorage o sessionStorage
- Enviar en header `Authorization: Bearer {token}` en cada request

---

### 2. Mesero carga todas las mesas

**Endpoint:**
```
GET /api/staff/tables
Authorization: Bearer {token}
```

**Respuesta:**
```json
[
  {
    "number": 1,
    "status": "OCCUPIED",
    "capacity": 4,
    "activeOrders": 2,
    "totalBill": 45.50
  },
  {
    "number": 2,
    "status": "AVAILABLE",
    "capacity": 2,
    "activeOrders": 0,
    "totalBill": 0.0
  },
  {
    "number": 3,
    "status": "OCCUPIED",
    "capacity": 4,
    "activeOrders": 1,
    "totalBill": 32.15
  },
  {
    "number": 5,
    "status": "OCCUPIED",
    "capacity": 6,
    "activeOrders": 2,
    "totalBill": 56.32
  }
]
```

**Colores sugeridos en Frontend:**
- `AVAILABLE` → Verde
- `OCCUPIED` → Amarillo/Naranja
- `RESERVED` → Azul
- `DIRTY` → Rojo

---

### 3. Mesero consulta órdenes de una mesa específica

**Endpoint:**
```
GET /api/staff/tables/{tableNumber}/orders
Authorization: Bearer {token}
```

**Ejemplo:**
```bash
GET /api/staff/tables/5/orders
```

**Respuesta:**
```json
[
  {
    "id": 42,
    "status": "PENDING",
    "items": [
      {
        "name": "Hamburguesa",
        "quantity": 2,
        "notes": "Sin picante"
      },
      {
        "name": "Gaseosa",
        "quantity": 1
      }
    ],
    "total": 35.04,
    "createdAt": "2026-04-13T15:30:00"
  },
  {
    "id": 43,
    "status": "IN_PREPARATION",
    "items": [...],
    "total": 18.50,
    "createdAt": "2026-04-13T15:45:00"
  }
]
```

---

### 4. Mesero actualiza estado de una orden

**Endpoint:**
```
PATCH /api/staff/orders/{id}/status
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "status": "IN_PREPARATION"
}
```

**Estados permitidos:**
- `PENDING` → `IN_PREPARATION`
- `IN_PREPARATION` → `SERVED`
- `SERVED` → `PAID`

**Respuesta:**
```json
{
  "success": true,
  "orderId": 42,
  "newStatus": "IN_PREPARATION",
  "message": "Orden actualizada a IN_PREPARATION"
}
```

---

### 5. Mesero genera factura de una mesa

**Endpoint:**
```
GET /api/staff/orders/{id}/invoice
Authorization: Bearer {token}
```

**Respuesta:**
```json
{
  "invoiceNumber": "INV-2026-0042",
  "tableNumber": 5,
  "date": "2026-04-13T16:15:00",
  "items": [
    {
      "orderId": 42,
      "description": "Hamburguesa x2",
      "price": 25.98
    },
    {
      "orderId": 42,
      "description": "Gaseosa x1",
      "price": 2.50
    },
    {
      "orderId": 43,
      "description": "Pizza x1",
      "price": 15.00
    }
  ],
  "subtotal": 43.48,
  "tax": 6.52,
  "total": 50.00,
  "pdfUrl": "https://api/invoices/INV-2026-0042.pdf"
}
```

---

### 6. Mesero confirma pago

**Endpoint:**
```
POST /api/staff/orders/{id}/confirm-payment
Authorization: Bearer {token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "paymentMethod": "CARD",
  "amount": 50.00,
  "tip": 5.00
}
```

**Respuesta:**
```json
{
  "success": true,
  "orderId": 42,
  "status": "PAID",
  "totalPaid": 55.00,
  "message": "Pago confirmado"
}
```

---

### 7. Mesero cierra sesión de mesa

**Endpoint:**
```
DELETE /api/staff/tables/{tableNumber}/session
Authorization: Bearer {token}
```

**Respuesta:**
```json
{
  "success": true,
  "message": "Sesión de mesa 5 cerrada",
  "tableNumber": 5
}
```

---

### 8. Mesero verifica si mesa está activa

**Endpoint:**
```
GET /api/staff/tables/{tableNumber}/session/status
Authorization: Bearer {token}
```

**Respuesta:**
```json
{
  "isActive": true,
  "tableNumber": 5,
  "activeSince": "2026-04-13T15:20:00"
}
```

---

## 🔐 Autenticación y Cookies

### Cliente (Sesión de Mesa)

La **cookie `sessionToken` se envía automáticamente** en cada request (HttpOnly):

```javascript
// NO necesita hacer nada especial
// El navegador envía automáticamente la cookie

fetch('/api/client/orders', {
  method: 'GET',
  credentials: 'include' // Importante: incluir cookies
})
```

### Mesero (JWT Token)

Guardar el token y enviar en cada request:

```javascript
// Después de login, guardar el token
const token = response.data.token;
localStorage.setItem('token', token);

// En cada request, enviar en header
fetch('/api/staff/tables', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
```

---

## 📊 Flujo Completo Ejemplo

### Escenario: Cliente ordena y paga

```
1. Cliente escanea QR
   GET /api/client/session?table=5
   ↓ Recibe sessionToken en cookie

2. Cliente abre menú
   GET /api/menu/products
   ↓ Consulta productos disponibles

3. Cliente crea orden
   POST /api/client/orders
   Body: { items: [...], paymentMethod: "CASH" }
   ↓ Orden creada, ID 42

4. Mesero ve la orden
   GET /api/staff/tables/5/orders
   ↓ Ve orden pendiente

5. Mesero marca como en preparación
   PATCH /api/staff/orders/42/status
   Body: { status: "IN_PREPARATION" }

6. Mesero lleva el plato
   PATCH /api/staff/orders/42/status
   Body: { status: "SERVED" }

7. Cliente confirma entrega
   PATCH /api/client/orders/42/confirm-delivery
   Body: { rating: 5 }

8. Mesero genera factura
   GET /api/staff/orders/42/invoice
   ↓ Recibe PDF

9. Mesero confirma pago
   POST /api/staff/orders/42/confirm-payment
   Body: { paymentMethod: "CASH", amount: 35.04 }

10. Mesero cierra mesa
    DELETE /api/staff/tables/5/session
    ↓ Mesa lista para siguiente cliente
```

---

## 🎨 Componentes Frontend Sugeridos

### Para Cliente:
1. **QR Scanner** - Lee código QR
2. **Menu View** - Muestra productos
3. **Cart/Carrito** - Agrega items
4. **Order List** - Listado de órdenes
5. **Order Details** - Detalles de orden
6. **Payment** - Métodos de pago
7. **Rating** - Calificación

### Para Mesero:
1. **Login Form** - Autenticación
2. **Tables Grid** - Vista de mesas
3. **Table Details** - Órdenes de mesa
4. **Order Status Updater** - Cambiar estado
5. **Invoice Generator** - Generar factura
6. **Payment Confirmation** - Confirmar pago

---

## ⚠️ Notas Importantes

1. **Cookie vs Token:**
   - Cliente: Cookie automática (no manipular manualmente)
   - Mesero: Token en localStorage (sí manipular)

2. **Validaciones:**
   - Cliente: SessionToken valida que pertenece a la mesa
   - Mesero: JWT token valida autenticación y roles

3. **Estados de Orden:**
   - `PENDING` → `IN_PREPARATION` → `SERVED` → `PAID`
   - Cada transición valida el estado anterior

4. **Seguridad:**
   - SessionToken: HttpOnly, Secure (HTTPS)
   - JWT Token: Guardar en localStorage seguro
   - CORS configurado para origen del frontend

5. **Errores Comunes:**
   - Olvidar `credentials: 'include'` en fetch
   - Olvidar `Authorization: Bearer` en header
   - Intentar transiciones de estado inválidas

---

## 📞 Soporte

Para más detalles sobre un endpoint específico, consultar el código del controlador:
- `OrderController.java` - Lógica de órdenes
- `TableSessionController.java` - Sesiones de mesa
- `StaffTableController.java` - Operaciones de staff


