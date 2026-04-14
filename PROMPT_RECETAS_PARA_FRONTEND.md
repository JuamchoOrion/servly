# 🎯 PROMPT PARA INTEGRACIÓN DE RECETAS EN FRONTEND

## 📋 Copia este prompt y úsalo como guía para tu equipo de frontend

---

## PROMPT COMPLETO PARA RECETAS

```
========================================
INTEGRACIÓN DE MÓDULO DE RECETAS
========================================

OBJETIVO:
Integrar el sistema completo de CRUD de Recetas en la plataforma frontend.
Las recetas son composiciones de items que especifican ingredientes para productos.

BASE URL: http://localhost:8081
AUTENTICACIÓN: Bearer Token (JWT) requerido con rol ADMIN

========================================
1. CREAR RECETA
========================================

ENDPOINT: POST /api/admin/recipes
MÉTODO: POST
AUTENTICACIÓN: Sí (Bearer Token)

DESCRIPCIÓN:
Crea una nueva receta con sus items asociados.

ESTRUCTURA DEL REQUEST:
{
  "name": "string (obligatorio)",
  "quantity": "integer (obligatorio)",
  "description": "string",
  "itemDetails": [
    {
      "itemId": "number (obligatorio)",
      "quantity": "number (obligatorio)",
      "annotation": "string (opcional)",
      "isOptional": "boolean (por defecto: false)"
    }
  ]
}

EJEMPLO DE REQUEST:
POST http://localhost:8081/api/admin/recipes
Authorization: Bearer {tu_token_aqui}
Content-Type: application/json

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

RESPUESTA ESPERADA (201 Created):
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

CASOS DE ERROR:
- 400 Bad Request: "Una receta debe tener al menos un item detail"
  → Solución: Agregar al menos un itemDetail
  
- 404 Not Found: "Item no encontrado: {id}"
  → Solución: Verificar que el itemId existe
  
- 400 Bad Request: "No puedes usar un item inactivo en una receta"
  → Solución: Usar un item con active: true

========================================
2. OBTENER TODAS LAS RECETAS
========================================

ENDPOINT: GET /api/admin/recipes
MÉTODO: GET
AUTENTICACIÓN: Sí (Bearer Token)

DESCRIPCIÓN:
Obtiene la lista completa de todas las recetas.

EJEMPLO DE REQUEST:
GET http://localhost:8081/api/admin/recipes
Authorization: Bearer {tu_token_aqui}

RESPUESTA ESPERADA (200 OK):
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
  },
  {
    "id": 3,
    "name": "Ensalada César",
    "quantity": 1,
    "description": "Ensalada fresca con aderezo César",
    "itemDetails": [...]
  }
]

IMPLEMENTACIÓN EN JAVASCRIPT:
const token = localStorage.getItem('authToken');

fetch('http://localhost:8081/api/admin/recipes', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => {
  console.log('Recetas cargadas:', data);
  // Mostrar en tabla, dropdown, etc.
  displayRecipesInTable(data);
})
.catch(error => console.error('Error:', error));

CASOS DE ERRROR:
- 401 Unauthorized: Token inválido o expirado
  → Solución: Obtener nuevo token
  
- 403 Forbidden: Usuario sin permiso
  → Solución: Usar usuario con rol ADMIN

========================================
3. OBTENER RECETA POR ID
========================================

ENDPOINT: GET /api/admin/recipes/{id}
MÉTODO: GET
AUTENTICACIÓN: Sí (Bearer Token)
PARÁMETRO: id (number) - ID de la receta

DESCRIPCIÓN:
Obtiene los detalles específicos de una receta.

EJEMPLO DE REQUEST:
GET http://localhost:8081/api/admin/recipes/1
Authorization: Bearer {tu_token_aqui}

RESPUESTA ESPERADA (200 OK):
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
    }
  ]
}

IMPLEMENTACIÓN EN JAVASCRIPT:
const token = localStorage.getItem('authToken');
const recipeId = 1; // Cambiar según sea necesario

fetch(`http://localhost:8081/api/admin/recipes/${recipeId}`, {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => {
  console.log('Detalles de receta:', data);
  // Cargar formulario de edición
  loadRecipeForm(data);
})
.catch(error => console.error('Error:', error));

