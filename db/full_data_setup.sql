-- ========================================
-- DỮ LIỆU ĐẦY ĐỦ CHO HỆ THỐNG QUẢN LÝ ĐỒ ĂN NHANH
-- Dựa trên 30 món ăn hiện có
-- ========================================

USE quanlybandoannhanh;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ========================================
-- 1. NHÀ CUNG CẤP (SUPPLIERS)
-- ========================================
DELETE FROM inventory_transactions;
DELETE FROM product_ingredients;
DELETE FROM suppliers;
DELETE FROM ingredients WHERE ingredient_id > 0;

INSERT INTO suppliers (supplier_id, supplier_name, phone, email, address, notes, is_active) VALUES
(1, 'Công ty CP Thực phẩm Việt', '0281234567', 'thucphamviet@email.com', '123 Nguyễn Văn Linh, Q.7, TP.HCM', 'Cung cấp thịt, gà, cá tươi sống', 1),
(2, 'Công ty TNHH Rau Sạch Đà Lạt', '0632345678', 'rausachdalat@email.com', '45 Phan Đình Phùng, Đà Lạt', 'Rau củ quả tươi từ Đà Lạt', 1),
(3, 'Đại lý Nước giải khát ABC', '0283456789', 'nuocgiaiktabc@email.com', '789 Điện Biên Phủ, Q.3, TP.HCM', 'Coca-Cola, Pepsi, nước ngọt các loại', 1),
(4, 'Công ty Sữa Việt Nam', '0284567890', 'suavietnam@email.com', '321 Lê Văn Việt, TP.Thủ Đức', 'Sữa tươi, kem, phô mai', 1),
(5, 'Siêu thị Tổng Hợp Metro', '0285678901', 'metro@email.com', '100 Xa lộ Hà Nội, TP.Thủ Đức', 'Gia vị, bột, dầu ăn, đồ khô', 1),
(6, 'Trang trại Gà Minh Phát', '0286789012', 'gaminhphat@email.com', '50 Quốc lộ 1A, Bình Dương', 'Gà tươi, trứng gà', 1),
(7, 'Nhà phân phối Bánh Hoàng Kim', '0287890123', 'banhhoangkim@email.com', '200 Trường Chinh, Q.Tân Bình', 'Bánh burger, bánh mì, bánh pizza', 1),
(8, 'Công ty Hải sản Biển Đông', '0288901234', 'haisanbiendong@email.com', '88 Nguyễn Tất Thành, Q.4', 'Cá, hải sản tươi sống', 1);

-- ========================================
-- 2. NGUYÊN LIỆU (INGREDIENTS)
-- ========================================

INSERT INTO ingredients (ingredient_id, ingredient_name, unit, current_stock, min_stock_level, unit_price, supplier, is_active) VALUES
-- Thịt & Protein
(1, 'Thịt bò xay', 'kg', 50, 10, 180000, 'Công ty CP Thực phẩm Việt', 1),
(2, 'Gà tươi nguyên con', 'kg', 80, 20, 75000, 'Trang trại Gà Minh Phát', 1),
(3, 'Phi lê gà', 'kg', 60, 15, 95000, 'Trang trại Gà Minh Phát', 1),
(4, 'Xúc xích', 'kg', 30, 8, 120000, 'Công ty CP Thực phẩm Việt', 1),
(5, 'Cá phi lê', 'kg', 40, 10, 130000, 'Công ty Hải sản Biển Đông', 1),
(6, 'Pepperoni', 'kg', 20, 5, 250000, 'Công ty CP Thực phẩm Việt', 1),

-- Rau củ
(7, 'Khoai tây', 'kg', 100, 25, 18000, 'Công ty TNHH Rau Sạch Đà Lạt', 1),
(8, 'Hành tây', 'kg', 50, 10, 15000, 'Công ty TNHH Rau Sạch Đà Lạt', 1),
(9, 'Rau xà lách', 'kg', 30, 8, 25000, 'Công ty TNHH Rau Sạch Đà Lạt', 1),
(10, 'Cà chua', 'kg', 40, 10, 20000, 'Công ty TNHH Rau Sạch Đà Lạt', 1),
(11, 'Dưa chuột', 'kg', 30, 8, 18000, 'Công ty TNHH Rau Sạch Đà Lạt', 1),

