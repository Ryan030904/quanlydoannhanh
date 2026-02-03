USE FastFoodManagement;

SET @db := DATABASE();

SET @has_ing_supplier_id := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'ingredients' AND column_name = 'supplier_id'
);

SET @has_sup_is_active := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'suppliers' AND column_name = 'is_active'
);

SET @has_sup_status := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = @db AND table_name = 'suppliers' AND column_name = 'status'
);

SET @active_col := IF(@has_sup_is_active > 0, 'is_active', IF(@has_sup_status > 0, 'status', NULL));
SET @active_where := IF(@active_col IS NULL, '1=1', CONCAT('s.', @active_col, ' = 1'));

SET @join_ing := IF(@has_ing_supplier_id > 0,
  ' LEFT JOIN ingredients i ON i.supplier_id = s.supplier_id ',
  ' '
);

SET @where_ing_null := IF(@has_ing_supplier_id > 0, ' AND i.ingredient_id IS NULL ', ' ');

SET @list_sql := CONCAT(
  'SELECT s.supplier_id, s.supplier_name, s.phone, s.email, s.address, s.notes',
  ' FROM suppliers s',
  ' LEFT JOIN inventory_transactions it ON it.supplier_id = s.supplier_id',
  @join_ing,
  ' WHERE ', @active_where,
  ' AND it.transaction_id IS NULL',
  @where_ing_null,
  ' ORDER BY s.supplier_id'
);

PREPARE stmt FROM @list_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @do_update := 1;

SET @update_sql := IF(
  @do_update = 1 AND @active_col IS NOT NULL,
  CONCAT(
    'UPDATE suppliers s',
    ' LEFT JOIN inventory_transactions it ON it.supplier_id = s.supplier_id',
    @join_ing,
    ' SET s.', @active_col, ' = 0',
    ' WHERE ', @active_where,
    ' AND it.transaction_id IS NULL',
    @where_ing_null
  ),
  'SELECT 0'
);

PREPARE stmt2 FROM @update_sql;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
