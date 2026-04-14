# 📊 Flujo Visual de CRUD de Recetas

## 🎯 Diagrama General

```
┌─────────────────────────────────────────────────────────┐
│            SISTEMA DE RECETAS (FRONTEND)                │
└─────────────────────────────────────────────────────────┘

    ↓
    
┌─────────────────────────────────────────────────────────┐
│          INTERFAZ DE USUARIO (Formulario)               │
├─────────────────────────────────────────────────────────┤
│ • Campo: Nombre de Receta                              │
│ • Campo: Cantidad                                       │
│ • Campo: Descripción                                    │
│ • Lista Dinámica de Items (ItemDetails)                │
│ • Botones: Guardar, Limpiar                            │
└─────────────────────────────────────────────────────────┘

    ↓
    
┌─────────────────────────────────────────────────────────┐
│      VALIDACIÓN EN FRONTEND                             │
├─────────────────────────────────────────────────────────┤
│ ✓ Nombre no vacío                                       │
│ ✓ Quantity > 0                                          │
│ ✓ Mínimo 1 ItemDetail                                   │
│ ✓ ItemId válido                                         │
│ ✓ Quantity > 0 para cada item                           │
└─────────────────────────────────────────────────────────┘

    ↓ (si todo es válido)
    
┌─────────────────────────────────────────────────────────┐
│  API REQUEST (JSON + Token JWT)                         │
├─────────────────────────────────────────────────────────┤
│ POST /api/admin/recipes                                 │
│ Authorization: Bearer {token}                           │
│ Content-Type: application/json                          │
│                                                         │
│ {                                                       │
│   "name": "Hamburguesa",                                │
│   "quantity": 1,                                        │
│   "description": "Deliciosa",                           │
│   "itemDetails": [                                      │
│     {"itemId": 1, "quantity": 2, ...}                   │
│   ]                                                     │
│ }                                                       │
└─────────────────────────────────────────────────────────┘

    ↓
    
┌─────────────────────────────────────────────────────────┐
│  BACKEND PROCESSING                                     │
├─────────────────────────────────────────────────────────┤
│ 1. Validar Token JWT                                    │
│ 2. Validar Rol (ADMIN)                                  │
│ 3. Crear Receta                                         │
│ 4. Crear ItemDetails                                    │
│ 5. Guardar en BD                                        │
│ 6. Retornar con IDs                                     │
└─────────────────────────────────────────────────────────┘

    ↓
    
┌─────────────────────────────────────────────────────────┐
│  RESPONSE (201 Created)                                 │
├─────────────────────────────────────────────────────────┤
│ {                                                       │
│   "id": 1,                                              │
│   "name": "Hamburguesa",                                │
│   "quantity": 1,                                        │
│   "description": "Deliciosa",                           │
│   "itemDetails": [                                      │
│     {                                                   │
│       "id": 1,                                          │
│       "itemId": 1,                                      │
│       "itemName": "Pan",                                │
│       "quantity": 2,                                    │
│       ...                                               │
│     }                                                   │
│   ]                                                     │
│ }                                                       │
└─────────────────────────────────────────────────────────┘

    ↓
    
┌─────────────────────────────────────────────────────────┐
│  ACTUALIZAR UI                                          │
├─────────────────────────────────────────────────────────┤
│ • Mostrar mensaje de éxito                              │
│ • Limpiar formulario                                    │
│ • Recargar lista de recetas                             │
│ • Actualizar dropdown de recetas                        │
└─────────────────────────────────────────────────────────┘
```

---

## 🔄 Operaciones CRUD

### CREATE (POST)
```
USUARIO                    FRONTEND                    BACKEND                     BD
   │                          │                           │                         │
   │─ Completa Forma ────────→ │                           │                         │
   │                          │─ Valida Datos ──────────→ │                         │
   │                          │                           │─ Verifica Token ───────→│
   │                          │                           │← Token Válido ─────────│
   │                          │                           │─ Crear Receta ────────→│
   │                          │                           │                         │← Insert
   │                          │                           │← ID (1) Creado ────────│
   │                          │← Response (201) ─────────│                         │
   │← Mostrar Éxito ──────────│                           │                         │
```

### READ (GET - Listado)
```
USUARIO              FRONTEND                  BACKEND                  BD
   │                    │                          │                    │
   │─ Click Cargar ───→ │                          │                    │
   │                    │─ GET /recipes ──────────→│                    │
   │                    │                          │─ Query ───────────→│
   │                    │                          │← Recetas List ────│
   │                    │← JSON Array (200) ───────│                    │
   │← Mostrar Tabla ───│                          │                    │
```

