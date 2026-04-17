-- Script para limpiar duplicados de recipe_id en product
-- Fecha: 2026-04-16

-- 1. Primero, marcar como eliminados los productos duplicados (mantener solo el más reciente)
UPDATE product p1
SET deleted = true, deleted_at = NOW()
WHERE id NOT IN (
    SELECT MAX(id)
    FROM product
    WHERE deleted = false AND recipe_id IS NOT NULL
    GROUP BY recipe_id
)
AND deleted = false
AND recipe_id IS NOT NULL;

-- 2. Eliminar el constraint unique antiguo si existe
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- 3. Crear un constraint unique que considere el soft delete
-- Este index permitirá que haya múltiples NULLs en recipe_id
ALTER TABLE product
ADD CONSTRAINT uk_product_recipe_not_deleted
UNIQUE (recipe_id)
WHERE deleted = false AND recipe_id IS NOT NULL;

-- 4. Verificar que no haya recipes eliminadas siendo referenciadas
-- Si las hay, hacer soft delete del product
UPDATE product p
SET deleted = true, deleted_at = NOW()
WHERE p.recipe_id IS NOT NULL
AND EXISTS (SELECT 1 FROM recipe r WHERE r.id = p.recipe_id AND r.deleted = true)
AND p.deleted = false;

COMMIT;

