# Guía: Gestión de Productos con Imágenes en Cloudinary

## 📋 Resumen
Los productos se pueden crear, actualizar y obtener con imágenes almacenadas en **Cloudinary**. Las imágenes se suben automáticamente cuando se envían en la solicitud.

---

## 1️⃣ CREAR PRODUCTO CON IMAGEN

### Endpoint
```
POST /api/admin/products/with-image
```

### Parámetros (FormData)
| Parámetro | Tipo | Obligatorio | Descripción |
|-----------|------|------------|-------------|
| `name` | String | ✅ | Nombre del producto |
| `price` | BigDecimal | ✅ | Precio del producto |
| `description` | String | ✅ | Descripción del producto |
| `categoryId` | Long | ✅ | ID de la categoría |
| `active` | Boolean | ✅ | Si está activo o no |
| `recipeId` | Long | ❌ | ID de la receta asociada |
| `image` | File | ❌ | Archivo de imagen (JPG, PNG, etc) |

### Ejemplo con JavaScript (Fetch)
```javascript
const formData = new FormData();
formData.append('name', 'Pasta Carbonara');
formData.append('price', '12.99');
formData.append('description', 'Pasta clásica italiana');
formData.append('categoryId', '1');
formData.append('active', 'true');
formData.append('recipeId', '1');
formData.append('image', fileInputElement.files[0]); // <input type="file" />

const response = await fetch('http://localhost:8081/api/admin/products/with-image', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
});

const product = await response.json();
console.log('Producto creado:', product);
// Response:
// {
//   "id": 5,
//   "name": "Pasta Carbonara",
//   "price": 12.99,
//   "description": "Pasta clásica italiana",
//   "imageUrl": "https://res.cloudinary.com/dvzmidfp6/image/upload/v1713408000/products/abc123.jpg",
//   "active": true,
//   "recipeId": 1
// }
```

### Ejemplo con Axios
```javascript
const createProductWithImage = async (productData, imageFile, token) => {
  const formData = new FormData();
  formData.append('name', productData.name);
  formData.append('price', productData.price);
  formData.append('description', productData.description);
  formData.append('categoryId', productData.categoryId);
  formData.append('active', productData.active);
  formData.append('recipeId', productData.recipeId);
  formData.append('image', imageFile);

  const response = await axios.post(
    'http://localhost:8081/api/admin/products/with-image',
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    }
  );

  return response.data;
};

// Uso:
const producto = await createProductWithImage(
  {
    name: 'Burger Clásica',
    price: 8.99,
    description: 'Hamburguesa con queso',
    categoryId: 2,
    active: true,
    recipeId: 2
  },
  event.target.files[0],
  token
);
```

### Ejemplo con cURL
```bash
curl -X POST http://localhost:8081/api/admin/products/with-image \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "name=Pasta Carbonara" \
  -F "price=12.99" \
  -F "description=Pasta clásica italiana" \
  -F "categoryId=1" \
  -F "active=true" \
  -F "recipeId=1" \
  -F "image=@/ruta/a/imagen.jpg"
```

---

## 2️⃣ OBTENER PRODUCTO (CON IMAGEN)

### Endpoint
```
GET /api/admin/products/{id}
```

### Parámetros
| Parámetro | Tipo | Ubicación | Descripción |
|-----------|------|-----------|-------------|
| `id` | Long | Path | ID del producto |

### Ejemplo con JavaScript
```javascript
const getProduct = async (productId, token) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/products/${productId}`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );

  const product = await response.json();
  console.log('Producto obtenido:', product);
  
  // Response:
  // {
  //   "id": 5,
  //   "name": "Pasta Carbonara",
  //   "price": 12.99,
  //   "description": "Pasta clásica italiana",
  //   "imageUrl": "https://res.cloudinary.com/dvzmidfp6/image/upload/v1713408000/products/abc123.jpg",
  //   "active": true,
  //   "recipeId": 1,
  //   "createdAt": "2024-04-16T10:30:00"
  // }
};

// Uso:
const producto = await getProduct(5, token);

