# Ejemplos de Llamadas API - cURL y JavaScript

## Tabla de Contenidos
1. [Autenticación](#autenticación)
2. [Crear Estructura Base](#crear-estructura-base)
3. [Órdenes Sin Variaciones](#órdenes-sin-variaciones)
4. [Órdenes Con Variaciones](#órdenes-con-variaciones)
5. [Flujo Completo](#flujo-completo)

---

## Autenticación

### cURL - Obtener Token

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

**Guardar el token en variable de ambiente:**

```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
API_URL="http://localhost:8081"
```

### JavaScript - Login y Guardar Token

```javascript
async function login() {
  const response = await fetch('http://localhost:8081/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin',
      password: 'password123'
    })
  });

  const data = await response.json();
  localStorage.setItem('token', data.token);
  return data.token;
}

// Usar el token en solicitudes posteriores
async function apiCall(method, endpoint, body = null) {
  const token = localStorage.getItem('token');
  
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  };

  if (body) {
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`http://localhost:8081${endpoint}`, options);
  return response.json();
}
```

---

## Crear Estructura Base

### 1. Crear Mesas

**cURL:**
```bash
# Mesa #5
curl -X POST $API_URL/api/restaurant/tables \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tableNumber": 5,
    "capacity": 4,
    "location": "Entrada"
  }'

# Mesa #10
curl -X POST $API_URL/api/restaurant/tables \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tableNumber": 10,
    "capacity": 6,
    "location": "Fondo"
  }'
```

**JavaScript:**
```javascript
async function createTables() {
  const tables = [
    { tableNumber: 5, capacity: 4, location: "Entrada" },
    { tableNumber: 10, capacity: 6, location: "Fondo" }
  ];

  for (const table of tables) {
    const response = await apiCall('POST', '/api/restaurant/tables', table);
    console.log(`Mesa creada:`, response);
  }
}
```

### 2. Crear Categorías

**cURL:**
```bash
curl -X POST $API_URL/api/products/categories \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hamburguesas",
    "description": "Hamburguesas caseras"
  }'
```

**JavaScript:**
```javascript
async function createCategories() {
  const category = {
    name: "Hamburguesas",
    description: "Hamburguesas caseras"
  };

  const response = await apiCall('POST', '/api/products/categories', category);
  console.log(`Categoría creada:`, response);
  return response.id;
}
```

### 3. Crear Items de Inventario

**cURL:**
```bash
# Carne Molida
curl -X POST $API_URL/api/inventory/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Carne Molida",
    "description": "Carne molida para hamburguesas",
    "measurementUnit": "GRAM",
    "currentStock": 10000,
    "minStock": 500,
    "maxStock": 20000
  }'

# Queso Cheddar
curl -X POST $API_URL/api/inventory/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Queso Cheddar",
    "description": "Queso cheddar para hamburguesas",
    "measurementUnit": "SLICE",
    "currentStock": 500,
    "minStock": 10,
    "maxStock": 1000
  }'

# Lechuga
curl -X POST $API_URL/api/inventory/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Lechuga",
    "description": "Lechuga fresca",
    "measurementUnit": "LEAF",
    "currentStock": 1000,
    "minStock": 50,
    "maxStock": 2000
  }'
```

**JavaScript:**
```javascript
async function createItems() {
  const items = [
    {
      name: "Carne Molida",
      description: "Carne molida para hamburguesas",
      measurementUnit: "GRAM",
      currentStock: 10000,
      minStock: 500,
      maxStock: 20000
    },
    {
      name: "Queso Cheddar",
      description: "Queso cheddar para hamburguesas",
      measurementUnit: "SLICE",
      currentStock: 500,
      minStock: 10,
      maxStock: 1000
    },
    {
      name: "Lechuga",
      description: "Lechuga fresca",
      measurementUnit: "LEAF",
      currentStock: 1000,
      minStock: 50,
      maxStock: 2000
    }
  ];

  const itemIds = {};
  for (const item of items) {
    const response = await apiCall('POST', '/api/inventory/items', item);
    itemIds[item.name] = response.id;
    console.log(`Item creado: ${item.name} (ID: ${response.id})`);
  }
  return itemIds;
}
```

### 4. Crear Recetas

**cURL - Receta Simple:**
```bash
curl -X POST $API_URL/api/products/recipes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hamburguesa Simple",
    "description": "Hamburguesa con queso y lechuga",
    "itemDetails": [
      {
        "itemId": 1,
        "quantity": 150,
        "isOptional": false,
        "minQuantity": 100,
        "maxQuantity": 200
      },
      {
        "itemId": 2,
        "quantity": 1,
        "isOptional": false,
        "minQuantity": 0,
        "maxQuantity": 2
      },
      {
        "itemId": 3,
        "quantity": 2,
        "isOptional": false,
        "minQuantity": 1,
        "maxQuantity": 5
      }
    ]
  }'
