# 📦 GUÍA COMPLETA: APIs de PRODUCTOS - Admin Controller

## 🎯 Introducción

Este documento contiene **TODOS** los endpoints relacionados con **PRODUCTOS** disponibles en el Admin Controller, con ejemplos de solicitudes y respuestas para integración con el frontend.

---

## 🔐 Autenticación

### Token JWT
Todos los endpoints requieren un token JWT en el header:

```bash
Authorization: Bearer {tu_token_aqui}
```

### Roles Requeridos
- **ADMIN**: Para crear, actualizar y eliminar productos
- **STAFF**: Solo lectura de productos
- **ADMIN**: Acceso completo a todas las operaciones

---

## 📝 CRUD PRODUCTOS - Operaciones Completas

### 1️⃣ CREAR PRODUCTO
**Endpoint**: `POST /api/admin/products`

**Permisos**: ADMIN

**Request Headers**:
```json
{
  "Authorization": "Bearer {token}",
  "Content-Type": "application/json"
}
```

**Request Body**:
```json
{
  "name": "Hamburguesa Clásica",
  "description": "Hamburguesa de carne con queso, tomate y lechuga",
  "price": 25000,
  "categoryId": 1,
  "recipeId": 5,
  "active": true
}
```

**Response (201 Created)**:
```json
{
  "id": 42,
  "name": "Hamburguesa Clásica",
  "description": "Hamburguesa de carne con queso, tomate y lechuga",
  "price": 25000,
  "category": {
    "id": 1,
    "name": "Comidas Principales",
    "description": "Platos principales y fuertes"
  },
  "recipe": {
    "id": 5,
    "name": "Receta Hamburguesa",
    "description": "Proceso de preparación"
  },
  "active": true,
  "createdAt": "2026-04-13T14:30:00",
  "updatedAt": "2026-04-13T14:30:00"
}
```

**Ejemplo con JavaScript/Fetch**:
```javascript
const crearProducto = async (token, productoData) => {
  const response = await fetch('http://localhost:8081/api/admin/products', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      name: productoData.name,
      description: productoData.description,
      price: productoData.price,
      categoryId: productoData.categoryId,
      recipeId: productoData.recipeId,
      active: true
    })
  });
  
  if (!response.ok) {
    throw new Error(`Error: ${response.statusText}`);
  }
  
  return await response.json();
};

// Uso:
const nuevoProducto = await crearProducto(token, {
  name: "Pizza Margarita",
  description: "Pizza con mozzarella y tomate",
  price: 30000,
  categoryId: 1,
  recipeId: 3
});
console.log('Producto creado:', nuevoProducto);
```

---

### 2️⃣ OBTENER TODOS LOS PRODUCTOS
**Endpoint**: `GET /api/admin/products`

**Permisos**: ADMIN, STAFF (lectura)

**Query Parameters (paginación)**:
- `page`: Número de página (0-indexed) - default: 0
- `size`: Cantidad de elementos por página - default: 10
- `sort`: Campo para ordenar (ej: `name`, `price`) - default: id

**Request (sin parámetros)**:
```bash
GET /api/admin/products
Authorization: Bearer {token}
```

**Request (con paginación)**:
```bash
GET /api/admin/products?page=0&size=5&sort=name,desc
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Café Americano",
      "description": "Café negro simple",
      "price": 5000,
      "category": {
        "id": 3,
        "name": "Bebidas"
      },
      "active": true,
      "createdAt": "2026-04-01T10:00:00"
    },
    {
      "id": 2,
      "name": "Café con Leche",
      "description": "Café con leche espumada",
      "price": 6500,
      "category": {
        "id": 3,
        "name": "Bebidas"
      },
      "active": true,
      "createdAt": "2026-04-01T10:05:00"
    }
  ],
  "pageable": {
    "size": 10,
    "number": 0,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

**Ejemplo con JavaScript/Fetch**:
```javascript
const obtenerProductos = async (token, pagina = 0, tamaño = 10) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/products?page=${pagina}&size=${tamaño}&sort=name,asc`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    }
  );
  
  if (!response.ok) {
    throw new Error(`Error: ${response.statusText}`);
  }
  
  return await response.json();
};

// Uso:
const productos = await obtenerProductos(token, 0, 10);
console.log(`Total de productos: ${productos.pageable.totalElements}`);
productos.content.forEach(p => console.log(p.name, p.price));
```

---

