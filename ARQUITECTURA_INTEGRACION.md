# 🏗️ Arquitectura e Integración Visual - Gestión de Productos

## 📊 Diagrama de Flujo Completo

```
┌─────────────────────────────────────────────────────────────────┐
│                    SISTEMA DE GESTIÓN DE PRODUCTOS               │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐
│   ADMINISTRADOR  │
│   (Dashboard)    │
└────────┬─────────┘
         │
         ├─────────────────────────────────────────────────────┐
         │                                                     │
         ▼                                                     ▼
    ┌─────────────┐                               ┌──────────────────┐
    │ Crear/Editar│                               │ Ver Catálogo     │
    │ Categorías  │                               │ Completo         │
    └─────────────┘                               └──────────────────┘
         │
         │    POST /products/categories
         │    {name, description}
         ▼
    ┌──────────────────────────────┐
    │  CATEGORÍAS DE PRODUCTOS     │
    │  (Hamburguesas, Bebidas...)  │
    └──────────────────────────────┘
         │
         ├─────────────────────────────────────┐
         │                                     │
         ▼                                     ▼
    ┌─────────────────────┐       ┌──────────────────────┐
    │ Crear Items         │       │ Crear Recetas        │
    │ de Inventario       │       │ (Recetas)            │
    └─────────────────────┘       └──────────────────────┘
         │                              │
         │ POST /inventory/items        │ POST /products/recipes
         │ {name, unit, stock}         │ {name, itemDetails}
         │                              │
         ▼                              ▼
    ┌─────────────────────┐       ┌──────────────────────┐
    │ ITEMS               │       │ RECETAS              │
    │ - Pan               │       │ - Hamburguesa Simple │
    │ - Queso             │       │ - Hamburguesa Premium│
    │ - Lechuga           │       └──────────────────────┘
    │ - Tomate            │              │
    └─────────────────────┘              │
         │                               │
         │                    ┌──────────┘
         │                    │
         └────────┬───────────┘
                  │
                  ▼
         ┌─────────────────────┐
         │  CREAR PRODUCTOS    │
         │  POST /products     │
         │ {name, price, ...}  │
         └─────────────────────┘
                  │
         ┌────────┴────────┐
         │                 │
         ▼                 ▼
    ┌─────────────┐   ┌─────────────┐
    │ PRODUCTO    │   │ PRODUCTO    │
    │ SIN RECETA  │   │ CON RECETA  │
    │ (Bebida)    │   │ (Comida)    │
    └─────────────┘   └─────────────┘
         │                 │
         └────────┬────────┘
                  │
                  ▼
         ┌──────────────────┐
         │ MENÚ DEL CLIENTE │
         │ GET /products    │
         │ GET /menu/       │
         │ products         │
         └──────────────────┘
                  │
                  ▼
    ┌────────────────────────────┐
    │   CLIENTE EN LA APP        │
    │ (Ve y selecciona productos)│
    └────────────────────────────┘
```

---

## 🔄 Ciclo de Vida de un Producto

```
1. CREACIÓN
   └─ Administrador accede al sistema
   └─ Valida credenciales
   └─ Accede a "Gestión de Productos"

2. DEFINICIÓN
   ├─ Selecciona/Crea CATEGORÍA
   ├─ Crea/Selecciona ITEMS de inventario
   ├─ Crea/Selecciona RECETA (opcional)
   └─ Completa datos del PRODUCTO

3. VALIDACIÓN
   ├─ Valida nombre (mín 3 caracteres)
   ├─ Valida descripción (mín 10 caracteres)
   ├─ Valida precio (> 0)
   └─ Verifica categoría seleccionada

4. ALMACENAMIENTO
   └─ POST /products
   └─ Backend valida y almacena
   └─ Retorna ID del producto

5. PUBLICACIÓN
   ├─ Producto creado con active=true
   ├─ Aparece en GET /products/active
   └─ Cliente puede verlo en menú

6. GESTIÓN
   ├─ Actualizar: PUT /products/{id}
   ├─ Modificar stock: PUT /inventory/items/{id}
   └─ Desactivar: PUT /products/{id} (active=false)

7. ELIMINACIÓN (Opcional)
   └─ DELETE /products/{id}
```

---

## 📈 Estructura de Datos

### Categoría
```json
{
  "id": 1,
  "name": "Hamburguesas",
  "description": "Hamburguesas caseras"
}
```

### Item de Inventario
```json
{
  "id": 5,
  "name": "Queso Cheddar",
  "description": "Queso para hamburguesas",
  "measurementUnit": "SLICE",
  "currentStock": 100,
  "minStock": 10,
  "maxStock": 200
}
```