```

**cURL - Receta Premium (Personalizable):**
```bash
curl -X POST $API_URL/api/products/recipes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hamburguesa Premium",
    "description": "Hamburguesa personalizable",
    "itemDetails": [
      {
        "itemId": 1,
        "quantity": 200,
        "isOptional": false,
        "minQuantity": 150,
        "maxQuantity": 300
      },
      {
        "itemId": 2,
        "quantity": 1,
        "isOptional": true,
        "minQuantity": 0,
        "maxQuantity": 3
      },
      {
        "itemId": 3,
        "quantity": 2,
        "isOptional": true,
        "minQuantity": 0,
        "maxQuantity": 5
      }
    ]
  }'
```

**JavaScript:**
```javascript
async function createRecipes(itemIds) {
  const recipes = [
    {
      name: "Hamburguesa Simple",
      description: "Hamburguesa con queso y lechuga",
      itemDetails: [
        {
          itemId: itemIds["Carne Molida"],
          quantity: 150,
          isOptional: false,
          minQuantity: 100,
          maxQuantity: 200
        },
        {
          itemId: itemIds["Queso Cheddar"],
          quantity: 1,
          isOptional: false,
          minQuantity: 0,
          maxQuantity: 2
        },
        {
          itemId: itemIds["Lechuga"],
          quantity: 2,
          isOptional: false,
          minQuantity: 1,
          maxQuantity: 5
        }
      ]
    },
    {
      name: "Hamburguesa Premium",
      description: "Hamburguesa personalizable",
      itemDetails: [
        {
          itemId: itemIds["Carne Molida"],
          quantity: 200,
          isOptional: false,
          minQuantity: 150,
          maxQuantity: 300
        },
        {
          itemId: itemIds["Queso Cheddar"],
          quantity: 1,
          isOptional: true,
          minQuantity: 0,
          maxQuantity: 3
        },
        {
          itemId: itemIds["Lechuga"],
          quantity: 2,
          isOptional: true,
          minQuantity: 0,
          maxQuantity: 5
        }
      ]
    }
  ];

  const recipeIds = {};
  for (const recipe of recipes) {
    const response = await apiCall('POST', '/api/products/recipes', recipe);
    recipeIds[recipe.name] = response.id;
    console.log(`Receta creada: ${recipe.name} (ID: ${response.id})`);
  }
  return recipeIds;
}
```

### 5. Crear Productos

**cURL - Bebida (Sin Receta):**
```bash
curl -X POST $API_URL/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Coca Cola",
    "description": "Refresco gaseoso",
    "price": 2.50,
    "productCategoryId": 1,
    "active": true
  }'
```

**cURL - Hamburguesa Simple (Con Receta Simple):**
```bash
curl -X POST $API_URL/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hamburguesa Simple",
    "description": "Hamburguesa con queso y lechuga",
    "price": 8.99,
    "productCategoryId": 1,
    "recipeId": 1,
    "active": true
  }'
```

**cURL - Hamburguesa Premium (Con Receta Personalizable):**
```bash
curl -X POST $API_URL/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hamburguesa Premium",
    "description": "Hamburguesa personalizable",
    "price": 12.99,
    "productCategoryId": 1,
    "recipeId": 2,
    "active": true
  }'
```

**JavaScript:**
```javascript
async function createProducts(recipeIds, categoryId) {
  const products = [
    {
      name: "Coca Cola",
      description: "Refresco gaseoso",
      price: 2.50,
      productCategoryId: categoryId,
      active: true
    },
    {
      name: "Hamburguesa Simple",
      description: "Hamburguesa con queso y lechuga",
      price: 8.99,
      productCategoryId: categoryId,
      recipeId: recipeIds["Hamburguesa Simple"],
      active: true
    },
    {
      name: "Hamburguesa Premium",
      description: "Hamburguesa personalizable",
      price: 12.99,
      productCategoryId: categoryId,
      recipeId: recipeIds["Hamburguesa Premium"],
      active: true
    }
  ];

  const productIds = {};
  for (const product of products) {
    const response = await apiCall('POST', '/api/products', product);
    productIds[product.name] = response.id;
    console.log(`Producto creado: ${product.name} (ID: ${response.id})`);
  }
  return productIds;
}
```

---

## Órdenes Sin Variaciones

### Crear Orden Mesa #5 Sin Variaciones

**cURL:**
```bash
curl -X POST $API_URL/api/orders/table \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tableNumber": 5,
    "items": [
      {
        "productId": 2,
        "quantity": 2,
        "itemQuantityOverrides": null
      },
      {
        "productId": 1,
        "quantity": 2,
        "itemQuantityOverrides": null
      }
    ]
  }'