-- Bánh & Bột
(12, 'Bánh burger', 'cái', 300, 50, 3500, 'Nhà phân phối Bánh Hoàng Kim', 1),
(13, 'Bánh mì sandwich', 'cái', 250, 40, 4000, 'Nhà phân phối Bánh Hoàng Kim', 1),
(14, 'Bánh tortilla', 'cái', 200, 30, 3000, 'Nhà phân phối Bánh Hoàng Kim', 1),
(15, 'Đế pizza', 'cái', 150, 25, 8000, 'Nhà phân phối Bánh Hoàng Kim', 1),
(16, 'Bánh hotdog', 'cái', 200, 30, 3500, 'Nhà phân phối Bánh Hoàng Kim', 1),
(17, 'Bột chiên giòn', 'kg', 50, 10, 28000, 'Siêu thị Tổng Hợp Metro', 1),

-- Sốt & Gia vị
(18, 'Phô mai lát', 'gói', 100, 20, 45000, 'Công ty Sữa Việt Nam', 1),
(19, 'Phô mai bột', 'kg', 30, 8, 150000, 'Công ty Sữa Việt Nam', 1),
(20, 'Sốt mayonnaise', 'lít', 20, 5, 55000, 'Siêu thị Tổng Hợp Metro', 1),
(21, 'Tương cà', 'lít', 25, 5, 35000, 'Siêu thị Tổng Hợp Metro', 1),
(22, 'Tương ớt', 'lít', 20, 5, 40000, 'Siêu thị Tổng Hợp Metro', 1),
(23, 'Sốt pizza', 'lít', 15, 3, 65000, 'Siêu thị Tổng Hợp Metro', 1),

-- Đồ uống
(24, 'Coca-Cola lon', 'lon', 500, 100, 8000, 'Đại lý Nước giải khát ABC', 1),
(25, 'Pepsi lon', 'lon', 500, 100, 8000, 'Đại lý Nước giải khát ABC', 1),
(26, '7Up lon', 'lon', 400, 80, 8000, 'Đại lý Nước giải khát ABC', 1),
(27, 'Fanta lon', 'lon', 400, 80, 8000, 'Đại lý Nước giải khát ABC', 1),
(28, 'Nước suối chai', 'chai', 600, 100, 4000, 'Đại lý Nước giải khát ABC', 1),
(29, 'Cam tươi', 'kg', 50, 10, 35000, 'Công ty TNHH Rau Sạch Đà Lạt', 1),
(30, 'Trà khô', 'kg', 10, 2, 200000, 'Siêu thị Tổng Hợp Metro', 1),
(31, 'Cà phê xay', 'kg', 15, 3, 250000, 'Siêu thị Tổng Hợp Metro', 1),
(32, 'Sữa tươi', 'lít', 100, 20, 30000, 'Công ty Sữa Việt Nam', 1),
(33, 'Trân châu', 'kg', 20, 5, 60000, 'Siêu thị Tổng Hợp Metro', 1),

-- Tráng miệng
(34, 'Kem vanilla', 'lít', 40, 10, 80000, 'Công ty Sữa Việt Nam', 1),
(35, 'Kem dâu', 'lít', 35, 8, 85000, 'Công ty Sữa Việt Nam', 1),
(36, 'Kem socola', 'lít', 35, 8, 85000, 'Công ty Sữa Việt Nam', 1),
(37, 'Sốt socola', 'lít', 15, 3, 70000, 'Siêu thị Tổng Hợp Metro', 1),
(38, 'Sốt dâu', 'lít', 15, 3, 70000, 'Siêu thị Tổng Hợp Metro', 1),
(39, 'Bánh flan mix', 'kg', 20, 5, 90000, 'Siêu thị Tổng Hợp Metro', 1),
(40, 'Bột brownie', 'kg', 15, 3, 120000, 'Siêu thị Tổng Hợp Metro', 1),
(41, 'Bột pudding', 'kg', 15, 3, 100000, 'Siêu thị Tổng Hợp Metro', 1),

-- Dầu & Phụ gia
(42, 'Dầu ăn', 'lít', 80, 15, 35000, 'Siêu thị Tổng Hợp Metro', 1),
(43, 'Muối', 'kg', 30, 5, 8000, 'Siêu thị Tổng Hợp Metro', 1),
(44, 'Đường', 'kg', 40, 10, 20000, 'Siêu thị Tổng Hợp Metro', 1),
(45, 'Đá viên', 'kg', 200, 50, 5000, 'Siêu thị Tổng Hợp Metro', 1),
(46, 'Trứng gà', 'quả', 500, 100, 3500, 'Trang trại Gà Minh Phát', 1);

