# 🎯 PROMPTS - APIs de PRODUCTOS para Frontend

Este documento contiene **prompts listos para usar** que puedes pasar a tu equipo de frontend o a un LLM para la implementación de cada funcionalidad.

---

## 📦 PROMPT 1: CREAR PRODUCTO

**Para**: Implementar la funcionalidad de creación de productos en el frontend

```
Necesito que implementes un formulario para CREAR PRODUCTOS en la aplicación.

REQUISITOS:
1. El formulario debe tener los siguientes campos:
   - Nombre del producto (texto, requerido, máximo 100 caracteres)
   - Descripción (textarea, requerido, máximo 500 caracteres)
   - Precio (número, requerido, mayor a 0)
   - Categoría (select/dropdown, requerido, debe traerse de la API)
   - Receta (select/dropdown, requerido, debe traerse de la API)
   - Estado activo (checkbox, por defecto marcado)

2. Validaciones:
   - Validar que todos los campos requeridos estén llenos
   - Validar que el precio sea un número positivo
   - Mostrar mensaje de error si falta algún campo

3. Endpoint a usar:
   - POST /api/admin/products
   - Headers: { Authorization: Bearer {token}, Content-Type: application/json }
   - Body:
   {
     "name": "Hamburguesa Clásica",
     "description": "Hamburguesa de carne con queso",
     "price": 25000,
     "categoryId": 1,
     "recipeId": 5,
     "active": true
   }

4. Respuesta esperada (201 Created):
   {
     "id": 42,
     "name": "Hamburguesa Clásica",
     "price": 25000,
     "active": true,
     "createdAt": "2026-04-13T14:30:00"
   }

5. Manejo de errores:
   - Si recibe 400: Mostrar error de validación
   - Si recibe 401: Redirigir a login (token inválido)
   - Si recibe 403: Mostrar "No tienes permisos"
   - Si recibe 500: Mostrar "Error del servidor, intenta de nuevo"

6. Después de crear:
   - Mostrar mensaje de éxito "Producto creado exitosamente"
   - Limpiar el formulario
   - Opcionalmente: Redirigir a lista de productos o recargar

NOTA: Necesitarás traer las categorías y recetas desde las APIs correspondientes al cargar el formulario.
```

---

## 📋 PROMPT 2: LISTAR PRODUCTOS

**Para**: Mostrar una tabla o listado de todos los productos

```
Necesito que implementes una tabla para LISTAR TODOS LOS PRODUCTOS.

REQUISITOS:
1. La tabla debe mostrar las siguientes columnas:
   - ID del producto
   - Nombre
   - Descripción (truncada a 50 caracteres)
   - Precio
   - Categoría
   - Estado (Activo/Inactivo)
   - Fecha de creación
   - Acciones (Editar, Activar/Desactivar, Eliminar)

2. Paginación:
   - Mostrar 10 productos por página
   - Permitir cambiar de página
   - Mostrar total de productos

3. Endpoint a usar:
   - GET /api/admin/products?page=0&size=10&sort=name,asc
   - Headers: { Authorization: Bearer {token} }

4. Respuesta esperada (200 OK):
   {
     "content": [
       {
         "id": 1,
         "name": "Café Americano",
         "price": 5000,
         "category": { "id": 3, "name": "Bebidas" },
         "active": true
       }
     ],
     "pageable": {
       "totalElements": 25,
       "totalPages": 3
     }
   }

5. Funcionalidades:
   - Botón de "Nuevo Producto" que lleva al formulario de creación
   - Filtros (opcional): por categoría, por estado activo/inactivo
   - Ordenamiento: por nombre, precio, fecha

6. Estilos:
   - Tabla responsive
   - Filas coloreadas alternadamente
   - Botones de acción en cada fila
   - Loading spinner mientras se cargan los datos

NOTA: Esta vista es de solo lectura para STAFF, pero ADMIN puede ver todas las acciones.
```

---

## 🔍 PROMPT 3: OBTENER PRODUCTO POR ID

**Para**: Ver detalles completos de un producto específico

```
Necesito que implementes una vista de DETALLES DE PRODUCTO.

REQUISITOS:
1. Mostrar todos los detalles del producto:
   - ID
   - Nombre
   - Descripción completa
   - Precio
   - Categoría (con descripción)
   - Receta asociada (si existe)
   - Estado (Activo/Inactivo)
   - Fecha de creación
   - Fecha de actualización

2. Endpoint a usar:
   - GET /api/admin/products/{id}
   - Headers: { Authorization: Bearer {token} }
   - Ejemplo: GET /api/admin/products/42

3. Respuesta esperada (200 OK):
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
       "name": "Receta Hamburguesa"
     },
     "active": true,
     "createdAt": "2026-04-13T14:30:00",
     "updatedAt": "2026-04-13T16:00:00"
   }

4. Botones de acción:
   - Editar: Lleva al formulario de edición
   - Eliminar: Muestra confirmación y luego elimina
   - Activar/Desactivar: Cambia el estado
   - Volver: Regresa a la lista

5. Manejo de errores:
   - Si el producto no existe (404): Mostrar "Producto no encontrado"
   - Si hay error de conexión: Mostrar "Error al cargar el producto"

NOTA: Desde esta vista también se puede acceder a las opciones de edición y eliminación.
```

