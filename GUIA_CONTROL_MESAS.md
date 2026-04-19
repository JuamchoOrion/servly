# Cambios Implementados - Control de Estado de Mesas

## Resumen
Se implementó validación para evitar abrir sesiones en mesas ocupadas y se agrega la lógica para liberar mesas automáticamente cuando una orden se paga.

---

## 1. Nueva Excepción Personalizada

### Archivo: `TableOccupiedException.java`
**Ubicación:** `src/main/java/co/edu/uniquindio/servly/exception/`

```java
public class TableOccupiedException extends RuntimeException {
    private final Integer tableNumber;
    private final String status;
    
    public TableOccupiedException(Integer tableNumber, String status) {
        super(String.format("La mesa número %d no está disponible (estado: %s). 
               No se puede abrir una sesión.", tableNumber, status));
    }
}
```

**Propósito:** Lanzar excepción cuando se intenta abrir sesión en una mesa no disponible.
**HTTP Status:** 409 Conflict

---

## 2. Cambios en `TableSessionService.java`

### Cambios Realizados:

#### a) Validación de Estado de Mesa (openSession)
```java
// Antes: No validaba el estado de la mesa
// Ahora: Valida que la mesa esté AVAILABLE
if (!table.getStatus().equals(RestaurantTable.TableStatus.AVAILABLE)) {
    throw new TableOccupiedException(tableNumber, table.getStatus().toString());
}
```

#### b) Cambiar Estado al Abrir Sesión
```java
// Cuando se abre una sesión exitosamente:
table.setStatus(RestaurantTable.TableStatus.OCCUPIED);
restaurantTableRepository.save(table);
```

**Flujo:**
- Cliente escanea QR de mesa → `GET /api/client/session?table=5`
- Sistema valida que mesa esté AVAILABLE
- Si está OCCUPIED o en otro estado → retorna 409 Conflict
- Si es AVAILABLE → se crea sesión y mesa cambia a OCCUPIED

#### c) Liberar Mesa al Cerrar Sesión (closeSession)
```java
// Cuando se cierra la sesión:
table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
restaurantTableRepository.save(table);
```

---

## 3. Cambios en `OrderService.java`

### Cambio en `updateOrderStatus()`

Agregada lógica para liberar la mesa automáticamente cuando una orden se paga:

```java
// Si cambia a PAID y es una orden de mesa, liberar la mesa
if (request.getStatus().equals(OrderTableState.PAID) && 
    order.getOrderType().equals(OrderType.TABLE)) {
    
    if (order.getSource() instanceof TableSource) {
        TableSource tableSource = (TableSource) order.getSource();
        RestaurantTable table = tableSource.getRestaurantTable();
        if (table != null) {
            table.setStatus(RestaurantTable.TableStatus.AVAILABLE);
            tableRepository.save(table);
        }
    }
}
```

**Flujo:**
1. Camarero/Cajero paga la orden: `PATCH /api/orders/{orderId}/status` con `status: PAID`
2. Sistema valida que sea una orden de mesa
3. Sistema obtiene la mesa asociada
4. Cambia estado de mesa a AVAILABLE automáticamente
5. Mesa queda disponible para nuevos clientes

---

## 4. Cambios en `GlobalExceptionHandler.java`

Agregado manejador para `TableOccupiedException`:

```java
@ExceptionHandler(TableOccupiedException.class)
public ResponseEntity<MessageResponse> handleTableOccupied(
        TableOccupiedException ex, HttpServletRequest request) {
    log.warn("TableOccupiedException en {}: Mesa {}, Estado {}", 
             request.getRequestURI(), ex.getTableNumber(), ex.getStatus());
    
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new MessageResponse(ex.getMessage()));
}
```

**Respuesta de Error:**
```
HTTP/1.1 409 Conflict
Content-Type: application/json

{
  "message": "La mesa número 5 no está disponible (estado: OCCUPIED). 
              No se puede abrir una sesión."
}
```

---

## 5. Estados de Mesa

La mesa tiene estos estados (enum `TableStatus`):

| Estado | Descripción |
|--------|-------------|
| `AVAILABLE` | Mesa disponible para clientes |
| `OCCUPIED` | Hay clientes usando la mesa (sesión activa) |
| `MAINTENANCE` | Mesa en mantenimiento |
| `RESERVED` | Mesa reservada |

---

## Flujo Completo de Mesa

### Escenario 1: Cliente Nuevo
```
1. Mesa está AVAILABLE
2. Cliente escanea QR → GET /api/client/session?table=5
3. ✓ Sesión se abre → mesa cambia a OCCUPIED
4. Cliente realiza órdenes
5. Cliente paga → PATCH /api/orders/{id}/status con PAID
6. ✓ Mesa cambia a AVAILABLE automáticamente
```

### Escenario 2: Mesa Ocupada (Error)
```
1. Mesa está OCCUPIED (cliente anterior aún en mesa)
2. Nuevo cliente intenta escanear QR → GET /api/client/session?table=5
3. ✗ Sistema retorna: 409 Conflict
4. Mensaje: "La mesa número 5 no está disponible (estado: OCCUPIED)"
```

### Escenario 3: Cierre Manual de Sesión
```
1. Mesa está OCCUPIED
2. Camarero cierra sesión → DELETE /api/staff/tables/5/session
3. ✓ Sesión se cierra y mesa cambia a AVAILABLE
```

---

## Testing

### Test 1: Abrir sesión en mesa ocupada (debe fallar)
```http
GET http://localhost:8081/api/client/session?table=5
```
**Esperado:** 409 Conflict con mensaje personalizado

### Test 2: Abrir sesión en mesa disponible (debe funcionar)
```http
GET http://localhost:8081/api/client/session?table=1
```
**Esperado:** 200 OK con sessionToken

### Test 3: Pagar orden cambia mesa a disponible
```http
PATCH http://localhost:8081/api/orders/1/status
Content-Type: application/json

{"status": "PAID"}
```
**Esperado:** 200 OK y mesa cambia a AVAILABLE

---

## Ventajas de Esta Implementación

1. ✅ **Previene doble-asignación**: No permite abrir 2 sesiones en la misma mesa
2. ✅ **Automatización**: Mesa se libera automáticamente al pagar
3. ✅ **Error claro**: Cliente recibe mensaje específico (409 Conflict)
4. ✅ **Transaccional**: Cambios se persisten en BD
5. ✅ **Logging**: Se registran todas las transiciones de estado
6. ✅ **Mantenimiento**: Se puede cambiar estado manual si es necesario

---

## Notas Importantes

- La transición de estado de mesa está integrada en el flujo de sesiones y órdenes
- Los cambios son automáticos y no requieren intervención manual
- Cualquier estado de mesa (MAINTENANCE, RESERVED) también bloquea nuevas sesiones
- El sistema es idempotente: si se intenta pagar 2 veces, no hay efecto negativo

