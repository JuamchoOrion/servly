# Ejemplos Prácticos - Integración Mesas y Órdenes

## 🚀 Ejemplos en JavaScript/Fetch

### Cliente - Flujo Completo

#### 1. Abrir Sesión (Escanear QR)

```javascript
// Cuando el cliente abre la página después de escanear QR
async function openTableSession(tableNumber) {
  try {
    const response = await fetch(`/api/client/session?table=${tableNumber}`);
    const data = await response.json();
    
    console.log('Sesión abierta:', data);
    // {
    //   sessionToken: "eyJ...",
    //   tableNumber: 5,
    //   expiresIn: 14400,
    //   message: "Sesión abierta para mesa 5"
    // }
    
    // La cookie se establece automáticamente
    // Puedes acceder al número de mesa desde la cookie
    const tableFromCookie = getCookie('tableNumber');
    console.log('Mesa:', tableFromCookie);
    
    return data;
  } catch (error) {
    console.error('Error al abrir sesión:', error);
  }
}

function getCookie(name) {
  const nameEQ = name + "=";
  const cookies = document.cookie.split(';');
  for (let cookie of cookies) {
    cookie = cookie.trim();
    if (cookie.indexOf(nameEQ) === 0) {
      return cookie.substring(nameEQ.length);
    }
  }
  return null;
}
```

#### 2. Crear Primer Pedido

```javascript
async function createOrder(items, paymentMethod = 'CASH') {
  try {
    const response = await fetch('/api/client/orders', {
      method: 'POST',
      credentials: 'include', // ⭐ IMPORTANTE: incluir cookies
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        items: items,
        paymentMethod: paymentMethod
      })
    });
    
    if (!response.ok) {
      throw new Error(`Error: ${response.statusText}`);
    }
    
    const order = await response.json();
    console.log('Orden creada:', order);
    
    return order;
  } catch (error) {
    console.error('Error al crear orden:', error);
    throw error;
  }
}

// Ejemplo de uso
const itemsToOrder = [
  {
    id: 1,
    quantity: 2,
    notes: "Sin picante"
  },
  {
    id: 3,
    quantity: 1,
    notes: ""
  }
];

createOrder(itemsToOrder, 'CARD');
```

#### 3. Consultar Mis Órdenes

```javascript
async function getMyOrders() {
  try {
    const response = await fetch('/api/client/orders', {
      method: 'GET',
      credentials: 'include' // ⭐ Incluir cookies
    });
    
    const orders = await response.json();
    console.log('Mis órdenes:', orders);
    
    // Mostrar en la UI
    displayOrders(orders);
    
    return orders;
  } catch (error) {
    console.error('Error al obtener órdenes:', error);
  }
}

function displayOrders(orders) {
  const orderList = document.getElementById('orderList');
  orderList.innerHTML = '';
  
  orders.forEach(order => {
    const orderElement = document.createElement('div');
    orderElement.className = 'order-card';
    orderElement.innerHTML = `
      <h3>Orden #${order.id}</h3>
      <p>Estado: <span class="status-${order.status}">${order.status}</span></p>
      <p>Total: $${order.total.toFixed(2)}</p>
      <p>Fecha: ${new Date(order.createdAt).toLocaleString('es-CO')}</p>
      <button onclick="viewOrderDetails(${order.id})">Ver Detalles</button>
    `;
    orderList.appendChild(orderElement);
  });
}
```

#### 4. Ver Detalles de Orden

```javascript
async function getOrderDetails(orderId) {
  try {
    const response = await fetch(`/api/client/orders/${orderId}`, {
      method: 'GET',
      credentials: 'include'
    });
    
    const order = await response.json();
    console.log('Detalles de orden:', order);
    
    displayOrderDetails(order);
    
    return order;
  } catch (error) {
    console.error('Error al obtener detalles:', error);
  }
}

function displayOrderDetails(order) {
  const detailsDiv = document.getElementById('orderDetails');
  
  let itemsHTML = order.items.map(item => `
    <tr>
      <td>${item.name}</td>
      <td>${item.quantity}</td>
      <td>$${item.price.toFixed(2)}</td>
      <td>${item.notes || '-'}</td>
    </tr>
  `).join('');
  
  detailsDiv.innerHTML = `
    <h2>Orden #${order.id}</h2>
    <p>Estado: <strong>${order.status}</strong></p>
    <table>
      <thead>
        <tr>
          <th>Producto</th>
          <th>Cantidad</th>
          <th>Precio</th>
          <th>Notas</th>
        </tr>
      </thead>
      <tbody>
        ${itemsHTML}
      </tbody>
    </table>
    <div class="totals">
      <p>Subtotal: $${order.subtotal.toFixed(2)}</p>
      <p>IVA: $${order.tax.toFixed(2)}</p>
      <p><strong>Total: $${order.total.toFixed(2)}</strong></p>
    </div>
    <button onclick="requestHelp(${order.id})">Solicitar Ayuda</button>
    <button onclick="confirmDelivery(${order.id})">Confirmar Entrega</button>
  `;
}
```

