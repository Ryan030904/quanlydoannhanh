USE quanlybandoannhanh;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET collation_connection = 'utf8mb4_unicode_ci';

START TRANSACTION;

-- Detect FK column name in product_ingredients (product_id vs item_id)
SET @pi_pid := (
  SELECT CASE
    WHEN EXISTS (
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE BINARY TABLE_SCHEMA = BINARY DATABASE()
        AND BINARY TABLE_NAME = BINARY 'product_ingredients'
        AND BINARY COLUMN_NAME = BINARY 'product_id'
    ) THEN 'product_id'
    WHEN EXISTS (
      SELECT 1
      FROM INFORMATION_SCHEMA.COLUMNS
      WHERE BINARY TABLE_SCHEMA = BINARY DATABASE()
        AND BINARY TABLE_NAME = BINARY 'product_ingredients'
        AND BINARY COLUMN_NAME = BINARY 'item_id'
    ) THEN 'item_id'
    ELSE 'product_id'
  END
);

-- Normalize collation for dynamic SQL parts (avoid #1267 in phpMyAdmin)
SET @pi_pid := CONVERT(@pi_pid USING utf8mb4) COLLATE utf8mb4_unicode_ci;

-- Xóa công thức cũ của các món SPxxx (nếu có) để seed lại sạch
DELETE pi
FROM product_ingredients pi
JOIN products p ON p.product_id = pi.product_id
WHERE CONVERT(p.product_code USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE 'SP%';

DROP TEMPORARY TABLE IF EXISTS tmp_recipe_seed;
CREATE TEMPORARY TABLE tmp_recipe_seed (
  product_code VARCHAR(50) NOT NULL,
  ingredient_name VARCHAR(200) NOT NULL,
  quantity_needed DECIMAL(10,2) NOT NULL
);

INSERT INTO tmp_recipe_seed (product_code, ingredient_name, quantity_needed) VALUES
-- SP001: Burger bò phô mai
('SP001','Thịt bò xay',0.15),
('SP001','Bánh burger',1),
('SP001','Phô mai lát',1),
('SP001','Rau xà lách',0.03),
('SP001','Cà chua',0.05),
('SP001','Hành tây',0.03),
('SP001','Sốt mayonnaise',0.02),
('SP001','Tương cà',0.02),

-- SP002: Burger gà giòn
('SP002','Phi lê gà',0.12),
('SP002','Bánh burger',1),
('SP002','Bột chiên giòn',0.03),
('SP002','Rau xà lách',0.03),
('SP002','Cà chua',0.05),
('SP002','Sốt mayonnaise',0.02),
('SP002','Dầu ăn',0.05),

-- SP003: Gà rán 2 miếng
('SP003','Gà tươi nguyên con',0.35),
('SP003','Bột chiên giòn',0.05),
('SP003','Dầu ăn',0.10),
('SP003','Muối',0.01),
('SP003','Trứng gà',1),

-- SP004: Gà rán 3 miếng
('SP004','Gà tươi nguyên con',0.50),
('SP004','Bột chiên giòn',0.08),
('SP004','Dầu ăn',0.15),
('SP004','Muối',0.02),
('SP004','Trứng gà',2),

-- SP005: Pizza Pepperoni (1 miếng)
('SP005','Đế pizza',1),
('SP005','Pepperoni',0.08),
('SP005','Phô mai lát',2),
('SP005','Sốt pizza',0.05),

-- SP006: Hotdog xúc xích nướng
('SP006','Xúc xích',0.10),
('SP006','Bánh hotdog',1),
('SP006','Tương cà',0.02),
('SP006','Tương ớt',0.01),
('SP006','Hành tây',0.02),

-- SP007: Sandwich cá chiên
('SP007','Cá phi lê',0.12),
('SP007','Bánh mì sandwich',1),
('SP007','Bột chiên giòn',0.03),
('SP007','Rau xà lách',0.03),
('SP007','Sốt mayonnaise',0.02),
('SP007','Dầu ăn',0.05),

-- SP008: Wrap gà giòn
('SP008','Phi lê gà',0.10),
('SP008','Bánh tortilla',1),
('SP008','Bột chiên giòn',0.03),
('SP008','Rau xà lách',0.04),
('SP008','Cà chua',0.04),
('SP008','Sốt mayonnaise',0.02),
('SP008','Dầu ăn',0.04),

-- SP009-SP012: Nước ngọt
('SP009','Coca-Cola lon',1),
('SP010','Pepsi lon',1),
('SP011','7Up lon',1),
('SP012','Fanta lon',1),

-- SP013: Nước suối
('SP013','Nước suối chai',1),

-- SP014: Trà sữa truyền thống
('SP014','Trà khô',0.01),
('SP014','Sữa tươi',0.10),
('SP014','Trân châu',0.03),
('SP014','Đường',0.02),
('SP014','Đá viên',0.10),

-- SP015: Cà phê sữa đá
('SP015','Cà phê xay',0.02),
('SP015','Sữa tươi',0.05),
('SP015','Đường',0.02),
('SP015','Đá viên',0.10),

-- SP016: Nước cam ép
('SP016','Cam tươi',0.25),
('SP016','Đường',0.01),
('SP016','Đá viên',0.10),

-- SP017: Kem vanilla
('SP017','Kem vanilla',0.10),

-- SP018: Kem dâu
('SP018','Kem dâu',0.10),

-- SP019: Bánh flan caramel
('SP019','Bánh flan mix',0.05),
('SP019','Sữa tươi',0.10),
('SP019','Trứng gà',2),
('SP019','Đường',0.02),

-- SP020: Sundae socola
('SP020','Kem vanilla',0.08),
('SP020','Sốt socola',0.03),

-- SP021: Sundae dâu
('SP021','Kem vanilla',0.08),
('SP021','Sốt dâu',0.03),

-- SP022: Bánh brownie socola
('SP022','Bột brownie',0.08),
('SP022','Trứng gà',1),
('SP022','Dầu ăn',0.02),

-- SP023: Pudding socola
('SP023','Bột pudding',0.05),
('SP023','Sữa tươi',0.10),
('SP023','Sốt socola',0.02),

-- SP024: Khoai tây chiên
('SP024','Khoai tây',0.15),
('SP024','Dầu ăn',0.10),
('SP024','Muối',0.01),

-- SP025: Khoai tây lắc phô mai
('SP025','Khoai tây',0.15),
('SP025','Phô mai bột',0.02),
('SP025','Dầu ăn',0.10),
('SP025','Muối',0.01),

-- SP026: Gà popcorn
('SP026','Phi lê gà',0.12),
('SP026','Bột chiên giòn',0.04),
('SP026','Dầu ăn',0.08),
('SP026','Muối',0.01),

-- SP027: Nugget gà (6 miếng)
('SP027','Phi lê gà',0.12),
('SP027','Bột chiên giòn',0.04),
('SP027','Dầu ăn',0.08),
('SP027','Trứng gà',1),

-- SP028: Phô mai que
('SP028','Phô mai lát',3),
('SP028','Bột chiên giòn',0.03),
('SP028','Dầu ăn',0.06),
('SP028','Trứng gà',1),

-- SP029: Hành tây chiên
('SP029','Hành tây',0.15),
('SP029','Bột chiên giòn',0.03),
('SP029','Dầu ăn',0.08),

-- SP030: Salad rau trộn
('SP030','Rau xà lách',0.10),
('SP030','Cà chua',0.08),
('SP030','Dưa chuột',0.05),
('SP030','Hành tây',0.03),
('SP030','Sốt mayonnaise',0.03);

-- Insert công thức: join theo product_code và ingredient_name (normalize '-' để tránh lệch Coca-Cola/Coca Cola)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed)
SELECT p.product_id, i.ingredient_id, t.quantity_needed
FROM tmp_recipe_seed t
JOIN products p
  ON CONVERT(p.product_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
   = CONVERT(t.product_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
JOIN ingredients i
  ON LOWER(CONVERT(REPLACE(i.ingredient_name,'-','') USING utf8mb4) COLLATE utf8mb4_unicode_ci)
   = LOWER(CONVERT(REPLACE(t.ingredient_name,'-','') USING utf8mb4) COLLATE utf8mb4_unicode_ci)
ON DUPLICATE KEY UPDATE quantity_needed = VALUES(quantity_needed);

COMMIT;

-- Báo các dòng không map được (thiếu product_code hoặc lệch tên nguyên liệu)
SELECT t.product_code, t.ingredient_name, t.quantity_needed,
       p.product_id,
       i.ingredient_id
FROM tmp_recipe_seed t
LEFT JOIN products p
  ON CONVERT(p.product_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
   = CONVERT(t.product_code USING utf8mb4) COLLATE utf8mb4_unicode_ci
LEFT JOIN ingredients i
  ON LOWER(CONVERT(REPLACE(i.ingredient_name,'-','') USING utf8mb4) COLLATE utf8mb4_unicode_ci)
   = LOWER(CONVERT(REPLACE(t.ingredient_name,'-','') USING utf8mb4) COLLATE utf8mb4_unicode_ci)
WHERE p.product_id IS NULL OR i.ingredient_id IS NULL
ORDER BY t.product_code, t.ingredient_name;

-- Kiểm tra nhanh số dòng công thức mỗi món
SELECT p.product_id, p.product_code, p.product_name, COUNT(pi.ingredient_id) AS recipe_lines
FROM products p
LEFT JOIN product_ingredients pi ON p.product_id = pi.product_id
WHERE CONVERT(p.product_code USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE 'SP%'
GROUP BY p.product_id
ORDER BY p.product_id;
