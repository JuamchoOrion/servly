-- Agregar columna image_url a la tabla product
ALTER TABLE product
ADD COLUMN image_url VARCHAR(500),
ADD COLUMN public_id VARCHAR(255);