// Mostrar imagen en HTML:
const img = document.createElement('img');
img.src = producto.imageUrl;
img.alt = producto.name;
document.getElementById('product-image').appendChild(img);
```

### Ejemplo con Axios
```javascript
const getProduct = async (productId, token) => {
  const response = await axios.get(
    `http://localhost:8081/api/admin/products/${productId}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );

  return response.data;
};
```

---

## 3️⃣ ACTUALIZAR PRODUCTO CON IMAGEN

### Endpoint
```
PUT /api/admin/products/{id}/with-image
```

### Parámetros (FormData)
| Parámetro | Tipo | Obligatorio | Descripción |
|-----------|------|------------|-------------|
| `name` | String | ❌ | Nuevo nombre |
| `price` | BigDecimal | ❌ | Nuevo precio |
| `description` | String | ❌ | Nueva descripción |
| `active` | Boolean | ❌ | Cambiar estado |
| `image` | File | ❌ | Nueva imagen (reemplaza la anterior) |

### Ejemplo con JavaScript
```javascript
const updateProductWithImage = async (productId, updateData, newImage, token) => {
  const formData = new FormData();
  
  // Solo agregar campos que se quieren actualizar
  if (updateData.name) formData.append('name', updateData.name);
  if (updateData.price) formData.append('price', updateData.price);
  if (updateData.description) formData.append('description', updateData.description);
  if (updateData.active !== undefined) formData.append('active', updateData.active);
  if (newImage) formData.append('image', newImage);

  const response = await fetch(
    `http://localhost:8081/api/admin/products/${productId}/with-image`,
    {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    }
  );

  const updatedProduct = await response.json();
  console.log('Producto actualizado:', updatedProduct);
  // Response: Producto con la nueva imagen URL
};

// Uso - Actualizar solo el nombre y la imagen:
await updateProductWithImage(
  5,
  { name: 'Pasta Carbonara Premium' },
  event.target.files[0],
  token
);

// Uso - Actualizar precio y cambiar imagen:
await updateProductWithImage(
  5,
  { price: '14.99' },
  imagenNueva,
  token
);
```

### Ejemplo con Axios
```javascript
const updateProductWithImage = async (productId, updateData, newImage, token) => {
  const formData = new FormData();
  
  Object.entries(updateData).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      formData.append(key, value);
    }
  });
  
  if (newImage) formData.append('image', newImage);

  const response = await axios.put(
    `http://localhost:8081/api/admin/products/${productId}/with-image`,
    formData,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    }
  );

  return response.data;
};
```

---

## 4️⃣ LISTAR PRODUCTOS (TODOS)

### Endpoint
```
GET /api/admin/products?page=0&size=10
```

### Parámetros (Query)
| Parámetro | Tipo | Descripción |
|-----------|------|-------------|
| `page` | Integer | Número de página (comienza en 0) |
| `size` | Integer | Cantidad de registros por página |

### Ejemplo con JavaScript
```javascript
const getAllProducts = async (page = 0, size = 10, token) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/products?page=${page}&size=${size}`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );

  const data = await response.json();
  console.log('Productos:', data);
  
  // Response:
  // {
  //   "content": [
  //     {
  //       "id": 1,
  //       "name": "Pasta Carbonara",
  //       "price": 12.99,
  //       "imageUrl": "https://res.cloudinary.com/...",
  //       "active": true
  //     },
  //     // ... más productos
  //   ],
  //   "totalElements": 25,
  //   "totalPages": 3,
  //   "currentPage": 0
  // }
};

// Uso:
const productos = await getAllProducts(0, 10, token);

// Mostrar en tabla o grid
productos.content.forEach(producto => {
  const html = `
    <div class="producto-card">
      <img src="${producto.imageUrl}" alt="${producto.name}">
      <h3>${producto.name}</h3>
      <p>$${producto.price}</p>
    </div>
  `;
  // Agregar al DOM
});
```

---

## 5️⃣ LISTAR PRODUCTOS ACTIVOS

### Endpoint
```
GET /api/admin/products/active?page=0&size=10
```