### 3️⃣ OBTENER PRODUCTOS ACTIVOS
**Endpoint**: `GET /api/admin/products/active`

**Permisos**: ADMIN, STAFF (lectura)

**Descripción**: Retorna solo productos con estado `active = true`

**Request**:
```bash
GET /api/admin/products/active?page=0&size=10
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Café Americano",
      "description": "Café negro simple",
      "price": 5000,
      "active": true,
      "createdAt": "2026-04-01T10:00:00"
    },
    {
      "id": 2,
      "name": "Café con Leche",
      "description": "Café con leche espumada",
      "price": 6500,
      "active": true,
      "createdAt": "2026-04-01T10:05:00"
    }
  ],
  "pageable": {
    "size": 10,
    "number": 0,
    "totalElements": 15,
    "totalPages": 2
  }
}
```

**Ejemplo con JavaScript/Fetch**:
```javascript
const obtenerProductosActivos = async (token) => {
  const response = await fetch('http://localhost:8081/api/admin/products/active?size=50', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  return await response.json();
};

// Uso:
const activos = await obtenerProductosActivos(token);
const listaDePrecios = activos.content.map(p => ({
  nombre: p.name,
  precio: p.price
}));
console.table(listaDePrecios);
```

---

### 4️⃣ OBTENER PRODUCTO POR ID
**Endpoint**: `GET /api/admin/products/{id}`

**Permisos**: ADMIN, STAFF (lectura)

**Path Parameters**:
- `id`: ID del producto (Long)

**Request**:
```bash
GET /api/admin/products/42
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "id": 42,
  "name": "Hamburguesa Clásica",
  "description": "Hamburguesa de carne con queso, tomate y lechuga",
  "price": 25000,
  "category": {
    "id": 1,
    "name": "Comidas Principales",
    "description": "Platos principales del menú"
  },
  "recipe": {
    "id": 5,
    "name": "Receta Hamburguesa Clásica",
    "description": "Procedimiento paso a paso"
  },
  "active": true,
  "createdAt": "2026-04-13T14:30:00",
  "updatedAt": "2026-04-13T14:35:00"
}
```

**Response (404 Not Found)**:
```json
{
  "timestamp": "2026-04-13T15:00:00",
  "status": 404,
  "error": "Producto no encontrado",
  "message": "El producto con ID 999 no existe"
}
```

**Ejemplo con JavaScript/Fetch**:
```javascript
const obtenerProductoPorId = async (token, productId) => {
  try {
    const response = await fetch(`http://localhost:8081/api/admin/products/${productId}`, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      if (response.status === 404) {
        throw new Error('Producto no encontrado');
      }
      throw new Error(`Error: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('Error al obtener producto:', error.message);
    throw error;
  }
};

// Uso:
const producto = await obtenerProductoPorId(token, 42);
console.log(`${producto.name}: $${producto.price}`);
```

---

### 5️⃣ ACTUALIZAR PRODUCTO
**Endpoint**: `PUT /api/admin/products/{id}`

**Permisos**: ADMIN (solo admin puede actualizar)

**Path Parameters**:
- `id`: ID del producto (Long)

**Query Parameters** (todos opcionales):
- `name`: Nuevo nombre del producto
- `description`: Nueva descripción
- `price`: Nuevo precio (BigDecimal)
- `active`: Estado (true/false)

**Request**:
```bash
PUT /api/admin/products/42?name=Hamburguesa Premium&price=35000
Authorization: Bearer {token}
```

**O con parámetros en body** (mejor práctica):
```bash
PUT /api/admin/products/42
Authorization: Bearer {token}
Content-Type: application/x-www-form-urlencoded

name=Hamburguesa Premium&price=35000&description=Hamburguesa con ingredientes premium&active=true
```

**Response (200 OK)**:
```json
{
  "id": 42,
  "name": "Hamburguesa Premium",
  "description": "Hamburguesa con ingredientes premium",
  "price": 35000,
  "category": {
    "id": 1,
    "name": "Comidas Principales"
  },
  "active": true,
  "createdAt": "2026-04-13T14:30:00",
  "updatedAt": "2026-04-13T16:00:00"
}
```

**Ejemplo con JavaScript/Fetch**:
```javascript
const actualizarProducto = async (token, productId, cambios) => {
  const params = new URLSearchParams();
  
  if (cambios.name) params.append('name', cambios.name);
  if (cambios.description) params.append('description', cambios.description);
  if (cambios.price) params.append('price', cambios.price);
  if (cambios.active !== undefined) params.append('active', cambios.active);
  
  const response = await fetch(
    `http://localhost:8081/api/admin/products/${productId}?${params.toString()}`,
    {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    }
  );
  
  if (!response.ok) {
    throw new Error(`Error: ${response.statusText}`);
  }
  
  return await response.json();
};