CASOS DE ERROR:
- 404 Not Found: "Receta no encontrada: {id}"
  → Solución: Verificar que el ID existe

========================================
4. ACTUALIZAR RECETA
========================================

ENDPOINT: PUT /api/admin/recipes/{id}
MÉTODO: PUT
AUTENTICACIÓN: Sí (Bearer Token)
PARÁMETRO: id (number) - ID de la receta

DESCRIPCIÓN:
Actualiza los detalles de una receta existente.
Nota: Los itemDetails se reemplazan completamente (no se mezclan).

ESTRUCTURA DEL REQUEST:
{
  "name": "string (obligatorio)",
  "quantity": "integer (obligatorio)",
  "description": "string",
  "itemDetails": [
    {
      "itemId": "number (obligatorio)",
      "quantity": "number (obligatorio)",
      "annotation": "string (opcional)",
      "isOptional": "boolean (por defecto: false)"
    }
  ]
}

EJEMPLO DE REQUEST:
PUT http://localhost:8081/api/admin/recipes/1
Authorization: Bearer {tu_token_aqui}
Content-Type: application/json

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
    },
    {
      "itemId": 6,
      "quantity": 100,
      "annotation": "papas fritas",
      "isOptional": true
    }
  ]
}

RESPUESTA ESPERADA (200 OK):
{
  "id": 1,
  "name": "Hamburguesa Premium v2",
  "quantity": 1,
  "description": "Hamburguesa mejorada con más ingredientes",
  "itemDetails": [...]
}

IMPLEMENTACIÓN EN JAVASCRIPT:
const token = localStorage.getItem('authToken');
const recipeId = 1;

