-- ================================================================
-- SQL COMPLETO PARA EJECUTAR EN SUPABASE
-- Fixes para Recetas, Productos y Órdenes
-- Ejecutar en el orden mostrado
-- ================================================================

-- ================================================================
-- 1. FIX: Permitir múltiples productos con la misma receta
-- ================================================================
-- Problema: UNIQUE constraint en recipe_id impedía duplicados
-- Solución: Eliminar la restricción UNIQUE

ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- Verificación:
-- SELECT * FROM product;


-- ================================================================
-- 2. FIX: Implementar Soft Delete en Recetas
-- ================================================================
-- Problema: No podías eliminar recetas referenciadas por productos
-- Solución: Soft Delete (marcar como DELETED en lugar de eliminar)

ALTER TABLE recipe ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- Verificación:
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'recipe'
-- ORDER BY ordinal_position;

-- Ver recetas activas:
-- SELECT id, name, status FROM recipe WHERE status = 'ACTIVE';

-- Ver recetas eliminadas:
-- SELECT id, name, status, deleted_at FROM recipe WHERE status = 'DELETED';


-- ================================================================
-- 3. OPCIONAL: FIX Check Constraint en Orders
-- ================================================================
-- Problema: CHECK constraint no permitía status = 'PAID'
-- Solución: Expandir los valores permitidos en el constraint

-- NOTA: Solo si tienes este constraint. Verifica primero:
-- SELECT constraint_name FROM information_schema.table_constraints
-- WHERE table_name = 'orders' AND constraint_type = 'CHECK';

-- Si existe, ejecuta esto:
-- ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
-- ALTER TABLE orders ADD CONSTRAINT orders_status_check
--   CHECK (status IN ('PENDING', 'IN_PREPARATION', 'SERVED', 'PAID', 'CANCELLED'));


-- ================================================================
-- VERIFICACIÓN FINAL
-- ================================================================

-- 1. Ver estructura de recipes
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'recipe'
ORDER BY ordinal_position;

-- 2. Ver cantidad de productos por receta
SELECT recipe_id, COUNT(*) as cantidad_productos
FROM product
WHERE recipe_id IS NOT NULL
GROUP BY recipe_id
ORDER BY cantidad_productos DESC;

-- 3. Ver recetas activas
SELECT id, name, status, deleted_at FROM recipe WHERE status = 'ACTIVE' LIMIT 10;

-- 4. Ver constraint en products
SELECT constraint_name
FROM information_schema.table_constraints
WHERE table_name = 'product' AND constraint_type = 'UNIQUE';

-- ================================================================
-- ESTADO ESPERADO DESPUÉS DE EJECUTAR ESTE SQL
-- ================================================================
-- ✓ product.recipe_id ya NO tiene restricción UNIQUE
-- ✓ recipe tiene columna 'status' (default = 'ACTIVE')
-- ✓ recipe tiene columna 'deleted_at' (NULL si no eliminada)
-- ✓ Multiple productos pueden usar la misma recipe
-- ✓ Las queries automáticamente filtran por status = 'ACTIVE'
-- ================================================================