// Uso:
const actualizado = await actualizarProducto(token, 42, {
  name: "Hamburguesa Deluxe",
  price: 38000
});
console.log('Producto actualizado:', actualizado);
```

---

### 6️⃣ ACTIVAR PRODUCTO
**Endpoint**: `PATCH /api/admin/products/{id}/activate`

**Permisos**: ADMIN

**Path Parameters**:
- `id`: ID del producto (Long)

**Request**:
```bash
PATCH /api/admin/products/42/activate
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "id": 42,
  "name": "Hamburguesa Clásica",
  "description": "Hamburguesa de carne con queso, tomate y lechuga",
  "price": 25000,
  "active": true,
  "updatedAt": "2026-04-13T16:10:00"
}
```

**Ejemplo con JavaScript/Fetch**:
```javascript
const activarProducto = async (token, productId) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/products/${productId}/activate`,
    {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  if (!response.ok) {
    throw new Error(`Error: ${response.statusText}`);
  }
  
  return await response.json();
};

// Uso:
const productoActivado = await activarProducto(token, 42);
console.log(`Producto ${productoActivado.name} activado`);
```

---

### 7️⃣ DESACTIVAR PRODUCTO
**Endpoint**: `PATCH /api/admin/products/{id}/deactivate`

**Permisos**: ADMIN

**Path Parameters**:
- `id`: ID del producto (Long)

**Request**:
```bash
PATCH /api/admin/products/42/deactivate
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "id": 42,
  "name": "Hamburguesa Clásica",
  "description": "Hamburguesa de carne con queso, tomate y lechuga",
  "price": 25000,
  "active": false,
  "updatedAt": "2026-04-13T16:15:00"
}
```

**Ejemplo con JavaScript/Fetch**:
```javascript
const desactivarProducto = async (token, productId) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/products/${productId}/deactivate`,
    {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  return await response.json();
};

// Uso:
const productoDesactivado = await desactivarProducto(token, 42);
console.log(`Producto ${productoDesactivado.name} desactivado`);
```

---

### 8️⃣ ELIMINAR PRODUCTO
**Endpoint**: `DELETE /api/admin/products/{id}`

**Permisos**: ADMIN

**Path Parameters**:
- `id`: ID del producto (Long)

**Nota**: Es un borrado **LÓGICO** (soft delete), no elimina la fila de la base de datos

**Request**:
```bash
DELETE /api/admin/products/42
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "message": "Producto eliminado correctamente"
}
```

**Ejemplo con JavaScript/Fetch**:
```javascript
const eliminarProducto = async (token, productId) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/products/${productId}`,
    {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  if (!response.ok) {
    throw new Error(`Error: ${response.statusText}`);
  }
  
  return await response.json();
};

// Uso:
await eliminarProducto(token, 42);
console.log('Producto eliminado');
```

---

## 📂 CRUD CATEGORÍAS DE PRODUCTOS

### 1️⃣ CREAR CATEGORÍA
**Endpoint**: `POST /api/admin/product-categories`

**Permisos**: ADMIN

**Request Body**:
```json
{
  "name": "Bebidas Calientes",
  "description": "Café, té y otras bebidas calientes"
}
```

**Response (201 Created)**:
```json
{
  "id": 10,
  "name": "Bebidas Calientes",
  "description": "Café, té y otras bebidas calientes",
  "active": true,
  "createdAt": "2026-04-13T14:30:00"
}
```

**Ejemplo con JavaScript**:
```javascript
const crearCategoria = async (token, nombre, descripcion) => {
  const response = await fetch('http://localhost:8081/api/admin/product-categories', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      name: nombre,
      description: descripcion
    })
  });
  
  return await response.json();
};

// Uso:
const categoria = await crearCategoria(token, "Postres", "Pasteles y postres varios");
console.log('Categoría creada:', categoria);
```

---

### 2️⃣ OBTENER TODAS LAS CATEGORÍAS
**Endpoint**: `GET /api/admin/product-categories`

**Permisos**: ADMIN, STAFF (lectura)