#### 5. Solicitar Ayuda al Mesero

```javascript
async function requestHelp(orderId) {
  try {
    const message = prompt('¿Qué necesita? (opcional):');
    
    const response = await fetch(`/api/client/orders/${orderId}/request-help`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        message: message || ''
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      showNotification('✓ Mesero notificado. Llegará en breve.', 'success');
    }
    
    return result;
  } catch (error) {
    console.error('Error al solicitar ayuda:', error);
    showNotification('Error al notificar al mesero', 'error');
  }
}
```

#### 6. Confirmar Entrega de Orden

```javascript
async function confirmDelivery(orderId) {
  try {
    // Mostrar modal de calificación
    const rating = await showRatingModal();
    if (rating === null) return; // Usuario canceló
    
    const response = await fetch(`/api/client/orders/${orderId}/confirm-delivery`, {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        rating: rating,
        feedback: document.getElementById('feedbackText').value || ''
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      showNotification('✓ Gracias por su calificación', 'success');
      // Recargar órdenes
      getMyOrders();
    }
    
    return result;
  } catch (error) {
    console.error('Error al confirmar entrega:', error);
    showNotification('Error al confirmar entrega', 'error');
  }
}

async function showRatingModal() {
  return new Promise((resolve) => {
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
      <div class="modal-content">
        <h3>¿Cómo fue tu experiencia?</h3>
        <div class="rating-stars">
          ${[1,2,3,4,5].map(i => `
            <button class="star" data-rating="${i}" onclick="selectRating(${i})">⭐</button>
          `).join('')}
        </div>
        <textarea id="feedbackText" placeholder="Déjanos tu comentario (opcional)"></textarea>
        <div class="modal-buttons">
          <button onclick="confirmRating(${rating})">Enviar</button>
          <button onclick="closeModal()">Cancelar</button>
        </div>
      </div>
    `;
    
    document.body.appendChild(modal);
  });
}
```

---

### Mesero - Flujo Completo

#### 1. Autenticarse (Login)

```javascript
async function loginMesero(email, password) {
  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        email: email,
        password: password
      })
    });
    
    if (!response.ok) {
      throw new Error('Credenciales inválidas');
    }
    
    const data = await response.json();
    
    // Guardar token
    localStorage.setItem('token', data.token);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(data.user));
    
    console.log('Mesero autenticado:', data.user.name);
    
    // Redirigir a panel
    window.location.href = '/staff/dashboard';
    
    return data;
  } catch (error) {
    console.error('Error de login:', error);
    showNotification('Error de autenticación', 'error');
    throw error;
  }
}

