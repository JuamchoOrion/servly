# Ejemplos: Consumo de Productos con Imágenes en Diferentes Tecnologías

## 1️⃣ React Hooks - Crear Producto

```jsx
import React, { useState } from 'react';
import axios from 'axios';

function CrearProducto({ token }) {
  const [formData, setFormData] = useState({
    name: '',
    price: '',
    description: '',
    categoryId: '',
    active: true,
    recipeId: ''
  });
  const [imagen, setImagen] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState(null);
  const [productoCreado, setProductoCreado] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'active' ? value === 'true' : value
    }));
  };

  const handleImageChange = (e) => {
    setImagen(e.target.files[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setCargando(true);
    setError(null);

    try {
      const formDataEnvio = new FormData();
      Object.entries(formData).forEach(([key, value]) => {
        if (value !== null && value !== undefined) {
          formDataEnvio.append(key, value);
        }
      });
      
      if (imagen) {
        formDataEnvio.append('image', imagen);
      }

      const response = await axios.post(
        'http://localhost:8081/api/admin/products/with-image',
        formDataEnvio,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'multipart/form-data'
          }
        }
      );

      setProductoCreado(response.data);
      alert('¡Producto creado exitosamente!');
      
      // Limpiar formulario
      setFormData({
        name: '', price: '', description: '', categoryId: '', active: true, recipeId: ''
      });
      setImagen(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Error al crear el producto');
      console.error('Error:', err);
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="crear-producto">
      <h2>Crear Nuevo Producto</h2>
      
      {error && <div className="alert alert-danger">{error}</div>}
      {productoCreado && (
        <div className="alert alert-success">
          <p>✅ Producto creado: {productoCreado.name}</p>
          <img src={productoCreado.imageUrl} alt={productoCreado.name} style={{ width: '200px' }} />
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>Nombre</label>
          <input
            type="text"
            name="name"
            value={formData.name}
            onChange={handleChange}
            required
            placeholder="Pasta Carbonara"
          />
        </div>

        <div className="form-group">
          <label>Precio</label>
          <input
            type="number"
            step="0.01"
            name="price"
            value={formData.price}
            onChange={handleChange}
            required
            placeholder="12.99"
          />
        </div>

        <div className="form-group">
          <label>Descripción</label>
          <textarea
            name="description"
            value={formData.description}
            onChange={handleChange}
            required
            placeholder="Descripción del producto"
          />
        </div>

        <div className="form-group">
          <label>ID Categoría</label>
          <input
            type="number"
            name="categoryId"
            value={formData.categoryId}
            onChange={handleChange}
            required
          />
        </div>

        <div className="form-group">
          <label>Receta ID (Opcional)</label>
          <input
            type="number"
            name="recipeId"
            value={formData.recipeId}
            onChange={handleChange}
          />
        </div>

        <div className="form-group">
          <label>Imagen</label>
          <input
            type="file"
            accept="image/*"
            onChange={handleImageChange}
          />
          {imagen && <p>✅ Imagen seleccionada: {imagen.name}</p>}
        </div>

        <div className="form-group">
          <label>
            <input
              type="checkbox"
              name="active"
              checked={formData.active}
              onChange={(e) => setFormData(prev => ({ ...prev, active: e.target.checked }))}
            />
            Activo
          </label>
        </div>

        <button type="submit" disabled={cargando}>
          {cargando ? '⏳ Creando...' : '✅ Crear Producto'}
        </button>
      </form>
    </div>
  );
}

export default CrearProducto;
```

---

## 2️⃣ Vue 3 - Obtener y Mostrar Producto