**Request**:
```bash
GET /api/admin/product-categories
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "name": "Comidas Principales",
    "description": "Platos principales del menú",
    "active": true,
    "createdAt": "2026-04-01T10:00:00"
  },
  {
    "id": 3,
    "name": "Bebidas",
    "description": "Bebidas variadas",
    "active": true,
    "createdAt": "2026-04-01T10:10:00"
  },
  {
    "id": 5,
    "name": "Postres",
    "description": "Postres y dulces",
    "active": true,
    "createdAt": "2026-04-13T14:30:00"
  }
]
```

**Ejemplo con JavaScript**:
```javascript
const obtenerCategorias = async (token) => {
  const response = await fetch('http://localhost:8081/api/admin/product-categories', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  return await response.json();
};

// Uso:
const categorias = await obtenerCategorias(token);
categorias.forEach(c => console.log(`${c.name}: ${c.description}`));
```

---

### 3️⃣ OBTENER CATEGORÍAS ACTIVAS
**Endpoint**: `GET /api/admin/product-categories/active`

**Permisos**: ADMIN, STAFF (lectura)

**Request**:
```bash
GET /api/admin/product-categories/active
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
[
  {
    "id": 1,
    "name": "Comidas Principales",
    "description": "Platos principales del menú",
    "active": true
  },
  {
    "id": 3,
    "name": "Bebidas",
    "description": "Bebidas variadas",
    "active": true
  }
]
```

---

### 4️⃣ OBTENER CATEGORÍA POR ID
**Endpoint**: `GET /api/admin/product-categories/{id}`

**Permisos**: ADMIN, STAFF (lectura)

**Request**:
```bash
GET /api/admin/product-categories/1
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "id": 1,
  "name": "Comidas Principales",
  "description": "Platos principales del menú",
  "active": true,
  "createdAt": "2026-04-01T10:00:00"
}
```

---

### 5️⃣ ACTUALIZAR CATEGORÍA
**Endpoint**: `PUT /api/admin/product-categories/{id}`

**Permisos**: ADMIN

**Request Body**:
```json
{
  "name": "Comidas Principales Actualizadas",
  "description": "Nuevos platos principales del menú"
}
```

**Response (200 OK)**:
```json
{
  "id": 1,
  "name": "Comidas Principales Actualizadas",
  "description": "Nuevos platos principales del menú",
  "active": true,
  "updatedAt": "2026-04-13T16:00:00"
}
```

**Ejemplo con JavaScript**:
```javascript
const actualizarCategoria = async (token, categoriaId, nombre, descripcion) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/product-categories/${categoriaId}`,
    {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        name: nombre,
        description: descripcion
      })
    }
  );
  
  return await response.json();
};

// Uso:
const actualizada = await actualizarCategoria(token, 1, "Comidas Nuevas", "Descripción nueva");
console.log('Categoría actualizada:', actualizada);
```

---

### 6️⃣ ELIMINAR CATEGORÍA
**Endpoint**: `DELETE /api/admin/product-categories/{id}`

**Permisos**: ADMIN

**Request**:
```bash
DELETE /api/admin/product-categories/5
Authorization: Bearer {token}
```

**Response (200 OK)**:
```json
{
  "message": "Categoría de producto eliminada correctamente"
}
```

**Ejemplo con JavaScript**:
```javascript
const eliminarCategoria = async (token, categoriaId) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/product-categories/${categoriaId}`,
    {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  return await response.json();
};

// Uso:
await eliminarCategoria(token, 5);
console.log('Categoría eliminada');
```

---

## 🧪 EJEMPLO COMPLETO: Flujo de Creación de Menú