---

## ✏️ PROMPT 4: ACTUALIZAR PRODUCTO

**Para**: Editar un producto existente

```
Necesito que implementes un formulario para EDITAR PRODUCTOS existentes.

REQUISITOS:
1. El formulario debe:
   - Cargar los datos actuales del producto
   - Permitir cambiar: nombre, descripción, precio, categoría, receta, estado
   - Mostrar un botón "Actualizar" y "Cancelar"

2. Campos:
   - Nombre (texto, requerido)
   - Descripción (textarea, requerido)
   - Precio (número, requerido, mayor a 0)
   - Categoría (select, requerido)
   - Receta (select, requerido)
   - Estado activo (checkbox)

3. Endpoint a usar:
   - PUT /api/admin/products/{id}?name=Nombre&description=Desc&price=25000&active=true
   - Método alternativo: PUT con URL params
   
   Ejemplo:
   PUT /api/admin/products/42?name=Hamburguesa Premium&price=35000

4. Respuesta esperada (200 OK):
   {
     "id": 42,
     "name": "Hamburguesa Premium",
     "description": "Hamburguesa con ingredientes premium",
     "price": 35000,
     "active": true,
     "updatedAt": "2026-04-13T16:00:00"
   }

5. Validaciones:
   - Verificar que al menos algo haya cambiado
   - Validar datos antes de enviar
   - Mostrar confirmación antes de actualizar

6. Después de actualizar:
   - Mostrar mensaje "Producto actualizado exitosamente"
   - Recargar los datos del producto
   - O redirigir a la lista

NOTA: Solo ADMIN puede actualizar productos. STAFF solo puede ver.
```

---

## 🗑️ PROMPT 5: ELIMINAR PRODUCTO

**Para**: Implementar la funcionalidad de eliminar un producto

```
Necesito que implementes la funcionalidad para ELIMINAR PRODUCTOS.

REQUISITOS:
1. Debe haber un botón "Eliminar" en:
   - La tabla de listado de productos
   - La vista de detalles del producto
   - El menú de acciones

2. Al hacer click en "Eliminar":
   - Mostrar un modal de confirmación:
     "¿Está seguro que desea eliminar el producto: {nombre}?"
   - Opciones: "Cancelar" | "Sí, eliminar"

3. Endpoint a usar:
   - DELETE /api/admin/products/{id}
   - Headers: { Authorization: Bearer {token} }
   - Ejemplo: DELETE /api/admin/products/42

4. Respuesta esperada (200 OK):
   {
     "message": "Producto eliminado correctamente"
   }

5. Después de eliminar:
   - Mostrar mensaje "Producto eliminado exitosamente"
   - Recargar la lista de productos
   - Si estaba en vista de detalles, redirigir a la lista

6. Manejo de errores:
   - Si recibe 404: "El producto no existe"
   - Si recibe 403: "No tienes permisos para eliminar"
   - Si recibe 500: "Error al eliminar el producto"

NOTA: Este es un borrado LÓGICO, el registro no se elimina de la BD, solo se marca como inactivo.
```

---

## ✅ PROMPT 6: ACTIVAR PRODUCTO

**Para**: Reactivar un producto que está inactivo

```
Necesito que implementes la funcionalidad para ACTIVAR PRODUCTOS.

REQUISITOS:
1. Debe haber un botón "Activar" que:
   - Aparezca solo si el producto está inactivo
   - Al hacer click, cambie el estado a activo

2. Endpoint a usar:
   - PATCH /api/admin/products/{id}/activate
   - Headers: { Authorization: Bearer {token} }
   - Ejemplo: PATCH /api/admin/products/42/activate

3. Respuesta esperada (200 OK):
   {
     "id": 42,
     "name": "Hamburguesa Clásica",
     "price": 25000,
     "active": true,
     "updatedAt": "2026-04-13T16:10:00"
   }

4. Después de activar:
   - Mostrar mensaje "Producto activado exitosamente"
   - Actualizar el estado en la UI
   - El botón "Activar" debe desaparecer y aparecer "Desactivar"

5. Ubicaciones donde debe aparecer este botón:
   - En la tabla de productos (columna de acciones)
   - En la vista de detalles del producto

NOTA: Solo ADMIN puede activar productos.
```

