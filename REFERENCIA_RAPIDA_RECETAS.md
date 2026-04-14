# ⚡ Referencia Rápida: CRUD de Recetas

## 🔗 Endpoints

| Operación | Método | URL | Auth | Status |
|-----------|--------|-----|------|--------|
| Crear | POST | `/api/admin/recipes` | Sí | 201 |
| Listar | GET | `/api/admin/recipes` | Sí | 200 |
| Obtener | GET | `/api/admin/recipes/{id}` | Sí | 200 |
| Actualizar | PUT | `/api/admin/recipes/{id}` | Sí | 200 |
| Eliminar | DELETE | `/api/admin/recipes/{id}` | Sí | 204 |

---

## 📦 Estructura Mínima de Datos

### Crear/Actualizar
```json
{
  "name": "Nombre",
  "quantity": 1,
  "description": "Descripción",
  "itemDetails": [
    {
      "itemId": 1,
      "quantity": 100,
      "annotation": "nota",
      "isOptional": false
    }
  ]
}
```

### Response
```json
{
  "id": 1,
  "name": "Nombre",
  "quantity": 1,
  "description": "Descripción",
  "itemDetails": [
    {
      "id": 1,
      "itemId": 1,
      "itemName": "Nombre Item",
      "quantity": 100,
      "annotation": "nota",
      "isOptional": false,
      "minQuantity": 100,
      "maxQuantity": 100
    }
  ]
}
```

---

## 🚀 Ejemplos Rápidos

### Crear
```javascript
fetch('http://localhost:8081/api/admin/recipes', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  body: JSON.stringify({
    name: 'Pizza',
    quantity: 1,
    description: 'Pizza italiana',
    itemDetails: [
      { itemId: 1, quantity: 2, annotation: 'masa', isOptional: false }
    ]
  })
}).then(r => r.json()).then(data => console.log(data));
```

### Listar
```javascript
fetch('http://localhost:8081/api/admin/recipes', {
  headers: { 'Authorization': 'Bearer ' + token }
}).then(r => r.json()).then(data => console.log(data));
```

### Obtener
```javascript
fetch('http://localhost:8081/api/admin/recipes/1', {
  headers: { 'Authorization': 'Bearer ' + token }
}).then(r => r.json()).then(data => console.log(data));
```

### Actualizar
```javascript
fetch('http://localhost:8081/api/admin/recipes/1', {
  method: 'PUT',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  },
  body: JSON.stringify({...datos...})
}).then(r => r.json()).then(data => console.log(data));
```

### Eliminar
```javascript
fetch('http://localhost:8081/api/admin/recipes/1', {
  method: 'DELETE',
  headers: { 'Authorization': 'Bearer ' + token }
}).then(r => {
  if(r.ok) console.log('Eliminado');
});
```

---

## ⚠️ Errores Comunes

| Error | Causa | Solución |
|-------|-------|----------|
| 401 | Token inválido | Obtén nuevo token |
| 403 | No es ADMIN | Usa usuario admin |
| 404 | Receta no existe | Verifica ID |
| 400 | Datos inválidos | Revisa validaciones |
| Sin itemDetails | Array vacío | Agrega mínimo 1 item |

---

## ✅ Checklist Frontend

- [ ] Importar token desde localStorage
- [ ] Crear función para cada endpoint
- [ ] Validar datos antes de enviar
- [ ] Mostrar estados: cargando, éxito, error
- [ ] Recargar lista después de crear/actualizar/eliminar
- [ ] Manejar errores de red
- [ ] Manejar tokens expirados (401)
- [ ] Agregar confirmación antes de eliminar
- [ ] Mostrar detalles completos en edición

---

## 🔑 Variables Importantes

```javascript
const token = localStorage.getItem('authToken');
const baseUrl = 'http://localhost:8081';
const endpoint = '/api/admin/recipes';

// Uso
const url = `${baseUrl}${endpoint}`;
const headers = {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json'
};
```

---

## 📊 Estados HTTP

| Código | Significado | Acción |
|--------|-------------|--------|
| 201 | Creado | Mostrar éxito, recargar |
| 200 | OK | Mostrar datos/éxito |
| 204 | Sin contenido | Mostrar éxito, recargar |
| 400 | Solicitud mala | Mostrar error al usuario |
| 401 | No autorizado | Redirigir a login |
| 403 | Prohibido | Mostrar error de permisos |
| 404 | No encontrado | Mostrar recurso no existe |
| 500 | Error servidor | Contactar soporte |

