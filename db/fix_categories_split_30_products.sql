USE quanlybandoannhanh;

START TRANSACTION;

INSERT IGNORE INTO categories (category_name, description, is_active) VALUES
('Gà rán','',1),
('Burger & sandwich','',1),
('Pizza & Hotdog','',1),
('Khoai tây & Snack','',1),
('Salad','',1),
('Đồ uống','',1),
('Tráng miệng','',1);

UPDATE categories SET is_active = 1
WHERE category_name IN ('Gà rán','Burger & sandwich','Pizza & Hotdog','Khoai tây & Snack','Salad','Đồ uống','Tráng miệng');

UPDATE categories SET is_active = 0
WHERE category_name IN ('Món chính','Đồ ăn vặt','Combo');

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE category_name='Burger & sandwich' LIMIT 1)
WHERE product_code IN ('SP001','SP002','SP007','SP008');

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE category_name='Pizza & Hotdog' LIMIT 1)
WHERE product_code IN ('SP005','SP006');

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE category_name='Gà rán' LIMIT 1)
WHERE product_code IN ('SP003','SP004','SP026','SP027');

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE category_name='Khoai tây & Snack' LIMIT 1)
WHERE product_code IN ('SP024','SP025','SP028','SP029');

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1)
WHERE product_code IN ('SP009','SP010','SP011','SP012','SP013','SP014','SP015','SP016');

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE category_name='Tráng miệng' LIMIT 1)
WHERE product_code IN ('SP017','SP018','SP019','SP020','SP021','SP022','SP023');

UPDATE products
SET category_id = (SELECT category_id FROM categories WHERE category_name='Salad' LIMIT 1)
WHERE product_code IN ('SP030');

COMMIT;

SELECT c.category_name, COUNT(p.product_id) AS product_count
FROM categories c
LEFT JOIN products p ON p.category_id = c.category_id
WHERE c.is_active = 1
GROUP BY c.category_id
ORDER BY c.category_name;