const updatedRecipe = {
  name: "Hamburguesa Premium v2",
  quantity: 1,
  description: "Hamburguesa mejorada",
  itemDetails: [
    { itemId: 1, quantity: 2, annotation: "pan tostado", isOptional: false },
    { itemId: 2, quantity: 200, annotation: "bien cocida", isOptional: false }
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
.then(data => {
  console.log('Receta actualizada:', data);
  // Mostrar mensaje de éxito
  showSuccessMessage('Receta actualizada exitosamente');
  // Recargar lista
  loadRecipes();
})
.catch(error => {
  console.error('Error:', error);
  showErrorMessage('Error al actualizar la receta');
});

CASOS DE ERROR:
- 404 Not Found: "Receta no encontrada: {id}"
  → Solución: Verificar que el ID existe
  
- 400 Bad Request: Errores de validación
  → Solución: Revisar datos ingresados

========================================
5. ELIMINAR RECETA
========================================

ENDPOINT: DELETE /api/admin/recipes/{id}
MÉTODO: DELETE
AUTENTICACIÓN: Sí (Bearer Token)
PARÁMETRO: id (number) - ID de la receta

DESCRIPCIÓN:
Elimina una receta completamente del sistema.
⚠️ ADVERTENCIA: Esta operación no se puede deshacer.

EJEMPLO DE REQUEST:
DELETE http://localhost:8081/api/admin/recipes/1
Authorization: Bearer {tu_token_aqui}

RESPUESTA ESPERADA (204 No Content):
(sin body)

IMPLEMENTACIÓN EN JAVASCRIPT:
const token = localStorage.getItem('authToken');
const recipeId = 1;

if (confirm('¿Estás seguro de que quieres eliminar esta receta?')) {
  fetch(`http://localhost:8081/api/admin/recipes/${recipeId}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  })
  .then(response => {
    if (response.ok) {
      console.log('Receta eliminada');
      showSuccessMessage('Receta eliminada exitosamente');
      loadRecipes(); // Recargar lista
    }
  })
  .catch(error => {
    console.error('Error:', error);
    showErrorMessage('Error al eliminar la receta');
  });
}

CASOS DE ERROR:
- 404 Not Found: "Receta no encontrada: {id}"
  → Solución: Verificar que el ID existe

========================================
ESTRUCTURA DE DATOS IMPORTANTE
========================================

ItemDetail:
{
  "itemId": number          ← ID del item a usar (obligatorio)
  "quantity": number        ← Cantidad requerida (obligatorio)
  "annotation": string      ← Notas especiales (opcional)
  "isOptional": boolean     ← ¿Es opcional? (por defecto: false)
}

Ejemplo de itemDetails completo:
[
  {
    "itemId": 1,
    "quantity": 2,
    "annotation": "bien tostado",
    "isOptional": false
  },
  {
    "itemId": 2,
    "quantity": 150,
    "annotation": "punto medio",
    "isOptional": false
  },
  {
    "itemId": 5,
    "quantity": 1,
    "annotation": "opcional",
    "isOptional": true
  }
]

========================================
FLUJO DE TRABAJO RECOMENDADO
========================================

1. CARGAR LISTA DE RECETAS
   → GET /api/admin/recipes
   → Mostrar en tabla con botones Editar y Eliminar

2. CREAR NUEVA RECETA
   → Mostrar formulario vacío
   → Usuario ingresa datos
   → POST /api/admin/recipes
   → Agregar a la lista

3. EDITAR RECETA
   → GET /api/admin/recipes/{id}
   → Cargar datos en formulario
   → Usuario modifica
   → PUT /api/admin/recipes/{id}
   → Actualizar en lista

4. ELIMINAR RECETA
   → Botón Eliminar en fila
   → Confirmar acción
   → DELETE /api/admin/recipes/{id}
   → Eliminar de lista

========================================
VALIDACIONES IMPORTANTES
========================================

ANTES DE ENVIAR AL SERVIDOR:

1. name: No puede estar vacío
   if (!recipe.name || recipe.name.trim() === '') {
     showError('El nombre es obligatorio');
     return;
   }

2. quantity: Debe ser > 0
   if (!recipe.quantity || recipe.quantity <= 0) {
     showError('La cantidad debe ser mayor a 0');
     return;
   }

3. itemDetails: Debe tener al menos 1 item
   if (!recipe.itemDetails || recipe.itemDetails.length === 0) {
     showError('Debe agregar al menos un item');
     return;
   }

4. Cada itemDetail:
   - itemId: debe ser número > 0
   - quantity: debe ser número > 0
   if (!item.itemId || item.itemId <= 0) {
     showError('Item inválido');
     return;
   }

========================================
HEADERS REQUERIDOS
========================================

Todos los requests DEBEN incluir:

Authorization: Bearer {JWT_TOKEN}

Donde JWT_TOKEN es obtenido de:
POST /api/auth/login

Ejemplo completo de headers:
{
  "Authorization": "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTY5NDYzNDAwMH0...",
  "Content-Type": "application/json"
}

========================================
MANEJO DE ERRORES
========================================

ERRORES COMUNES Y SOLUCIONES:

1. Error 401 (Unauthorized)
   Problema: Token inválido o expirado
   Solución: Obtener nuevo token mediante login
   
2. Error 403 (Forbidden)
   Problema: Usuario sin rol ADMIN
   Solución: Usar usuario con permisos de administrador
   
3. Error 400 (Bad Request)
   Problema: Datos inválidos en request
   Solución: Validar campos antes de enviar
   
4. Error 404 (Not Found)
   Problema: Receta o Item no existe
   Solución: Verificar IDs
   
5. Error 500 (Internal Server Error)
   Problema: Error en servidor
   Solución: Revisar logs del servidor

Implementación de manejo de errores:
fetch(url, options)
  .then(response => {
    if (!response.ok) {
      if (response.status === 401) {
        // Redirigir a login
      } else if (response.status === 403) {
        showError('No tienes permisos');
      } else if (response.status === 404) {
        showError('Recurso no encontrado');
      }
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  })
  .catch(error => console.error('Error:', error));

========================================
EJEMPLO DE PÁGINA COMPLETA (HTML/JS)
========================================

<!DOCTYPE html>
<html>
<head>
  <title>Gestión de Recetas</title>
</head>
<body>
  <h1>Recetas</h1>
  
  <button onclick="openCreateForm()">+ Nueva Receta</button>
  
  <table id="recipesTable">
    <thead>
      <tr>
        <th>ID</th>
        <th>Nombre</th>
        <th>Descripción</th>
        <th>Items</th>
        <th>Acciones</th>
      </tr>
    </thead>
    <tbody id="recipesBody"></tbody>
  </table>

  <script>
    const token = localStorage.getItem('authToken');
    const baseUrl = 'http://localhost:8081';

    // Cargar recetas al iniciar
    loadRecipes();

    function loadRecipes() {
      fetch(`${baseUrl}/api/admin/recipes`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      .then(r => r.json())
      .then(recipes => {
        const tbody = document.getElementById('recipesBody');
        tbody.innerHTML = recipes.map(r => `
          <tr>
            <td>${r.id}</td>
            <td>${r.name}</td>
            <td>${r.description}</td>
            <td>${r.itemDetails.length}</td>
            <td>
              <button onclick="editRecipe(${r.id})">Editar</button>
              <button onclick="deleteRecipe(${r.id})">Eliminar</button>
            </td>
          </tr>
        `).join('');
      });
    }

    function openCreateForm() {
      // Mostrar modal o nueva página
    }

    function editRecipe(id) {
      fetch(`${baseUrl}/api/admin/recipes/${id}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })
      .then(r => r.json())
      .then(recipe => {
        // Llenar formulario con datos
      });
    }

    function deleteRecipe(id) {
      if (confirm('¿Eliminar?')) {
        fetch(`${baseUrl}/api/admin/recipes/${id}`, {
          method: 'DELETE',
          headers: { 'Authorization': `Bearer ${token}` }
        })
        .then(() => loadRecipes());
      }
    }
  </script>
</body>
</html>

========================================
FIN DEL PROMPT
========================================
```

---

## 📌 Cómo Usar Este Prompt

1. **Copia el prompt anterior completo**
2. **Pásalo a tu equipo de frontend**
3. **Usa los ejemplos de código proporcionados**
4. **Adapta a tu framework (React, Vue, Angular, etc.)**
5. **Prueba cada endpoint con Postman o ThunderClient**

## 🔑 Puntos Clave a Recordar

- ✅ **Token requerido**: Todos los endpoints necesitan autenticación
- ✅ **itemDetails obligatorio**: Mínimo 1 item por receta
- ✅ **Reemplazo de items**: Al actualizar, los itemDetails se reemplazan (no se mezclan)
- ✅ **Items activos**: Solo puedes usar items con `active: true`
- ✅ **Validaciones**: Valida antes de enviar al servidor

---

## 🧪 Pruebas Rápidas

Puedes probar los endpoints directamente con curl:

```bash
# Crear receta
curl -X POST http://localhost:8081/api/admin/recipes \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pizza",
    "quantity": 1,
    "description": "Pizza de queso",
    "itemDetails": [{"itemId": 1, "quantity": 2}]
  }'

# Obtener todas
curl http://localhost:8081/api/admin/recipes \
  -H "Authorization: Bearer {token}"

# Obtener por ID
curl http://localhost:8081/api/admin/recipes/1 \
  -H "Authorization: Bearer {token}"

# Actualizar
curl -X PUT http://localhost:8081/api/admin/recipes/1 \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{...}'

# Eliminar
curl -X DELETE http://localhost:8081/api/admin/recipes/1 \
  -H "Authorization: Bearer {token}"
```

---

¡Listo! Con este prompt tu equipo de frontend tendrá todo lo necesario para integrar las recetas. 🚀