### Receta
```json
{
  "id": 10,
  "name": "Hamburguesa Simple",
  "description": "Hamburguesa básica",
  "itemDetails": [
    {
      "id": 25,
      "itemId": 6,
      "quantity": 200,
      "isOptional": false,
      "minQuantity": 150,
      "maxQuantity": 250
    }
  ]
}
```

### Producto
```json
{
  "id": 2,
  "name": "Hamburguesa Simple",
  "description": "Hamburguesa básica con queso",
  "price": 8.99,
  "active": true,
  "category": {
    "id": 1,
    "name": "Hamburguesas"
  },
  "recipe": {
    "id": 10,
    "name": "Hamburguesa Simple"
  }
}
```

---

## 🎨 Flujo de Interfaz del Admin

### Paso 1: Entrada
```
┌─────────────────────────────┐
│ Dashboard del Administrador │
├─────────────────────────────┤
│ ☐ Gestión de Categorías    │
│ ☐ Gestión de Inventario     │
│ ☐ Gestión de Recetas        │
│ ☐ Gestión de Productos ◄─── Selecciona
│ ☐ Ver Catálogo             │
└─────────────────────────────┘
```

### Paso 2: Crear Producto
```
┌──────────────────────────────────┐
│  CREAR NUEVO PRODUCTO             │
├──────────────────────────────────┤
│ Nombre: [Hamburguesa Simple ___]  │
│                                   │
│ Descripción:                       │
│ [Hamburguesa con queso y lechuga]  │
│ [____________________________]      │
│                                   │
│ Precio: [$] 8.99                  │
│                                   │
│ Categoría: [Hamburguesas ▼]       │
│                                   │
│ Receta: [Hamburguesa Simple ▼]   │
│                                   │
│ ☐ Activo                          │
│                                   │
│  [Guardar]  [Cancelar]            │
└──────────────────────────────────┘
```

### Paso 3: Lista de Productos
```
┌──────────────────────────────────────────┐
│  LISTA DE PRODUCTOS                      │
├──────────────────────────────────────────┤
│ Filtrar por categoría: [Todas ▼]         │
├──────────────────────────────────────────┤
│ Nombre        │ Categ  │ Precio │ Acciones│
├──────────────────────────────────────────┤
│Coca Cola      │Bebida  │ $2.50  │ ✏️ 🗑️  │
│Hamburguesa    │Hamburgu│ $8.99  │ ✏️ 🗑️  │
│Premium        │esa     │ $12.99 │ ✏️ 🗑️  │
└──────────────────────────────────────────┘
```

---

## 💻 Componentes Necesarios en Frontend

### Vista: Gestión de Productos

```
ProductManagement.vue
├── Header
│   ├── Título "Gestión de Productos"
│   └── Botón "+ Crear Nuevo"
├── Filtros
│   ├── Select de Categorías
│   └── Search por nombre
├── Tabla de Productos
│   ├── Columnas: Nombre, Categoría, Precio, Receta, Acciones
│   ├── Filas con botones Editar y Eliminar
│   └── Paginación
└── Modal para Crear/Editar
    ├── ProductForm.vue
    └── Manejo de errores
```

### Componentes Auxiliares

```
├── ProductForm.vue          (Formulario de producto)
├── CategorySelect.vue       (Selector de categorías)
├── RecipeSelect.vue         (Selector de recetas)
├── ProductTable.vue         (Tabla de productos)
├── InventoryManager.vue     (Gestor de inventario)
├── RecipeBuilder.vue        (Constructor de recetas)
└── ErrorNotification.vue    (Notificaciones de error)
```

---

## 🔌 Integración con Backend

### Request Típico
```
POST /api/products
Content-Type: application/json
Authorization: Bearer token_aqui

{
  "name": "Hamburguesa Simple",
  "description": "Hamburguesa básica",
  "price": 8.99,
  "productCategoryId": 1,
  "recipeId": 10,
  "active": true
}
```

### Response Típico
```
201 Created
Content-Type: application/json

{
  "id": 2,
  "name": "Hamburguesa Simple",
  "description": "Hamburguesa básica",
  "price": 8.99,
  "category": {
    "id": 1,
    "name": "Hamburguesas"
  },
  "recipe": {
    "id": 10,
    "name": "Hamburguesa Simple"
  },
  "active": true
}
```

---

## 🔍 Flujo de Búsqueda y Visualización (Cliente)