-- ========================================
-- 3. CÔNG THỨC MÓN ĂN (PRODUCT_INGREDIENTS)
-- ========================================
DELETE FROM product_ingredients;

-- SM001: Burger bò phô mai (product_id = 1)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(1, 1, 0.15),   -- Thịt bò xay 150g
(1, 12, 1),     -- Bánh burger 1 cái
(1, 18, 1),     -- Phô mai lát 1 gói
(1, 9, 0.03),   -- Rau xà lách 30g
(1, 10, 0.05),  -- Cà chua 50g
(1, 8, 0.03),   -- Hành tây 30g
(1, 20, 0.02),  -- Sốt mayo 20ml
(1, 21, 0.02);  -- Tương cà 20ml

-- SM002: Burger gà giòn (product_id = 2)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(2, 3, 0.12),   -- Phi lê gà 120g
(2, 12, 1),     -- Bánh burger 1 cái
(2, 17, 0.03), -- Bột chiên giòn 30g
(2, 9, 0.03),   -- Rau xà lách 30g
(2, 10, 0.05),  -- Cà chua 50g
(2, 20, 0.02),  -- Sốt mayo 20ml
(2, 42, 0.05);  -- Dầu ăn 50ml

-- SM003: Gà rán 2 miếng (product_id = 3)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(3, 2, 0.35),   -- Gà tươi 350g (2 miếng)
(3, 17, 0.05),  -- Bột chiên giòn 50g
(3, 42, 0.1),   -- Dầu ăn 100ml
(3, 43, 0.01),  -- Muối 10g
(3, 46, 1);     -- Trứng gà 1 quả

-- SM004: Gà rán 3 miếng (product_id = 4)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(4, 2, 0.5),    -- Gà tươi 500g (3 miếng)
(4, 17, 0.08),  -- Bột chiên giòn 80g
(4, 42, 0.15),  -- Dầu ăn 150ml
(4, 43, 0.015), -- Muối 15g
(4, 46, 2);     -- Trứng gà 2 quả

-- SM005: Pizza Pepperoni (product_id = 5)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(5, 15, 1),     -- Đế pizza 1 cái
(5, 6, 0.08),   -- Pepperoni 80g
(5, 18, 2),     -- Phô mai lát 2 gói
(5, 23, 0.05);  -- Sốt pizza 50ml

-- SM006: Hotdog xúc xích nướng (product_id = 6)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(6, 4, 0.1),    -- Xúc xích 100g (1 cây)
(6, 16, 1),     -- Bánh hotdog 1 cái
(6, 21, 0.02),  -- Tương cà 20ml
(6, 22, 0.01),  -- Tương ớt 10ml
(6, 8, 0.02);   -- Hành tây 20g

-- SM007: Sandwich cá chiên (product_id = 7)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(7, 5, 0.12),   -- Cá phi lê 120g
(7, 13, 1),     -- Bánh mì sandwich 1 cái
(7, 17, 0.03),  -- Bột chiên giòn 30g
(7, 9, 0.03),   -- Rau xà lách 30g
(7, 20, 0.02),  -- Sốt mayo 20ml
(7, 42, 0.05);  -- Dầu ăn 50ml

-- SM008: Wrap gà giòn (product_id = 8)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(8, 3, 0.1),    -- Phi lê gà 100g
(8, 14, 1),     -- Bánh tortilla 1 cái
(8, 17, 0.03),  -- Bột chiên giòn 30g
(8, 9, 0.04),   -- Rau xà lách 40g
(8, 10, 0.04),  -- Cà chua 40g
(8, 20, 0.02),  -- Sốt mayo 20ml
(8, 42, 0.04);  -- Dầu ăn 40ml

-- SD001-SD004: Nước ngọt (product_id = 9-12)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(9, 24, 1),     -- Coca-Cola 1 lon
(10, 25, 1),    -- Pepsi 1 lon
(11, 26, 1),    -- 7Up 1 lon
(12, 27, 1);    -- Fanta 1 lon

-- SD005: Nước suối đóng chai (product_id = 13)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(13, 28, 1);    -- Nước suối 1 chai

