-- Agregar columnas de soft delete a la tabla recipe si no existen
DO $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_name='recipe' AND column_name='deleted') THEN
        ALTER TABLE recipe ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_name='recipe' AND column_name='deleted_at') THEN
        ALTER TABLE recipe ADD COLUMN deleted_at TIMESTAMP;
    END IF;
END $$;