---

## 🎯 Validaciones Requeridas

```javascript
// Antes de enviar
if (!forma.name || forma.name.trim() === '') throw 'Nombre requerido';
if (forma.quantity < 1) throw 'Cantidad debe ser > 0';
if (forma.itemDetails.length === 0) throw 'Mínimo 1 item';

// Para cada item
forma.itemDetails.forEach(item => {
  if (item.itemId < 1) throw 'ItemId inválido';
  if (item.quantity < 1) throw 'Cantidad debe ser > 0';
});
```

---

## 🔄 Flujo Típico

```javascript
// 1. Cargar recetas
async function cargar() {
  const recetas = await fetch('/api/admin/recipes').then(r => r.json());
  mostrarTabla(recetas);
}

// 2. Crear
async function crear(datos) {
  const nueva = await fetch('/api/admin/recipes', {
    method: 'POST',
    body: JSON.stringify(datos)
  }).then(r => r.json());
  cargar(); // Recargar tabla
}

// 3. Editar
async function editar(id, datos) {
  const actualizada = await fetch(`/api/admin/recipes/${id}`, {
    method: 'PUT',
    body: JSON.stringify(datos)
  }).then(r => r.json());
  cargar();
}

// 4. Eliminar
async function eliminar(id) {
  await fetch(`/api/admin/recipes/${id}`, {
    method: 'DELETE'
  });
  cargar();
}
```

---

## 📝 Headers Requeridos

```javascript
const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${token}`
};

// En fetch
fetch(url, {
  method: 'POST',
  headers: headers,
  body: JSON.stringify(datos)
});
```

---

## 🎨 UI Mínima Requerida

```html
<!-- Botón para crear -->
<button onclick="abrirFormulario()">+ Nueva</button>

<!-- Formulario -->
<form onsubmit="guardar(event)">
  <input name="name" required>
  <input name="quantity" type="number" min="1" required>
  <textarea name="description"></textarea>
  
  <!-- Items dinámicos -->
  <div id="items"></div>
  <button type="button" onclick="agregarItem()">+ Item</button>
  
  <button type="submit">Guardar</button>
</form>

<!-- Tabla -->
<table id="recetas">
  <thead>
    <tr>
      <th>Nombre</th>
      <th>Items</th>
      <th>Acciones</th>
    </tr>
  </thead>
  <tbody id="lista"></tbody>
</table>
```

---

## 🔗 Relación con Productos

```
Receta → Vinculada a → Producto
  ↓
  itemDetails
  ↓
  Items (inventario)
  ↓
  Stock
```

Cuando creas un producto, especificas una receta que define qué items necesita.

---

## 📚 Documentos Relacionados

1. **GUIA_INTEGRACION_RECETAS_FRONTEND.md** - Guía completa
2. **EJEMPLOS_PRACTICOS_RECETAS_FRONTEND.md** - Ejemplos con código
3. **PROMPT_RECETAS_PARA_FRONTEND.md** - Prompt detallado
4. **test-crud-recetas.http** - Tests HTTP
5. **FLUJO_VISUAL_CRUD_RECETAS.md** - Diagramas

---

## ⚡ Copy-Paste Ready

```javascript
const token = localStorage.getItem('authToken');
const base = 'http://localhost:8081';

// Crear
const crear = (datos) => fetch(`${base}/api/admin/recipes`, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify(datos)
}).then(r => r.json());

// Listar
const listar = () => fetch(`${base}/api/admin/recipes`, {
  headers: { 'Authorization': `Bearer ${token}` }
}).then(r => r.json());

// Obtener
const obtener = (id) => fetch(`${base}/api/admin/recipes/${id}`, {
  headers: { 'Authorization': `Bearer ${token}` }
}).then(r => r.json());

// Actualizar
const actualizar = (id, datos) => fetch(`${base}/api/admin/recipes/${id}`, {
  method: 'PUT',
  headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
  body: JSON.stringify(datos)
}).then(r => r.json());

// Eliminar
const eliminar = (id) => fetch(`${base}/api/admin/recipes/${id}`, {
  method: 'DELETE',
  headers: { 'Authorization': `Bearer ${token}` }
});

// Uso
listar().then(data => console.log(data));
```

---

¡Listo para copiar y usar! ✅