-- SD006: Trà sữa truyền thống (product_id = 14)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(14, 30, 0.01), -- Trà khô 10g
(14, 32, 0.1),  -- Sữa tươi 100ml
(14, 33, 0.03), -- Trân châu 30g
(14, 44, 0.02), -- Đường 20g
(14, 45, 0.1);  -- Đá viên 100g

-- SD007: Cà phê sữa đá (product_id = 15)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(15, 31, 0.015), -- Cà phê xay 15g
(15, 32, 0.05),  -- Sữa tươi 50ml
(15, 44, 0.02),  -- Đường 20g
(15, 45, 0.1);   -- Đá viên 100g

-- SD008: Nước cam ép (product_id = 16)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(16, 29, 0.25), -- Cam tươi 250g (2-3 quả)
(16, 44, 0.01), -- Đường 10g
(16, 45, 0.1);  -- Đá viên 100g

-- ST001: Kem vanilla (product_id = 17)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(17, 34, 0.1);  -- Kem vanilla 100ml (2 viên)

-- ST002: Kem dâu (product_id = 18)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(18, 35, 0.1);  -- Kem dâu 100ml (2 viên)

-- ST003: Bánh flan caramel (product_id = 19)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(19, 39, 0.05), -- Bánh flan mix 50g
(19, 32, 0.1),  -- Sữa tươi 100ml
(19, 46, 2),    -- Trứng gà 2 quả
(19, 44, 0.02); -- Đường 20g

-- ST004: Sundae socola (product_id = 20)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(20, 34, 0.08), -- Kem vanilla 80ml
(20, 37, 0.03); -- Sốt socola 30ml

-- ST005: Sundae dâu (product_id = 21)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(21, 34, 0.08), -- Kem vanilla 80ml
(21, 38, 0.03); -- Sốt dâu 30ml

-- ST006: Bánh brownie socola (product_id = 22)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(22, 40, 0.08), -- Bột brownie 80g
(22, 46, 1),    -- Trứng gà 1 quả
(22, 42, 0.02); -- Dầu ăn 20ml

-- ST007: Pudding socola (product_id = 23)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(23, 41, 0.05), -- Bột pudding 50g
(23, 32, 0.1),  -- Sữa tươi 100ml
(23, 37, 0.02); -- Sốt socola 20ml

-- SS001: Khoai tây chiên (product_id = 24)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(24, 7, 0.15),  -- Khoai tây 150g
(24, 42, 0.1),  -- Dầu ăn 100ml
(24, 43, 0.005); -- Muối 5g

-- SS002: Khoai tây lắc phô mai (product_id = 25)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(25, 7, 0.15),  -- Khoai tây 150g
(25, 19, 0.02), -- Phô mai bột 20g
(25, 42, 0.1),  -- Dầu ăn 100ml
(25, 43, 0.005); -- Muối 5g

-- SS003: Gà popcorn (product_id = 26)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(26, 3, 0.12),  -- Phi lê gà 120g
(26, 17, 0.04), -- Bột chiên giòn 40g
(26, 42, 0.08), -- Dầu ăn 80ml
(26, 43, 0.005); -- Muối 5g

-- SS004: Nugget gà (product_id = 27)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(27, 3, 0.12),  -- Phi lê gà 120g (6 miếng)
(27, 17, 0.04), -- Bột chiên giòn 40g
(27, 42, 0.08), -- Dầu ăn 80ml
(27, 46, 1);    -- Trứng gà 1 quả

-- SS005: Phô mai que (product_id = 28)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(28, 18, 3),    -- Phô mai lát 3 gói
(28, 17, 0.03), -- Bột chiên giòn 30g
(28, 42, 0.06), -- Dầu ăn 60ml
(28, 46, 1);    -- Trứng gà 1 quả

-- SS006: Hành tây chiên (product_id = 29)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(29, 8, 0.15),  -- Hành tây 150g
(29, 17, 0.03), -- Bột chiên giòn 30g
(29, 42, 0.08); -- Dầu ăn 80ml

-- SS007: Salad rau trộn (product_id = 30)
INSERT INTO product_ingredients (product_id, ingredient_id, quantity_needed) VALUES
(30, 9, 0.1),   -- Rau xà lách 100g
(30, 10, 0.08), -- Cà chua 80g
(30, 11, 0.05), -- Dưa chuột 50g
(30, 8, 0.03),  -- Hành tây 30g
(30, 20, 0.03); -- Sốt mayo 30ml