```vue
<template>
  <div class="producto-viewer">
    <h2>Ver Producto</h2>
    
    <div v-if="cargando" class="spinner">Cargando...</div>
    <div v-if="error" class="alert alert-danger">{{ error }}</div>
    
    <div v-if="producto" class="producto-card">
      <img :src="producto.imageUrl" :alt="producto.name" class="producto-imagen">
      <div class="producto-info">
        <h3>{{ producto.name }}</h3>
        <p class="descripcion">{{ producto.description }}</p>
        <p class="precio">${{ producto.price }}</p>
        <span v-if="producto.active" class="badge badge-success">Activo</span>
        <span v-else class="badge badge-secondary">Inactivo</span>
      </div>
    </div>

    <input 
      v-model.number="productoId" 
      type="number" 
      placeholder="Ingresa ID del producto"
      @keyup.enter="obtenerProducto"
    >
    <button @click="obtenerProducto">Buscar Producto</button>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const productoId = ref(null);
const producto = ref(null);
const cargando = ref(false);
const error = ref(null);

const token = localStorage.getItem('authToken');

const obtenerProducto = async () => {
  if (!productoId.value) {
    error.value = 'Por favor ingresa un ID';
    return;
  }

  cargando.value = true;
  error.value = null;

  try {
    const response = await fetch(
      `http://localhost:8081/api/admin/products/${productoId.value}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );

    if (!response.ok) {
      throw new Error('Producto no encontrado');
    }

    producto.value = await response.json();
  } catch (err) {
    error.value = err.message;
    producto.value = null;
  } finally {
    cargando.value = false;
  }
};
</script>

<style scoped>
.producto-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  gap: 20px;
}

.producto-imagen {
  width: 300px;
  height: 300px;
  object-fit: cover;
  border-radius: 8px;
}

.producto-info {
  flex: 1;
}

.precio {
  font-size: 24px;
  font-weight: bold;
  color: #28a745;
}
</style>
```

---

## 3️⃣ Angular - Actualizar Producto con Imagen

```typescript
// producto.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProductoService {
  private apiUrl = 'http://localhost:8081/api/admin/products';

  constructor(private http: HttpClient) { }

  actualizarConImagen(
    id: number,
    datos: any,
    archivo?: File
  ): Observable<any> {
    const formData = new FormData();
    
    Object.entries(datos).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        formData.append(key, value as string);
      }
    });

    if (archivo) {
      formData.append('image', archivo);
    }

    const token = localStorage.getItem('authToken');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.put(
      `${this.apiUrl}/${id}/with-image`,
      formData,
      { headers }
    );
  }
}

// producto-editor.component.ts
import { Component, OnInit } from '@angular/core';
import { ProductoService } from './producto.service';

@Component({
  selector: 'app-producto-editor',
  templateUrl: './producto-editor.component.html',
  styleUrls: ['./producto-editor.component.css']
})
export class ProductoEditorComponent implements OnInit {
  imagenSeleccionada: File | null = null;
  datos = {
    name: 'Burger Premium',
    price: 10.99,
    description: 'Hamburguesa de calidad',
    active: true
  };
  cargando = false;

  constructor(private productoService: ProductoService) { }

  ngOnInit(): void { }

  onImagenSeleccionada(event: any) {
    this.imagenSeleccionada = event.target.files[0];
  }

