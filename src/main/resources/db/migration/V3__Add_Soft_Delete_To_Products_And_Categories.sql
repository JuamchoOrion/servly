-- V3__Add_Soft_Delete_To_Products_And_Categories.sql
-- Agregar campos de soft delete a las tablas de productos y categorías

-- Agregar columnas de soft delete a la tabla 'product'
ALTER TABLE product ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE product ADD COLUMN deleted_at TIMESTAMP NULL;

-- Agregar índice para mejorar performance en consultas con deleted = false
CREATE INDEX idx_product_deleted ON product(deleted);
CREATE INDEX idx_product_deleted_deleted_at ON product(deleted, deleted_at);

-- Agregar columnas de soft delete a la tabla 'product_categories'
ALTER TABLE product_categories ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE product_categories ADD COLUMN deleted_at TIMESTAMP NULL;

-- Agregar índices para mejorar performance
CREATE INDEX idx_product_categories_deleted ON product_categories(deleted);
CREATE INDEX idx_product_categories_deleted_deleted_at ON product_categories(deleted, deleted_at);

-- Las columnas deleted y deleted_at para item_categories ya existen desde la migración anterior

