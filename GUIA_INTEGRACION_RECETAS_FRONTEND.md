# 📋 Guía de Integración: CRUD de Recetas en el Frontend

## 📌 Resumen
Las recetas son composiciones de items que especifican qué ingredientes se necesitan para hacer un producto. Cada receta contiene:
- **ItemDetails**: Lista de items con cantidades, anotaciones y opcionalidad

---

## 🔑 Conceptos Clave

### ¿Qué es una Receta?
Una receta es una plantilla que define los ingredientes (items) necesarios para crear un producto. Por ejemplo:
```
Receta: "Hamburguesa Clásica"
├── Item: Pan (cantidad: 2)
├── Item: Carne molida (cantidad: 150g)
├── Item: Lechuga (cantidad: 1 hoja - opcional)
├── Item: Tomate (cantidad: 2 rodajas - opcional)
└── Item: Queso (cantidad: 1 rebanada)
```

### ItemDetail
Cada item en una receta es un **ItemDetail** que contiene:
- `itemId`: ID del item
- `quantity`: Cantidad requerida
- `annotation`: Notas especiales (ej: "bien dorado", "sin cebolla")
- `isOptional`: Si el item es opcional
- `minQuantity`: Cantidad mínima
- `maxQuantity`: Cantidad máxima

---

## 🔗 Endpoints de Recetas

### 1️⃣ CREAR RECETA
```
POST /api/admin/recipes
Authorization: Bearer {admin_token}
Content-Type: application/json

Request Body:
{
  "name": "Hamburguesa Premium",
  "quantity": 1,
  "description": "Hamburguesa con ingredientes de calidad",
  "itemDetails": [
    {
      "itemId": 1,
      "quantity": 2,
      "annotation": "pan tostado",
      "isOptional": false
    },
    {
      "itemId": 2,
      "quantity": 150,
      "annotation": "bien cocida",
      "isOptional": false
    },
    {
      "itemId": 5,
      "quantity": 1,
      "annotation": "opcional - cebolla",
      "isOptional": true
    }
  ]
}

Response (201 Created):
{
  "id": 1,
  "name": "Hamburguesa Premium",
  "quantity": 1,
  "description": "Hamburguesa con ingredientes de calidad",
  "itemDetails": [
    {
      "id": 1,
      "itemId": 1,
      "itemName": "Pan de Hamburguesa",
      "quantity": 2,
      "annotation": "pan tostado",
      "isOptional": false,
      "minQuantity": 2,
      "maxQuantity": 2
    },
    {
      "id": 2,
      "itemId": 2,
      "itemName": "Carne Molida",
      "quantity": 150,
      "annotation": "bien cocida",
      "isOptional": false,
      "minQuantity": 150,
      "maxQuantity": 150
    },
    {
      "id": 3,
      "itemId": 5,
      "itemName": "Cebolla",
      "quantity": 1,
      "annotation": "opcional - cebolla",
      "isOptional": true,
      "minQuantity": 0,
      "maxQuantity": 1
    }
  ]
}
```

### 2️⃣ OBTENER TODAS LAS RECETAS
```
GET /api/admin/recipes
Authorization: Bearer {admin_token}

Response (200 OK):
[
  {
    "id": 1,
    "name": "Hamburguesa Premium",
    "quantity": 1,
    "description": "Hamburguesa con ingredientes de calidad",
    "itemDetails": [...]
  },
  {
    "id": 2,
    "name": "Pizza Margherita",
    "quantity": 1,
    "description": "Pizza clásica italiana",
    "itemDetails": [...]
  }
]
```

### 3️⃣ OBTENER RECETA POR ID
```
GET /api/admin/recipes/1
Authorization: Bearer {admin_token}

Response (200 OK):
{
  "id": 1,
  "name": "Hamburguesa Premium",
  "quantity": 1,
  "description": "Hamburguesa con ingredientes de calidad",
  "itemDetails": [...]
}
```

