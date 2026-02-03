USE quanlybandoannhanh;

START TRANSACTION;

-- Chuẩn hoá chuỗi supplier trong ingredients để filter theo tên không bị lệch do khoảng trắng
UPDATE ingredients
SET supplier = TRIM(supplier)
WHERE supplier IS NOT NULL;

-- Tự động thêm NCC còn thiếu dựa theo supplier text đang có trong ingredients
INSERT INTO suppliers (supplier_name, is_active)
SELECT DISTINCT TRIM(i.supplier) AS supplier_name, 1 AS is_active
FROM ingredients i
LEFT JOIN suppliers s
  ON LOWER(TRIM(s.supplier_name)) = LOWER(TRIM(i.supplier))
WHERE i.supplier IS NOT NULL
  AND TRIM(i.supplier) <> ''
  AND s.supplier_id IS NULL;

COMMIT;

-- Kiểm tra: còn ingredient nào có supplier nhưng chưa tồn tại trong suppliers?
SELECT DISTINCT i.supplier
FROM ingredients i
LEFT JOIN suppliers s
  ON LOWER(TRIM(s.supplier_name)) = LOWER(TRIM(i.supplier))
WHERE i.supplier IS NOT NULL
  AND TRIM(i.supplier) <> ''
  AND s.supplier_id IS NULL
ORDER BY i.supplier;

-- Kiểm tra: supplier bị trùng tên (ignore-case/trim) có thể làm UI khó hiểu
SELECT LOWER(TRIM(supplier_name)) AS normalized_name, COUNT(*) AS cnt
FROM suppliers
GROUP BY LOWER(TRIM(supplier_name))
HAVING COUNT(*) > 1
ORDER BY cnt DESC;
