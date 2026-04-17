-- Script SQL para Supabase - Agregar soft delete a todas las tablas necesarias
-- Ejecutar esto directamente en Supabase SQL Editor
-- Fecha: 2026-04-16

BEGIN;

-- 1. Agregar columnas de soft delete a 'recipe'
ALTER TABLE recipe
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- 2. Agregar columnas de soft delete a 'product'
ALTER TABLE product
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- 3. Agregar columnas de soft delete a 'product_categories'
ALTER TABLE product_categories
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- 4. Marcar como no eliminados todos los registros existentes
UPDATE recipe SET deleted = false, deleted_at = NULL WHERE deleted IS NULL;
UPDATE product SET deleted = false, deleted_at = NULL WHERE deleted IS NULL;
UPDATE product_categories SET deleted = false, deleted_at = NULL WHERE deleted IS NULL;

-- 5. Manejar duplicados en product.recipe_id - marcar como eliminados excepto el más reciente
UPDATE product p1
SET deleted = true, deleted_at = NOW()
WHERE p1.id NOT IN (
    SELECT MAX(p2.id)
    FROM product p2
    WHERE p2.deleted = false
    AND p2.recipe_id IS NOT NULL
    GROUP BY p2.recipe_id
)
AND p1.deleted = false
AND p1.recipe_id IS NOT NULL;

-- 6. Eliminar el constraint unique antiguo si existe
ALTER TABLE product
DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- 7. Crear un constraint unique que considere el soft delete (solo aplica a registros no eliminados)
CREATE UNIQUE INDEX IF NOT EXISTS idx_product_recipe_not_deleted
ON product(recipe_id)
WHERE deleted = false AND recipe_id IS NOT NULL;

-- 8. Marcar como eliminados los productos cuya receta fue eliminada
UPDATE product p
SET deleted = true, deleted_at = NOW()
WHERE p.recipe_id IS NOT NULL
AND EXISTS (SELECT 1 FROM recipe r WHERE r.id = p.recipe_id AND r.deleted = true)
AND p.deleted = false;

-- 9. Marcar como eliminados los productos de categorías eliminadas
UPDATE product p
SET deleted = true, deleted_at = NOW()
WHERE EXISTS (SELECT 1 FROM product_categories pc WHERE pc.id = p.category_id AND pc.deleted = true)
AND p.deleted = false;

COMMIT;

-- Verificación: ver el estado de los datos
-- SELECT COUNT(*) as total_products, COUNT(CASE WHEN deleted = false THEN 1 END) as active_products FROM product;
-- SELECT COUNT(*) as total_recipes, COUNT(CASE WHEN deleted = false THEN 1 END) as active_recipes FROM recipe;
-- SELECT COUNT(*) as total_categories, COUNT(CASE WHEN deleted = false THEN 1 END) as active_categories FROM product_categories;