### 4️⃣ ACTUALIZAR RECETA
```
PUT /api/admin/recipes/1
Authorization: Bearer {admin_token}
Content-Type: application/json

Request Body:
{
  "name": "Hamburguesa Premium v2",
  "quantity": 1,
  "description": "Hamburguesa mejorada con más ingredientes",
  "itemDetails": [
    {
      "itemId": 1,
      "quantity": 2,
      "annotation": "pan tostado",
      "isOptional": false
    },
    {
      "itemId": 2,
      "quantity": 200,
      "annotation": "bien cocida",
      "isOptional": false
    }
  ]
}

Response (200 OK):
{
  "id": 1,
  "name": "Hamburguesa Premium v2",
  ...
}
```

### 5️⃣ ELIMINAR RECETA
```
DELETE /api/admin/recipes/1
Authorization: Bearer {admin_token}

Response (204 No Content):
(sin body)
```

---

## 📝 Ejemplos de Implementación en Frontend

### JavaScript/Fetch API

#### Crear Receta
```javascript
const token = localStorage.getItem('authToken');

const newRecipe = {
  name: "Pizza Margarita",
  quantity: 1,
  description: "Pizza clásica con tomate, mozzarella y albahaca",
  itemDetails: [
    { itemId: 1, quantity: 1, annotation: "masa fina", isOptional: false },
    { itemId: 3, quantity: 200, annotation: "fresca", isOptional: false },
    { itemId: 4, quantity: 150, annotation: "mozzarella", isOptional: false },
    { itemId: 10, quantity: 5, annotation: "hojas frescas", isOptional: true }
  ]
};

fetch('http://localhost:8081/api/admin/recipes', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify(newRecipe)
})
.then(response => response.json())
.then(data => {
  console.log('Receta creada:', data);
  // Actualizar UI
})
.catch(error => console.error('Error:', error));
```

#### Obtener Todas las Recetas
```javascript
const token = localStorage.getItem('authToken');

fetch('http://localhost:8081/api/admin/recipes', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => {
  console.log('Recetas:', data);
  // Mostrar en tabla o lista
  displayRecipes(data);
})
.catch(error => console.error('Error:', error));
```

#### Actualizar Receta
```javascript
const token = localStorage.getItem('authToken');
const recipeId = 1;

const updatedRecipe = {
  name: "Pizza Margarita Especial",
  quantity: 1,
  description: "Pizza con ingredientes premium",
  itemDetails: [
    { itemId: 1, quantity: 1, annotation: "masa gruesa", isOptional: false },
    { itemId: 3, quantity: 250, annotation: "fresca", isOptional: false }
  ]
};

fetch(`http://localhost:8081/api/admin/recipes/${recipeId}`, {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify(updatedRecipe)
})
.then(response => response.json())
.then(data => console.log('Receta actualizada:', data))
.catch(error => console.error('Error:', error));
```

#### Eliminar Receta
```javascript
const token = localStorage.getItem('authToken');
const recipeId = 1;