-- ========================================
-- 4. LỊCH SỬ NHẬP HÀNG (INVENTORY_TRANSACTIONS)
-- ========================================

-- Nhập hàng tuần trước (giả sử ngày 25/01/2026)
INSERT INTO inventory_transactions (ingredient_id, transaction_type, quantity, unit_price, total_cost, reason, supplier_id, employee_id, transaction_date) VALUES
-- Nhập thịt & protein từ Công ty Thực phẩm Việt
(1, 'import', 30, 180000, 5400000, 'Nhập hàng định kỳ', 1, 1, '2026-01-25 08:30:00'),
(4, 'import', 20, 120000, 2400000, 'Nhập hàng định kỳ', 1, 1, '2026-01-25 08:30:00'),
(6, 'import', 15, 250000, 3750000, 'Nhập hàng định kỳ', 1, 1, '2026-01-25 08:30:00'),

-- Nhập gà từ Trang trại Gà Minh Phát
(2, 'import', 50, 75000, 3750000, 'Nhập gà tươi', 6, 1, '2026-01-25 09:00:00'),
(3, 'import', 40, 95000, 3800000, 'Nhập phi lê gà', 6, 1, '2026-01-25 09:00:00'),
(46, 'import', 300, 3500, 1050000, 'Nhập trứng gà', 6, 1, '2026-01-25 09:00:00'),

-- Nhập cá từ Hải sản Biển Đông
(5, 'import', 25, 130000, 3250000, 'Nhập cá phi lê', 8, 1, '2026-01-25 10:00:00'),

-- Nhập rau củ từ Rau Sạch Đà Lạt
(7, 'import', 80, 18000, 1440000, 'Nhập khoai tây', 2, 1, '2026-01-26 08:00:00'),
(8, 'import', 40, 15000, 600000, 'Nhập hành tây', 2, 1, '2026-01-26 08:00:00'),
(9, 'import', 20, 25000, 500000, 'Nhập rau xà lách', 2, 1, '2026-01-26 08:00:00'),
(10, 'import', 30, 20000, 600000, 'Nhập cà chua', 2, 1, '2026-01-26 08:00:00'),
(11, 'import', 20, 18000, 360000, 'Nhập dưa chuột', 2, 1, '2026-01-26 08:00:00'),
(29, 'import', 40, 35000, 1400000, 'Nhập cam tươi', 2, 1, '2026-01-26 08:00:00'),

-- Nhập bánh từ Bánh Hoàng Kim
(12, 'import', 200, 3500, 700000, 'Nhập bánh burger', 7, 1, '2026-01-26 14:00:00'),
(13, 'import', 150, 4000, 600000, 'Nhập bánh mì sandwich', 7, 1, '2026-01-26 14:00:00'),
(14, 'import', 100, 3000, 300000, 'Nhập bánh tortilla', 7, 1, '2026-01-26 14:00:00'),
(15, 'import', 80, 8000, 640000, 'Nhập đế pizza', 7, 1, '2026-01-26 14:00:00'),
(16, 'import', 100, 3500, 350000, 'Nhập bánh hotdog', 7, 1, '2026-01-26 14:00:00'),

-- Nhập nước ngọt từ ABC
(24, 'import', 300, 8000, 2400000, 'Nhập Coca-Cola', 3, 1, '2026-01-27 09:00:00'),
(25, 'import', 300, 8000, 2400000, 'Nhập Pepsi', 3, 1, '2026-01-27 09:00:00'),
(26, 'import', 250, 8000, 2000000, 'Nhập 7Up', 3, 1, '2026-01-27 09:00:00'),
(27, 'import', 250, 8000, 2000000, 'Nhập Fanta', 3, 1, '2026-01-27 09:00:00'),
(28, 'import', 400, 4000, 1600000, 'Nhập nước suối', 3, 1, '2026-01-27 09:00:00'),

-- Nhập sữa & kem từ Sữa Việt Nam
(18, 'import', 60, 45000, 2700000, 'Nhập phô mai lát', 4, 1, '2026-01-27 14:00:00'),
(19, 'import', 20, 150000, 3000000, 'Nhập phô mai bột', 4, 1, '2026-01-27 14:00:00'),
(32, 'import', 80, 30000, 2400000, 'Nhập sữa tươi', 4, 1, '2026-01-27 14:00:00'),
(34, 'import', 30, 80000, 2400000, 'Nhập kem vanilla', 4, 1, '2026-01-27 14:00:00'),
(35, 'import', 25, 85000, 2125000, 'Nhập kem dâu', 4, 1, '2026-01-27 14:00:00'),
(36, 'import', 25, 85000, 2125000, 'Nhập kem socola', 4, 1, '2026-01-27 14:00:00'),