  actualizar() {
    this.cargando = true;
    this.productoService.actualizarConImagen(5, this.datos, this.imagenSeleccionada)
      .subscribe({
        next: (response) => {
          alert('✅ Producto actualizado correctamente');
          console.log('Respuesta:', response);
          this.cargando = false;
        },
        error: (err) => {
          alert('❌ Error: ' + err.message);
          this.cargando = false;
        }
      });
  }
}
```

---

## 4️⃣ jQuery - Listar Productos

```html
<!DOCTYPE html>
<html>
<head>
  <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
  <style>
    .productos-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
      gap: 20px;
      padding: 20px;
    }
    .producto-card {
      border: 1px solid #ddd;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }
    .producto-card img {
      width: 100%;
      height: 200px;
      object-fit: cover;
    }
    .producto-info {
      padding: 15px;
    }
    .precio {
      font-size: 18px;
      font-weight: bold;
      color: #28a745;
    }
  </style>
</head>
<body>

<button id="btnCargar">Cargar Productos</button>
<div id="productosContainer" class="productos-grid"></div>

<script>
$(document).ready(function() {
  const token = localStorage.getItem('authToken');

  $('#btnCargar').click(function() {
    $.ajax({
      url: 'http://localhost:8081/api/admin/products?page=0&size=10',
      type: 'GET',
      headers: {
        'Authorization': 'Bearer ' + token
      },
      success: function(response) {
        mostrarProductos(response.content);
      },
      error: function(err) {
        alert('Error: ' + err.statusText);
      }
    });
  });

  function mostrarProductos(productos) {
    let html = '';
    
    productos.forEach(p => {
      html += `
        <div class="producto-card">
          <img src="${p.imageUrl}" alt="${p.name}">
          <div class="producto-info">
            <h3>${p.name}</h3>
            <p>${p.description}</p>
            <p class="precio">$${p.price}</p>
            <small>${p.active ? '✅ Activo' : '❌ Inactivo'}</small>
          </div>
        </div>
      `;
    });

    $('#productosContainer').html(html);
  }
});
</script>

</body>
</html>
```

---

## 5️⃣ TypeScript + Clase Service

```typescript
// models/producto.model.ts
export interface Producto {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  active: boolean;
  recipeId?: number;
  createdAt?: string;
}

export interface CreateProductoRequest {
  name: string;
  price: number;
  description: string;
  categoryId: number;
  active: boolean;
  recipeId?: number;
  imageUrl?: string;
}

// services/producto.service.ts
export class ProductoService {
  private apiUrl = 'http://localhost:8081/api/admin/products';
  private token: string;

  constructor(token: string) {
    this.token = token;
  }

  async crearConImagen(
    datos: CreateProductoRequest,
    archivo: File
  ): Promise<Producto> {
    const formData = new FormData();
    
    Object.entries(datos).forEach(([key, value]) => {
      formData.append(key, String(value));
    });
    formData.append('image', archivo);

    const response = await fetch(`${this.apiUrl}/with-image`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.token}`
      },
      body: formData
    });

    if (!response.ok) {
      throw new Error(`Error: ${response.statusText}`);
    }

    return response.json();
  }

  async obtener(id: number): Promise<Producto> {
    const response = await fetch(`${this.apiUrl}/${id}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });

    if (!response.ok) {
      throw new Error(`Error: ${response.statusText}`);
    }

    return response.json();
  }

  async actualizar(
    id: number,
    datos: Partial<Producto>,
    archivo?: File
  ): Promise<Producto> {
    const formData = new FormData();
    
    Object.entries(datos).forEach(([key, value]) => {
      if (value !== null && value !== undefined) {
        formData.append(key, String(value));
      }
    });

    if (archivo) {
      formData.append('image', archivo);
    }

    const response = await fetch(`${this.apiUrl}/${id}/with-image`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${this.token}`
      },
      body: formData
    });

    if (!response.ok) {
      throw new Error(`Error: ${response.statusText}`);
    }

    return response.json();
  }

  async listar(pagina: number = 0, cantidad: number = 10): Promise<{
    content: Producto[];
    totalPages: number;
    totalElements: number;
  }> {
    const response = await fetch(
      `${this.apiUrl}?page=${pagina}&size=${cantidad}`,
      {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${this.token}`
        }
      }
    );

    if (!response.ok) {
      throw new Error(`Error: ${response.statusText}`);
    }

    return response.json();
  }

  async eliminar(id: number): Promise<void> {
    const response = await fetch(`${this.apiUrl}/${id}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${this.token}`
      }
    });

    if (!response.ok) {
      throw new Error(`Error: ${response.statusText}`);
    }
  }
}

// Uso:
const token = localStorage.getItem('authToken')!;
const productoService = new ProductoService(token);

// Crear
const nuevoProducto = await productoService.crearConImagen(
  {
    name: 'Pizza Margarita',
    price: 15.99,
    description: 'Pizza clásica italiana',
    categoryId: 1,
    active: true,
    recipeId: 1
  },
  archivoImagen
);

