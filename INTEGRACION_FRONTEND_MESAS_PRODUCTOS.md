# Guía de Integración Frontend - Gestión de Mesas y Productos

## 📋 Índice
1. [Autenticación y Tokens](#autenticación-y-tokens)
2. [Gestión de Sesiones de Mesa](#gestión-de-sesiones-de-mesa)
3. [Endpoints de Productos/Menú](#endpoints-de-productosmenú)
4. [Endpoints de Órdenes (Cliente)](#endpoints-de-órdenes-cliente)
5. [Endpoints de Órdenes (Staff/Admin)](#endpoints-de-órdenes-staffadmin)
6. [Flujos de Negocio Completos](#flujos-de-negocio-completos)
7. [Códigos de Error](#códigos-de-error)

---

## 🔐 Autenticación y Tokens

### Tipos de Token

#### 1. **sessionToken (Sesión de Mesa)**
- **Tipo**: Cookie (HttpOnly)
- **Duración**: 4 horas
- **Roles**: ROLE_CLIENT
- **Se obtiene**: Al escanear QR o hacer GET a `/api/client/session`
- **Uso**: Automáticamente enviado en cookies para endpoints de cliente

```javascript
// NO se envía manualmente, está en la cookie
// El navegador lo envía automáticamente
fetch('http://localhost:8081/api/client/orders', {
  method: 'GET',
  credentials: 'include' // Importante para enviar cookies
})
```

#### 2. **adminToken (JWT Bearer)**
- **Tipo**: Bearer Token en header
- **Duración**: 24 horas
- **Roles**: ROLE_ADMIN, ROLE_STAFF
- **Se obtiene**: Login en la aplicación admin
- **Uso**: Header `Authorization: Bearer {token}`

```javascript
// Se envía en el header
fetch('http://localhost:8081/api/staff/orders/pending', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer eyJhbGciOiJIUzUxMiJ9...'
  }
})
```

---

## 🪟 Gestión de Sesiones de Mesa

### 1. Abrir Sesión de Mesa (Escanear QR)

**Endpoint**: `GET /api/client/session?table={numero_mesa}`

**Autenticación**: No requerida

**Parámetros**:
- `table` (query): Número de mesa (ej: 10)

**Respuesta (200 OK)**:
```json
{
  "message": "Sesión iniciada para mesa 10",
  "tableNumber": 10,
  "sessionToken": "abc123xyz...",
  "expiresAt": "2026-04-03T22:16:06"
}
```

**Cookie establecida automáticamente**:
```
Set-Cookie: sessionToken=abc123xyz...; Path=/; HttpOnly; SameSite=Lax
```

**Ejemplo Frontend**:
```javascript
async function abrirSesionMesa(numeroMesa) {
  try {
    const response = await fetch(
      `http://localhost:8081/api/client/session?table=${numeroMesa}`,
      {
        method: 'GET',
        credentials: 'include' // Guardar cookie
      }
    );
    
    const data = await response.json();
    console.log('Sesión iniciada:', data);
    // La cookie se guarda automáticamente
    // No necesitas hacer nada más
    return data;
  } catch (error) {
    console.error('Error al abrir sesión:', error);
  }
}

// Uso
abrirSesionMesa(10);
```

---

## 📦 Endpoints de Productos/Menú

### 1. Obtener Todos los Productos Activos (Sin Paginación)

**Endpoint**: `GET /api/products/active`

**Autenticación**: No requerida

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Jugo Naranja Premium",
      "price": 5.99,
      "description": "Jugo natural de naranja",
      "category": "BEBIDA",
      "image": "jugo-naranja.jpg",
      "active": true
    },
    {
      "id": 2,
      "name": "Arepa de Queso",
      "price": 3.50,
      "description": "Arepa rellena de queso",
      "category": "ENTRADA",
      "image": "arepa-queso.jpg",
      "active": true
    }
  ]
}
```

**Ejemplo Frontend**:
```javascript
async function obtenerProductosActivos() {
  try {
    const response = await fetch(
      'http://localhost:8081/api/products/active'
    );
    const data = await response.json();
    console.log('Productos:', data.data);
    return data.data;
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### 2. Obtener Productos Activos con Paginación (Menú)

**Endpoint**: `GET /api/menu/products?page={pagina}&size={cantidad}`

**Autenticación**: No requerida

**Parámetros**:
- `page` (query, default=0): Número de página (comienza en 0)
- `size` (query, default=10): Cantidad de resultados por página

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Jugo Naranja Premium",
        "price": 5.99,
        "description": "Jugo natural de naranja",
        "category": "BEBIDA",
        "image": "jugo-naranja.jpg",
        "active": true
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalPages": 1,
      "totalElements": 1
    }
  }
}
```

**Ejemplo Frontend**:
```javascript
async function obtenerProductosConPaginacion(pagina = 0, cantidad = 10) {
  try {
    const response = await fetch(
      `http://localhost:8081/api/menu/products?page=${pagina}&size=${cantidad}`
    );
    const data = await response.json();
    console.log('Productos:', data.data.content);
    console.log('Total páginas:', data.data.pageable.totalPages);
    return data.data;
  } catch (error) {
    console.error('Error:', error);
  }
}

// Uso
obtenerProductosConPaginacion(0, 10); // Primera página con 10 items
```

### 3. Obtener Detalles de un Producto

**Endpoint**: `GET /api/products/{productId}`

**Autenticación**: No requerida (pero requiere sesión de mesa)

**Parámetros**:
- `productId` (path): ID del producto

**Respuesta (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "Jugo Naranja Premium",
    "price": 5.99,
    "description": "Jugo natural de naranja recién exprimido",
    "category": "BEBIDA",
    "image": "jugo-naranja.jpg",
    "active": true,
    "recipe": {
      "id": 1,
      "name": "Jugo de Naranja",
      "items": [
        {
          "id": 1,
          "name": "Naranja",
          "requiredQuantity": 3,
          "unit": "UNIDAD"
        }
      ]
    }
  }
}
```

**Errores**:
- **401**: Sin sesión de mesa válida
- **404**: Producto no encontrado

---

## 🛒 Endpoints de Órdenes (Cliente)

### 1. Crear una Orden

**Endpoint**: `POST /api/client/orders`

**Autenticación**: Requerida (sessionToken en cookie)

**Headers**:
```
Content-Type: application/json
```

**Body**:
```json
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
```

**Respuesta (201 Created)**:
```json
{
  "id": 5,
  "tableNumber": 10,
  "status": "PENDING",
  "total": 14.47,
  "items": [
    {
      "id": 8,
      "quantity": 2,
      "subtotal": 11.98,
      "item_id": 1,
      "item_name": "Jugo Naranja Premium",
      "unit_price": 5.99,
      "tax_percent": 0.08
    },
    {
      "id": 9,
      "quantity": 1,
      "subtotal": 3.50,
      "item_id": 2,
      "item_name": "Arepa de Queso",
      "unit_price": 3.50,
      "tax_percent": 0.08
    }
  ],
  "createdAt": "2026-04-03T19:50:00"
}
```

**Errores**:
- **400**: Producto no encontrado o sin stock
- **401**: Sin sesión válida
- **422**: Stock insuficiente

**Ejemplo Frontend**:
```javascript
async function crearOrden(productos) {
  try {
    const response = await fetch(
      'http://localhost:8081/api/client/orders',
      {
        method: 'POST',
        credentials: 'include', // Enviar cookie de sesión
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          products: productos
        })
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }

    const data = await response.json();
    console.log('Orden creada:', data);
    return data;
  } catch (error) {
    console.error('Error:', error);
  }
}

// Uso
crearOrden([
  { productId: 1, quantity: 2 },
  { productId: 2, quantity: 1 }
]);
```

### 2. Obtener Órdenes de la Mesa Actual

**Endpoint**: `GET /api/client/orders`

**Autenticación**: Requerida (sessionToken en cookie)

**Respuesta (200 OK)**:
```json
[
  {
    "id": 5,
    "tableNumber": 10,
    "status": "PENDING",
    "total": 14.47,
    "items": [...],
    "createdAt": "2026-04-03T19:50:00"
  },
  {
    "id": 6,
    "tableNumber": 10,
    "status": "IN_PREPARATION",
    "total": 7.49,
    "items": [...],
    "createdAt": "2026-04-03T19:55:00"
  }
]
```

**Ejemplo Frontend**:
```javascript
async function obtenerOrdenesActuales() {
  try {
    const response = await fetch(
      'http://localhost:8081/api/client/orders',
      {
        method: 'GET',
        credentials: 'include'
      }
    );

    const ordenes = await response.json();
    console.log('Órdenes:', ordenes);
    return ordenes;
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### 3. Obtener Detalle de una Orden

**Endpoint**: `GET /api/client/orders/{orderId}`

**Autenticación**: Requerida (sessionToken en cookie)

**Parámetros**:
- `orderId` (path): ID de la orden

**Respuesta (200 OK)**:
```json
{
  "id": 5,
  "tableNumber": 10,
  "status": "SERVED",
  "total": 14.47,
  "subtotal": 13.40,
  "tax": 1.07,
  "items": [
    {
      "id": 8,
      "quantity": 2,
      "subtotal": 11.98,
      "item_id": 1,
      "item_name": "Jugo Naranja Premium",
      "unit_price": 5.99,
      "tax_percent": 0.08
    }
  ],
  "createdAt": "2026-04-03T19:50:00"
}
```

**Ejemplo Frontend**:
```javascript
async function obtenerDetalleOrden(orderId) {
  try {
    const response = await fetch(
      `http://localhost:8081/api/client/orders/${orderId}`,
      {
        method: 'GET',
        credentials: 'include'
      }
    );

    const orden = await response.json();
    console.log('Orden:', orden);
    return orden;
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### 4. Solicitar Ayuda (Botón de Llamada del Mesero)

**Endpoint**: `POST /api/client/orders/{orderId}/request-help`

**Autenticación**: Requerida (sessionToken en cookie)

**Parámetros**:
- `orderId` (path): ID de la orden

**Body**: Sin cuerpo requerido

**Respuesta (200 OK)**:
```json
{
  "message": "Ayuda solicitada",
  "tableNumber": 10,
  "orderId": 5
}
```

**Ejemplo Frontend**:
```javascript
async function solicitarAyuda(orderId) {
  try {
    const response = await fetch(
      `http://localhost:8081/api/client/orders/${orderId}/request-help`,
      {
        method: 'POST',
        credentials: 'include'
      }
    );

    const resultado = await response.json();
    console.log('Mesero notificado:', resultado);
    return resultado;
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### 5. Confirmar Entrega (Cliente confirma que recibió)

**Endpoint**: `GET /api/client/orders/{orderId}/confirm-delivery`

**Autenticación**: Requerida (sessionToken en cookie)

**Parámetros**:
- `orderId` (path): ID de la orden

**Respuesta (200 OK)**:
```json
{
  "message": "Entrega confirmada",
  "orderId": 5,
  "status": "SERVED"
}
```

---

## 👨‍💼 Endpoints de Órdenes (Staff/Admin)

### ⚠️ Importante: Todos requieren Bearer Token

```javascript
const adminToken = 'eyJhbGciOiJIUzUxMiJ9...'; // Obtenido al login

const headers = {
  'Authorization': `Bearer ${adminToken}`,
  'Content-Type': 'application/json'
};
```

### 1. Obtener Órdenes Pendientes

**Endpoint**: `GET /api/staff/orders/pending`

**Autenticación**: Requerida (ROLE_ADMIN, ROLE_STAFF)

**Respuesta (200 OK)**:
```json
[
  {
    "id": 5,
    "tableNumber": 10,
    "status": "PENDING",
    "total": 14.47,
    "items": [
      {
        "item_id": 1,
        "item_name": "Jugo Naranja Premium",
        "quantity": 2
      }
    ],
    "createdAt": "2026-04-03T19:50:00"
  }
]
```

**Ejemplo Frontend**:
```javascript
async function obtenerOrdenesPendientes(adminToken) {
  try {
    const response = await fetch(
      'http://localhost:8081/api/staff/orders/pending',
      {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      }
    );

    const ordenes = await response.json();
    return ordenes;
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### 2. Obtener Órdenes en Preparación

**Endpoint**: `GET /api/staff/orders/in-preparation`

**Autenticación**: Requerida (ROLE_ADMIN, ROLE_STAFF)

**Respuesta**: Similar a órdenes pendientes, pero con status `IN_PREPARATION`

### 3. Cambiar Estado de una Orden

**Endpoint**: `POST /api/staff/orders/{orderId}/status`

**Autenticación**: Requerida (ROLE_ADMIN)

**Parámetros**:
- `orderId` (path): ID de la orden

**Body**:
```json
{
  "status": "IN_PREPARATION"
}
```

**Estados permitidos**:
- `PENDING` → `IN_PREPARATION`
- `IN_PREPARATION` → `SERVED`
- `SERVED` → `PAID`
- Cualquier estado → `CANCELLED`

**Respuesta (200 OK)**:
```json
{
  "id": 5,
  "status": "IN_PREPARATION",
  "message": "Estado actualizado a IN_PREPARATION"
}
```

**Ejemplo Frontend**:
```javascript
async function cambiarEstadoOrden(orderId, nuevoEstado, adminToken) {
  try {
    const response = await fetch(
      `http://localhost:8081/api/staff/orders/${orderId}/status`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${adminToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          status: nuevoEstado
        })
      }
    );

    const resultado = await response.json();
    console.log('Estado actualizado:', resultado);
    return resultado;
  } catch (error) {
    console.error('Error:', error);
  }
}

