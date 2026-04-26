# Resumen de Cambios: Almacenamiento de Items Opcionales en Órdenes

## ✅ Cambios Realizados

### 1. **Order_detail.java** - Entidad
- ✅ Agregado campo `optionalItems: String` con anotación `@Column(columnDefinition = "TEXT")`
- ✅ Almacena JSON con items opcionales: `[{"itemId": 5, "itemName": "Queso", "quantity": 2}]`

### 2. **OrderDetailDTO.java** - DTO
- ✅ Agregado campo `optionalItems: String` con anotación `@JsonProperty("optional_items")`
- ✅ Se devuelve al frontend con las órdenes

### 3. **OrderService.java** - Lógica
- ✅ Agregado método `convertOptionalItemsToJson(Product, Map<Long, Integer>)`
  - Convierte itemId + cantidad a JSON con nombres
  - Busca nombres en la receta del producto
  - Retorna null si no hay opcionales

- ✅ Actualizado `createTableOrder()`
  - Llama a `convertOptionalItemsToJson()` 
  - Asigna JSON a `Order_detail.optionalItems`

- ✅ Actualizado `createDeliveryOrder()`
  - Mismo proceso que mesa

- ✅ Actualizado `createTableOrderFromStaff()`
  - Mesero también puede agregar items opcionales

- ✅ Actualizado `toDTO()`
  - Incluye `optionalItems` en respuesta

### 4. **Migración Flyway** 
- ✅ Creado `V9__Add_optional_items_to_order_detail.sql`
- ✅ Agrega columna `optional_items TEXT` a tabla `order_detail`

### 5. **Documentación**
- ✅ Creado `OPTIONAL_ITEMS_STORAGE.md` - Guía completa
- ✅ Creado `OPTIONAL_ITEMS_EXAMPLE.json` - Ejemplo de respuesta API

## 📊 Estructura de Datos

### Formato JSON Guardado
```json
[
  {"itemId": 5, "itemName": "Queso extra", "quantity": 2},
  {"itemId": 6, "itemName": "Salsa especial", "quantity": 1}
]
```

## 🔄 Flujo de Datos

1. **Frontend envía** → `itemQuantityOverrides: {5: 2, 6: 1}`
2. **Backend recibe** → Mapa de itemId a cantidad
3. **Backend procesa** → Busca nombres y convierte a JSON
4. **Backend almacena** → JSON en `order_detail.optional_items`
5. **Frontend obtiene** → Campo `optional_items` en respuesta

## 🚀 Cómo Usar

### Request (Frontend → Backend)
```json
POST /api/table-orders
{
  "tableNumber": 1,
  "products": [
    {
      "productId": 10,
      "quantity": 2,
      "annotations": "Sin cebolla",
      "itemQuantityOverrides": {
        "5": 2,
        "8": 1
      }
    }
  ]
}
```

### Response (Backend → Frontend)
```json
{
  "items": [
    {
      "item_name": "Hamburguesa",
      "quantity": 2,
      "annotations": "Sin cebolla",
      "optional_items": "[{\"itemId\":5,\"itemName\":\"Queso\",\"quantity\":2},{\"itemId\":8,\"itemName\":\"Bacon\",\"quantity\":1}]"
    }
  ]
}
```

## ✨ Características

- ✅ Items opcionales con nombre y cantidad
- ✅ Almacenamiento en JSON para flexibilidad
- ✅ Sin cambios en endpoints existentes
- ✅ Compatible con órdenes de mesa, delivery y mesero
- ✅ Backward compatible (órdenes antiguas: null)

## 🧪 Compilación

```bash
./gradlew compileJava
# BUILD SUCCESSFUL ✅
```

## 📋 Próximos Pasos

1. El frontend debe parsear el JSON de `optional_items`
2. Mostrar items extras de forma clara en la UI
3. El mesero confirma que se agregaron los extras
4. Ejecutar migración en BD con Flyway

