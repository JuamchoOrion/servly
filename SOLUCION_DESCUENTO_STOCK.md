# 🔴 PROBLEMA: Stock No Se Descuenta en Órdenes

## Situación Actual

### El Problema
Tienes 300 unidades de "Pollo picado" en stock, pero al crear múltiples órdenes el stock se sigue mostrando como 300.

```
Stock inicial: 300 unidades

Creas orden 1: 1x Pollo  → Valida ✅ → Stock sigue siendo 300 ❌
Creas orden 2: 1x Pollo  → Valida ✅ → Stock sigue siendo 300 ❌
Creas orden 3: 1x Pollo  → Valida ✅ → Stock sigue siendo 300 ❌
...
Creas orden 100: 1x Pollo → Valida ✅ → Stock sigue siendo 300 ❌

Resultado: 100 órdenes pendientes pero stock sigue siendo 300 🤦
```

---

## Causa Raíz

En `OrderService.java` hay un método que **EXISTE pero NUNCA SE LLAMA**:

```java
public void confirmPaymentAndDeductInventory(Long orderId) {
    // Este método descuenta el stock
    // PERO NUNCA SE LLAMABA DESDE NINGÚN LUGAR ❌
}
```

### El Flujo Incompleto

```
Cuando creas orden:
  1. ✅ Valida que haya items en stock
  2. ❌ NO descuenta el stock
  3. ❌ No hay endpoint para descontar después
```

---

## Solución Implementada

### Nuevo Endpoint Agregado

```http
POST /api/staff/orders/{orderId}/confirm-payment
Authorization: Bearer {user_jwt_token}
```

**Qué hace:**
1. Valida que la orden existe
2. Recorre cada item de la receta
3. Descuenta del inventario
4. Registra en logs

**Respuesta:**
```json
{
  "message": "Pago confirmado. Inventario descontado."
}
```

---

## Nuevo Flujo de Órdenes

### Antes (Incompleto)
```
Cliente ordena
    ↓
Sistema valida stock (pero NO descuenta)
    ↓
Orden en PENDING
    ↓
❌ Stock nunca se descuenta
```

### Después (Completo)
```
Cliente ordena
    ↓
Sistema valida stock (sin descontar)
    ↓
Orden en PENDING
    ↓
Cliente paga en caja
    ↓
Mesero confirma pago:
    POST /api/staff/orders/{id}/confirm-payment
    ↓
🔥 AQUÍ DESCUENTA EL STOCK 🔥
    ↓
Stock actualizado correctamente
```

---

## Con Números

### Antes
```
Stock: 300 unidades

1. Creas orden de 1 Pollo → Stock: 300 (validó pero no descontó)
2. Creas orden de 2 Pollo → Stock: 300 (validó pero no descontó)
3. Creas orden de 3 Pollo → Stock: 300 (validó pero no descontó)

Total: 6 órdenes = 6 pollos comprometidos
Stock mostrado: 300 (pero solo 294 realmente disponibles)

INCONSISTENCIA ❌
```

### Después
```
Stock: 300 unidades

1. Creas orden de 1 Pollo
   → Stock: 300 (sin descontar, aún no pagó)
   → Paga
   → POST /api/staff/orders/1/confirm-payment
   → Stock: 299 ✅

2. Creas orden de 2 Pollo
   → Stock: 299 (sin descontar, aún no pagó)
   → Paga
   → POST /api/staff/orders/2/confirm-payment
   → Stock: 297 ✅

3. Creas orden de 3 Pollo
   → Stock: 297 (sin descontar, aún no pagó)
   → Paga
   → POST /api/staff/orders/3/confirm-payment
   → Stock: 294 ✅

Total: 3 órdenes pagadas = 6 pollos descontados
Stock: 294 (correcto y consistente)
```

---

## Cómo Probar

### 1. Abre sesión de mesa
```bash
POST http://localhost:8081/api/client/session
{
  "tableNumber": 10
}
```
Obtienes: `sessionToken`

### 2. Crea una orden
```bash
POST http://localhost:8081/api/client/orders
Cookie: sessionToken={tu_token}
{
  "products": [{"productId": 1, "quantity": 1}]
}
```
Obtienes: `orderId` (ej: 5)

### 3. ⭐ Confirma pago (DESCUENTA STOCK)
```bash
POST http://localhost:8081/api/staff/orders/5/confirm-payment
Authorization: Bearer {user_jwt_token}
```

Respuesta:
```json
{
  "message": "Pago confirmado. Inventario descontado."
}
```

El stock se descuenta **EN ESTE MOMENTO**.

---

## Estados de la Orden

```
PENDING (creado)
    ↓
[Cliente paga]
    ↓
PENDING (pagado)
    ↓ POST /api/staff/orders/{id}/confirm-payment
    ↓ 🔥 AQUÍ SE DESCUENTA EL STOCK 🔥
    ↓
IN_PREPARATION (cocina prepara)
    ↓
SERVED (mesero sirve)
    ↓
[Cliente confirma]
    ↓
SERVED (completado)
```

---

## Validaciones Importantes

### Al Crear Orden (Sin Descontar)
```java
validateRecipeWithVariations(product, quantity, overrides)
// Verifica: ¿Hay suficientes items?
// Descuenta: ❌ NO
// Intención: Validar antes de que pague
```

### Al Confirmar Pago (Descuenta)
```java
confirmPaymentAndDeductInventory(orderId)
// Verifica: ✅ Orden existe
// Descuenta: ✅ SÍ DESCUENTA
// Intención: Comprometer stock después del pago
```

---

## ¿Por Qué Así?

### ¿Por qué no descontar al crear la orden?
- ❌ El cliente aún no pagó
- ❌ Podría cambiar de opinión
- ❌ La orden podría cancelarse
- ✅ Mejor esperar confirmación de pago

### ¿Por qué descontar al confirmar pago?
- ✅ El cliente ya pagó
- ✅ La orden es "oficial"
- ✅ El stock es una obligación cumplida
- ✅ Consistencia garantizada

---

## Archivo de Prueba

Usa este archivo HTTP para probar el flujo completo:
```
test-flujo-descuento-stock.http
```

Incluye todos los pasos en orden.

---

## Resumen

| Aspecto | Antes | Ahora |
|---------|-------|-------|
| Stock al crear orden | No descuenta | No descuenta |
| Endpoint para pagar | ❌ No existe | ✅ `/confirm-payment` |
| Stock al pagar | - | ✅ Descuenta aquí |
| Consistencia | ❌ Inconsistente | ✅ Consistente |
| Auditoría | ❌ No | ✅ Registra logs |

---

## Próximas Mejoras

1. **Descuento automático**: Integrar con pasarela de pago para descontar automáticamente
2. **Devoluciones**: Endpoint para revertir stock si se cancela DESPUÉS de pagar
3. **Prevención de doble descuento**: Marcar orden como "inventario descargado"
4. **Alertas de stock bajo**: Notificar cuando se acaba el inventario

---

**Conclusión**: El stock ahora se descuenta en el momento en que el mesero/cashier confirma que el cliente pagó, garantizando consistencia entre órdenes pendientes e inventario real.

