-- Agregar columnas de soft delete a product_categories si no existen
DO $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_name='product_categories' AND column_name='deleted') THEN
        ALTER TABLE product_categories ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_name='product_categories' AND column_name='deleted_at') THEN
        ALTER TABLE product_categories ADD COLUMN deleted_at TIMESTAMP;
    END IF;
END $$;

-- Agregar columnas de soft delete a product si no existen
DO $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_name='product' AND column_name='deleted') THEN
        ALTER TABLE product ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
    END IF;

    IF NOT EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_name='product' AND column_name='deleted_at') THEN
        ALTER TABLE product ADD COLUMN deleted_at TIMESTAMP;
    END IF;
END $$;