// Uso:
// loginMesero('mesero@restaurante.com', 'contraseña123');
```

#### 2. Cargar Todas las Mesas

```javascript
async function loadTables() {
  try {
    const token = localStorage.getItem('token');
    
    const response = await fetch('/api/staff/tables', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    if (!response.ok) {
      if (response.status === 401) {
        // Token expiró
        redirectToLogin();
        return;
      }
      throw new Error('Error al cargar mesas');
    }
    
    const tables = await response.json();
    console.log('Mesas cargadas:', tables);
    
    displayTablesGrid(tables);
    
    return tables;
  } catch (error) {
    console.error('Error:', error);
    showNotification('Error al cargar mesas', 'error');
  }
}

function displayTablesGrid(tables) {
  const container = document.getElementById('tablesContainer');
  container.innerHTML = '';
  
  tables.forEach(table => {
    const tableCard = document.createElement('div');
    tableCard.className = `table-card status-${table.status}`;
    tableCard.innerHTML = `
      <div class="table-number">${table.number}</div>
      <div class="table-status">${table.status}</div>
      <div class="table-capacity">👥 ${table.capacity}</div>
      <div class="active-orders">📋 ${table.activeOrders}</div>
      ${table.totalBill > 0 ? `<div class="total-bill">$${table.totalBill.toFixed(2)}</div>` : ''}
      <button onclick="viewTableOrders(${table.number})">Ver Órdenes</button>
    `;
    container.appendChild(tableCard);
  });
}
```

#### 3. Consultar Órdenes de una Mesa

```javascript
async function getTableOrders(tableNumber) {
  try {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`/api/staff/tables/${tableNumber}/orders`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const orders = await response.json();
    console.log(`Órdenes de mesa ${tableNumber}:`, orders);
    
    displayTableOrders(tableNumber, orders);
    
    return orders;
  } catch (error) {
    console.error('Error:', error);
    showNotification('Error al cargar órdenes', 'error');
  }
}

function displayTableOrders(tableNumber, orders) {
  const modal = document.createElement('div');
  modal.className = 'modal';
  
  const ordersHTML = orders.map(order => `
    <div class="order-item">
      <div class="order-id">Orden #${order.id}</div>
      <div class="order-status">
        <span class="status-badge status-${order.status}">${order.status}</span>
      </div>
      <div class="order-items">
        ${order.items.map(item => `
          <div class="item">
            ${item.name} x${item.quantity}
            ${item.notes ? `<span class="notes">(${item.notes})</span>` : ''}
          </div>
        `).join('')}
      </div>
      <div class="order-total">$${order.total.toFixed(2)}</div>
      <div class="order-time">${new Date(order.createdAt).toLocaleTimeString('es-CO')}</div>
      <div class="order-actions">
        <button onclick="updateOrderStatus(${order.id}, '${getNextStatus(order.status)}')">
          Siguiente Estado
        </button>
      </div>
    </div>
  `).join('');
  
  modal.innerHTML = `
    <div class="modal-content">
      <h2>Órdenes - Mesa ${tableNumber}</h2>
      <div class="orders-list">
        ${ordersHTML}
      </div>
      <button onclick="this.closest('.modal').remove()">Cerrar</button>
    </div>
  `;
  
  document.body.appendChild(modal);
}

function getNextStatus(currentStatus) {
  const transitions = {
    'PENDING': 'IN_PREPARATION',
    'IN_PREPARATION': 'SERVED',
    'SERVED': 'PAID'
  };
  return transitions[currentStatus] || currentStatus;
}
```

#### 4. Actualizar Estado de Orden

```javascript
async function updateOrderStatus(orderId, newStatus) {
  try {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`/api/staff/orders/${orderId}/status`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        status: newStatus
      })
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    const result = await response.json();
    
    showNotification(`✓ Orden actualizada a ${newStatus}`, 'success');
    
    // Recargar mesas
    loadTables();
    
    return result;
  } catch (error) {
    console.error('Error:', error);
    showNotification(`Error: ${error.message}`, 'error');
  }
}
```

#### 5. Generar Factura

```javascript
async function generateInvoice(orderId) {
  try {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`/api/staff/orders/${orderId}/invoice`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const invoice = await response.json();
    console.log('Factura:', invoice);
    
    displayInvoice(invoice);
    
    return invoice;
  } catch (error) {
    console.error('Error:', error);
    showNotification('Error al generar factura', 'error');
  }
}

function displayInvoice(invoice) {
  const modal = document.createElement('div');
  modal.className = 'modal';
  
  const itemsHTML = invoice.items.map(item => `
    <tr>
      <td>${item.description}</td>
      <td>$${item.price.toFixed(2)}</td>
    </tr>
  `).join('');
  
  modal.innerHTML = `
    <div class="modal-content invoice">
      <h2>Factura #${invoice.invoiceNumber}</h2>
      <p>Mesa: ${invoice.tableNumber}</p>
      <p>Fecha: ${new Date(invoice.date).toLocaleString('es-CO')}</p>
      
      <table>
        <tr>
          <th>Descripción</th>
          <th>Precio</th>
        </tr>
        ${itemsHTML}
      </table>
      
      <div class="invoice-totals">
        <p>Subtotal: $${invoice.subtotal.toFixed(2)}</p>
        <p>IVA: $${invoice.tax.toFixed(2)}</p>
        <p><strong>Total: $${invoice.total.toFixed(2)}</strong></p>
      </div>
      
      <div class="invoice-actions">
        <button onclick="window.open('${invoice.pdfUrl}')">Descargar PDF</button>
        <button onclick="printInvoice()">Imprimir</button>
        <button onclick="confirmPayment(${invoice.orderId})">Confirmar Pago</button>
        <button onclick="this.closest('.modal').remove()">Cerrar</button>
      </div>
    </div>
  `;
  
  document.body.appendChild(modal);
}
```

#### 6. Confirmar Pago

```javascript
async function confirmPayment(orderId) {
  try {
    const token = localStorage.getItem('token');
    
    // Mostrar opciones de pago
    const paymentMethod = await showPaymentModal();
    if (!paymentMethod) return; // Usuario canceló
    
    const response = await fetch(`/api/staff/orders/${orderId}/confirm-payment`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        paymentMethod: paymentMethod.method,
        amount: paymentMethod.amount,
        tip: paymentMethod.tip || 0
      })
    });
    
    if (!response.ok) {
      throw new Error('Error al procesar pago');
    }
    
    const result = await response.json();
    
    showNotification(`✓ Pago de $${result.totalPaid.toFixed(2)} confirmado`, 'success');
    
    // Recargar mesas
    loadTables();
    
    return result;
  } catch (error) {
    console.error('Error:', error);
    showNotification('Error al confirmar pago', 'error');
  }
}