fetch(`http://localhost:8081/api/admin/recipes/${recipeId}`, {
  method: 'DELETE',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => {
  if (response.ok) {
    console.log('Receta eliminada');
    // Actualizar UI
  }
})
.catch(error => console.error('Error:', error));
```

### React/TypeScript Ejemplo Completo

```typescript
import React, { useState, useEffect } from 'react';

interface ItemDetail {
  itemId: number;
  quantity: number;
  annotation?: string;
  isOptional: boolean;
}

interface Recipe {
  id?: number;
  name: string;
  quantity: number;
  description: string;
  itemDetails: ItemDetail[];
}

export function RecipeManager() {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const token = localStorage.getItem('authToken');

  // GET - Cargar todas las recetas
  const fetchRecipes = async () => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8081/api/admin/recipes', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const data = await response.json();
      setRecipes(data);
    } catch (err) {
      setError('Error cargando recetas');
    } finally {
      setLoading(false);
    }
  };

  // POST - Crear receta
  const createRecipe = async (recipe: Recipe) => {
    try {
      const response = await fetch('http://localhost:8081/api/admin/recipes', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(recipe)
      });
      const newRecipe = await response.json();
      setRecipes([...recipes, newRecipe]);
    } catch (err) {
      setError('Error creando receta');
    }
  };

  // PUT - Actualizar receta
  const updateRecipe = async (id: number, recipe: Recipe) => {
    try {
      const response = await fetch(`http://localhost:8081/api/admin/recipes/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(recipe)
      });
      const updated = await response.json();
      setRecipes(recipes.map(r => r.id === id ? updated : r));
    } catch (err) {
      setError('Error actualizando receta');
    }
  };

  // DELETE - Eliminar receta
  const deleteRecipe = async (id: number) => {
    try {
      await fetch(`http://localhost:8081/api/admin/recipes/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      setRecipes(recipes.filter(r => r.id !== id));
    } catch (err) {
      setError('Error eliminando receta');
    }
  };

  useEffect(() => {
    fetchRecipes();
  }, []);

  return (
    <div className="recipe-manager">
      <h1>Gestión de Recetas</h1>
      
      {error && <div className="error">{error}</div>}
      {loading && <div>Cargando...</div>}

      <table>
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Descripción</th>
            <th>Items</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {recipes.map(recipe => (
            <tr key={recipe.id}>
              <td>{recipe.name}</td>
              <td>{recipe.description}</td>
              <td>{recipe.itemDetails.length}</td>
              <td>
                <button onClick={() => updateRecipe(recipe.id!, recipe)}>Editar</button>
                <button onClick={() => deleteRecipe(recipe.id!)}>Eliminar</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

---

## 🔒 Seguridad y Autenticación

### Token JWT
- Todos los endpoints requieren un token JWT en el header `Authorization: Bearer {token}`
- El token debe tener rol **ADMIN**
- Obtenlo del endpoint de login: `POST /api/auth/login`

### Headers Requeridos
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json
```

---

## ⚠️ Errores Comunes

### Error: "Una receta debe tener al menos un item detail"
**Causa**: El array `itemDetails` está vacío
**Solución**: Agrega al menos un item detail con los datos del item

### Error: "Item no encontrado: 99"
**Causa**: El `itemId` no existe en la base de datos
**Solución**: Verifica que el item exista antes de crear la receta

### Error: "No puedes usar un item inactivo en una receta"
**Causa**: El item tiene `active: false`
**Solución**: Activa el item o usa un item diferente

### Error: "Receta no encontrada: 5"
**Causa**: El ID de la receta no existe
**Solución**: Verifica el ID de la receta

---

## 📊 Flujo Completo de Integración

```
1. LISTAR RECETAS
   GET /api/admin/recipes
   ↓
2. CREAR NUEVA RECETA
   POST /api/admin/recipes
   ↓
3. VER DETALLES
   GET /api/admin/recipes/{id}
   ↓
4. ACTUALIZAR SI ES NECESARIO
   PUT /api/admin/recipes/{id}
   ↓
5. ELIMINAR SI ES NECESARIO
   DELETE /api/admin/recipes/{id}
```

---

## 🎯 Casos de Uso

### Crear Receta de Hamburguesa
```javascript
const hamburguesa = {
  name: "Hamburguesa Clásica",
  quantity: 1,
  description: "Hamburguesa de res con queso",
  itemDetails: [
    { itemId: 1, quantity: 2, annotation: "tostado", isOptional: false },
    { itemId: 2, quantity: 150, annotation: "punto", isOptional: false },
    { itemId: 3, quantity: 1, annotation: "cheddar", isOptional: false },
    { itemId: 4, quantity: 2, annotation: "", isOptional: true }
  ]
};

// Crear receta
fetch('http://localhost:8081/api/admin/recipes', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  },
  body: JSON.stringify(hamburguesa)
});
```

---

## 📚 Variables Importantes

| Variable | Tipo | Ejemplo | Descripción |
|----------|------|---------|-------------|
| `id` | Long | 1 | ID único de la receta |
| `name` | String | "Pizza Margherita" | Nombre de la receta |
| `quantity` | Integer | 1 | Cantidad que produce la receta |
| `description` | String | "Pizza clásica" | Descripción de la receta |
| `itemId` | Long | 5 | ID del item a usar |
| `quantity` (itemDetail) | Integer | 150 | Cantidad del item necesaria |
| `annotation` | String | "bien cocida" | Notas especiales |
| `isOptional` | Boolean | true/false | Si el item es opcional |

---

## 🚀 Próximos Pasos

1. **Integrar en formulario**: Crear formulario para CRUD de recetas
2. **Validación**: Validar datos antes de enviar
3. **Feedback**: Mostrar mensajes de éxito/error
4. **Listado**: Mostrar recetas en tabla con acciones
5. **Conexión con Productos**: Asignar recetas a productos