// Uso
cambiarEstadoOrden(5, 'IN_PREPARATION', adminToken);
```

### 4. Confirmar Pago de una Orden

**Endpoint**: `POST /api/staff/orders/{orderId}/confirm-payment`

**Autenticación**: Requerida (ROLE_ADMIN)

**Parámetros**:
- `orderId` (path): ID de la orden

**Body**: Sin cuerpo requerido

**Respuesta (200 OK)**:
```json
{
  "id": 5,
  "message": "Pago confirmado",
  "status": "PENDING",
  "total": 14.47
}
```

**Nota**: El estado cambia a `PENDING` (lista para cocina) después de confirmar el pago

**Ejemplo Frontend**:
```javascript
async function confirmarPago(orderId, adminToken) {
  try {
    const response = await fetch(
      `http://localhost:8081/api/staff/orders/${orderId}/confirm-payment`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      }
    );

    const resultado = await response.json();
    console.log('Pago confirmado:', resultado);
    return resultado;
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### 5. Obtener Órdenes de una Mesa Específica

**Endpoint**: `GET /api/staff/tables/{tableNumber}/orders`

**Autenticación**: Requerida (ROLE_ADMIN, ROLE_STAFF)

**Parámetros**:
- `tableNumber` (path): Número de la mesa

**Respuesta (200 OK)**:
```json
[
  {
    "id": 5,
    "tableNumber": 10,
    "status": "SERVED",
    "total": 14.47,
    "items": [...]
  },
  {
    "id": 6,
    "tableNumber": 10,
    "status": "PENDING",
    "total": 7.49,
    "items": [...]
  }
]
```

### 6. Obtener Factura de una Orden

**Endpoint**: `GET /api/staff/orders/{orderId}/invoice`

**Autenticación**: Requerida (ROLE_ADMIN)

**Respuesta (200 OK)**:
```json
{
  "orderId": 5,
  "tableNumber": 10,
  "items": [
    {
      "itemName": "Jugo Naranja Premium",
      "quantity": 2,
      "unitPrice": 5.99,
      "subtotal": 11.98,
      "taxPercent": 8
    }
  ],
  "subtotal": 13.40,
  "tax": 1.07,
  "total": 14.47,
  "createdAt": "2026-04-03T19:50:00"
}
```

### 7. Cancelar una Orden

**Endpoint**: `POST /api/staff/orders/{orderId}/cancel`

**Autenticación**: Requerida (ROLE_ADMIN)

**Body**: Sin cuerpo requerido

**Respuesta (200 OK)**:
```json
{
  "id": 5,
  "status": "CANCELLED",
  "message": "Orden cancelada"
}
```

---

## 🔄 Flujos de Negocio Completos

### Flujo 1: Cliente Escaneando QR y Ordenando

```
1. Cliente escanea QR
   ↓
   GET /api/client/session?table=10
   ← Cookie: sessionToken

2. Cliente ve el menú
   ↓
   GET /api/menu/products?page=0&size=10
   
3. Cliente selecciona productos y crea orden
   ↓
   POST /api/client/orders
   {
     "products": [
       { "productId": 1, "quantity": 2 }
     ]
   }
   ← { "id": 5, "status": "PENDING", ... }

4. Cliente espera su comida (consultando estado)
   ↓
   GET /api/client/orders/5
   ← { "status": "PENDING" }
   
   (Espera...)
   
   GET /api/client/orders/5
   ← { "status": "IN_PREPARATION" }
   
   (Más espera...)
   
   GET /api/client/orders/5
   ← { "status": "SERVED" }

5. Cliente llama al mesero si necesita (botón de ayuda)
   ↓
   POST /api/client/orders/5/request-help
   ← { "message": "Ayuda solicitada" }

6. Cliente confirma que recibió la comida
   ↓
   GET /api/client/orders/5/confirm-delivery
   ← { "status": "SERVED" }

7. Mesero procesa el pago
   ↓
   (Status se actualiza a PAID después de confirmación de pago)
   GET /api/client/orders/5
   ← { "status": "PAID" }
```

### Flujo 2: Admin Confirmando Pago y Cocina Preparando

```
1. Admin ve órdenes pendientes
   ↓
   GET /api/staff/orders/pending
   ← [ { "id": 5, "status": "PENDING" }, ... ]

2. Admin confirma pago del cliente
   ↓
   POST /api/staff/orders/5/confirm-payment
   ← { "status": "PENDING" }

3. Cocina ve órdenes para preparar
   ↓
   GET /api/staff/orders/pending
   ← [ { "id": 5, "tableNumber": 10, ... } ]

4. Cocina marca como "En Preparación"
   ↓
   POST /api/staff/orders/5/status
   {
     "status": "IN_PREPARATION"
   }
   ← { "status": "IN_PREPARATION" }

5. Admin ve órdenes en preparación
   ↓
   GET /api/staff/orders/in-preparation
   ← [ { "id": 5, "status": "IN_PREPARATION" } ]

6. Cocina termina y marca como "Servida"
   ↓
   POST /api/staff/orders/5/status
   {
     "status": "SERVED"
   }
   ← { "status": "SERVED" }

7. Admin genera factura cuando cliente va a pagar
   ↓
   GET /api/staff/orders/5/invoice
   ← { "total": 14.47, "items": [...] }

8. Admin confirma pago (marca como PAID)
   ↓
   POST /api/staff/orders/5/status
   {
     "status": "PAID"
   }
   ← { "status": "PAID" }
```

---

## 🔐 Manejo de Cookies vs Bearer Tokens

### Para Endpoints de Cliente (sessionToken)

```javascript
// ✅ CORRECTO - credentials: 'include' envía automáticamente la cookie
fetch('http://localhost:8081/api/client/orders', {
  method: 'GET',
  credentials: 'include'
})

// ❌ INCORRECTO - La cookie no se envía sin credentials
fetch('http://localhost:8081/api/client/orders', {
  method: 'GET'
})

// ❌ INCORRECTO - No puedes enviar manualmente una cookie
fetch('http://localhost:8081/api/client/orders', {
  method: 'GET',
  headers: {
    'Cookie': 'sessionToken=...' // No funciona así
  }
})
```

### Para Endpoints de Staff/Admin (adminToken)

```javascript
const adminToken = 'eyJhbGciOiJIUzUxMiJ9...';

// ✅ CORRECTO - Header Authorization
fetch('http://localhost:8081/api/staff/orders/pending', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${adminToken}`
  }
})