```

**JavaScript:**
```javascript
async function createOrderTableSimple(productIds) {
  const order = {
    tableNumber: 5,
    items: [
      {
        productId: productIds["Hamburguesa Simple"],
        quantity: 2,
        itemQuantityOverrides: null
      },
      {
        productId: productIds["Coca Cola"],
        quantity: 2,
        itemQuantityOverrides: null
      }
    ]
  };

  const response = await apiCall('POST', '/api/orders/table', order);
  console.log('Orden creada:', response);
  return response.id;
}
```

### Obtener Órdenes de Mesa #5

**cURL:**
```bash
curl -X GET "$API_URL/api/orders/table/5" \
  -H "Authorization: Bearer $TOKEN"
```

**JavaScript:**
```javascript
async function getOrdersByTable(tableNumber) {
  const response = await apiCall('GET', `/api/orders/table/${tableNumber}`);
  console.log(`Órdenes de mesa ${tableNumber}:`, response);
  return response;
}
```

### Actualizar Estado: PENDING → IN_PREPARATION

**cURL:**
```bash
curl -X PATCH "$API_URL/api/orders/1/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_PREPARATION"
  }'
```

**JavaScript:**
```javascript
async function updateOrderStatus(orderId, newStatus) {
  const response = await apiCall('PATCH', `/api/orders/${orderId}/status`, {
    status: newStatus
  });
  console.log(`Orden ${orderId} actualizada a ${newStatus}:`, response);
  return response;
}

// Uso:
// await updateOrderStatus(1, "IN_PREPARATION");
// await updateOrderStatus(1, "SERVED");
// await updateOrderStatus(1, "PAID");
```

### Confirmar Pago

**cURL:**
```bash
curl -X POST "$API_URL/api/orders/1/confirm-payment" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**JavaScript:**
```javascript
async function confirmPayment(orderId) {
  const response = await apiCall('POST', `/api/orders/${orderId}/confirm-payment`);
  console.log(`Pago confirmado para orden ${orderId}:`, response);
  return response;
}
```

---

## Órdenes Con Variaciones

### Crear Orden Mesa #10 Con Variaciones

**cURL:**
```bash
curl -X POST $API_URL/api/orders/table \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tableNumber": 10,
    "items": [
      {
        "productId": 3,
        "quantity": 1,
        "itemQuantityOverrides": {
          "2": 2,
          "3": 3
        }
      }
    ]
  }'
```

**JavaScript:**
```javascript
async function createOrderTableWithVariations(productIds, itemIds) {
  const order = {
    tableNumber: 10,
    items: [
      {
        productId: productIds["Hamburguesa Premium"],
        quantity: 1,
        itemQuantityOverrides: {
          [itemIds["Queso Cheddar"]]: 2,  // Doble queso
          [itemIds["Lechuga"]]: 3         // Triple lechuga
        }
      }
    ]
  };

  const response = await apiCall('POST', '/api/orders/table', order);
  console.log('Orden con variaciones creada:', response);
  return response.id;
}
```

### Crear Orden Delivery Con Variaciones

**cURL:**
```bash
curl -X POST $API_URL/api/orders/delivery \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "María González",
    "address": "Av. Principal 123",
    "phoneNumber": "+57 312 789 0123",
    "deliveryTime": "2026-04-03T20:00:00",
    "items": [
      {
        "productId": 3,
        "quantity": 2,
        "itemQuantityOverrides": {
          "2": 2,
          "3": 3
        }
      }
    ]
  }'
```

**JavaScript:**
```javascript
async function createOrderDeliveryWithVariations(productIds, itemIds) {
  const order = {
    clientName: "María González",
    address: "Av. Principal 123",
    phoneNumber: "+57 312 789 0123",
    deliveryTime: "2026-04-03T20:00:00",
    items: [
      {
        productId: productIds["Hamburguesa Premium"],
        quantity: 2,
        itemQuantityOverrides: {
          [itemIds["Queso Cheddar"]]: 2,  // 2 quesos por hamburguesa
          [itemIds["Lechuga"]]: 3         // 3 lechugas por hamburguesa
        }
      }
    ]
  };

  const response = await apiCall('POST', '/api/orders/delivery', order);
  console.log('Orden delivery con variaciones creada:', response);
  return response.id;
}
```

