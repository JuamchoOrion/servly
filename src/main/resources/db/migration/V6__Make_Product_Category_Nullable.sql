-- Make category_id nullable in product table
ALTER TABLE product
ALTER COLUMN category_id DROP NOT NULL;

-- Add index for faster lookups
CREATE INDEX idx_product_category_id ON product(category_id) WHERE deleted = false;

