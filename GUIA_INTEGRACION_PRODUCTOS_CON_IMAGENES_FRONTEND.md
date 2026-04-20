# Guía de Integración: Productos con Imágenes para el Frontend

## 📋 Resumen
Los endpoints del cliente ahora devuelven **productos con imágenes de Cloudinary**. Todos los endpoints públicos incluyen el campo `imageUrl`.

---

## 🔌 Endpoints Disponibles para Clientes

### 1. **Obtener todos los productos (sin paginación)**
```
GET /api/menu/products
```

**Respuesta:**
```json
[
  {
    "id": 1,
    "name": "Hamburguesa Clásica",
    "basePrice": 15.99,
    "description": "Hamburguesa de res con queso y lechuga",
    "imageUrl": "https://res.cloudinary.com/dvzmidfp6/image/upload/v1234567890/products/hamburguesa_abc123.jpg",
    "recipeItems": [
      {
        "id": 1,
        "itemId": 5,
        "itemName": "Pan de hamburguesa",
        "baseQuantity": 1,
        "annotation": null,
        "isOptional": false,
        "minQuantity": 1,
        "maxQuantity": 1
      },
      {
        "id": 2,
        "itemId": 6,
        "itemName": "Carne molida",
        "baseQuantity": 150,
        "annotation": "gramos",
        "isOptional": false,
        "minQuantity": 1,
        "maxQuantity": 3
      }
    ]
  },
  {
    "id": 2,
    "name": "Pizza Margherita",
    "basePrice": 22.50,
    "description": "Pizza con tomate, mozzarella y albahaca",
    "imageUrl": "https://res.cloudinary.com/dvzmidfp6/image/upload/v1234567890/products/pizza_xyz789.jpg",
    "recipeItems": [...]
  }
]
```

---

### 2. **Obtener productos con paginación**
```
GET /api/products/active?page=0&size=10&sort=id,asc
```

**Parámetros opcionales:**
- `page`: Número de página (comenzando desde 0)
- `size`: Cantidad de productos por página (default: 10)
- `sort`: Campo para ordenar (default: id,asc)

**Respuesta:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Hamburguesa Clásica",
      "basePrice": 15.99,
      "description": "Hamburguesa de res con queso y lechuga",
      "imageUrl": "https://res.cloudinary.com/dvzmidfp6/image/upload/v1234567890/products/hamburguesa_abc123.jpg",
      "recipeItems": [...]
    }
  ],
  "pageable": {
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 10,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalElements": 5,
  "totalPages": 1,
  "first": true,
  "numberOfElements": 5,
  "size": 10,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "empty": false
}
```

---

### 3. **Obtener un producto específico**
```
GET /api/menu/products/{id}
```

**Ejemplo:**
```
GET /api/menu/products/1
```

**Respuesta:**
```json
{
  "id": 1,
  "name": "Hamburguesa Clásica",
  "basePrice": 15.99,
  "description": "Hamburguesa de res con queso y lechuga",
  "imageUrl": "https://res.cloudinary.com/dvzmidfp6/image/upload/v1234567890/products/hamburguesa_abc123.jpg",
  "recipeItems": [
    {
      "id": 1,
      "itemId": 5,
      "itemName": "Pan de hamburguesa",
      "baseQuantity": 1,
      "annotation": null,
      "isOptional": false,
      "minQuantity": 1,
      "maxQuantity": 1
    }
  ]
}
```

---

## 💻 Ejemplos de Implementación en Frontend

### JavaScript/React

#### **Cargar lista de productos sin paginación:**
```javascript
async function cargarProductos() {
  try {
    const respuesta = await fetch('http://localhost:8081/api/menu/products');
    const productos = await respuesta.json();
    
    productos.forEach(producto => {
      mostrarProducto(producto);
    });
  } catch (error) {
    console.error('Error cargando productos:', error);
  }
}