### Ejemplo con JavaScript
```javascript
const getActiveProducts = async (page = 0, size = 10, token) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/products/active?page=${page}&size=${size}`,
    {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );

  const data = await response.json();
  return data.content; // Solo productos activos
};
```

---

## 6️⃣ ELIMINAR PRODUCTO

### Endpoint
```
DELETE /api/admin/products/{id}
```

### Ejemplo con JavaScript
```javascript
const deleteProduct = async (productId, token) => {
  const response = await fetch(
    `http://localhost:8081/api/admin/products/${productId}`,
    {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );

  const result = await response.json();
  console.log(result.message); // "Producto eliminado correctamente"
};
```

---

## 📸 Notas sobre Cloudinary

✅ **Las imágenes se almacenan en Cloudinary**
- Cloud Name: `dvzmidfp6`
- Carpeta: `products` (para productos)
- URL retornada: Segura y permanente
- Formato: HTTPS

✅ **La URL se devuelve automáticamente**
- La respuesta incluye `imageUrl` con la URL completa
- Puedes usar esta URL directamente en `<img>` tags
- La imagen se almacena indefinidamente

✅ **Actualizaciones de imagen**
- Al actualizar con una nueva imagen, la antigua se reemplaza
- La URL es diferente para cada subida

---

## 🔐 Autenticación
Todos los endpoints requieren:
- Header: `Authorization: Bearer {token}`
- El token se obtiene en el login

---

## ✨ Ejemplo Completo: Formulario en React

```jsx
import React, { useState } from 'react';
import axios from 'axios';

function ProductForm({ token }) {
  const [formData, setFormData] = useState({
    name: '',
    price: '',
    description: '',
    categoryId: '',
    active: true,
    recipeId: ''
  });
  const [image, setImage] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleImageChange = (e) => {
    setImage(e.target.files[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const submitData = new FormData();
      Object.entries(formData).forEach(([key, value]) => {
        submitData.append(key, value);
      });
      if (image) submitData.append('image', image);

      const response = await axios.post(
        'http://localhost:8081/api/admin/products/with-image',
        submitData,
        {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'multipart/form-data'
          }
        }
      );

      console.log('Producto creado:', response.data);
      alert('Producto creado exitosamente');
      // Limpiar formulario
      setFormData({ name: '', price: '', description: '', categoryId: '', active: true, recipeId: '' });
      setImage(null);
    } catch (error) {
      console.error('Error:', error);
      alert('Error al crear el producto');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <input
        type="text"
        name="name"
        placeholder="Nombre"
        value={formData.name}
        onChange={handleInputChange}
        required
      />
      <input
        type="number"
        step="0.01"
        name="price"
        placeholder="Precio"
        value={formData.price}
        onChange={handleInputChange}
        required
      />
      <textarea
        name="description"
        placeholder="Descripción"
        value={formData.description}
        onChange={handleInputChange}
        required
      />
      <input
        type="number"
        name="categoryId"
        placeholder="ID Categoría"
        value={formData.categoryId}
        onChange={handleInputChange}
        required
      />
      <input
        type="file"
        accept="image/*"
        onChange={handleImageChange}
      />
      <button type="submit" disabled={loading}>
        {loading ? 'Subiendo...' : 'Crear Producto'}
      </button>
    </form>
  );
}

export default ProductForm;
```

---

## 🐛 Solución de Problemas

| Problema | Solución |
|----------|----------|
| Error 401 | Token inválido o expirado, haz login nuevamente |
| Error 400 | Parámetros faltantes o inválidos |
| Error 413 | Archivo muy grande, máximo 10MB |
| Error 500 | Contacta con el servidor, Cloudinary puede estar down |
| Imagen no se sube | Verifica que no esté vacía y sea un formato válido |

---

## 📝 Resumen de Endpoints

| Método | Endpoint | Descripción |
|--------|----------|------------|
| POST | `/api/admin/products/with-image` | Crear con imagen |
| GET | `/api/admin/products/{id}` | Obtener producto |
| PUT | `/api/admin/products/{id}/with-image` | Actualizar con imagen |
| GET | `/api/admin/products?page=0&size=10` | Listar todos |
| GET | `/api/admin/products/active?page=0&size=10` | Listar activos |
| DELETE | `/api/admin/products/{id}` | Eliminar |