---

## Flujo Completo

### Script Completo de Integración

```javascript
// CONFIGURACIÓN
const API_URL = 'http://localhost:8081';
let token = '';

// PASO 1: LOGIN
async function setupAuth() {
  const response = await fetch(`${API_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: 'admin',
      password: 'password123'
    })
  });

  const data = await response.json();
  token = data.token;
  console.log('✓ Autenticado');
  return token;
}

// LLAMADA API GENÉRICA
async function apiCall(method, endpoint, body = null) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  };

  if (body) options.body = JSON.stringify(body);

  const response = await fetch(`${API_URL}${endpoint}`, options);
  if (!response.ok) {
    const error = await response.json();
    console.error(`Error en ${endpoint}:`, error);
    throw error;
  }
  return response.json();
}

// EJECUTAR FLUJO COMPLETO
async function runCompleteFlow() {
  try {
    console.log('🚀 Iniciando flujo completo...\n');

    // 1. Autenticarse
    await setupAuth();

    // 2. Crear mesas
    console.log('\n📍 Creando mesas...');
    const table1 = await apiCall('POST', '/api/restaurant/tables', {
      tableNumber: 5,
      capacity: 4,
      location: 'Entrada'
    });
    const table2 = await apiCall('POST', '/api/restaurant/tables', {
      tableNumber: 10,
      capacity: 6,
      location: 'Fondo'
    });
    console.log(`✓ Mesas creadas (IDs: ${table1.id}, ${table2.id})`);

    // 3. Crear categoría
    console.log('\n📂 Creando categoría...');
    const category = await apiCall('POST', '/api/products/categories', {
      name: 'Hamburguesas',
      description: 'Hamburguesas caseras'
    });
    console.log(`✓ Categoría creada (ID: ${category.id})`);

    // 4. Crear items
    console.log('\n📦 Creando items de inventario...');
    const items = {};
    const itemList = [
      { name: 'Carne Molida', unit: 'GRAM', stock: 10000 },
      { name: 'Queso Cheddar', unit: 'SLICE', stock: 500 },
      { name: 'Lechuga', unit: 'LEAF', stock: 1000 }
    ];

    for (const item of itemList) {
      const created = await apiCall('POST', '/api/inventory/items', {
        name: item.name,
        description: `${item.name} para hamburguesas`,
        measurementUnit: item.unit,
        currentStock: item.stock,
        minStock: 10,
        maxStock: item.stock * 2
      });
      items[item.name] = created.id;
    }
    console.log(`✓ Items creados:`, Object.keys(items));

    // 5. Crear recetas
    console.log('\n📋 Creando recetas...');
    const simpleRecipe = await apiCall('POST', '/api/products/recipes', {
      name: 'Hamburguesa Simple',
      description: 'Hamburguesa con queso y lechuga',
      itemDetails: [
        { itemId: items['Carne Molida'], quantity: 150, isOptional: false, minQuantity: 100, maxQuantity: 200 },
        { itemId: items['Queso Cheddar'], quantity: 1, isOptional: false, minQuantity: 0, maxQuantity: 2 },
        { itemId: items['Lechuga'], quantity: 2, isOptional: false, minQuantity: 1, maxQuantity: 5 }
      ]
    });

    const premiumRecipe = await apiCall('POST', '/api/products/recipes', {
      name: 'Hamburguesa Premium',
      description: 'Hamburguesa personalizable',
      itemDetails: [
        { itemId: items['Carne Molida'], quantity: 200, isOptional: false, minQuantity: 150, maxQuantity: 300 },
        { itemId: items['Queso Cheddar'], quantity: 1, isOptional: true, minQuantity: 0, maxQuantity: 3 },
        { itemId: items['Lechuga'], quantity: 2, isOptional: true, minQuantity: 0, maxQuantity: 5 }
      ]
    });
    console.log(`✓ Recetas creadas (IDs: ${simpleRecipe.id}, ${premiumRecipe.id})`);

    // 6. Crear productos
    console.log('\n🍔 Creando productos...');
    const cocaCola = await apiCall('POST', '/api/products', {
      name: 'Coca Cola',
      description: 'Refresco gaseoso',
      price: 2.50,
      productCategoryId: category.id,
      active: true
    });

    const simpleBurger = await apiCall('POST', '/api/products', {
      name: 'Hamburguesa Simple',
      description: 'Hamburguesa con queso y lechuga',
      price: 8.99,
      productCategoryId: category.id,
      recipeId: simpleRecipe.id,
      active: true
    });

    const premiumBurger = await apiCall('POST', '/api/products', {
      name: 'Hamburguesa Premium',
      description: 'Hamburguesa personalizable',
      price: 12.99,
      productCategoryId: category.id,
      recipeId: premiumRecipe.id,
      active: true
    });
    console.log(`✓ Productos creados (IDs: ${cocaCola.id}, ${simpleBurger.id}, ${premiumBurger.id})`);

    // 7. Crear orden sin variaciones (Mesa #5)
    console.log('\n📝 Creando orden sin variaciones (Mesa #5)...');
    const order1 = await apiCall('POST', '/api/orders/table', {
      tableNumber: 5,
      items: [
        { productId: simpleBurger.id, quantity: 2, itemQuantityOverrides: null },
        { productId: cocaCola.id, quantity: 2, itemQuantityOverrides: null }
      ]
    });
    console.log(`✓ Orden creada (ID: ${order1.id}, Total: $${order1.total})`);

    // 8. Crear orden con variaciones (Mesa #10)
    console.log('\n📝 Creando orden con variaciones (Mesa #10)...');
    const order2 = await apiCall('POST', '/api/orders/table', {
      tableNumber: 10,
      items: [
        {
          productId: premiumBurger.id,
          quantity: 1,
          itemQuantityOverrides: {
            [items['Queso Cheddar']]: 2,
            [items['Lechuga']]: 3
          }
        }
      ]
    });
    console.log(`✓ Orden con variaciones creada (ID: ${order2.id}, Total: $${order2.total})`);

    // 9. Procesar pago - Orden #1
    console.log(`\n💳 Procesando pago Orden #${order1.id}...`);
    await apiCall('PATCH', `/api/orders/${order1.id}/status`, { status: 'IN_PREPARATION' });
    console.log('  → Estado: IN_PREPARATION');
    
    await apiCall('PATCH', `/api/orders/${order1.id}/status`, { status: 'SERVED' });
    console.log('  → Estado: SERVED (inventario descuento)');
    
    const paidOrder1 = await apiCall('POST', `/api/orders/${order1.id}/confirm-payment`);
    console.log(`✓ Pago confirmado: ${paidOrder1.status}`);

    // 10. Procesar pago - Orden #2
    console.log(`\n💳 Procesando pago Orden #${order2.id}...`);
    await apiCall('PATCH', `/api/orders/${order2.id}/status`, { status: 'IN_PREPARATION' });
    console.log('  → Estado: IN_PREPARATION');
    
    await apiCall('PATCH', `/api/orders/${order2.id}/status`, { status: 'SERVED' });
    console.log('  → Estado: SERVED (inventario descuento con variaciones)');
    
    const paidOrder2 = await apiCall('POST', `/api/orders/${order2.id}/confirm-payment`);
    console.log(`✓ Pago confirmado: ${paidOrder2.status}`);

    // 11. Resumen final
    console.log('\n✅ FLUJO COMPLETADO CON ÉXITO\n');
    console.log('📊 Resumen:');
    console.log(`  • Orden #${order1.id}: ${paidOrder1.status} - $${paidOrder1.total}`);
    console.log(`  • Orden #${order2.id}: ${paidOrder2.status} - $${paidOrder2.total}`);

  } catch (error) {
    console.error('❌ Error en el flujo:', error);
  }
}

// Ejecutar
runCompleteFlow();
```

---

## Testing con Postman

### Colección Postman

Importa esta colección en Postman para probar todos los endpoints:

```json
{
  "info": {
    "name": "Servly API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Login",
          "request": {
            "method": "POST",
            "url": "{{API_URL}}/api/auth/login",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"username\": \"admin\",\n  \"password\": \"password123\"\n}"
            }
          }
        }
      ]
    }
  ]
}
```

---

## Notas Importantes

1. **Token JWT**: Reemplaza `$TOKEN` con tu token real después de login
2. **IDs Dinámicos**: Los IDs generados pueden variar, ajusta según tu ejecución
3. **CORS**: Si tienes problemas de CORS, verifica la configuración en el backend
4. **Validación**: El sistema valida inventario antes de crear órdenes
5. **Transacciones**: Las transiciones de estado están validadas en el backend