// ❌ INCORRECTO - Sin Bearer
fetch('http://localhost:8081/api/staff/orders/pending', {
  method: 'GET',
  headers: {
    'Authorization': adminToken // Falta "Bearer "
  }
})

// ❌ INCORRECTO - No uses cookies
fetch('http://localhost:8081/api/staff/orders/pending', {
  method: 'GET',
  credentials: 'include' // No es necesario ni recomendado
})
```

---

## ⚠️ Códigos de Error

### 400 Bad Request
```json
{
  "message": "No hay {nombreProducto} suficientes en inventario"
}
```
**Causa**: Stock insuficiente del producto

### 401 Unauthorized
```json
{
  "message": "Full authentication is required to access this resource"
}
```
**Causa**: 
- Sin sesión de mesa válida (endpoints de cliente)
- Sin token Bearer válido (endpoints de staff)
- Token expirado

### 403 Forbidden
```json
{
  "message": "Access Denied"
}
```
**Causa**: Token válido pero rol insuficiente (ej: ROLE_STAFF intentando operación ROLE_ADMIN)

### 404 Not Found
```json
{
  "message": "Orden no encontrada: 999"
}
```
**Causa**: El recurso no existe

### 422 Unprocessable Entity
```json
{
  "message": "No hay {nombreProducto} suficientes en inventario"
}
```
**Causa**: Validación de negocio fallida

### 500 Internal Server Error
```json
{
  "error": "Error inesperado en {endpoint}",
  "message": "Descripción del error"
}
```

---

## 📱 Ejemplo Completo en React

```javascript
// src/hooks/useRestaurant.js

