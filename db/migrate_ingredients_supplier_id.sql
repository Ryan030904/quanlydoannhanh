USE quanlybandoannhanh;

START TRANSACTION;

-- 1) Add supplier_id column if missing
SET @has_col := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ingredients'
    AND COLUMN_NAME = 'supplier_id'
);

SET @sql := IF(@has_col = 0,
  'ALTER TABLE ingredients ADD COLUMN supplier_id INT NULL AFTER supplier',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) Ensure supplier text is trimmed
UPDATE ingredients SET supplier = TRIM(supplier) WHERE supplier IS NOT NULL;

-- 3) Insert any missing suppliers from ingredients.supplier
INSERT INTO suppliers (supplier_name, is_active)
SELECT DISTINCT TRIM(i.supplier) AS supplier_name, 1
FROM ingredients i
LEFT JOIN suppliers s
  ON LOWER(TRIM(s.supplier_name)) = LOWER(TRIM(i.supplier))
WHERE i.supplier IS NOT NULL
  AND TRIM(i.supplier) <> ''
  AND s.supplier_id IS NULL;

-- 4) Map supplier_id from supplier name
UPDATE ingredients i
JOIN suppliers s
  ON LOWER(TRIM(s.supplier_name)) = LOWER(TRIM(i.supplier))
SET i.supplier_id = s.supplier_id
WHERE i.supplier IS NOT NULL
  AND TRIM(i.supplier) <> ''
  AND (i.supplier_id IS NULL OR i.supplier_id = 0);

-- 5) Normalize supplier text to exactly match supplier_name when supplier_id is present
UPDATE ingredients i
JOIN suppliers s ON s.supplier_id = i.supplier_id
SET i.supplier = s.supplier_name
WHERE i.supplier_id IS NOT NULL;

-- 6) Add FK constraint if missing
SET @has_fk := (
  SELECT COUNT(*)
  FROM information_schema.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ingredients'
    AND COLUMN_NAME = 'supplier_id'
    AND REFERENCED_TABLE_NAME = 'suppliers'
);

SET @sql := IF(@has_fk = 0,
  'ALTER TABLE ingredients ADD CONSTRAINT fk_ingredients_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id) ON DELETE SET NULL',
  'SELECT 1'
);
PREPARE stmt2 FROM @sql; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

COMMIT;

-- Check: any ingredient has supplier text but cannot map to supplier_id?
SELECT i.ingredient_id, i.ingredient_name, i.supplier
FROM ingredients i
WHERE i.supplier IS NOT NULL
  AND TRIM(i.supplier) <> ''
  AND i.supplier_id IS NULL
ORDER BY i.ingredient_id;

-- Check: how many ingredients are mapped
SELECT
  SUM(CASE WHEN supplier_id IS NULL THEN 1 ELSE 0 END) AS unmapped,
  SUM(CASE WHEN supplier_id IS NOT NULL THEN 1 ELSE 0 END) AS mapped
FROM ingredients;
