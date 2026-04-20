-- ============================================================================
-- SCRIPT SQL: FLUJO DE PAGO COMPLETO - SERVLY
-- ============================================================================
-- Este script crea todos los datos necesarios para ejecutar un flujo
-- de pago completo sin depender de datos externos.
-- Puede ejecutarse múltiples veces (usa DELETE para limpiar primero)
-- ============================================================================

-- PASO 0: LIMPIAR DATOS ANTERIORES (Opcional)
-- Descomenta si necesitas empezar de cero

DELETE FROM order_details WHERE order_id IN (
  SELECT id FROM orders WHERE source_type = 'TABLE' AND source_id IN (
    SELECT id FROM table_sources
  )
);

DELETE FROM orders WHERE source_type = 'TABLE' AND source_id IN (
  SELECT id FROM table_sources
);

DELETE FROM table_sources WHERE restaurant_table_id IN (
  SELECT id FROM restaurant_tables WHERE table_number IN (5, 10)
);

DELETE FROM restaurant_tables WHERE table_number IN (5, 10);

-- ============================================================================
-- PASO 1: CREAR MESAS
-- ============================================================================
INSERT INTO restaurant_tables (table_number, capacity, location, status)
VALUES
  (5, 4, 'Entrada', 'AVAILABLE'),
  (10, 6, 'Fondo', 'AVAILABLE')
ON CONFLICT (table_number) DO NOTHING;

-- Obtener IDs de las mesas creadas
-- (Los IDs serán diferentes, asegúrate de actualizar abajo)

-- ============================================================================
-- PASO 2: CREAR CATEGORÍAS DE PRODUCTOS
-- ============================================================================
INSERT INTO product_categories (name, description)
VALUES
  ('Hamburguesas', 'Hamburguesas caseras'),
  ('Bebidas', 'Bebidas y refrescos')
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- PASO 3: CREAR ITEMS DE INVENTARIO (Ingredientes)
-- ============================================================================
INSERT INTO items (name, description, measurement_unit, current_stock, min_stock, max_stock)
VALUES
  ('Carne Molida', 'Carne molida para hamburguesas', 'GRAM', 10000, 500, 20000),
  ('Queso Cheddar', 'Queso cheddar para hamburguesas', 'SLICE', 500, 10, 1000),
  ('Lechuga', 'Lechuga fresca para ensaladas', 'LEAF', 1000, 50, 2000)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- PASO 4: CREAR RECETAS
-- ============================================================================

-- Receta Simple: Hamburguesa Simple
INSERT INTO recipes (name, description)
VALUES ('Hamburguesa Simple', 'Hamburguesa con queso y lechuga')
RETURNING id AS recipe_simple_id;

-- Receta Personalizable: Hamburguesa Premium
INSERT INTO recipes (name, description)
VALUES ('Hamburguesa Premium', 'Hamburguesa personalizable')
RETURNING id AS recipe_premium_id;

-- ============================================================================
-- PASO 5: AGREGAR ITEMS A RECETAS
-- ============================================================================

-- Items para Hamburguesa Simple (Recipe ID que obtuviste arriba)
INSERT INTO item_details (recipe_id, item_id, quantity, is_optional, min_quantity, max_quantity)
VALUES
  (1, 1, 150, false, 100, 200),      -- 150g de carne (rango: 100-200)
  (1, 2, 1, false, 0, 2),            -- 1 queso (rango: 0-2)
  (1, 3, 2, false, 1, 5);            -- 2 lechugas (rango: 1-5)

-- Items para Hamburguesa Premium (personalizable)
INSERT INTO item_details (recipe_id, item_id, quantity, is_optional, min_quantity, max_quantity)
VALUES
  (2, 1, 200, false, 150, 300),      -- 200g de carne (rango: 150-300)
  (2, 2, 1, true, 0, 3),             -- 1 queso OPCIONAL (rango: 0-3)
  (2, 3, 2, true, 0, 5);             -- 2 lechugas OPCIONAL (rango: 0-5)

-- ============================================================================
-- PASO 6: CREAR PRODUCTOS
-- ============================================================================

-- Bebida simple (sin receta)
INSERT INTO products (name, description, price, product_category_id, recipe_id, active)
VALUES ('Coca Cola', 'Refresco gaseoso', 2.50, 2, NULL, true)
RETURNING id AS product_cocacola_id;

-- Hamburguesa Simple (con receta simple)
INSERT INTO products (name, description, price, product_category_id, recipe_id, active)
VALUES ('Hamburguesa Simple', 'Hamburguesa con queso y lechuga', 8.99, 1, 1, true)
RETURNING id AS product_simple_id;

-- Hamburguesa Premium (con receta personalizable)
INSERT INTO products (name, description, price, product_category_id, recipe_id, active)
VALUES ('Hamburguesa Premium', 'Hamburguesa personalizable', 12.99, 1, 2, true)
RETURNING id AS product_premium_id;

-- ============================================================================
-- PASO 7: CREAR TABLE_SOURCE (Fuente de mesa)
-- ============================================================================

-- Para mesa #5
INSERT INTO table_sources (restaurant_table_id, table_number)
SELECT id, 5 FROM restaurant_tables WHERE table_number = 5
RETURNING id AS ts_mesa5_id;

-- Para mesa #10
INSERT INTO table_sources (restaurant_table_id, table_number)
SELECT id, 10 FROM restaurant_tables WHERE table_number = 10
RETURNING id AS ts_mesa10_id;

-- ============================================================================
-- PASO 8: CREAR ÓRDENES (SIN VARIACIONES)
-- ============================================================================