import { useState, useCallback } from 'react';

const API_BASE = 'http://localhost:8081';

export function useRestaurant() {
  const [adminToken, setAdminToken] = useState(null);
  const [currentTable, setCurrentTable] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);

  // Cliente: Abrir sesión de mesa
  const openTableSession = useCallback(async (tableNumber) => {
    setLoading(true);
    try {
      const response = await fetch(
        `${API_BASE}/api/client/session?table=${tableNumber}`,
        { method: 'GET', credentials: 'include' }
      );
      const data = await response.json();
      setCurrentTable(tableNumber);
      return data;
    } catch (error) {
      console.error('Error:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  // Cliente: Obtener menú
  const getMenu = useCallback(async (page = 0, size = 10) => {
    setLoading(true);
    try {
      const response = await fetch(
        `${API_BASE}/api/menu/products?page=${page}&size=${size}`
      );
      const data = await response.json();
      return data.data;
    } catch (error) {
      console.error('Error:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  // Cliente: Crear orden
  const createOrder = useCallback(async (products) => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/client/orders`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ products })
      });
      
      if (!response.ok) {
        throw new Error('Error al crear orden');
      }
      
      const order = await response.json();
      setOrders([...orders, order]);
      return order;
    } catch (error) {
      console.error('Error:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [orders]);

  // Cliente: Obtener órdenes
  const getClientOrders = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/api/client/orders`, {
        method: 'GET',
        credentials: 'include'
      });
      const data = await response.json();
      setOrders(data);
      return data;
    } catch (error) {
      console.error('Error:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, []);

  // Admin: Confirmar pago
  const confirmPayment = useCallback(async (orderId) => {
    if (!adminToken) throw new Error('Token no disponible');
    
    setLoading(true);
    try {
      const response = await fetch(
        `${API_BASE}/api/staff/orders/${orderId}/confirm-payment`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${adminToken}`
          }
        }
      );
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [adminToken]);

  // Admin: Cambiar estado
  const updateOrderStatus = useCallback(async (orderId, status) => {
    if (!adminToken) throw new Error('Token no disponible');
    
    setLoading(true);
    try {
      const response = await fetch(
        `${API_BASE}/api/staff/orders/${orderId}/status`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${adminToken}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ status })
        }
      );
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error:', error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [adminToken]);

  return {
    adminToken,
    setAdminToken,
    currentTable,
    orders,
    loading,
    openTableSession,
    getMenu,
    createOrder,
    getClientOrders,
    confirmPayment,
    updateOrderStatus
  };
}
```

---

## 🎯 Resumen de Permisos

| Endpoint | Autenticación | Rol Requerido | Descripción |
|----------|---------------|---------------|-------------|
| GET `/api/client/session` | No | - | Abrir sesión de mesa |
| GET `/api/menu/products` | No | - | Listar productos |
| GET `/api/products/active` | No | - | Listar todos productos activos |
| POST `/api/client/orders` | Cookie | CLIENT | Crear orden |
| GET `/api/client/orders` | Cookie | CLIENT | Listar órdenes |
| GET `/api/client/orders/{id}` | Cookie | CLIENT | Ver detalles orden |
| POST `/api/client/orders/{id}/request-help` | Cookie | CLIENT | Solicitar ayuda |
| GET `/api/client/orders/{id}/confirm-delivery` | Cookie | CLIENT | Confirmar entrega |
| GET `/api/staff/orders/pending` | Bearer | STAFF/ADMIN | Listar pendientes |
| GET `/api/staff/orders/in-preparation` | Bearer | STAFF/ADMIN | Listar en preparación |
| POST `/api/staff/orders/{id}/status` | Bearer | ADMIN | Cambiar estado |
| POST `/api/staff/orders/{id}/confirm-payment` | Bearer | ADMIN | Confirmar pago |
| GET `/api/staff/orders/{id}/invoice` | Bearer | ADMIN | Generar factura |
| POST `/api/staff/orders/{id}/cancel` | Bearer | ADMIN | Cancelar orden |
| GET `/api/staff/tables/{id}/orders` | Bearer | STAFF/ADMIN | Órdenes de mesa |


