-- ============================================================================
-- SQL PARA CONFIGURAR BASE DE DATOS PARA PRUEBAS DE MESA Y ÓRDENES
-- ============================================================================
-- Este SQL configura:
-- 1. Mesa número 10
-- 2. Producto "Pollo" con receta
-- 3. Items en inventario
-- 4. Suficiente stock (300 unidades de Pollo picado)
-- ============================================================================

-- 1. CREAR/VERIFICAR MESA
INSERT INTO restaurant_tables (table_number, capacity, status)
VALUES (10, 4, 'AVAILABLE')
ON CONFLICT (table_number) DO NOTHING;

-- 2. CREAR INVENTARIO (si no existe)
INSERT INTO inventories (name, location)
VALUES ('Main Kitchen', 'Kitchen')
ON CONFLICT (name) DO NOTHING;

-- 3. CREAR ITEMS EN EL INVENTARIO
-- Item 2: Pollo picado (debe tener suficiente stock)
INSERT INTO items (name, description)
VALUES ('Pollo picado', 'Pollo picado para preparación')
ON CONFLICT (name) DO NOTHING;

-- 4. AGREGAR STOCK AL INVENTARIO
-- Buscar IDs
WITH inventory_id AS (
  SELECT id FROM inventories WHERE name = 'Main Kitchen' LIMIT 1
),
item_id AS (
  SELECT id FROM items WHERE name = 'Pollo picado' LIMIT 1
)
INSERT INTO item_stock (quantity, inventory_id, item_id, location)
SELECT 300, (SELECT id FROM inventory_id), (SELECT id FROM item_id), 'Main Kitchen'
WHERE NOT EXISTS (
  SELECT 1 FROM item_stock
  WHERE item_id = (SELECT id FROM item_id)
  AND inventory_id = (SELECT id FROM inventory_id)
);

-- Si el anterior no funciona, usa este:
-- UPDATE item_stock SET quantity = 300
-- WHERE item_id = 2 AND inventory_id = 1;

-- 5. CREAR CATEGORÍA (si no existe)
INSERT INTO categories (name, description)
VALUES ('Pollo', 'Platos de pollo')
ON CONFLICT (name) DO NOTHING;

-- 6. CREAR PRODUCTO
-- Nota: El producto debe estar activo y tener precio
INSERT INTO products (name, description, price, is_active, category_id)
SELECT 'Pollo', 'Plato de pollo delicioso', 25000.00, true,
       (SELECT id FROM categories WHERE name = 'Pollo' LIMIT 1)
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'Pollo' AND category_id =
                  (SELECT id FROM categories WHERE name = 'Pollo' LIMIT 1));

-- 7. CREAR RECETA PARA EL PRODUCTO
-- Obtener IDs necesarios
WITH product_id AS (
  SELECT id FROM products WHERE name = 'Pollo' LIMIT 1
),
item_id AS (
  SELECT id FROM items WHERE name = 'Pollo picado' LIMIT 1
)
INSERT INTO recipes (name, description, product_id)
SELECT 'Receta Pollo', 'Receta para pollo picado', (SELECT id FROM product_id)
WHERE NOT EXISTS (
  SELECT 1 FROM recipes WHERE product_id = (SELECT id FROM product_id)
)
RETURNING id;

-- 8. CREAR ITEM_DETAIL (vincula item con receta)
-- Este paso conecta el item 'Pollo picado' con la receta
WITH recipe_id AS (
  SELECT id FROM recipes WHERE name = 'Receta Pollo' LIMIT 1
),
item_id AS (
  SELECT id FROM items WHERE name = 'Pollo picado' LIMIT 1
)
INSERT INTO item_details (quantity, item_id, recipe_id)
SELECT 1, (SELECT id FROM item_id), (SELECT id FROM recipe_id)
WHERE NOT EXISTS (
  SELECT 1 FROM item_details
  WHERE item_id = (SELECT id FROM item_id)
  AND recipe_id = (SELECT id FROM recipe_id)
);

-- 9. VINCULAR RECETA AL PRODUCTO
-- Actualizar el producto para que apunte a la receta
UPDATE products
SET recipe_id = (SELECT id FROM recipes WHERE name = 'Receta Pollo' LIMIT 1)
WHERE name = 'Pollo' AND recipe_id IS NULL;

-- ============================================================================
-- VERIFICACIÓN: Ejecuta estos queries para confirmar la configuración
-- ============================================================================

-- Ver mesa creada
SELECT * FROM restaurant_tables WHERE table_number = 10;

-- Ver inventario
SELECT * FROM inventories WHERE name = 'Main Kitchen';

-- Ver items
SELECT * FROM items WHERE name = 'Pollo picado';

-- Ver stock del item
SELECT * FROM item_stock WHERE item_id = (SELECT id FROM items WHERE name = 'Pollo picado' LIMIT 1);

-- Ver producto creado
SELECT * FROM products WHERE name = 'Pollo';

-- Ver receta
SELECT * FROM recipes WHERE name = 'Receta Pollo';

-- Ver item_details
SELECT * FROM item_details WHERE recipe_id = (SELECT id FROM recipes WHERE name = 'Receta Pollo' LIMIT 1);

-- ============================================================================
-- SI NECESITAS REINICIAR COMPLETAMENTE, EJECUTA ESTO:
-- ============================================================================
/*
-- ADVERTENCIA: Esto borra datos, ejecuta solo si sabes qué haces
DELETE FROM item_details WHERE recipe_id IN (SELECT id FROM recipes WHERE name = 'Receta Pollo');
DELETE FROM recipes WHERE name = 'Receta Pollo';
DELETE FROM item_stock WHERE item_id IN (SELECT id FROM items WHERE name = 'Pollo picado');
DELETE FROM items WHERE name = 'Pollo picado';
DELETE FROM products WHERE name = 'Pollo';
DELETE FROM categories WHERE name = 'Pollo';
DELETE FROM restaurant_tables WHERE table_number = 10;
*/

