-- Script SQL para ejecutar manualmente en Supabase
-- Solución paso a paso para arreglar los problemas de FK

-- PASO 1: Eliminar cualquier FK que apunte a restaurant_tables
ALTER TABLE IF EXISTS table_sessions DROP CONSTRAINT IF EXISTS fk_table_sessions_restaurant_table CASCADE;
ALTER TABLE IF EXISTS table_source DROP CONSTRAINT IF EXISTS fk_table_source_restaurant_table CASCADE;

-- PASO 2: Si id ya existe y es PK, usarlo. Si no, crear uno nuevo
-- Primero, eliminar la PK actual si es table_number
DO $$
DECLARE
  constraint_name text;
BEGIN
  SELECT c.constraint_name INTO constraint_name
  FROM information_schema.table_constraints c
  WHERE c.table_name = 'restaurant_tables'
  AND c.constraint_type = 'PRIMARY KEY';

  IF constraint_name IS NOT NULL AND constraint_name != 'id_pkey' THEN
    EXECUTE format('ALTER TABLE restaurant_tables DROP CONSTRAINT %I', constraint_name);
  END IF;
END $$;

-- PASO 3: Agregar columna id si no existe
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'restaurant_tables' AND column_name = 'id'
  ) THEN
    ALTER TABLE restaurant_tables ADD COLUMN id SERIAL;
  END IF;
END $$;

-- PASO 4: Crear PRIMARY KEY en id (esto es obligatorio para FK)
ALTER TABLE restaurant_tables ADD PRIMARY KEY (id);

-- PASO 5: Hacer table_number UNIQUE
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE table_name = 'restaurant_tables'
    AND constraint_type = 'UNIQUE'
  ) THEN
    ALTER TABLE restaurant_tables ADD CONSTRAINT uk_restaurant_table_number UNIQUE (table_number);
  END IF;
END $$;

-- PASO 6: Convertir restaurant_table_id en table_sessions a INTEGER
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'table_sessions'
    AND column_name = 'restaurant_table_id'
    AND data_type = 'character varying'
  ) THEN
    ALTER TABLE table_sessions
    ALTER COLUMN restaurant_table_id TYPE INTEGER USING CAST(restaurant_table_id AS INTEGER);
  END IF;
END $$;

-- PASO 7: Agregar FK nuevamente (ahora con PK en id)
ALTER TABLE table_sessions
ADD CONSTRAINT fk_table_sessions_restaurant_table
FOREIGN KEY (restaurant_table_id) REFERENCES restaurant_tables(id) ON DELETE RESTRICT;

-- PASO 8: Crear índices
CREATE INDEX IF NOT EXISTS idx_table_sessions_restaurant_table_id ON table_sessions(restaurant_table_id);
CREATE INDEX IF NOT EXISTS idx_table_sessions_token ON table_sessions(session_token);
