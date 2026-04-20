-- Script para arreglar los problemas de soft delete en la BD
-- Fecha: 2026-04-16

-- 1. Agregar columna 'deleted' a la tabla 'recipe' si no existe
ALTER TABLE recipe
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- 2. Agregar @Where clause implícito en queries
-- Asegurarse de que todas las tablas de soft delete tengan las columnas
ALTER TABLE product
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

ALTER TABLE product_categories
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- 3. Actualizar el constraint UNIQUE en product
-- El constraint actual solo permite una receta por producto si no está soft-deleted
-- Primero, obtener el nombre del constraint
-- ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- 4. Crear nuevo constraint que considere el soft delete
-- Crear un índice único solo para productos no eliminados
CREATE UNIQUE INDEX IF NOT EXISTS idx_product_recipe_active
ON product(recipe_id)
WHERE deleted = false AND recipe_id IS NOT NULL;

-- 5. Verificar que no haya duplicados en recipe_id para productos no eliminados
-- Si hay duplicados, hacer soft delete de los duplicados:
-- DELETE FROM product
-- WHERE id NOT IN (
--   SELECT MAX(id) FROM product
--   WHERE deleted = false AND recipe_id IS NOT NULL
--   GROUP BY recipe_id
-- ) AND deleted = false AND recipe_id IS NOT NULL;

-- 6. Asegurarse de que los datos existentes tengan los valores correctos
UPDATE recipe SET deleted = false, deleted_at = NULL WHERE deleted IS NULL;
UPDATE product SET deleted = false, deleted_at = NULL WHERE deleted IS NULL;
UPDATE product_categories SET deleted = false, deleted_at = NULL WHERE deleted IS NULL;

COMMIT;

