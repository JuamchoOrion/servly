-- Modificar la relación entre Product y Recipe de OneToOne a ManyToOne
-- Eliminar la restricción única en recipe_id si existe

-- Primero, eliminar la restricción única
ALTER TABLE product DROP CONSTRAINT IF EXISTS ukn7l68cove4a44kdep3s30rikv;

-- Opcional: Si quieres asegurar que la columna recipe_id permite múltiples productos con la misma receta
-- La columna ya debería permitir NULL y múltiples valores, así que no hay cambio estructural necesario