async function showPaymentModal() {
  return new Promise((resolve) => {
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
      <div class="modal-content">
        <h3>Confirmar Pago</h3>
        <label>
          Método de Pago:
          <select id="paymentMethod">
            <option value="CASH">Efectivo</option>
            <option value="CARD">Tarjeta</option>
            <option value="QR_PAYMENT">QR Payment</option>
          </select>
        </label>
        <label>
          Monto:
          <input type="number" id="amount" step="0.01" placeholder="0.00">
        </label>
        <label>
          Propina:
          <input type="number" id="tip" step="0.01" placeholder="0.00" value="0">
        </label>
        <div class="modal-buttons">
          <button onclick="submitPayment()">Confirmar</button>
          <button onclick="this.closest('.modal').remove()">Cancelar</button>
        </div>
      </div>
    `;
    
    window.submitPayment = () => {
      const method = document.getElementById('paymentMethod').value;
      const amount = parseFloat(document.getElementById('amount').value);
      const tip = parseFloat(document.getElementById('tip').value) || 0;
      
      if (!amount || amount <= 0) {
        alert('Ingrese un monto válido');
        return;
      }
      
      resolve({
        method: method,
        amount: amount,
        tip: tip
      });
      
      modal.remove();
    };
    
    document.body.appendChild(modal);
  });
}
```

#### 7. Cerrar Sesión de Mesa

```javascript
async function closeTableSession(tableNumber) {
  try {
    if (!confirm(`¿Cerrar sesión de mesa ${tableNumber}?`)) {
      return;
    }
    
    const token = localStorage.getItem('token');
    
    const response = await fetch(`/api/staff/tables/${tableNumber}/session`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    
    const result = await response.json();
    
    if (result.success) {
      showNotification(`✓ Mesa ${tableNumber} cerrada`, 'success');
      loadTables();
    }
    
    return result;
  } catch (error) {
    console.error('Error:', error);
    showNotification('Error al cerrar sesión', 'error');
  }
}
```

---

## 🛠️ Funciones Auxiliares

```javascript
function showNotification(message, type = 'info') {
  const notification = document.createElement('div');
  notification.className = `notification notification-${type}`;
  notification.textContent = message;
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 15px 20px;
    background: ${type === 'success' ? '#4CAF50' : type === 'error' ? '#f44336' : '#2196F3'};
    color: white;
    border-radius: 4px;
    z-index: 9999;
    animation: slideIn 0.3s ease;
  `;
  
  document.body.appendChild(notification);
  
  setTimeout(() => {
    notification.style.animation = 'slideOut 0.3s ease';
    setTimeout(() => notification.remove(), 300);
  }, 3000);
}

function redirectToLogin() {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  window.location.href = '/login';
}

function getAuthToken() {
  return localStorage.getItem('token');
}

function isLoggedIn() {
  return !!localStorage.getItem('token');
}

function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  window.location.href = '/login';
}
```

---

## 📱 Ejemplo Completo de Página Cliente

```html
<!DOCTYPE html>
<html>
<head>
  <title>Menú de Mesa</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      max-width: 800px;
      margin: 0 auto;
      padding: 20px;
    }
    
    .order-card {
      border: 1px solid #ddd;
      border-radius: 8px;
      padding: 15px;
      margin: 10px 0;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    
    .status-PENDING { background-color: #fff3cd; }
    .status-IN_PREPARATION { background-color: #cfe2ff; }
    .status-SERVED { background-color: #d1e7dd; }
    .status-PAID { background-color: #f8d7da; }
    
    button {
      background-color: #007bff;
      color: white;
      padding: 10px 20px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
    
    button:hover {
      background-color: #0056b3;
    }
  </style>
</head>
<body>
  <h1>🍽️ Menú - Mesa <span id="tableNumber"></span></h1>
  
  <div id="orderList"></div>
  <button onclick="location.href='/menu'">➕ Hacer Nuevo Pedido</button>
  <button onclick="logout()">🚪 Salir</button>
  
  <script>
    // Cuando carga la página
    window.addEventListener('load', async () => {
      const tableNumber = getCookie('tableNumber');
      document.getElementById('tableNumber').textContent = tableNumber;
      
      // Cargar órdenes cada 5 segundos
      await getMyOrders();
      setInterval(getMyOrders, 5000);
    });
  </script>
</body>
</html>
```

Este documento proporciona ejemplos prácticos listos para usar en el frontend.