### READ (GET - Detalles)
```
USUARIO              FRONTEND                  BACKEND                  BD
   │                    │                          │                    │
   │─ Click Editar ────→│                          │                    │
   │                    │─ GET /recipes/1 ─────────→│                    │
   │                    │                          │─ findById(1) ─────→│
   │                    │                          │← Receta Detail ───│
   │                    │← JSON Object (200) ──────│                    │
   │← Cargar Forma ────│                          │                    │
```

### UPDATE (PUT)
```
USUARIO                    FRONTEND                    BACKEND                     BD
   │                          │                           │                         │
   │─ Modifica Datos ───────→ │                           │                         │
   │─ Click Guardar ────────→ │                           │                         │
   │                          │─ Valida Datos ──────────→ │                         │
   │                          │                           │─ Verifica Token ───────→│
   │                          │─ PUT /recipes/1 ─────────→│                         │
   │                          │                           │─ Update ──────────────→│
   │                          │                           │                         │← UPDATE
   │                          │                           │← Receta Updated ──────│
   │                          │← Response (200) ──────────│                         │
   │← Mostrar Éxito ──────────│                           │                         │
```

### DELETE (DELETE)
```
USUARIO                FRONTEND                  BACKEND                  BD
   │                     │                          │                    │
   │─ Click Eliminar ──→ │                          │                    │
   │                     │─ Confirmar? ─────────────→│                    │
   │─ Click Sí ────────→ │                          │                    │
   │                     │─ DELETE /recipes/1 ──────→│                    │
   │                     │                          │─ Delete ──────────→│
   │                     │                          │                    │← DELETE
   │                     │← Response (204) ──────────│                    │
   │← Mostrar Éxito ───│                          │                    │
```

---

## 📋 Estructura de Datos

### Receta Object
```javascript
{
  id: 1,                          // ← Auto generado por backend
  name: "Hamburguesa",            // ← Usuario ingresa
  quantity: 1,                    // ← Usuario ingresa
  description: "Deliciosa",       // ← Usuario ingresa
  itemDetails: [                  // ← Usuario completa
    {
      id: 1,                      // ← Auto generado
      itemId: 1,                  // ← Usuario selecciona
      itemName: "Pan",            // ← Desde tabla items
      quantity: 2,                // ← Usuario ingresa
      annotation: "tostado",      // ← Usuario ingresa (opcional)
      isOptional: false,          // ← Usuario marca
      minQuantity: 2,             // ← Auto calculado
      maxQuantity: 2              // ← Auto calculado
    }
  ]
}
```

---

## 🎨 Componentes UI Necesarios

### 1. Formulario de Receta
```
┌─────────────────────────────────────┐
│ ☒ CREAR NUEVA RECETA               │
├─────────────────────────────────────┤
│                                     │
│ Nombre:                             │
│ ┌──────────────────────────────────┐│
│ │ Hamburguesa Premium             ││
│ └──────────────────────────────────┘│
│                                     │
│ Cantidad:                           │
│ ┌──────────────────────────────────┐│
│ │ 1                                ││
│ └──────────────────────────────────┘│
│                                     │
│ Descripción:                        │
│ ┌──────────────────────────────────┐│
│ │ Hamburguesa con ingredientes de ││
│ │ calidad                          ││
│ └──────────────────────────────────┘│
│                                     │
└─────────────────────────────────────┘
```

### 2. Lista de Items (ItemDetails)
```
┌──────────────────────────────────────────────────────┐
│ ITEMS DE LA RECETA                                   │
├──────────────────────────────────────────────────────┤
│ Item | Cantidad | Anotación      | Opcional | Acción│
├──────────────────────────────────────────────────────┤
│  1   │    2     │ pan tostado    │    ☐     │ ✕    │
│  2   │   150    │ bien cocida    │    ☐     │ ✕    │
│  5   │    1     │ opcional       │    ☑     │ ✕    │
├──────────────────────────────────────────────────────┤
│ [+ Agregar Item]                                     │
└──────────────────────────────────────────────────────┘
```

### 3. Tabla de Recetas
```
┌──────────────────────────────────────────────────────┐
│ RECETAS EXISTENTES                    [+ Nueva]     │
├──────────────────────────────────────────────────────┤
│ ID │ Nombre   │ Descripción  │ Items │ Acciones    │
├──────────────────────────────────────────────────────┤
│ 1  │ Hambur.  │ Con ingred.  │  3    │ ✏ 🗑      │
│ 2  │ Pizza    │ Clásica      │  5    │ ✏ 🗑      │
│ 3  │ Ensalada │ César        │  4    │ ✏ 🗑      │
└──────────────────────────────────────────────────────┘
```