---

## ❌ PROMPT 7: DESACTIVAR PRODUCTO

**Para**: Desactivar un producto (sacarlo del menú sin eliminarlo)

```
Necesito que implementes la funcionalidad para DESACTIVAR PRODUCTOS.

REQUISITOS:
1. Debe haber un botón "Desactivar" que:
   - Aparezca solo si el producto está activo
   - Al hacer click, cambie el estado a inactivo
   - Opcionalmente mostrar confirmación antes de desactivar

2. Endpoint a usar:
   - PATCH /api/admin/products/{id}/deactivate
   - Headers: { Authorization: Bearer {token} }
   - Ejemplo: PATCH /api/admin/products/42/deactivate

3. Respuesta esperada (200 OK):
   {
     "id": 42,
     "name": "Hamburguesa Clásica",
     "price": 25000,
     "active": false,
     "updatedAt": "2026-04-13T16:15:00"
   }

4. Después de desactivar:
   - Mostrar mensaje "Producto desactivado exitosamente"
   - Actualizar el estado en la UI
   - El botón "Desactivar" debe desaparecer y aparecer "Activar"
   - Marcar visualmente el producto como inactivo (ej: con una clase CSS)

5. Ubicaciones donde debe aparecer:
   - En la tabla de productos (columna de acciones)
   - En la vista de detalles del producto

NOTA: Esto es útil cuando un producto no está disponible temporalmente pero podría volver a ofrecerse.
```

---

## 📂 PROMPT 8: CREAR CATEGORÍA DE PRODUCTOS

**Para**: Implementar creación de categorías

```
Necesito que implementes un formulario para CREAR CATEGORÍAS DE PRODUCTOS.

REQUISITOS:
1. Formulario con campos:
   - Nombre de la categoría (texto, requerido, máximo 100 caracteres)
   - Descripción (textarea, requerido, máximo 500 caracteres)

2. Validaciones:
   - Verificar que ambos campos estén llenos
   - Evitar duplicados (si posible)

3. Endpoint a usar:
   - POST /api/admin/product-categories
   - Headers: { Authorization: Bearer {token}, Content-Type: application/json }
   - Body:
   {
     "name": "Bebidas Calientes",
     "description": "Café, té y otras bebidas calientes"
   }

4. Respuesta esperada (201 Created):
   {
     "id": 10,
     "name": "Bebidas Calientes",
     "description": "Café, té y otras bebidas calientes",
     "active": true,
     "createdAt": "2026-04-13T14:30:00"
   }

5. Después de crear:
   - Mostrar mensaje "Categoría creada exitosamente"
   - Limpiar formulario
   - Actualizar el listado de categorías
   - O redirigir a vista de categorías

NOTA: Las categorías se usan al crear/editar productos.
```

---

## 📋 PROMPT 9: LISTAR CATEGORÍAS DE PRODUCTOS

**Para**: Mostrar todas las categorías disponibles

```
Necesito que implementes un listado de CATEGORÍAS DE PRODUCTOS.

REQUISITOS:
1. Mostrar una tabla/listado con:
   - ID de la categoría
   - Nombre
   - Descripción
   - Estado (Activo/Inactivo)
   - Fecha de creación
   - Acciones (Editar, Eliminar)

2. Endpoint a usar:
   - GET /api/admin/product-categories
   - Headers: { Authorization: Bearer {token} }

3. Respuesta esperada (200 OK):
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
     }
   ]

4. Funcionalidades:
   - Botón "Nueva Categoría" que abre el formulario de creación
   - Botón "Editar" para cada categoría
   - Botón "Eliminar" para cada categoría
   - Filtro por estado (activas/todas)

5. Estilos:
   - Tabla responsive
   - Colores diferentes para activas/inactivas

NOTA: Las categorías aparecen como opciones en los formularios de productos.
```

---

## ✏️ PROMPT 10: ACTUALIZAR CATEGORÍA

**Para**: Editar una categoría existente

```
Necesito que implementes un formulario para EDITAR CATEGORÍAS.

REQUISITOS:
1. Formulario que:
   - Cargue los datos actuales de la categoría
   - Permita cambiar nombre y descripción
   - Muestre botones "Actualizar" y "Cancelar"

2. Campos:
   - Nombre (texto, requerido)
   - Descripción (textarea, requerido)

3. Endpoint a usar:
   - PUT /api/admin/product-categories/{id}
   - Headers: { Authorization: Bearer {token}, Content-Type: application/json }
   - Body:
   {
     "name": "Comidas Principales Nuevas",
     "description": "Descripción actualizada"
   }
   
   Ejemplo: PUT /api/admin/product-categories/1

4. Respuesta esperada (200 OK):
   {
     "id": 1,
     "name": "Comidas Principales Nuevas",
     "description": "Descripción actualizada",
     "active": true,
     "updatedAt": "2026-04-13T16:00:00"
   }

5. Después de actualizar:
   - Mostrar mensaje "Categoría actualizada exitosamente"
   - Recargar los datos en la vista
   - Actualizar la lista de categorías

NOTA: Solo ADMIN puede actualizar categorías.
```

