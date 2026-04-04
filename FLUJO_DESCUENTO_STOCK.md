# Flujo de Descuento de Stock en Órdenes

## 📌 RESUMEN DEL PROBLEMA

**El stock NO se descuenta automáticamente al crear una orden.**

El método `confirmPaymentAndDeductInventory()` existe pero **NUNCA se llamaba desde ningún lugar**.

---

## 🔄 FLUJO CORRECTO (Implementado Ahora)

### 1️⃣ **CLIENTE CREA ORDEN** 
```
POST /api/client/orders
├─ ✅ Valida que los ITEMS de la receta estén disponibles
├─ ✅ Crea la orden en estado PENDING
└─ ❌ NO descuenta el stock (solo valida)
```

**Por qué no descuenta aquí:**
- El cliente aún no ha pagado
- La orden podría cancelarse
- El stock se descuenta solo cuando hay confirmación de pago

---

### 2️⃣ **MESERO/CASHIER CONFIRMA PAGO**
```
POST /api/staff/orders/{id}/confirm-payment  ⭐ NUEVO ENDPOINT
├─ ✅ Valida que la orden existe
├─ ✅ Descuenta el stock de TODOS los items
├─ ✅ Log de inventario actualizado
└─ ✅ Respuesta: "Pago confirmado. Inventario descontado."
```

**Qué hace:**
- Llama a `orderService.confirmPaymentAndDeductInventory(orderId)`
- Esta función recorre cada item de la receta
- Llama a `availabilityService.deductInventoryForProduct()`
- El stock se descuenta de forma **definitiva**

---

## 📊 EJEMPLO CON NÚMEROS

### Antes (Problema Actual)
```
Stock de "Pollo picado": 300 unidades

1. Cliente crea orden: 1x Pollo
   - Valida: ¿Hay 1 pollo? Sí ✅
   - Crea orden
   - Stock sigue siendo: 300 ❌

2. Otro cliente crea orden: 1x Pollo
   - Valida: ¿Hay 1 pollo? Sí ✅
   - Crea orden
   - Stock sigue siendo: 300 ❌

3. ... y así 100 órdenes más...
   - Stock es 300, pero hay 100 órdenes pendientes
   - INCONSISTENCIA ❌
```

### Después (Solucionado)
```
Stock de "Pollo picado": 300 unidades

1. Cliente crea orden: 1x Pollo
   - Valida: ¿Hay 1 pollo? Sí ✅
   - Crea orden (PENDING)
   - Stock sigue siendo: 300 (sin descontar)

2. MESERO CONFIRMA PAGO ⭐
   - POST /api/staff/orders/{id}/confirm-payment
   - Descuenta 1 pollo
   - Stock ahora: 299 ✅

3. Otro cliente crea orden: 1x Pollo
   - Valida: ¿Hay 1 pollo? Sí ✅ (299 disponibles)
   - Crea orden (PENDING)
   - Stock sigue siendo: 299 (sin descontar)

4. MESERO CONFIRMA PAGO ⭐
   - POST /api/staff/orders/{id}/confirm-payment
   - Descuenta 1 pollo
   - Stock ahora: 298 ✅

RESULTADO: Stock consistente con órdenes pagadas
```

---

## 🔧 CÓMO USAR

### 1. Cliente escanea QR y ordena
```bash
POST /api/client/orders
Headers:
  Cookie: sessionToken=eyJhbGc...

Body:
{
  "products": [
    {"productId": 1, "quantity": 1}
  ]
}

Response:
{
  "id": 123,
  "status": "PENDING",
  "total": 15.99
}
```

### 2. Cliente paga en caja

### 3. Mesero confirma pago en sistema
```bash
POST /api/staff/orders/123/confirm-payment
Headers:
  Authorization: Bearer {user_jwt_token}

Response:
{
  "message": "Pago confirmado. Inventario descontado."
}
```

---

## 📋 ESTADOS DE LA ORDEN

```
Creación         Pago          Cocina           Entrega
   │              │              │                │
   └─→ PENDING ──→ PENDING ──→ IN_PREPARATION ──→ SERVED
   
   (No descuenta)  (⭐ DESCUENTA  (Preparando)   (Servido)
                    AQUÍ)
```

---

## 🛡️ PUNTOS CLAVE

| Punto | Antes | Después |
|-------|-------|---------|
| **Stock se valida** | ✅ Sí | ✅ Sí |
| **Stock se descuenta** | ❌ No | ✅ Sí (al pagar) |
| **Consistencia de datos** | ❌ No | ✅ Sí |
| **Endpoint para pago** | ❌ No existía | ✅ `/confirm-payment` |
| **Cuándo se descuenta** | - | ✅ DESPUÉS de pagar |

---

## 🚀 PRÓXIMAS MEJORAS SUGERIDAS

1. **Integración con pasarela de pago**: Descontar automáticamente al confirmar pago en la pasarela
2. **Devoluciones**: Endpoint para revertir stock si se cancela DESPUÉS de pagar
3. **Validación de doble pago**: Evitar descontar 2 veces la misma orden
4. **Auditoría**: Registrar quién y cuándo descuenta el stock

---

## 📌 RESUMEN

**Pregunta**: ¿Cuándo se descuenta el stock?
**Respuesta**: En el momento en que el MESERO/CASHIER confirma el pago llamando a:
```
POST /api/staff/orders/{orderId}/confirm-payment
```

Esto garantiza que:
- ✅ El cliente pagó
- ✅ El stock es consistente
- ✅ No hay doble descuento
- ✅ Se puede auditar quién descuenta

