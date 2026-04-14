# 🎯 Ejemplos Prácticos de CRUD de Recetas para Frontend

## Resumen Rápido de Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/admin/recipes` | Crear receta |
| GET | `/api/admin/recipes` | Obtener todas |
| GET | `/api/admin/recipes/{id}` | Obtener por ID |
| PUT | `/api/admin/recipes/{id}` | Actualizar |
| DELETE | `/api/admin/recipes/{id}` | Eliminar |

---

## 1️⃣ CREAR RECETA

### Ejemplo Simple
```javascript
async function crearReceta() {
  const token = localStorage.getItem('authToken');
  
  const receta = {
    name: "Hamburguesa",
    quantity: 1,
    description: "Hamburguesa deliciosa",
    itemDetails: [
      { itemId: 1, quantity: 2, annotation: "pan tostado", isOptional: false },
      { itemId: 2, quantity: 150, annotation: "carne", isOptional: false }
    ]
  };

  const response = await fetch('http://localhost:8081/api/admin/recipes', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(receta)
  });

  const resultado = await response.json();
  console.log('Receta creada:', resultado);
  return resultado;
}
```

### Respuesta
```json
{
  "id": 1,
  "name": "Hamburguesa",
  "quantity": 1,
  "description": "Hamburguesa deliciosa",
  "itemDetails": [
    {
      "id": 1,
      "itemId": 1,
      "itemName": "Pan",
      "quantity": 2,
      "annotation": "pan tostado",
      "isOptional": false
    },
    {
      "id": 2,
      "itemId": 2,
      "itemName": "Carne Molida",
      "quantity": 150,
      "annotation": "carne",
      "isOptional": false
    }
  ]
}
```

---

## 2️⃣ OBTENER TODAS LAS RECETAS

### Ejemplo Simple
```javascript
async function obtenerRecetas() {
  const token = localStorage.getItem('authToken');
  
  const response = await fetch('http://localhost:8081/api/admin/recipes', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  const recetas = await response.json();
  console.log('Recetas:', recetas);
  return recetas;
}
```

### Respuesta
```json
[
  {
    "id": 1,
    "name": "Hamburguesa",
    "quantity": 1,
    "description": "Hamburguesa deliciosa",
    "itemDetails": [...]
  },
  {
    "id": 2,
    "name": "Pizza",
    "quantity": 1,
    "description": "Pizza italiana",
    "itemDetails": [...]
  }
]
```

---

## 3️⃣ OBTENER RECETA POR ID

### Ejemplo Simple
```javascript
async function obtenerRecetaPorId(id) {
  const token = localStorage.getItem('authToken');
  
  const response = await fetch(`http://localhost:8081/api/admin/recipes/${id}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  const receta = await response.json();
  console.log('Receta:', receta);
  return receta;
}