-- Orden para mesa #5: 2x Hamburguesa Simple + 2x Coca Cola
INSERT INTO orders (date, total, order_type, status, source_type, source_id)
VALUES
  (CURRENT_DATE, 22.98, 'TABLE', 'PENDING', 'TABLE',
   (SELECT id FROM table_sources WHERE table_number = 5))
RETURNING id AS order_simple_id;

-- Detalles de la orden de mesa #5
INSERT INTO order_details (order_id, product_id, quantity, unit_price, tax_percent, subtotal)
VALUES
  (1, 2, 2, 8.99, 0.08, 17.98),     -- 2x Hamburguesa Simple
  (1, 1, 2, 2.50, 0.08, 5.00);      -- 2x Coca Cola

-- ============================================================================
-- PASO 9: CREAR ÓRDENES (CON VARIACIONES)
-- ============================================================================

-- Orden para mesa #10: Hamburguesa Premium PERSONALIZADA
-- Variaciones: 2 quesos (en lugar de 1), 3 lechugas (en lugar de 2)
INSERT INTO orders (date, total, order_type, status, source_type, source_id)
VALUES
  (CURRENT_DATE, 12.99, 'TABLE', 'PENDING', 'TABLE',
   (SELECT id FROM table_sources WHERE table_number = 10))
RETURNING id AS order_premium_id;

-- Detalles de la orden de mesa #10
INSERT INTO order_details (order_id, product_id, quantity, unit_price, tax_percent, subtotal)
VALUES
  (2, 3, 1, 12.99, 0.08, 12.99);    -- 1x Hamburguesa Premium (con variaciones)

-- ============================================================================
-- PASO 10: SIMULAR FLUJO DE PAGO PARA ORDEN #1 (SIN VARIACIONES)
-- ============================================================================

-- Estado actual: PENDING

-- 1. Cambiar a IN_PREPARATION
UPDATE orders SET status = 'IN_PREPARATION' WHERE id = 1;

-- 2. Cambiar a SERVED (aquí se descuenta inventario)
UPDATE orders SET status = 'SERVED' WHERE id = 1;

-- Descontar inventario para orden #1
UPDATE items SET current_stock = current_stock - 300   -- 2x 150g de carne
  WHERE id = 1;
UPDATE items SET current_stock = current_stock - 2     -- 2x 1 queso
  WHERE id = 2;
UPDATE items SET current_stock = current_stock - 4     -- 2x 2 lechugas
  WHERE id = 3;

-- 3. Cambiar a PAID (confirmar pago)
UPDATE orders SET status = 'PAID' WHERE id = 1;

-- ============================================================================
-- PASO 11: SIMULAR FLUJO DE PAGO PARA ORDEN #2 (CON VARIACIONES)
-- ============================================================================

-- Estado actual: PENDING

-- 1. Cambiar a IN_PREPARATION
UPDATE orders SET status = 'IN_PREPARATION' WHERE id = 2;

-- 2. Cambiar a SERVED (aquí se descuenta inventario CON VARIACIONES)
UPDATE orders SET status = 'SERVED' WHERE id = 2;

-- Descontar inventario para orden #2 (con variaciones)
UPDATE items SET current_stock = current_stock - 200   -- 1x 200g de carne
  WHERE id = 1;
UPDATE items SET current_stock = current_stock - 2     -- 1x 2 quesos (VARIACIÓN)
  WHERE id = 2;
UPDATE items SET current_stock = current_stock - 3     -- 1x 3 lechugas (VARIACIÓN)
  WHERE id = 3;

-- 3. Cambiar a PAID (confirmar pago)
UPDATE orders SET status = 'PAID' WHERE id = 2;

-- ============================================================================
-- PASO 12: VERIFICAR ESTADO FINAL
-- ============================================================================

-- Ver órdenes creadas
SELECT
  o.id,
  o.order_type,
  o.status,
  o.total,
  ts.table_number,
  o.date
FROM orders o
LEFT JOIN table_sources ts ON o.source_id = ts.id AND o.source_type = 'TABLE'
WHERE o.id IN (1, 2);

-- Ver detalles de órdenes
SELECT
  od.id,
  o.id AS order_id,
  p.name,
  od.quantity,
  od.unit_price,
  od.subtotal
FROM order_details od
JOIN orders o ON od.order_id = o.id
JOIN products p ON od.product_id = p.id
WHERE o.id IN (1, 2);

-- Ver inventario final
SELECT
  id,
  name,
  current_stock,
  min_stock,
  max_stock
FROM items;

-- ============================================================================
-- RESULTADO ESPERADO
-- ============================================================================
--
-- ÓRDENES COMPLETADAS:
-- - Orden #1: Mesa 5, PAID, Total $22.98
--   * 2x Hamburguesa Simple
--   * 2x Coca Cola
--   * Inventario descuento:
--     - Carne: 300g (2 × 150)
--     - Queso: 2 slices (2 × 1)
--     - Lechuga: 4 hojas (2 × 2)
--
-- - Orden #2: Mesa 10, PAID, Total $12.99
--   * 1x Hamburguesa Premium CON VARIACIONES
--     - 2 quesos (en lugar de 1)
--     - 3 lechugas (en lugar de 2)
--   * Inventario descuento:
--     - Carne: 200g (1 × 200)
--     - Queso: 2 slices (VARIACIÓN × 1)
--     - Lechuga: 3 hojas (VARIACIÓN × 1)
--
-- INVENTARIO FINAL (suponiendo stock inicial):
-- - Carne Molida: 10000g - 500g = 9500g
-- - Queso Cheddar: 500 - 4 = 496 slices
-- - Lechuga: 1000 - 7 = 993 hojas
--
-- ============================================================================

