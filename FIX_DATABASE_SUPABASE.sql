-- ============================================
-- SQL PARA EJECUTAR EN SUPABASE
-- 1. Arregla la relación Product-Recipe
-- 2. Implementa Soft Delete en Recipe
-- ============================================

-- ============================================
-- PASO 1: Eliminar la restricción UNIQUE que impedía múltiples productos con la misma receta
-- ============================================
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- ============================================
-- PASO 2: Agregar columnas para Soft Delete en Recipe
-- ============================================
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE recipe ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- ============================================
-- PASO 3: Verificar que las columnas existan
-- ============================================
-- SELECT * FROM recipe;

-- ============================================
-- PASO 4: (OPCIONAL) Ver recetas eliminadas
-- ============================================
-- SELECT * FROM recipe WHERE status = 'DELETED';

-- ============================================
-- PASO 5: (OPCIONAL) Reactivar una receta eliminada
-- ============================================
-- UPDATE recipe SET status = 'ACTIVE', deleted_at = NULL WHERE id = 1;

-- ============================================
-- VERIFICACIÓN FINAL: Ejecuta esto para confirmar
-- ============================================

-- Ver la estructura actual de la tabla recipe
-- SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = 'recipe';

-- Ver cuántos productos usan cada receta
-- SELECT recipe_id, COUNT(*) as cantidad_productos
-- FROM product
-- WHERE recipe_id IS NOT NULL
-- GROUP BY recipe_id
-- ORDER BY cantidad_productos DESC;

-- Ver recetas activas
-- SELECT id, name, status FROM recipe WHERE status = 'ACTIVE';

-- ============================================
-- NOTAS IMPORTANTES:
-- ============================================
-- 1. Se elimina la restricción UNIQUE en recipe_id
--    → Ahora múltiples productos pueden compartir la misma receta
--
-- 2. Se agrega Soft Delete a Recipe
--    → status: ACTIVE o DELETED
--    → deleted_at: fecha/hora de eliminación
--
-- 3. Las recetas eliminadas no aparecerán en consultas normales
--    gracias a la anotación @Where en la entidad Recipe
--
-- 4. No se pierden datos, todo es reversible
-- ============================================

