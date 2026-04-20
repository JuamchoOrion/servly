-- EJEMPLO: Variaciones simples en ItemDetail
-- Admin crea Hamburguesa con base de 2 pan, 1 carne, 1 queso
-- Cliente puede elegir: +1 queso, -1 pan, +1 cebolla, etc

-- 1. Crear Item: Queso Americano
INSERT INTO item (name, unit_price, description)
VALUES ('Queso Americano', 3500.00, 'Slice de queso');

-- 2. Crear Recipe para Hamburguesa (ID: 5)
INSERT INTO recipe (name, description)
VALUES ('Hamburguesa Clásica', 'Base para hamburguesa');

-- 3. Agregar ItemDetails a la Recipe:
--    id=20: Pan (qty=2, base, no opcional)
INSERT INTO itema_detail (recipe_id, item_id, quantity, annotation, is_optional, min_quantity, max_quantity)
VALUES (5, 1, 2, 'Pan para hamburguesa', false, null, null);

--    id=21: Carne (qty=1, base, no opcional)
INSERT INTO itema_detail (recipe_id, item_id, quantity, annotation, is_optional, min_quantity, max_quantity)
VALUES (5, 2, 1, 'Carne de res', false, null, null);

--    id=22: Queso (qty=1, OPCIONAL, cliente puede elegir 0-3)
INSERT INTO itema_detail (recipe_id, item_id, quantity, annotation, is_optional, min_quantity, max_quantity)
VALUES (5, 10, 1, 'Queso opcional', true, 0, 3);

--    id=23: Lechuga (qty=1, OPCIONAL, cliente puede elegir 0-2)
INSERT INTO itema_detail (recipe_id, item_id, quantity, annotation, is_optional, min_quantity, max_quantity)
VALUES (5, 11, 1, 'Lechuga fresca', true, 0, 2);

--    id=24: Cebolla (qty=0, OPCIONAL, cliente puede agregar 0-2)
INSERT INTO itema_detail (recipe_id, item_id, quantity, annotation, is_optional, min_quantity, max_quantity)
VALUES (5, 12, 0, 'Cebolla', true, 0, 2);

-- 4. Crear Producto Hamburguesa
INSERT INTO product (name, price, description, recipe_id, category_id)
VALUES ('Hamburguesa Clásica', 25000.00, 'Hamburguesa casera', 5, 1);

/*
FLUJO DEL CLIENTE:

1. Cliente obtiene producto:
   GET /api/products/1

   Response:
   {
     "id": 1,
     "name": "Hamburguesa Clásica",
     "basePrice": 25000.00,
     "recipeItems": [
       {
         "id": 20,
         "itemId": 1,
         "itemName": "Pan",
         "baseQuantity": 2,
         "isOptional": false,
         "minQuantity": null,
         "maxQuantity": null
       },
       {
         "id": 22,
         "itemId": 10,
         "itemName": "Queso",
         "baseQuantity": 1,
         "isOptional": true,
         "minQuantity": 0,
         "maxQuantity": 3
       },
       {
         "id": 23,
         "itemId": 11,
         "itemName": "Lechuga",
         "baseQuantity": 1,
         "isOptional": true,
         "minQuantity": 0,
         "maxQuantity": 2
       },
       {
         "id": 24,
         "itemId": 12,
         "itemName": "Cebolla",
         "baseQuantity": 0,
         "isOptional": true,
         "minQuantity": 0,
         "maxQuantity": 2
       }
     ]
   }

2. Cliente crea orden eligiendo variaciones:
   POST /api/client/orders

   {
     "tableNumber": 5,
     "items": [
       {
         "productId": 1,
         "quantity": 2,
         "itemQuantityOverrides": {
           "10": 3,  // Extra queso (base=1, elige 3) → +2 quesos = +7000
           "12": 1   // Agregar cebolla (base=0, elige 1) → +1 cebolla
         }
       }
     ]
   }

   Cálculo precio:
   - Precio base: 25,000
   - Extra queso: 2 slices × 3,500 = 7,000
   - Cebolla: 1 × precio_cebolla
   - Total por hamburguesa: ~32,500
   - Total orden (×2): ~65,000

3. Al confirmar pago, se descuentan:
   - 4 pan (2×2)
   - 2 carne (2×1)
   - 6 queso (2×3)
   - 2 cebolla (2×1)
   - 4 lechuga (2×2 - quedó en base)

¿Más simple y directo? El admin simplemente:
- Crea la receta
- Define qué items son opcionales
- Define minQuantity y maxQuantity
- Y listo, el cliente elige cantidades

¡NO hay que crear "customizaciones" como entidades!
*/