console.log('✅ Producto creado:', nuevoProducto);
console.log('Imagen:', nuevoProducto.imageUrl);
```

---

## 6️⃣ Python + Requests

```python
import requests
from requests_toolbelt.multipart.encoder import MultipartEncoder

class ProductoService:
    def __init__(self, token):
        self.base_url = "http://localhost:8081/api/admin/products"
        self.token = token
        self.headers = {
            "Authorization": f"Bearer {token}"
        }

    def crear_con_imagen(self, datos: dict, archivo_path: str) -> dict:
        """Crear producto con imagen"""
        with open(archivo_path, 'rb') as f:
            files = {
                'image': (archivo_path, f, 'image/jpeg')
            }
            data = {k: str(v) for k, v in datos.items()}
            
            response = requests.post(
                f"{self.base_url}/with-image",
                data=data,
                files=files,
                headers=self.headers
            )
        
        response.raise_for_status()
        return response.json()

    def obtener(self, producto_id: int) -> dict:
        """Obtener producto por ID"""
        response = requests.get(
            f"{self.base_url}/{producto_id}",
            headers=self.headers
        )
        response.raise_for_status()
        return response.json()

    def actualizar_con_imagen(
        self, 
        producto_id: int, 
        datos: dict, 
        archivo_path: str = None
    ) -> dict:
        """Actualizar producto"""
        files = {}
        if archivo_path:
            files['image'] = (archivo_path, open(archivo_path, 'rb'), 'image/jpeg')
        
        data = {k: str(v) for k, v in datos.items()}
        
        response = requests.put(
            f"{self.base_url}/{producto_id}/with-image",
            data=data,
            files=files,
            headers=self.headers
        )
        response.raise_for_status()
        return response.json()

    def listar(self, pagina: int = 0, cantidad: int = 10) -> dict:
        """Listar productos"""
        response = requests.get(
            f"{self.base_url}?page={pagina}&size={cantidad}",
            headers=self.headers
        )
        response.raise_for_status()
        return response.json()

# Uso:
token = "tu_token_aqui"
servicio = ProductoService(token)

# Crear producto
nuevo = servicio.crear_con_imagen(
    {
        'name': 'Sushi Roll',
        'price': 18.99,
        'description': 'Sushi fresco',
        'categoryId': 2,
        'active': True,
        'recipeId': 5
    },
    '/ruta/a/imagen.jpg'
)

print("✅ Producto creado:", nuevo)
print("URL Imagen:", nuevo['imageUrl'])
```

---

## 7️⃣ Postman - Colección JSON

```json
{
  "info": {
    "name": "Productos API",
    "description": "Endpoints para CRUD de productos con imágenes"
  },
  "item": [
    {
      "name": "Crear Producto con Imagen",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8081/api/admin/products/with-image",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8081",
          "path": ["api", "admin", "products", "with-image"]
        },
        "body": {
          "mode": "formdata",
          "formdata": [
            {
              "key": "name",
              "value": "Pasta Carbonara",
              "type": "text"
            },
            {
              "key": "price",
              "value": "12.99",
              "type": "text"
            },
            {
              "key": "description",
              "value": "Pasta clásica italiana",
              "type": "text"
            },
            {
              "key": "categoryId",
              "value": "1",
              "type": "text"
            },
            {
              "key": "active",
              "value": "true",
              "type": "text"
            },
            {
              "key": "recipeId",
              "value": "1",
              "type": "text"
            },
            {
              "key": "image",
              "type": "file",
              "src": []
            }
          ]
        }
      }
    },
    {
      "name": "Obtener Producto",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{token}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8081/api/admin/products/5",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8081",
          "path": ["api", "admin", "products", "5"]
        }
      }
    }
  ]
}
```

---

¡Todos los ejemplos están listos para copiar y usar directamente en tus proyectos! 🎉