```
┌──────────────────────────────────┐
│   Cliente escanea QR de la mesa  │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│  GET /products/active            │
│  (Obtiene menú disponible)        │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│  Menú en Frontend                │
│  ├─ Categoría: Hamburguesas      │
│  │  ├─ Hamburguesa Simple $8.99  │
│  │  └─ Hamburguesa Premium $12.99│
│  ├─ Categoría: Bebidas           │
│  │  ├─ Coca Cola $2.50           │
│  │  └─ Jugo $3.50                │
│  └─ Categoría: Postres           │
│     └─ Flan $4.99                │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│  Cliente selecciona producto     │
│  GET /products/{productId}       │
│  (Ver detalles y receta)         │
└────────────┬─────────────────────┘
             │
             ▼
┌──────────────────────────────────┐
│  Detalles del Producto           │
│  Nombre: Hamburguesa Simple      │
│  Precio: $8.99                   │
│  Ingredientes:                   │
│  ☑ Pan (200g)                    │
│  ☑ Queso (1 slice)               │
│  ☐ Tomate (opcional)             │
│  [Agregar al carrito]            │
└──────────────────────────────────┘
```

---

## 🎯 Validaciones Implementadas

### En el Backend
```
✓ Nombre producto: mín 3 caracteres
✓ Descripción: mín 10 caracteres
✓ Precio: > 0
✓ Categoría: debe existir
✓ Receta: debe existir (si se proporciona)
✓ Items en receta: deben existir
✓ Cantidades: consistencia min/max
```

### En el Frontend (Antes de enviar)
```
✓ Validar formato de entrada
✓ Verificar campos requeridos
✓ Sanitizar strings
✓ Verificar tipos de datos
✓ Mostrar errores al usuario
```

---

## 🚀 Checklist de Implementación

### Fase 1: Preparación
- [ ] Crear carpeta de servicios
- [ ] Crear servicio ProductService
- [ ] Configurar autenticación
- [ ] Crear tipos/interfaces de datos

### Fase 2: Componentes Básicos
- [ ] FormProducto.vue
- [ ] ListaProductos.vue
- [ ] CrudCategoria.vue
- [ ] CrudInventario.vue

### Fase 3: Funcionalidad Completa
- [ ] Crear producto con receta
- [ ] Editar producto
- [ ] Eliminar producto
- [ ] Filtrar por categoría
- [ ] Buscar por nombre

### Fase 4: Interfaz de Usuario
- [ ] Diseño responsive
- [ ] Notificaciones de éxito/error
- [ ] Paginación
- [ ] Carga de datos

### Fase 5: Testing
- [ ] Probar cada endpoint
- [ ] Validar errores
- [ ] Pruebas de flujo completo

---

## 📱 Responsive Design

### Desktop (>1024px)
```
┌──────────────────────────────────────┐
│  Sidebar    │  Contenido Principal    │
│             │                        │
│ • Crear     │  [Título]              │
│ • Editar    │  [Tabla de Productos]  │
│ • Eliminar  │                        │
│ • Ver       │                        │
└──────────────────────────────────────┘
```

### Tablet (768px - 1024px)
```
┌────────────────────────────┐
│  Menú Hamburguesa          │
│  [CONTENIDO]               │
│  [Lista de productos]      │
└────────────────────────────┘
```

### Mobile (<768px)
```
┌──────────────┐
│ ☰ Menú       │
├──────────────┤
│  [Contenido] │
│              │
│ [Productos]  │
└──────────────┘
```

---

## 🔐 Seguridad

### Token de Autenticación
```javascript
// Guardar token en localStorage (después del login)
localStorage.setItem('adminToken', response.token);

// Incluir en todas las requests autenticadas
headers: {
  'Authorization': `Bearer ${localStorage.getItem('adminToken')}`
}
```

### Validación de Permisos
```javascript
// Solo administradores pueden:
// - POST /products (Crear)
// - PUT /products/{id} (Actualizar)
// - DELETE /products/{id} (Eliminar)

// Todos pueden:
// - GET /products (Ver)
// - GET /products/active (Ver menú)
```

---

## 📞 Soporte y Troubleshooting

### Problema: Error 401 Unauthorized
**Solución**: Verifica que el token sea válido y esté incluido en el header Authorization

### Problema: Error 400 Bad Request
**Solución**: Valida que todos los campos requeridos estén presentes y sean del tipo correcto

### Problema: Precio no se guarda correctamente
**Solución**: Asegúrate de que sea un número, no un string

### Problema: No puedo seleccionar receta
**Solución**: Verifica que la receta exista y tenga al menos un item

---

¡Documentación completa lista! 🎉