-- Nhập gia vị & đồ khô từ Metro
(17, 'import', 40, 28000, 1120000, 'Nhập bột chiên giòn', 5, 1, '2026-01-28 08:00:00'),
(20, 'import', 15, 55000, 825000, 'Nhập sốt mayo', 5, 1, '2026-01-28 08:00:00'),
(21, 'import', 20, 35000, 700000, 'Nhập tương cà', 5, 1, '2026-01-28 08:00:00'),
(22, 'import', 15, 40000, 600000, 'Nhập tương ớt', 5, 1, '2026-01-28 08:00:00'),
(23, 'import', 10, 65000, 650000, 'Nhập sốt pizza', 5, 1, '2026-01-28 08:00:00'),
(30, 'import', 8, 200000, 1600000, 'Nhập trà khô', 5, 1, '2026-01-28 08:00:00'),
(31, 'import', 10, 250000, 2500000, 'Nhập cà phê xay', 5, 1, '2026-01-28 08:00:00'),
(33, 'import', 15, 60000, 900000, 'Nhập trân châu', 5, 1, '2026-01-28 08:00:00'),
(37, 'import', 10, 70000, 700000, 'Nhập sốt socola', 5, 1, '2026-01-28 08:00:00'),
(38, 'import', 10, 70000, 700000, 'Nhập sốt dâu', 5, 1, '2026-01-28 08:00:00'),
(39, 'import', 15, 90000, 1350000, 'Nhập bánh flan mix', 5, 1, '2026-01-28 08:00:00'),
(40, 'import', 10, 120000, 1200000, 'Nhập bột brownie', 5, 1, '2026-01-28 08:00:00'),
(41, 'import', 10, 100000, 1000000, 'Nhập bột pudding', 5, 1, '2026-01-28 08:00:00'),
(42, 'import', 60, 35000, 2100000, 'Nhập dầu ăn', 5, 1, '2026-01-28 08:00:00'),
(43, 'import', 25, 8000, 200000, 'Nhập muối', 5, 1, '2026-01-28 08:00:00'),
(44, 'import', 30, 20000, 600000, 'Nhập đường', 5, 1, '2026-01-28 08:00:00'),
(45, 'import', 150, 5000, 750000, 'Nhập đá viên', 5, 1, '2026-01-28 08:00:00');

-- Nhập bổ sung tuần này (30/01/2026)
INSERT INTO inventory_transactions (ingredient_id, transaction_type, quantity, unit_price, total_cost, reason, supplier_id, employee_id, transaction_date) VALUES
(2, 'import', 30, 75000, 2250000, 'Bổ sung gà tươi', 6, 1, '2026-01-30 08:30:00'),
(7, 'import', 30, 18000, 540000, 'Bổ sung khoai tây', 2, 1, '2026-01-30 09:00:00'),
(12, 'import', 100, 3500, 350000, 'Bổ sung bánh burger', 7, 1, '2026-01-30 10:00:00'),
(24, 'import', 200, 8000, 1600000, 'Bổ sung Coca-Cola', 3, 1, '2026-01-30 11:00:00'),
(25, 'import', 200, 8000, 1600000, 'Bổ sung Pepsi', 3, 1, '2026-01-30 11:00:00'),
(46, 'import', 200, 3500, 700000, 'Bổ sung trứng gà', 6, 1, '2026-01-30 14:00:00');

SET FOREIGN_KEY_CHECKS = 1;

-- ========================================
-- 5. KIỂM TRA KẾT QUẢ
-- ========================================
SELECT 'Nhà cung cấp:' AS '', COUNT(*) AS 'Số lượng' FROM suppliers;
SELECT 'Nguyên liệu:' AS '', COUNT(*) AS 'Số lượng' FROM ingredients;
SELECT 'Công thức:' AS '', COUNT(*) AS 'Số lượng' FROM product_ingredients;
SELECT 'Lịch sử nhập:' AS '', COUNT(*) AS 'Số lượng' FROM inventory_transactions;
