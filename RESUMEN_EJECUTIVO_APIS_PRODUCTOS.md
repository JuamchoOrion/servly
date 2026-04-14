# 🎯 RESUMEN EJECUTIVO - APIs PRODUCTOS

---

## 📝 MENSAJE SIMPLE PARA PASAR AL FRONTEND

```
Hola equipo frontend,

Preparé la documentación COMPLETA para integrar las APIs de GESTIÓN DE PRODUCTOS.

Tienes 3 archivos listos:

1️⃣ GUIA_APIS_PRODUCTOS_ADMIN.md
   → Documentación técnica completa con ejemplos de código JavaScript
   → Para cuando necesites saber exactamente qué enviar y qué recibirás
   
2️⃣ PROMPTS_APIS_PRODUCTOS.md
   → 13 prompts listos para copiar y pegar
   → Usa esto para describir cada funcionalidad que necesitas implementar
   
3️⃣ test-apis-productos-admin.http
   → Archivo de pruebas HTTP con todos los endpoints
   → Prueba primero aquí antes de implementar en el frontend

ENDPOINTS PRINCIPALES:

PRODUCTOS:
- Crear:      POST   /api/admin/products
- Listar:     GET    /api/admin/products
- Obtener:    GET    /api/admin/products/{id}
- Actualizar: PUT    /api/admin/products/{id}
- Activar:    PATCH  /api/admin/products/{id}/activate
- Desactivar: PATCH  /api/admin/products/{id}/deactivate
- Eliminar:   DELETE /api/admin/products/{id}

CATEGORÍAS:
- Crear:      POST   /api/admin/product-categories
- Listar:     GET    /api/admin/product-categories
- Obtener:    GET    /api/admin/product-categories/{id}
- Actualizar: PUT    /api/admin/product-categories/{id}
- Eliminar:   DELETE /api/admin/product-categories/{id}

TODO REQUIERE: Authorization: Bearer {token}

¿Dudas? Abre GUIA_APIS_PRODUCTOS_ADMIN.md

Éxito! 🚀
```

---

## 📊 TABLA RÁPIDA DE ENDPOINTS

### Productos (8 operaciones)

```
┌──────────────┬──────────┬─────────────────────────────────┬──────────┐
│ Operación    │ Método   │ Endpoint                        │ Permisos │
├──────────────┼──────────┼─────────────────────────────────┼──────────┤
│ Crear        │ POST     │ /api/admin/products             │ ADMIN    │
│ Listar       │ GET      │ /api/admin/products             │ ADMIN    │
│ Obtener uno  │ GET      │ /api/admin/products/{id}        │ ADMIN    │
│ Listar act.  │ GET      │ /api/admin/products/active      │ ADMIN    │
│ Actualizar   │ PUT      │ /api/admin/products/{id}        │ ADMIN    │
│ Activar      │ PATCH    │ /api/admin/products/{id}/act    │ ADMIN    │
│ Desactivar   │ PATCH    │ /api/admin/products/{id}/deact  │ ADMIN    │
│ Eliminar     │ DELETE   │ /api/admin/products/{id}        │ ADMIN    │
└──────────────┴──────────┴─────────────────────────────────┴──────────┘
```

### Categorías (5 operaciones)

```
┌──────────────┬──────────┬──────────────────────────────────┬──────────┐
│ Operación    │ Método   │ Endpoint                         │ Permisos │
├──────────────┼──────────┼──────────────────────────────────┼──────────┤
│ Crear        │ POST     │ /api/admin/product-categories    │ ADMIN    │
│ Listar       │ GET      │ /api/admin/product-categories    │ ADMIN    │
│ Obtener uno  │ GET      │ /api/admin/product-categories/{} │ ADMIN    │
│ Listar act.  │ GET      │ /api/admin/product-categories/ac │ ADMIN    │
│ Actualizar   │ PUT      │ /api/admin/product-categories/{} │ ADMIN    │
│ Eliminar     │ DELETE   │ /api/admin/product-categories/{} │ ADMIN    │
└──────────────┴──────────┴──────────────────────────────────┴──────────┘
```

---

## 🔑 INFORMACIÓN IMPORTANTE

### Autenticación
```javascript
// SIEMPRE necesitas esto en el header:
Authorization: Bearer tu_token_jwt_aqui
Content-Type: application/json
```

### Campos de Producto
```json
{
  "name": "Nombre del producto",           // string, requerido
  "description": "Descripción",             // string, requerido
  "price": 25000,                           // número, requerido, > 0
  "categoryId": 1,                          // número, requerido
  "recipeId": 5,                            // número, requerido
  "active": true                            // booleano, por defecto true
}
```

### Campos de Categoría
```json
{
  "name": "Nombre de la categoría",        // string, requerido
  "description": "Descripción"              // string, requerido
}
```

### Códigos HTTP Esperados
- `201 Created`: Cuando creas un recurso (POST)
- `200 OK`: Cuando lees o actualizas (GET, PUT, PATCH)
- `400 Bad Request`: Datos inválidos
- `401 Unauthorized`: Token inválido
- `403 Forbidden`: Sin permisos (no eres ADMIN)
- `404 Not Found`: Recurso no existe
- `500 Server Error`: Error del servidor

---

## 💻 CÓDIGO JAVASCRIPT RÁPIDO

### Obtener productos
```javascript
const token = 'tu_token_aqui';

const response = await fetch('http://localhost:8081/api/admin/products', {
  headers: { 'Authorization': `Bearer ${token}` }
});

const datos = await response.json();
console.log(datos); // Array de productos
```

### Crear producto
```javascript
const token = 'tu_token_aqui';

const response = await fetch('http://localhost:8081/api/admin/products', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    name: 'Hamburguesa',
    description: 'Hamburguesa deliciosa',
    price: 25000,
    categoryId: 1,
    recipeId: 5,
    active: true
  })
});

const productoCreado = await response.json();
console.log(productoCreado);
```

