-- Fix database integrity issues
-- Execute this script to clean up the database errors

-- 1. Remover unique constraint en recipe_id si existe
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- 2. Asegurar que los campos de soft delete existan
ALTER TABLE product
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

ALTER TABLE recipe
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

ALTER TABLE product_categories
ADD COLUMN IF NOT EXISTS deleted BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- 3. Actualizar registros existentes
UPDATE product SET deleted = false WHERE deleted IS NULL;
UPDATE recipe SET status = 'ACTIVE' WHERE status IS NULL;
UPDATE product_categories SET deleted = false WHERE deleted IS NULL;

-- 4. Limpiar datos problemáticos (productos duplicados con misma receta)
-- Mantener solo el primero de cada receta y marcar el resto como eliminado
UPDATE product p1
SET deleted = true, deleted_at = NOW()
WHERE id NOT IN (
    SELECT MIN(id) FROM product
    WHERE recipe_id IS NOT NULL
    GROUP BY recipe_id
)
AND recipe_id IS NOT NULL
AND deleted = false;

-- 5. Crear índice sin unique en recipe_id
DROP INDEX IF EXISTS idx_product_recipe_id;
CREATE INDEX idx_product_recipe_id ON product(recipe_id) WHERE deleted = false;
CREATE INDEX idx_product_active ON product(active) WHERE deleted = false;
CREATE INDEX idx_product_category_id ON product(category_id) WHERE deleted = false;
CREATE INDEX idx_recipe_status ON recipe(status);
CREATE INDEX idx_product_categories_deleted ON product_categories(deleted);

-- Verificación
SELECT 'Products by recipe:' as check_type;
SELECT recipe_id, COUNT(*) as count
FROM product
WHERE deleted = false
GROUP BY recipe_id
HAVING COUNT(*) > 1;

SELECT 'Active recipes:' as check_type;
SELECT COUNT(*) as total FROM recipe WHERE status = 'ACTIVE';

SELECT 'Active products:' as check_type;
SELECT COUNT(*) as total FROM product WHERE deleted = false AND active = true;

