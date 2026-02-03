USE quanlybandoannhanh;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM inventory;
DELETE FROM product_ingredients;
DELETE FROM products;

ALTER TABLE products AUTO_INCREMENT = 1;

INSERT IGNORE INTO categories (category_name, description, is_active)
VALUES
('Gà rán','',1),
('Burger & sandwich','',1),
('Pizza & Hotdog','',1),
('Khoai tây & Snack','',1),
('Salad','',1),
('Đồ uống','',1),
('Tráng miệng','',1);

INSERT INTO products (product_code, product_name, category_id, price, description, image_url, is_available)
VALUES
('SP001','Burger bò phô mai',(SELECT category_id FROM categories WHERE category_name='Burger & sandwich' LIMIT 1),45000,'','img/Burger bò phô mai.jpg',1),
('SP002','Burger gà giòn',(SELECT category_id FROM categories WHERE category_name='Burger & sandwich' LIMIT 1),45000,'','img/Burger gà giòn.jpg',1),
('SP003','Gà rán 2 miếng',(SELECT category_id FROM categories WHERE category_name='Gà rán' LIMIT 1),35000,'','img/Gà rán 2 miếng.jpg',1),
('SP004','Gà rán 3 miếng',(SELECT category_id FROM categories WHERE category_name='Gà rán' LIMIT 1),45000,'','img/Gà rán 3 miếng.jpg',1),
('SP005','Pizza Pepperoni (1 miếng)',(SELECT category_id FROM categories WHERE category_name='Pizza & Hotdog' LIMIT 1),35000,'','img/Pizza Pepperoni(1 miếng).jpg',1),
('SP006','Hotdog xúc xích nướng',(SELECT category_id FROM categories WHERE category_name='Pizza & Hotdog' LIMIT 1),35000,'','img/Hotdog xúc xích nướng.jpg',1),
('SP007','Sandwich cá chiên',(SELECT category_id FROM categories WHERE category_name='Burger & sandwich' LIMIT 1),40000,'','img/Sandwich cá chiên.jpg',1),
('SP008','Wrap gà giòn',(SELECT category_id FROM categories WHERE category_name='Burger & sandwich' LIMIT 1),42000,'','img/Wrap gà giòn.png',1),

('SP009','Coca-Cola',(SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1),15000,'','img/Coca-Cola.png',1),
('SP010','Pepsi',(SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1),15000,'','img/Pepsi.png',1),
('SP011','7Up',(SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1),15000,'','img/7Up.png',1),
('SP012','Fanta cam',(SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1),15000,'','img/Fanta cam.png',1),
('SP013','Nước suối đóng chai',(SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1),12000,'','img/Nước suối đóng chai.png',1),
('SP014','Trà sữa truyền thống',(SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1),25000,'','img/Trà sữa truyền thống.png',1),
('SP015','Cà phê sữa đá',(SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1),22000,'','img/Cà phê sữa đá.png',1),
('SP016','Nước cam ép',(SELECT category_id FROM categories WHERE category_name='Đồ uống' LIMIT 1),20000,'','img/Nước cam ép.png',1),

('SP017','Kem vanilla',(SELECT category_id FROM categories WHERE category_name='Tráng miệng' LIMIT 1),18000,'','img/Kem vanilla.png',1),
('SP018','Kem dâu',(SELECT category_id FROM categories WHERE category_name='Tráng miệng' LIMIT 1),18000,'','img/Kem dâu.png',1),
('SP019','Bánh flan caramel',(SELECT category_id FROM categories WHERE category_name='Tráng miệng' LIMIT 1),15000,'','img/Bánh flan caramel.png',1),
('SP020','Sundae socola',(SELECT category_id FROM categories WHERE category_name='Tráng miệng' LIMIT 1),25000,'','img/Sundae socola.png',1),
('SP021','Sundae dâu',(SELECT category_id FROM categories WHERE category_name='Tráng miệng' LIMIT 1),25000,'','img/Sundae dâu.png',1),
('SP022','Bánh brownie socola',(SELECT category_id FROM categories WHERE category_name='Tráng miệng' LIMIT 1),22000,'','img/Bánh brownie socola.png',1),
('SP023','Pudding socola',(SELECT category_id FROM categories WHERE category_name='Tráng miệng' LIMIT 1),20000,'','img/Pudding socola.jpg',1),

('SP024','Khoai tây chiên',(SELECT category_id FROM categories WHERE category_name='Khoai tây & Snack' LIMIT 1),25000,'','img/Khoai tây chiên.jpg',1),
('SP025','Khoai tây lắc phô mai',(SELECT category_id FROM categories WHERE category_name='Khoai tây & Snack' LIMIT 1),30000,'','img/Khoai tây lắc phô mai.jpg',1),
('SP026','Gà popcorn',(SELECT category_id FROM categories WHERE category_name='Gà rán' LIMIT 1),30000,'','img/Gà popcorn.jpg',1),
('SP027','Nugget gà (6 miếng)',(SELECT category_id FROM categories WHERE category_name='Gà rán' LIMIT 1),35000,'','img/Nugget gà (6 miếng).jpg',1),
('SP028','Phô mai que',(SELECT category_id FROM categories WHERE category_name='Khoai tây & Snack' LIMIT 1),28000,'','img/Phô mai que.jpg',1),
('SP029','Hành tây chiên',(SELECT category_id FROM categories WHERE category_name='Khoai tây & Snack' LIMIT 1),25000,'','img/Hành tây chiên.jpg',1),
('SP030','Salad rau trộn',(SELECT category_id FROM categories WHERE category_name='Salad' LIMIT 1),30000,'','img/Salad rau trộn.jpg',1);

INSERT INTO inventory (item_id, quantity, unit)
SELECT product_id, 9999, 'phần' FROM products;

SET FOREIGN_KEY_CHECKS = 1;