### Actualizar producto
```javascript
const token = 'tu_token_aqui';
const productoId = 42;

const response = await fetch(
  `http://localhost:8081/api/admin/products/${productoId}?name=Nuevo Nombre&price=35000`,
  {
    method: 'PUT',
    headers: { 'Authorization': `Bearer ${token}` }
  }
);

const actualizado = await response.json();
console.log(actualizado);
```

### Activar/Desactivar producto
```javascript
const token = 'tu_token_aqui';
const productoId = 42;

// Activar
const response1 = await fetch(
  `http://localhost:8081/api/admin/products/${productoId}/activate`,
  {
    method: 'PATCH',
    headers: { 'Authorization': `Bearer ${token}` }
  }
);

// Desactivar
const response2 = await fetch(
  `http://localhost:8081/api/admin/products/${productoId}/deactivate`,
  {
    method: 'PATCH',
    headers: { 'Authorization': `Bearer ${token}` }
  }
);
```

### Eliminar producto
```javascript
const token = 'tu_token_aqui';
const productoId = 42;

const response = await fetch(
  `http://localhost:8081/api/admin/products/${productoId}`,
  {
    method: 'DELETE',
    headers: { 'Authorization': `Bearer ${token}` }
  }
);

const resultado = await response.json();
console.log(resultado); // { "message": "Producto eliminado correctamente" }
```

---

## 📦 ESTRUCTURA DE RESPUESTAS

### Crear/Obtener Producto
```json
{
  "id": 42,
  "name": "Hamburguesa Clásica",
  "description": "Hamburguesa de carne con queso, tomate y lechuga",
  "price": 25000,
  "category": {
    "id": 1,
    "name": "Comidas Principales"
  },
  "recipe": {
    "id": 5,
    "name": "Receta Hamburguesa"
  },
  "active": true,
  "createdAt": "2026-04-13T14:30:00",
  "updatedAt": "2026-04-13T16:00:00"
}
```

### Listar Productos (Paginado)
```json
{
  "content": [
    { "id": 1, "name": "Café", "price": 5000, ... },
    { "id": 2, "name": "Hamburguesa", "price": 25000, ... }
  ],
  "pageable": {
    "size": 10,
    "number": 0,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

### Crear/Obtener Categoría
```json
{
  "id": 1,
  "name": "Comidas Principales",
  "description": "Platos principales del menú",
  "active": true,
  "createdAt": "2026-04-01T10:00:00"
}
```

### Listar Categorías
```json
[
  { "id": 1, "name": "Comidas Principales", "description": "...", "active": true },
  { "id": 3, "name": "Bebidas", "description": "...", "active": true }
]
```

---

## 🎯 PASOS PARA EMPEZAR

### Paso 1: Obtener token
```bash
POST /api/auth/login
Body: { "email": "admin@servly.com", "password": "tu_contraseña" }
Response: { "token": "eyJ..." }
```

### Paso 2: Guardar token en variable
```javascript
const token = "eyJ...";
```

### Paso 3: Probar primer endpoint
```bash
GET http://localhost:8081/api/admin/products
Authorization: Bearer eyJ...
```

### Paso 4: Si funciona, ¡a implementar!
- Sigue los ejemplos de la guía
- Copia código y adapta
- Prueba en el navegador

---

## ⚠️ ERRORES COMUNES

| Error | Causa | Solución |
|-------|-------|----------|
| 401 Unauthorized | Token inválido o expirado | Verifica el token en el header |
| 403 Forbidden | Usuario no es ADMIN | Solo ADMIN puede crear/editar |
| 404 Not Found | Producto no existe | Verifica el ID |
| 400 Bad Request | Datos inválidos (ej: precio negativo) | Revisa los datos antes de enviar |
| 500 Server Error | Error en el servidor | Revisa logs del backend |

---

## 📱 PARA MOBILE/APPS

Si usas una librería HTTP diferente (Axios, etc.):

```javascript
// Axios ejemplo
const response = await axios.post('http://localhost:8081/api/admin/products', 
  {
    name: 'Hamburguesa',
    description: 'Deliciosa',
    price: 25000,
    categoryId: 1,
    recipeId: 5,
    active: true
  },
  {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  }
);
```

---

## 📚 DOCUMENTACIÓN COMPLETA

Abre `GUIA_APIS_PRODUCTOS_ADMIN.md` para:
- Ejemplos detallados
- Manejo de errores
- Validaciones
- Paginación
- Ordenamiento
- Y mucho más

---

## ✅ LISTA DE VERIFICACIÓN

- [ ] Obtuve mi token JWT
- [ ] Probé GET `/api/admin/products` con Postman/Thunder
- [ ] Entiendo la estructura de datos
- [ ] Entiendo los códigos de error
- [ ] Implementé crear producto
- [ ] Implementé listar productos
- [ ] Implementé editar producto
- [ ] Implementé activar/desactivar
- [ ] Implementé eliminar producto
- [ ] Implementé CRUD de categorías
- [ ] Las categorías carga en selects
- [ ] La paginación funciona
- [ ] Los errores se muestran
- [ ] Probé todo en el navegador
- [ ] ¡Está listo para producción!

---

## 🚀 ¡AHORA SÍ, A CODEAR!

Tienes todo lo que necesitas. ¡Éxito en la implementación! 🎉

Cualquier duda, revisa:
- GUIA_APIS_PRODUCTOS_ADMIN.md → Detalles técnicos
- test-apis-productos-admin.http → Prueba los endpoints

---

**Preparado**: 2026-04-13
**Estado**: ✅ Listo para usar