```javascript
// Clase auxiliar para manejar productos
class ProductManager {
  constructor(token) {
    this.token = token;
    this.baseUrl = 'http://localhost:8081/api/admin';
  }
  
  // Crear categoría
  async crearCategoria(nombre, descripcion) {
    const res = await fetch(`${this.baseUrl}/product-categories`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ name: nombre, description: descripcion })
    });
    return await res.json();
  }
  
  // Crear producto
  async crearProducto(nombre, descripcion, precio, categoryId, recipeId) {
    const res = await fetch(`${this.baseUrl}/products`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        name: nombre,
        description: descripcion,
        price: precio,
        categoryId: categoryId,
        recipeId: recipeId,
        active: true
      })
    });
    return await res.json();
  }
  
  // Obtener todos los productos
  async obtenerProductos(pagina = 0) {
    const res = await fetch(
      `${this.baseUrl}/products?page=${pagina}&size=10`,
      { headers: { 'Authorization': `Bearer ${this.token}` } }
    );
    return await res.json();
  }
  
  // Obtener categorías
  async obtenerCategorias() {
    const res = await fetch(
      `${this.baseUrl}/product-categories`,
      { headers: { 'Authorization': `Bearer ${this.token}` } }
    );
    return await res.json();
  }
  
  // Activar producto
  async activarProducto(productId) {
    const res = await fetch(
      `${this.baseUrl}/products/${productId}/activate`,
      {
        method: 'PATCH',
        headers: { 'Authorization': `Bearer ${this.token}` }
      }
    );
    return await res.json();
  }
}

// Ejemplo de uso:
const manager = new ProductManager('tu_token_aqui');

// 1. Crear categoría de bebidas
const bebidas = await manager.crearCategoria(
  'Bebidas',
  'Todas nuestras bebidas frías y calientes'
);
console.log('Categoría creada:', bebidas.id);

// 2. Crear producto
const cafe = await manager.crearProducto(
  'Café Americano',
  'Café negro simple, fuerte',
  5000,
  bebidas.id,
  1
);
console.log('Producto creado:', cafe.id);

// 3. Obtener todos los productos
const todosLosProductos = await manager.obtenerProductos();
console.log(`Total de productos: ${todosLosProductos.pageable.totalElements}`);

// 4. Listar categorías
const categorias = await manager.obtenerCategorias();
categorias.forEach(c => {
  console.log(`Categoría: ${c.name} (${c.description})`);
});
```

---

## ⚠️ CÓDIGOS DE ERROR COMUNES

### 400 - Bad Request
```json
{
  "timestamp": "2026-04-13T15:00:00",
  "status": 400,
  "error": "Validación fallida",
  "message": "El precio debe ser mayor a 0"
}
```

**Causas**:
- Parámetros inválidos
- Precio negativo o cero
- Categoría o receta no existen

### 401 - Unauthorized
```json
{
  "error": "Acceso denegado",
  "message": "Token inválido o expirado"
}
```

**Solución**: Verificar que el token sea válido y esté en el header `Authorization`

### 403 - Forbidden
```json
{
  "error": "Prohibido",
  "message": "No tienes permisos para realizar esta acción"
}
```

**Solución**: Verificar que el usuario tenga rol ADMIN

### 404 - Not Found
```json
{
  "error": "Producto no encontrado",
  "message": "El producto con ID 999 no existe"
}
```

**Solución**: Verificar que el ID del producto sea correcto

---

## 📋 RESUMEN RÁPIDO

| Operación | Método | Endpoint | Permisos |
|-----------|--------|----------|----------|
| Crear producto | POST | `/api/admin/products` | ADMIN |
| Listar productos | GET | `/api/admin/products` | ADMIN, STAFF |
| Listar activos | GET | `/api/admin/products/active` | ADMIN, STAFF |
| Obtener por ID | GET | `/api/admin/products/{id}` | ADMIN, STAFF |
| Actualizar | PUT | `/api/admin/products/{id}` | ADMIN |
| Activar | PATCH | `/api/admin/products/{id}/activate` | ADMIN |
| Desactivar | PATCH | `/api/admin/products/{id}/deactivate` | ADMIN |
| Eliminar | DELETE | `/api/admin/products/{id}` | ADMIN |
| Crear categoría | POST | `/api/admin/product-categories` | ADMIN |
| Listar categorías | GET | `/api/admin/product-categories` | ADMIN, STAFF |
| Obtener categoría | GET | `/api/admin/product-categories/{id}` | ADMIN, STAFF |
| Actualizar categoría | PUT | `/api/admin/product-categories/{id}` | ADMIN |
| Eliminar categoría | DELETE | `/api/admin/product-categories/{id}` | ADMIN |

---

## 🚀 Próximos Pasos

1. **Integrar en el Frontend**: Usar los ejemplos proporcionados
2. **Manejo de Errores**: Implementar try-catch en todas las llamadas
3. **Validación**: Validar datos antes de enviar
4. **Caching**: Cachear categorías para mejor rendimiento
5. **Testing**: Crear tests unitarios para cada función

---

**Documento versión**: 1.0
**Fecha**: 2026-04-13
**Estado**: ✅ COMPLETADO