// Uso
obtenerRecetaPorId(1);
```

### Respuesta
```json
{
  "id": 1,
  "name": "Hamburguesa",
  "quantity": 1,
  "description": "Hamburguesa deliciosa",
  "itemDetails": [
    {
      "id": 1,
      "itemId": 1,
      "itemName": "Pan",
      "quantity": 2,
      "annotation": "pan tostado",
      "isOptional": false,
      "minQuantity": 2,
      "maxQuantity": 2
    }
  ]
}
```

---

## 4️⃣ ACTUALIZAR RECETA

### Ejemplo Simple
```javascript
async function actualizarReceta(id) {
  const token = localStorage.getItem('authToken');
  
  const recetaActualizada = {
    name: "Hamburguesa Premium",
    quantity: 1,
    description: "Hamburguesa mejorada",
    itemDetails: [
      { itemId: 1, quantity: 2, annotation: "pan tostado", isOptional: false },
      { itemId: 2, quantity: 200, annotation: "carne premium", isOptional: false },
      { itemId: 3, quantity: 1, annotation: "queso", isOptional: true }
    ]
  };

  const response = await fetch(`http://localhost:8081/api/admin/recipes/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(recetaActualizada)
  });

  const resultado = await response.json();
  console.log('Receta actualizada:', resultado);
  return resultado;
}

// Uso
actualizarReceta(1);
```

### Respuesta
```json
{
  "id": 1,
  "name": "Hamburguesa Premium",
  "quantity": 1,
  "description": "Hamburguesa mejorada",
  "itemDetails": [...]
}
```

---

## 5️⃣ ELIMINAR RECETA

### Ejemplo Simple
```javascript
async function eliminarReceta(id) {
  const token = localStorage.getItem('authToken');
  
  const response = await fetch(`http://localhost:8081/api/admin/recipes/${id}`, {
    method: 'DELETE',
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  if (response.ok) {
    console.log('Receta eliminada exitosamente');
    return true;
  }
  return false;
}

// Uso
if (confirm('¿Eliminar receta?')) {
  eliminarReceta(1);
}
```

---

## 📝 Ejemplo Completo con Formulario (React)

```jsx
import React, { useState, useEffect } from 'react';

export function RecetaForm() {
  const [recetas, setRecetas] = useState([]);
  const [forma, setForma] = useState({
    name: '',
    quantity: 1,
    description: '',
    itemDetails: [{ itemId: '', quantity: '', annotation: '', isOptional: false }]
  });
  const [editandoId, setEditandoId] = useState(null);
  const token = localStorage.getItem('authToken');

  // Cargar recetas al iniciar
  useEffect(() => {
    cargarRecetas();
  }, []);

  // Cargar todas las recetas
  const cargarRecetas = async () => {
    const response = await fetch('http://localhost:8081/api/admin/recipes', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    setRecetas(await response.json());
  };

  // Manejar cambios en el formulario
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForma({ ...forma, [name]: value });
  };

  // Manejar cambios en itemDetails
  const handleItemChange = (index, field, value) => {
    const newItems = [...forma.itemDetails];
    newItems[index][field] = value;
    setForma({ ...forma, itemDetails: newItems });
  };

  // Agregar nuevo item al formulario
  const agregarItem = () => {
    setForma({
      ...forma,
      itemDetails: [
        ...forma.itemDetails,
        { itemId: '', quantity: '', annotation: '', isOptional: false }
      ]
    });
  };

  // Eliminar item del formulario
  const eliminarItemForm = (index) => {
    setForma({
      ...forma,
      itemDetails: forma.itemDetails.filter((_, i) => i !== index)
    });
  };

  // Crear o actualizar receta
  const guardarReceta = async (e) => {
    e.preventDefault();

    // Validaciones
    if (!forma.name.trim()) {
      alert('El nombre es obligatorio');
      return;
    }
    if (forma.itemDetails.length === 0) {
      alert('Debe tener al menos un item');
      return;
    }

    const url = editandoId
      ? `http://localhost:8081/api/admin/recipes/${editandoId}`
      : 'http://localhost:8081/api/admin/recipes';

    const metodo = editandoId ? 'PUT' : 'POST';

    // Preparar datos
    const datosReceta = {
      name: forma.name,
      quantity: parseInt(forma.quantity),
      description: forma.description,
      itemDetails: forma.itemDetails.map(item => ({
        itemId: parseInt(item.itemId),
        quantity: parseInt(item.quantity),
        annotation: item.annotation,
        isOptional: item.isOptional
      }))
    };

    const response = await fetch(url, {
      method: metodo,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(datosReceta)
    });

    if (response.ok) {
      alert(editandoId ? 'Receta actualizada' : 'Receta creada');
      setForma({
        name: '',
        quantity: 1,
        description: '',
        itemDetails: [{ itemId: '', quantity: '', annotation: '', isOptional: false }]
      });
      setEditandoId(null);
      cargarRecetas();
    } else {
      alert('Error al guardar receta');
    }
  };

  // Editar receta
  const editarReceta = async (id) => {
    const response = await fetch(`http://localhost:8081/api/admin/recipes/${id}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const receta = await response.json();
    setForma(receta);
    setEditandoId(id);
  };

  // Eliminar receta
  const eliminarRecetaConfirm = async (id) => {
    if (!window.confirm('¿Eliminar receta?')) return;

    const response = await fetch(`http://localhost:8081/api/admin/recipes/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    if (response.ok) {
      alert('Receta eliminada');
      cargarRecetas();
    }
  };

  return (
    <div className="receta-container">
      <h1>Gestión de Recetas</h1>

      {/* Formulario */}
      <form onSubmit={guardarReceta} className="receta-form">
        <h2>{editandoId ? 'Editar Receta' : 'Nueva Receta'}</h2>

        <div>
          <label>Nombre:</label>
          <input
            type="text"
            name="name"
            value={forma.name}
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label>Cantidad:</label>
          <input
            type="number"
            name="quantity"
            value={forma.quantity}
            onChange={handleChange}
            min="1"
          />
        </div>

        <div>
          <label>Descripción:</label>
          <textarea
            name="description"
            value={forma.description}
            onChange={handleChange}
          />
        </div>

        <h3>Items</h3>
        {forma.itemDetails.map((item, index) => (
          <div key={index} className="item-group">
            <input
              type="number"
              placeholder="ID Item"
              value={item.itemId}
              onChange={(e) => handleItemChange(index, 'itemId', e.target.value)}
              required
            />
            <input
              type="number"
              placeholder="Cantidad"
              value={item.quantity}
              onChange={(e) => handleItemChange(index, 'quantity', e.target.value)}
              required
            />
            <input
              type="text"
              placeholder="Anotación"
              value={item.annotation}
              onChange={(e) => handleItemChange(index, 'annotation', e.target.value)}
            />
            <label>
              Opcional:
              <input
                type="checkbox"
                checked={item.isOptional}
                onChange={(e) => handleItemChange(index, 'isOptional', e.target.checked)}
              />
            </label>
            <button
              type="button"
              onClick={() => eliminarItemForm(index)}
              className="btn-eliminar"
            >
              Eliminar
            </button>
          </div>
        ))}

        <button type="button" onClick={agregarItem} className="btn-agregar">
          + Agregar Item
        </button>

        <div className="form-actions">
          <button type="submit">{editandoId ? 'Actualizar' : 'Crear'}</button>
          <button
            type="button"
            onClick={() => {
              setForma({
                name: '',
                quantity: 1,
                description: '',
                itemDetails: [{ itemId: '', quantity: '', annotation: '', isOptional: false }]
              });
              setEditandoId(null);
            }}
          >
            Limpiar
          </button>
        </div>
      </form>

      {/* Tabla de Recetas */}
      <h2>Recetas Existentes</h2>
      <table className="receta-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Nombre</th>
            <th>Descripción</th>
            <th>Items</th>
            <th>Acciones</th>
          </tr>
        </thead>
        <tbody>
          {recetas.map(receta => (
            <tr key={receta.id}>
              <td>{receta.id}</td>
              <td>{receta.name}</td>
              <td>{receta.description}</td>
              <td>{receta.itemDetails.length}</td>
              <td>
                <button onClick={() => editarReceta(receta.id)} className="btn-editar">
                  Editar
                </button>
                <button
                  onClick={() => eliminarRecetaConfirm(receta.id)}
                  className="btn-eliminar"
                >
                  Eliminar
                </button>
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

## 🔐 Token de Autenticación

Para obtener un token:

```javascript
const obtenerToken = async () => {
  const response = await fetch('http://localhost:8081/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      email: 'admin@example.com',
      password: 'password123'
    })
  });

  const data = await response.json();
  localStorage.setItem('authToken', data.token);
  return data.token;
};

// Usar antes de cualquier operación
obtenerToken();
```

---

## ⚠️ Manejo de Errores

```javascript
const llamarAPI = async (url, opciones) => {
  try {
    const response = await fetch(url, opciones);
    
    if (!response.ok) {
      if (response.status === 401) {
        alert('Token expirado. Debes login nuevamente');
      } else if (response.status === 403) {
        alert('No tienes permisos');
      } else if (response.status === 404) {
        alert('Recurso no encontrado');
      } else {
        alert('Error en la solicitud');
      }
      return null;
    }
    
    return await response.json();
  } catch (error) {
    alert('Error de conexión: ' + error.message);
    return null;
  }
};
```

---

## 🚀 Próximos Pasos

1. Copia los ejemplos anteriores
2. Adapta a tu framework (React, Vue, Angular)
3. Prueba con tokens válidos
4. Integra con tu UI
5. Conecta con el módulo de productos

¡Listo para integrar! 🎉

