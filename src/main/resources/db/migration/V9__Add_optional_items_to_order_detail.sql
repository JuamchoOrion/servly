-- Agregar columna para almacenar items opcionales en las órdenes
ALTER TABLE order_detail
ADD COLUMN optional_items TEXT  COMMENT 'Items opcionales elegidos por el cliente en formato JSON: [{"itemId": 1, "itemName": "Extra", "quantity": 2}]';