---

## 🗑️ PROMPT 11: ELIMINAR CATEGORÍA

**Para**: Eliminar una categoría de productos

```
Necesito que implementes la funcionalidad para ELIMINAR CATEGORÍAS.

REQUISITOS:
1. Botón "Eliminar" en el listado de categorías

2. Al hacer click:
   - Mostrar confirmación: "¿Está seguro que desea eliminar {nombre}?"
   - Si hay productos asociados, mostrar advertencia
   - Opciones: "Cancelar" | "Sí, eliminar"

3. Endpoint a usar:
   - DELETE /api/admin/product-categories/{id}
   - Headers: { Authorization: Bearer {token} }
   - Ejemplo: DELETE /api/admin/product-categories/5

4. Respuesta esperada (200 OK):
   {
     "message": "Categoría de producto eliminada correctamente"
   }

5. Después de eliminar:
   - Mostrar mensaje "Categoría eliminada exitosamente"
   - Recargar el listado de categorías
   - Si hay productos asociados, permitir reasignarlos primero (si es posible)

NOTA: Verificar que no haya productos usando esta categoría antes de permitir eliminación.
```

---

## 🔗 PROMPT 12: OBTENER CATEGORÍAS ACTIVAS

**Para**: Obtener solo categorías activas (para select en formularios)

```
Necesito que al crear/editar un PRODUCTO, el campo de "Categoría" se llene con las categorías ACTIVAS.

REQUISITOS:
1. Endpoint a usar:
   - GET /api/admin/product-categories/active
   - Headers: { Authorization: Bearer {token} }

2. Respuesta esperada (200 OK):
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

3. Usar en:
   - Select de categoría en formulario de crear producto
   - Select de categoría en formulario de editar producto

4. Funcionalidad:
   - Cargar categorías al abrir el formulario
   - Mostrar como opciones en el select
   - Permitir seleccionar una

NOTA: Usar caché si es posible para evitar requests innecesarias.
```

---

## 📊 PROMPT 13: LISTAR PRODUCTOS ACTIVOS

**Para**: Mostrar solo productos activos (útil para menú del cliente)

```
Necesito que implementes una vista para mostrar solo PRODUCTOS ACTIVOS.

REQUISITOS:
1. Endpoint a usar:
   - GET /api/admin/products/active?page=0&size=10
   - Headers: { Authorization: Bearer {token} }

2. Respuesta esperada (200 OK):
   {
     "content": [
       {
         "id": 1,
         "name": "Café Americano",
         "description": "Café negro simple",
         "price": 5000,
         "category": { "id": 3, "name": "Bebidas" },
         "active": true
       }
     ],
     "pageable": { "totalElements": 15, "totalPages": 2 }
   }

3. Mostrar:
   - Tabla con nombre, descripción, precio y categoría
   - Paginación
   - Ordenamiento por precio, nombre, etc.

4. Filtros:
   - Por categoría
   - Por rango de precio

NOTA: Esta vista es útil tanto para ADMIN como para STAFF ver el menú disponible actualmente.
```

---

## 📋 RESUMEN DE PROMPTS

| # | Función | Endpoint | Método |
|----|---------|----------|--------|
| 1 | Crear producto | `/api/admin/products` | POST |
| 2 | Listar productos | `/api/admin/products` | GET |
| 3 | Obtener producto | `/api/admin/products/{id}` | GET |
| 4 | Actualizar producto | `/api/admin/products/{id}` | PUT |
| 5 | Eliminar producto | `/api/admin/products/{id}` | DELETE |
| 6 | Activar producto | `/api/admin/products/{id}/activate` | PATCH |
| 7 | Desactivar producto | `/api/admin/products/{id}/deactivate` | PATCH |
| 8 | Crear categoría | `/api/admin/product-categories` | POST |
| 9 | Listar categorías | `/api/admin/product-categories` | GET |
| 10 | Actualizar categoría | `/api/admin/product-categories/{id}` | PUT |
| 11 | Eliminar categoría | `/api/admin/product-categories/{id}` | DELETE |
| 12 | Categorías activas | `/api/admin/product-categories/active` | GET |
| 13 | Productos activos | `/api/admin/products/active` | GET |

---

**Documento versión**: 1.0
**Fecha**: 2026-04-13
**Estado**: ✅ COMPLETADO

*Usa estos prompts para comunicarte con tu equipo de frontend o con un LLM para implementar cada funcionalidad.*

