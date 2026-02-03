USE quanlybandoannhanh;
SET NAMES utf8mb4;

-- Add employees.username
SET @col := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'employees'
      AND COLUMN_NAME = 'username'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE employees ADD COLUMN username VARCHAR(50) UNIQUE AFTER employee_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

 -- Create permission_codes catalog
 SET @tbl := (
     SELECT COUNT(*)
     FROM INFORMATION_SCHEMA.TABLES
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'permission_codes'
 );
 SET @sql := IF(@tbl = 0,
     'CREATE TABLE permission_codes (code VARCHAR(3) PRIMARY KEY, name VARCHAR(100) NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci',
     'SELECT 1'
 );
 PREPARE stmt FROM @sql;
 EXECUTE stmt;
 DEALLOCATE PREPARE stmt;

 INSERT INTO permission_codes (code, name) VALUES
     ('PQ0','admin'),
     ('PQ2','Nhân viên')
 ON DUPLICATE KEY UPDATE name = VALUES(name);

 -- Add employees.permission_code (PQ0..PQ2)
 SET @col := (
     SELECT COUNT(*)
     FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'employees'
       AND COLUMN_NAME = 'permission_code'
 );
 SET @sql := IF(@col = 0,
     'ALTER TABLE employees ADD COLUMN permission_code VARCHAR(3) NOT NULL DEFAULT ''PQ2'' AFTER username',
     'SELECT 1'
 );
 PREPARE stmt FROM @sql;
 EXECUTE stmt;
 DEALLOCATE PREPARE stmt;

-- Add employees.password_hash
SET @col := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'employees'
      AND COLUMN_NAME = 'password_hash'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE employees ADD COLUMN password_hash VARCHAR(255) NULL AFTER username',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

 -- Add employees.password_plain (for UI display)
 SET @col := (
     SELECT COUNT(*)
     FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'employees'
       AND COLUMN_NAME = 'password_plain'
 );
 SET @sql := IF(@col = 0,
     'ALTER TABLE employees ADD COLUMN password_plain VARCHAR(255) NULL AFTER password_hash',
     'SELECT 1'
 );
 PREPARE stmt FROM @sql;
 EXECUTE stmt;
 DEALLOCATE PREPARE stmt;

-- Add categories.is_active
SET @col := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'categories'
      AND COLUMN_NAME = 'is_active'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE categories ADD COLUMN is_active BOOLEAN DEFAULT TRUE AFTER description',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add products.product_code
SET @col := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'products'
      AND COLUMN_NAME = 'product_code'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE products ADD COLUMN product_code VARCHAR(30) UNIQUE AFTER product_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add orders.order_number
SET @col := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'order_number'
);
SET @sql := IF(@col = 0,
    'ALTER TABLE orders ADD COLUMN order_number VARCHAR(50) UNIQUE NULL AFTER order_id',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Backfill defaults
UPDATE categories SET is_active = 1 WHERE is_active IS NULL;

UPDATE products
SET product_code = CONCAT('P', LPAD(product_id, 4, '0'))
WHERE product_code IS NULL OR product_code = '';

UPDATE orders
SET order_number = CONCAT('ORD-', LPAD(order_id, 6, '0'))
WHERE order_number IS NULL OR order_number = '';

-- Create usernames for existing employees (if not set)
UPDATE employees
SET username = CASE
    WHEN position = 'manager' THEN CONCAT('manager', employee_id)
    WHEN position = 'staff' THEN CONCAT('staff', employee_id)
    WHEN email IS NOT NULL AND email <> '' THEN LOWER(REPLACE(SUBSTRING_INDEX(email, '@', 1), '.', ''))
    ELSE CONCAT('user', employee_id)
END
WHERE username IS NULL OR username = '';

-- Set plain-text passwords (Java app will auto-upgrade to PBKDF2 on successful login)
UPDATE employees
SET password_hash = CASE
    WHEN username = 'admin' THEN 'admin123'
    WHEN username = 'staff' THEN 'staff123'
    ELSE password_hash
END;

 UPDATE employees
 SET password_plain = CASE
     WHEN username = 'admin' THEN 'admin123'
     WHEN username = 'staff' THEN 'staff123'
     WHEN password_hash IS NOT NULL AND password_hash <> '' AND password_hash NOT LIKE 'pbkdf2$%' THEN password_hash
     ELSE password_plain
 END
 WHERE password_plain IS NULL OR password_plain = '';

 -- Backfill permission_code
 UPDATE employees
 SET permission_code = CASE
     WHEN username = 'admin' THEN 'PQ0'
     WHEN position = 'manager' THEN 'PQ0'
     ELSE 'PQ2'
 END
 WHERE permission_code IS NULL OR permission_code = ''
    OR (username = 'admin' AND permission_code <> 'PQ0');

 INSERT INTO employees (full_name, username, password_hash, password_plain, position, role, is_active, permission_code)
 VALUES ('Quản trị', 'admin', 'admin123', 'admin123', 'manager', 'Manager', 1, 'PQ0')
 ON DUPLICATE KEY UPDATE
     full_name = VALUES(full_name),
     password_hash = VALUES(password_hash),
     password_plain = VALUES(password_plain),
     position = VALUES(position),
     role = VALUES(role),
     is_active = VALUES(is_active),
     permission_code = VALUES(permission_code);

 UPDATE employees
 SET is_active = 0
 WHERE username NOT IN ('admin','staff');

 INSERT INTO employees (full_name, username, password_hash, password_plain, position, role, is_active, permission_code)
 VALUES ('Nhân viên', 'staff', 'staff123', 'staff123', 'staff', 'Staff', 1, 'PQ2')
 ON DUPLICATE KEY UPDATE
     full_name = VALUES(full_name),
     password_hash = VALUES(password_hash),
     password_plain = VALUES(password_plain),
     position = VALUES(position),
     role = VALUES(role),
     is_active = VALUES(is_active),
     permission_code = VALUES(permission_code);

-- Optional: ensure admin/staff accounts are active
UPDATE employees
SET is_active = 1
WHERE username IN ('admin','staff');
