# Almacenamiento de Items Opcionales en Órdenes

## Descripción
A partir de esta actualización, el backend almacena los **items opcionales** elegidos por el cliente en cada detalle de la orden. Los items opcionales se guardan como JSON con su ID, nombre y cantidad.

## Estructura de Datos

### Campo en Base de Datos
```sql
ALTER TABLE order_detail ADD COLUMN optional_items TEXT;
```

### Formato JSON
Los items opcionales se almacenan en formato JSON en el campo `optional_items` de `order_detail`:

```json
[
  {
    "itemId": 5,
    "itemName": "Queso extra",
    "quantity": 2
  },
  {
    "itemId": 6,
    "itemName": "Salsa especial",
    "quantity": 1
  }
]
```

## Flujo de Datos

### 1. Cliente envía orden con items opcionales
El frontend envía un `CreateTableOrderRequest` o `CreateClientOrderRequest` con:

```json
{
  "tableNumber": 1,
  "products": [
    {
      "productId": 1,
      "quantity": 2,
      "annotations": "Sin picante",
      "itemQuantityOverrides": {
        "5": 2,    // 2 de queso extra (itemId: 5)
        "6": 1     // 1 de salsa especial (itemId: 6)
      }
    }
  ]
}
```

### 2. Backend procesa y guarda
El método `convertOptionalItemsToJson()` en `OrderService`:
- Recibe el mapa de itemIds con sus cantidades
- Busca el nombre de cada item en la receta del producto
- Convierte todo a formato JSON
- Almacena en `Order_detail.optionalItems`

### 3. Frontend recibe los items opcionales
La respuesta del GET de órdenes incluye:

```json
{
  "id": 1,
  "items": [
    {
      "id": 100,
      "item_id": 1,
      "item_name": "Hamburguesa",
      "quantity": 2,
      "unit_price": 15000,
      "annotations": "Sin picante",
      "optional_items": "[{\"itemId\":5,\"itemName\":\"Queso extra\",\"quantity\":2},{\"itemId\":6,\"itemName\":\"Salsa especial\",\"quantity\":1}]"
    }
  ],
  "status": "PENDING",
  "total": 30000
}
```

## Cambios en la Estructura

### Order_detail.java
```java
/**
 * Almacena items opcionales elegidos por el cliente
 * Formato JSON: {"itemId": 1, "itemName": "Extra queso", "quantity": 2}, ...
 * Ejemplo: [{"itemId": 5, "itemName": "Queso extra", "quantity": 2}, {"itemId": 6, "itemName": "Salsa especial", "quantity": 1}]
 */
@Column(columnDefinition = "TEXT")
private String optionalItems;
```

### OrderDetailDTO.java
```java
/**
 * Items opcionales elegidos por el cliente
 * Formato: [{"itemId": 5, "itemName": "Queso extra", "quantity": 2}, ...]
 */
@JsonProperty("optional_items")
private String optionalItems;
```

## Métodos Actualizados

### 1. `createTableOrder()` - Órdenes de mesa desde cliente
Ahora:
- Llama a `convertOptionalItemsToJson()` para procesar items opcionales
- Asigna el JSON al campo `optionalItems` de `Order_detail`

### 2. `createDeliveryOrder()` - Órdenes delivery
Ahora:
- Procesa items opcionales igual que órdenes de mesa
- Almacena el JSON con los items extra

### 3. `createTableOrderFromStaff()` - Órdenes del mesero
Ahora:
- Guarda items opcionales cuando el mesero crea una orden

### 4. `convertOptionalItemsToJson()` - Método auxiliar
Nuevo método que:
- Recibe el mapa de `itemQuantityOverrides`
- Busca cada item en la receta del producto para obtener el nombre
- Convierte a JSON con estructura: `[{itemId, itemName, quantity}, ...]`
- Retorna `null` si no hay items opcionales

### 5. `toDTO()` - Conversión a DTO
Ahora:
- Incluye `optionalItems` en `OrderDetailDTO`
- Los items opcionales se devuelven al frontend como JSON

## Migración de Base de Datos
Se creó la migración `V9__Add_optional_items_to_order_detail.sql`:

```sql
ALTER TABLE order_detail
ADD COLUMN optional_items TEXT COMMENT 'Items opcionales elegidos por el cliente en formato JSON';
```

## Notas Importantes

1. **Los items opcionales son opcionales**: Si el cliente no elige items extra, el valor será `null`
2. **JSON es el formato de almacenamiento**: Se usa JSON TEXT para máxima flexibilidad
3. **Compatibilidad hacia atrás**: Las órdenes existentes tendrán `null` en este campo
4. **Nombres de items se guardan**: Facilita el seguimiento en cocina sin necesidad de queries adicionales

## Ejemplo de Consulta en Cocina

El personal de cocina verá:
```
Orden #1:
- 2x Hamburguesa
  → Sin picante
  → Extras: 2x Queso extra, 1x Salsa especial
```

## Integración con el Frontend

El frontend debe:
1. Parsear el JSON del campo `optional_items`
2. Mostrar los items opcionales de forma clara en la orden
3. Permitir que el mesero confirme que se agregaron los extras