function mostrarProducto(producto) {
  const card = document.createElement('div');
  card.className = 'producto-card';
  card.innerHTML = `
    <img src="${producto.imageUrl}" alt="${producto.name}" />
    <h3>${producto.name}</h3>
    <p>${producto.description}</p>
    <span class="precio">$${producto.basePrice.toFixed(2)}</span>
    <button onclick="agregarAlCarrito(${producto.id})">Añadir</button>
  `;
  document.getElementById('menu').appendChild(card);
}
```

#### **Cargar productos con paginación:**
```javascript
async function cargarProductosPaginados(pagina = 0, cantidad = 10) {
  try {
    const url = `http://localhost:8081/api/products/active?page=${pagina}&size=${cantidad}`;
    const respuesta = await fetch(url);
    const datos = await respuesta.json();
    
    // Mostrar productos
    datos.content.forEach(producto => mostrarProducto(producto));
    
    // Actualizar información de paginación
    console.log(`Página ${datos.number + 1} de ${datos.totalPages}`);
    console.log(`Total de productos: ${datos.totalElements}`);
  } catch (error) {
    console.error('Error:', error);
  }
}
```

#### **Cargar un producto específico:**
```javascript
async function cargarProductoDetalle(id) {
  try {
    const respuesta = await fetch(`http://localhost:8081/api/menu/products/${id}`);
    const producto = await respuesta.json();
    
    // Mostrar detalle con imagen
    document.getElementById('detalle').innerHTML = `
      <div class="producto-detalle">
        <img src="${producto.imageUrl}" alt="${producto.name}" class="imagen-principal"/>
        <h2>${producto.name}</h2>
        <p>${producto.description}</p>
        <p class="precio">$${producto.basePrice.toFixed(2)}</p>
        <div class="items">
          <h4>Componentes:</h4>
          ${producto.recipeItems.map(item => `
            <div class="item">
              <span>${item.itemName}</span>
              <span>${item.baseQuantity} ${item.annotation || ''}</span>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  } catch (error) {
    console.error('Error:', error);
  }
}
```

---

### React Hooks (Recomendado)

```javascript
import React, { useState, useEffect } from 'react';

function MenuProductos() {
  const [productos, setProductos] = useState([]);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    fetch('http://localhost:8081/api/menu/products')
      .then(res => res.json())
      .then(datos => {
        setProductos(datos);
        setCargando(false);
      })
      .catch(error => {
        console.error('Error:', error);
        setCargando(false);
      });
  }, []);

  if (cargando) return <div>Cargando menú...</div>;

  return (
    <div className="menu-grid">
      {productos.map(producto => (
        <div key={producto.id} className="producto-card">
          <img 
            src={producto.imageUrl} 
            alt={producto.name}
            onError={(e) => e.target.src = '/placeholder.jpg'}
          />
          <h3>{producto.name}</h3>
          <p>{producto.description}</p>
          <p className="precio">${producto.basePrice}</p>
          <button onClick={() => agregarAlCarrito(producto)}>
            Agregar al carrito
          </button>
        </div>
      ))}
    </div>
  );
}

export default MenuProductos;
```

---

## 🖼️ Notas Importantes sobre Imágenes

1. **URL de Cloudinary**: Todas las imágenes vienen desde Cloudinary con el formato:
   ```
   https://res.cloudinary.com/dvzmidfp6/image/upload/v{version}/products/{nombre}.jpg
   ```

2. **Validación de imágenes**: Siempre maneja el caso donde `imageUrl` sea `null`:
   ```javascript
   <img src={producto.imageUrl || '/placeholder.jpg'} />
   ```

3. **Optimización**: Puedes añadir parámetros a la URL de Cloudinary:
   ```javascript
   // Comprimir imagen
   const optimizedUrl = producto.imageUrl?.replace('/upload/', '/upload/w_300,q_80/');
   
   <img src={optimizedUrl} />
   ```

4. **Responsivo**:
   ```css
   .producto-card img {
     width: 100%;
     height: 250px;
     object-fit: cover;
     border-radius: 8px;
   }
   ```

---

## ✅ Cambios Realizados

✔️ Agregado campo `imageUrl` a `ProductWithRecipeDTO`  
✔️ Actualizado `ProductMenuService` para incluir la imagen  
✔️ Todos los endpoints de cliente ahora devuelven imágenes:
- `GET /api/menu/products`
- `GET /api/products/active?page=0&size=10`
- `GET /api/menu/products/{id}`

---

## 📞 Soporte

Para más detalles sobre los items y sus opciones de variación, consulta el campo `recipeItems` en cada producto.

