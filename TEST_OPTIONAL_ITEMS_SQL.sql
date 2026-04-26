-- =====================================================
-- TEST: Verificar que items opcionales se guardan correctamente
-- =====================================================

-- 1. Verificar que la columna existe
SELECT COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'order_detail' AND COLUMN_NAME = 'optional_items';

-- Resultado esperado:
-- | COLUMN_NAME    | DATA_TYPE |
-- | optional_items | TEXT      |

-- =====================================================

-- 2. Verificar órdenes con items opcionales
SELECT
  od.id,
  od.quantity,
  od.unit_price,
  od.annotations,
  od.optional_items,
  p.name as product_name
FROM order_detail od
LEFT JOIN product p ON od.product_id = p.id
WHERE od.optional_items IS NOT NULL
ORDER BY od.id DESC
LIMIT 10;

-- Resultado esperado: Órdenes con campo optional_items lleno con JSON

-- =====================================================

-- 3. Parsear JSON y ver contenido (PostgreSQL)
SELECT
  od.id,
  p.name as product_name,
  od.quantity,
  jsonb_array_elements(od.optional_items::jsonb) as optional_item
FROM order_detail od
LEFT JOIN product p ON od.product_id = p.id
WHERE od.optional_items IS NOT NULL
ORDER BY od.id DESC;

-- Resultado esperado:
-- | id  | product_name      | quantity | optional_item                                    |
-- | 45  | Hamburguesa       | 2        | {"itemId": 5, "itemName": "Queso", "quantity": 2} |
-- | 45  | Hamburguesa       | 2        | {"itemId": 8, "itemName": "Bacon", "quantity": 1} |

-- =====================================================

-- 4. Contar cuántos detalles tienen items opcionales
SELECT
  COUNT(*) as total_detalles,
  SUM(CASE WHEN optional_items IS NOT NULL THEN 1 ELSE 0 END) as con_extras,
  SUM(CASE WHEN optional_items IS NULL THEN 1 ELSE 0 END) as sin_extras
FROM order_detail;

-- Resultado esperado:
-- | total_detalles | con_extras | sin_extras |
-- | 150            | 45         | 105        |

-- =====================================================

-- 5. Ver órdenes completas con sus items y extras
SELECT
  o.id as order_id,
  o.date,
  o.total,
  o.status,
  json_object_agg(
    od.id::text,
    json_build_object(
      'product', p.name,
      'quantity', od.quantity,
      'annotations', od.annotations,
      'optional_items', od.optional_items
    )
  ) as items_with_extras
FROM "order" o
LEFT JOIN order_detail od ON o.id = od.order_id
LEFT JOIN product p ON od.product_id = p.id
WHERE o.status IN ('PENDING', 'IN_PREPARATION', 'SERVED')
GROUP BY o.id, o.date, o.total, o.status
ORDER BY o.id DESC
LIMIT 5;

-- =====================================================

-- 6. Órdenes de una mesa específica con sus extras
SELECT
  o.id as order_id,
  ts.table_number,
  od.id as detail_id,
  p.name as product_name,
  od.quantity,
  od.annotations,
  od.optional_items,
  od.subtotal
FROM "order" o
JOIN table_source ts ON o.source_id = ts.id
JOIN order_detail od ON o.id = od.order_id
JOIN product p ON od.product_id = p.id
WHERE ts.table_number = 3
AND o.status NOT IN ('CANCELLED', 'PAID')
ORDER BY o.id DESC, od.id;

-- =====================================================

-- 7. Buscar órdenes con extras específicos (Queso extra)
SELECT
  o.id as order_id,
  ts.table_number,
  p.name as product_name,
  od.quantity,
  od.optional_items
FROM "order" o
JOIN table_source ts ON o.source_id = ts.id
JOIN order_detail od ON o.id = od.order_id
JOIN product p ON od.product_id = p.id
WHERE od.optional_items LIKE '%Queso%'
OR od.optional_items LIKE '%queso%'
ORDER BY o.id DESC;

-- =====================================================

-- 8. Estadísticas de items extras más solicitados
SELECT
  jsonb_array_elements(optional_items::jsonb)->>'itemName' as extra_name,
  COUNT(*) as veces_solicitado,
  SUM((jsonb_array_elements(optional_items::jsonb)->'quantity')::int) as cantidad_total
FROM order_detail
WHERE optional_items IS NOT NULL
GROUP BY extra_name
ORDER BY veces_solicitado DESC;

-- Resultado esperado (ejemplo):
-- | extra_name        | veces_solicitado | cantidad_total |
-- | Queso extra       | 42               | 78             |
-- | Bacon             | 31               | 41             |
-- | Salsa especial    | 28               | 35             |
-- | Cebolla extra     | 19               | 24             |

-- =====================================================

-- 9. Verificar integridad: órdenes con JSON válido
SELECT
  COUNT(*) as total_con_extras,
  SUM(CASE
    WHEN optional_items IS NOT NULL
    AND optional_items != '[]'
    AND optional_items != 'null' THEN 1
    ELSE 0
  END) as json_valido,
  SUM(CASE
    WHEN optional_items = '[]' OR optional_items = 'null' THEN 1
    ELSE 0
  END) as json_vacio
FROM order_detail
WHERE optional_items IS NOT NULL;

-- =====================================================

-- 10. Backup: Exportar todas las órdenes con extras
SELECT
  o.id as order_id,
  COALESCE(ts.table_number, 0) as table_number,
  o.date,
  o.status,
  json_agg(
    json_build_object(
      'product_id', od.product_id,
      'product_name', p.name,
      'quantity', od.quantity,
      'unit_price', od.unit_price,
      'annotations', od.annotations,
      'optional_items', od.optional_items::jsonb
    )
  ) as items
FROM "order" o
LEFT JOIN table_source ts ON o.source_id = ts.id
LEFT JOIN order_detail od ON o.id = od.order_id
LEFT JOIN product p ON od.product_id = p.id
WHERE o.date >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY o.id, ts.table_number, o.date, o.status
ORDER BY o.date DESC;