---

## 🔐 Flujo de Autenticación

```
┌──────────────┐
│   Usuario    │
│   Admin      │
└──────┬───────┘
       │
       ├─ Email & Password
       │
       ↓
┌──────────────────────────────┐
│ POST /api/auth/login         │
│ {                            │
│   "email": "admin@...",      │
│   "password": "xxx"          │
│ }                            │
└──────┬───────────────────────┘
       │
       ├─ JWT Token: eyJhbGc...
       │
       ↓
┌──────────────────────────────┐
│ localStorage.setItem(        │
│   'authToken',               │
│   token                      │
│ )                            │
└──────┬───────────────────────┘
       │
       └─ Token guardado ✓
       
       ↓
       
┌──────────────────────────────┐
│ TODAS LAS LLAMADAS A API     │
│ incluyen:                    │
│ Authorization: Bearer token  │
└──────────────────────────────┘
```

---

## ⚙️ Flujo de Validación

```
Frontend Validaciones:
├─ ¿Nombre vacío?
│  └─ SÍ → Error ❌
│  └─ NO → Siguiente ↓
│
├─ ¿Cantidad > 0?
│  └─ NO → Error ❌
│  └─ SÍ → Siguiente ↓
│
├─ ¿ItemDetails vacío?
│  └─ SÍ → Error ❌
│  └─ NO → Siguiente ↓
│
└─ Para cada Item:
   ├─ ¿ItemId válido?
   │  └─ NO → Error ❌
   │  └─ SÍ → Siguiente ↓
   │
   └─ ¿Quantity > 0?
      └─ NO → Error ❌
      └─ SÍ → Enviar ✓

Backend Validaciones:
├─ ¿Token válido?
│  └─ NO → 401 ❌
│  └─ SÍ → Siguiente ↓
│
├─ ¿Rol ADMIN?
│  └─ NO → 403 ❌
│  └─ SÍ → Siguiente ↓
│
├─ ¿ItemDetails.length > 0?
│  └─ NO → 400 ❌
│  └─ SÍ → Siguiente ↓
│
└─ Para cada Item:
   ├─ ¿Item existe?
   │  └─ NO → 404 ❌
   │  └─ SÍ → Siguiente ↓
   │
   └─ ¿Item activo?
      └─ NO → 400 ❌
      └─ SÍ → Guardar ✓
```

---

## 🔄 Ciclo de Vida de una Receta

```
1. CREACIÓN
   Formulario vacío → Usuario llena datos → POST /api/admin/recipes
   → Receta creada con ID

2. LECTURA
   ID asignado → GET /api/admin/recipes/{id}
   → Datos disponibles para edición

3. ACTUALIZACIÓN
   Datos cargados → Usuario modifica → PUT /api/admin/recipes/{id}
   → Receta actualizada

4. ELIMINACIÓN
   Receta existente → DELETE /api/admin/recipes/{id}
   → Receta removida de BD

5. PRÓXIMO USO
   En módulo de Productos → Crear producto con receta
   → Receta vinculada a producto
```

---

## 📲 Flujo en Página Completa

```
PÁGINA DE RECETAS
├─ Header
│  └─ Título: "Gestión de Recetas"
│
├─ Botones de Acción
│  └─ "+ Nueva Receta"
│
├─ Formulario (Oculto/Modal)
│  ├─ Campos de Receta
│  ├─ Lista Dinámica de Items
│  └─ Botones: Guardar, Cancelar
│
└─ Tabla de Recetas
   ├─ Listado de todas las recetas
   ├─ Datos: ID, Nombre, Descripción, Items
   └─ Acciones: Editar, Eliminar
```

---

## 🚀 Resumen de Pasos para el Frontend

```
1. CARGAR PÁGINA
   └─ GET /api/admin/recipes → Mostrar en tabla

2. CREAR
   └─ Abrir formulario → Usuario ingresa → POST → Actualizar tabla

3. EDITAR
   └─ Click editar → GET /id → Llenar formulario → Usuario modifica 
      → PUT → Actualizar tabla

4. ELIMINAR
   └─ Click eliminar → Confirmar → DELETE → Actualizar tabla

5. EN CADA ACCIÓN
   └─ Mostrar mensajes: "Cargando...", "Éxito", "Error"
```

---

✅ **Documento completado - Flujo visual de CRUD de Recetas**

