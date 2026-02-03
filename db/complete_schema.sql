-- ========================================
-- SCHEMA HOÀN CHỈNH CHO HỆ THỐNG QUẢN LÝ BÁN ĐỒ ĂN NHANH
-- Bao gồm tất cả bảng cần thiết cho 14 tab
-- ========================================

-- Tạo database (nếu chưa có)
CREATE DATABASE IF NOT EXISTS FastFoodManagement
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
USE FastFoodManagement;

-- ========================================
-- 1. BẢNG DANH MỤC SẢN PHẨM (Tab 3: Món ăn)
-- ========================================
CREATE TABLE IF NOT EXISTS categories (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 2. BẢNG SẢN PHẨM / MÓN ĂN (Tab 1, 3: Bán hàng, Món ăn)
-- ========================================
CREATE TABLE IF NOT EXISTS products (
    product_id INT PRIMARY KEY AUTO_INCREMENT,
    product_code VARCHAR(50) NULL,
    product_name VARCHAR(200) NOT NULL,
    category_id INT,
    price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    description TEXT,
    image_url VARCHAR(500),
    is_available TINYINT(1) NOT NULL DEFAULT 1,
    preparation_time INT DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_products_name ON products(product_name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);

-- ========================================
-- 3. BẢNG NGUYÊN LIỆU (Tab 2, 4: Nhập hàng, Nguyên liệu)
-- ========================================
CREATE TABLE IF NOT EXISTS ingredients (
    ingredient_id INT PRIMARY KEY AUTO_INCREMENT,
    ingredient_name VARCHAR(100) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    current_stock DECIMAL(12,2) DEFAULT 0,
    min_stock_level DECIMAL(12,2) DEFAULT 0,
    unit_price DECIMAL(12,2) DEFAULT 0,
    supplier VARCHAR(200),
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_ingredients_stock ON ingredients(current_stock);

-- ========================================
-- 4. BẢNG CÔNG THỨC MÓN ĂN (Tab 5: Công thức)
-- ========================================
CREATE TABLE IF NOT EXISTS product_ingredients (
    product_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    quantity_needed DECIMAL(10,2) NOT NULL DEFAULT 1,
    PRIMARY KEY (product_id, ingredient_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(ingredient_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 5. BẢNG NHÂN VIÊN (Tab 10, 12: Nhân viên, Tài khoản)
-- ========================================
CREATE TABLE IF NOT EXISTS employees (
    employee_id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255),
    email VARCHAR(100),
    phone VARCHAR(20),
    position ENUM('manager', 'cashier', 'chef', 'waiter', 'staff') NOT NULL DEFAULT 'staff',
    role ENUM('Manager', 'Cashier') DEFAULT 'Cashier',
    permission_code VARCHAR(10) DEFAULT NULL COMMENT 'Mã phân quyền: PQ0=Admin, PQ2=Nhân viên',
    salary DECIMAL(12,2) DEFAULT 0,
    hire_date DATE,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 6. BẢNG NHÀ CUNG CẤP (Tab 11: Nhà cung cấp)
-- ========================================
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id INT PRIMARY KEY AUTO_INCREMENT,
    supplier_name VARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    notes TEXT,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 7. BẢNG KHÁCH HÀNG (Tab 9: Khách hàng)
-- ========================================
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    loyalty_points INT DEFAULT 0,
    membership_level ENUM('bronze', 'silver', 'gold', 'platinum') DEFAULT 'bronze',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 8. BẢNG KHUYẾN MÃI (Tab 1, 8: Bán hàng, Khuyến mãi)
-- ========================================
CREATE TABLE IF NOT EXISTS promotions (
    promotion_id INT PRIMARY KEY AUTO_INCREMENT,
    promotion_name VARCHAR(200) NOT NULL,
    promotion_code VARCHAR(50) UNIQUE,
    description TEXT,
    discount_type ENUM('percentage', 'fixed_amount') NOT NULL DEFAULT 'percentage',
    discount_value DECIMAL(12,2) NOT NULL DEFAULT 0,
    min_order_amount DECIMAL(12,2) DEFAULT 0,
    max_discount_amount DECIMAL(12,2) DEFAULT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    applicable_products TEXT,
    usage_limit INT DEFAULT NULL,
    usage_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_promotions_active ON promotions(is_active, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(promotion_code);

-- ========================================
-- 9. BẢNG ĐƠN HÀNG (Tab 1, 6: Bán hàng, Hóa đơn)
-- ========================================
CREATE TABLE IF NOT EXISTS orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id INT,
    employee_id INT,
    order_type ENUM('dine_in', 'takeaway', 'delivery') DEFAULT 'takeaway',
    status ENUM('pending', 'preparing', 'ready', 'served', 'completed', 'cancelled') DEFAULT 'pending',
    subtotal DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) DEFAULT 0,
    tax_amount DECIMAL(12,2) DEFAULT 0,
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_method ENUM('cash', 'card', 'mobile_banking', 'ewallet') DEFAULT 'cash',
    payment_status ENUM('pending', 'paid', 'refunded') DEFAULT 'pending',
    payment_reference VARCHAR(200),
    notes TEXT,
    customer_name VARCHAR(200),
    order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_time TIMESTAMP NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_orders_date ON orders(order_time);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_employee ON orders(employee_id);

-- ========================================
-- 10. BẢNG CHI TIẾT ĐƠN HÀNG (Tab 6: Hóa đơn)
-- ========================================
CREATE TABLE IF NOT EXISTS order_details (
    order_detail_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT,
    product_name VARCHAR(200),
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_price DECIMAL(12,2) NOT NULL DEFAULT 0,
    special_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_order_details_order ON order_details(order_id);

-- ========================================
-- 11. BẢNG GIAO DỊCH KHO (Tab 2, 7: Nhập hàng, Hóa đơn nhập)
-- ========================================
CREATE TABLE IF NOT EXISTS inventory_transactions (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    ingredient_id INT NOT NULL,
    transaction_type ENUM('import', 'export', 'adjustment', 'sale') NOT NULL DEFAULT 'import',
    quantity DECIMAL(12,2) NOT NULL,
    unit_price DECIMAL(12,2) DEFAULT 0,
    total_cost DECIMAL(12,2) DEFAULT 0,
    reason TEXT,
    supplier_id INT,
    employee_id INT,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(ingredient_id) ON DELETE RESTRICT,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id) ON DELETE SET NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_inv_trans_date ON inventory_transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_inv_trans_type ON inventory_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_inv_trans_ingredient ON inventory_transactions(ingredient_id);

-- ========================================
-- 12. BẢNG TỒN KHO SẢN PHẨM (Tab 1: Bán hàng - số lượng)
-- ========================================
CREATE TABLE IF NOT EXISTS inventory (
    inventory_id INT PRIMARY KEY AUTO_INCREMENT,
    item_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    unit VARCHAR(50) DEFAULT 'phần',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 13. BẢNG ÁP DỤNG KHUYẾN MÃI CHO ĐƠN HÀNG
-- ========================================
CREATE TABLE IF NOT EXISTS order_promotions (
    order_promotion_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    promotion_id INT,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 14. BẢNG THANH TOÁN (Chi tiết thanh toán)
-- ========================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    method ENUM('cash', 'card', 'mobile_banking', 'ewallet') DEFAULT 'cash',
    reference VARCHAR(200),
    paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- 15. BẢNG LOG HOẠT ĐỘNG (Audit)
-- ========================================
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    action VARCHAR(200),
    table_name VARCHAR(100),
    record_id INT,
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES employees(employee_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- DỮ LIỆU MẪU
-- ========================================

-- Danh mục
INSERT IGNORE INTO categories (category_name, description) VALUES
('Món chính', 'Các món ăn chính'),
('Đồ uống', 'Nước giải khát'),
('Tráng miệng', 'Các món tráng miệng'),
('Đồ ăn vặt', 'Snacks và đồ ăn nhẹ'),
('Combo', 'Các combo tiết kiệm');

-- Nhân viên mẫu (có tài khoản đăng nhập với permission_code)
INSERT IGNORE INTO employees (full_name, username, password_hash, email, phone, position, role, permission_code, salary, hire_date) VALUES
('Nguyễn Văn Quản Lý', 'manager', 'manager', 'manager@fastfood.com', '0901234567', 'manager', 'Manager', 'PQ0', 15000000, '2024-01-01'),
('Trần Thị Thu Ngân', 'cashier', 'cashier', 'cashier@fastfood.com', '0902345678', 'cashier', 'Cashier', 'PQ2', 8000000, '2024-02-01'),
('Lê Văn Bếp', 'chef', 'chef', 'chef@fastfood.com', '0903456789', 'chef', 'Cashier', 'PQ2', 10000000, '2024-03-01'),
('Admin', 'admin', 'admin', 'admin@fastfood.com', '0900000000', 'manager', 'Manager', 'PQ0', 20000000, '2024-01-01');

-- Sản phẩm mẫu
INSERT IGNORE INTO products (product_code, product_name, category_id, price, description) VALUES
('SP001', 'Cơm gà chiên', 1, 45000, 'Cơm chiên với gà và rau củ'),
('SP002', 'Mì xào bò', 1, 55000, 'Mì xào với thịt bò'),
('SP003', 'Phở bò', 1, 50000, 'Phở bò truyền thống'),
('SP004', 'Bún chả', 1, 45000, 'Bún chả Hà Nội'),
('SP005', 'Coca Cola', 2, 15000, 'Nước ngọt có ga 330ml'),
('SP006', 'Pepsi', 2, 15000, 'Nước ngọt có ga 330ml'),
('SP007', 'Nước cam', 2, 20000, 'Nước cam ép tươi'),
('SP008', 'Trà đào', 2, 25000, 'Trà đào cam sả'),
('SP009', 'Kem vanilla', 3, 18000, 'Kem vanilla'),
('SP010', 'Bánh flan', 3, 15000, 'Bánh flan caramel'),
('SP011', 'Khoai tây chiên', 4, 25000, 'Khoai tây chiên giòn'),
('SP012', 'Gà rán', 4, 35000, 'Gà rán giòn'),
('SP013', 'Combo 1', 5, 75000, 'Cơm gà + Coca + Kem'),
('SP014', 'Combo 2', 5, 85000, 'Phở bò + Trà đào + Bánh flan');

-- Nguyên liệu mẫu
INSERT IGNORE INTO ingredients (ingredient_name, unit, current_stock, min_stock_level, unit_price, supplier) VALUES
('Gà tươi', 'kg', 50, 10, 80000, 'Công ty CP Thực phẩm'),
('Thịt bò', 'kg', 30, 8, 150000, 'Công ty CP Thực phẩm'),
('Gạo', 'kg', 100, 20, 25000, 'Đại lý Gạo Việt'),
('Mì', 'kg', 50, 10, 30000, 'Đại lý Gạo Việt'),
('Bánh phở', 'kg', 40, 10, 35000, 'Đại lý Gạo Việt'),
('Rau xà lách', 'kg', 20, 5, 25000, 'Chợ đầu mối'),
('Cà chua', 'kg', 15, 3, 20000, 'Chợ đầu mối'),
('Hành tây', 'kg', 10, 2, 15000, 'Chợ đầu mối'),
('Dầu ăn', 'lít', 30, 5, 35000, 'Siêu thị'),
('Đường', 'kg', 20, 5, 20000, 'Siêu thị'),
('Muối', 'kg', 10, 2, 8000, 'Siêu thị'),
('Nước mắm', 'lít', 15, 3, 45000, 'Siêu thị'),
('Coca Cola lon', 'lon', 200, 50, 8000, 'Đại lý nước giải khát'),
('Pepsi lon', 'lon', 200, 50, 8000, 'Đại lý nước giải khát'),
('Cam', 'kg', 30, 10, 30000, 'Chợ đầu mối'),
('Kem vanilla', 'hộp', 20, 5, 120000, 'Siêu thị');

-- Nhà cung cấp mẫu
INSERT IGNORE INTO suppliers (supplier_name, phone, email, address, notes) VALUES
('Công ty CP Thực phẩm Việt', '0281234567', 'contact@thucphamviet.com', '123 Nguyễn Văn Linh, Q.7, TP.HCM', 'Cung cấp thịt tươi'),
('Đại lý Gạo Việt', '0287654321', 'gaoviet@gmail.com', '456 Lê Văn Việt, Q.9, TP.HCM', 'Cung cấp gạo, mì, bún'),
('Chợ đầu mối Thủ Đức', '0289876543', 'chodaumoi@gmail.com', 'Chợ đầu mối Thủ Đức', 'Rau củ quả tươi'),
('Đại lý nước giải khát ABC', '0282345678', 'abc@drinks.com', '789 Điện Biên Phủ, Q.3, TP.HCM', 'Nước ngọt, bia');

-- Khách hàng mẫu
INSERT IGNORE INTO customers (full_name, phone, email, address, loyalty_points, membership_level) VALUES
('Nguyễn Văn A', '0911111111', 'nguyenvana@gmail.com', '123 ABC, Q.1', 100, 'bronze'),
('Trần Thị B', '0922222222', 'tranthib@gmail.com', '456 DEF, Q.2', 500, 'silver'),
('Lê Văn C', '0933333333', 'levanc@gmail.com', '789 GHI, Q.3', 1500, 'gold'),
('Phạm Thị D', '0944444444', 'phamthid@gmail.com', '321 JKL, Q.4', 3000, 'platinum');

-- Khuyến mãi mẫu
INSERT IGNORE INTO promotions (promotion_name, promotion_code, description, discount_type, discount_value, min_order_amount, max_discount_amount, start_date, end_date) VALUES
('Giảm 10%', 'SALE10', 'Giảm 10% cho đơn hàng từ 100k', 'percentage', 10, 100000, 50000, '2024-01-01', '2026-12-31'),
('Giảm 50k', 'SALE50K', 'Giảm 50k cho đơn hàng từ 300k', 'fixed_amount', 50000, 300000, NULL, '2024-01-01', '2026-12-31'),
('Giảm 20%', 'SALE20', 'Giảm 20% cho đơn hàng từ 200k', 'percentage', 20, 200000, 100000, '2024-01-01', '2026-12-31'),
('Giảm 5%', 'SALE5', 'Giảm 5% mọi đơn hàng', 'percentage', 5, 0, 30000, '2024-01-01', '2026-12-31'),
('Happy Hour', 'HAPPY', 'Giảm 15% từ 14h-17h', 'percentage', 15, 50000, 40000, '2024-01-01', '2026-12-31');

-- Thêm inventory cho sản phẩm
INSERT IGNORE INTO inventory (item_id, quantity, unit)
SELECT product_id, 100, 'phần' FROM products WHERE NOT EXISTS (
    SELECT 1 FROM inventory WHERE inventory.item_id = products.product_id
);

-- ========================================
-- CẬP NHẬT CỘT NẾU THIẾU (ALTER TABLE)
-- ========================================

-- Thêm cột promotion_code nếu chưa có
-- ALTER TABLE promotions ADD COLUMN IF NOT EXISTS promotion_code VARCHAR(50) UNIQUE AFTER promotion_name;

-- Thêm cột max_discount_amount nếu chưa có  
-- ALTER TABLE promotions ADD COLUMN IF NOT EXISTS max_discount_amount DECIMAL(12,2) DEFAULT NULL AFTER min_order_amount;

-- Thêm cột usage_limit và usage_count nếu chưa có
-- ALTER TABLE promotions ADD COLUMN IF NOT EXISTS usage_limit INT DEFAULT NULL;
-- ALTER TABLE promotions ADD COLUMN IF NOT EXISTS usage_count INT DEFAULT 0;

-- Thêm cột is_active cho ingredients nếu chưa có
-- ALTER TABLE ingredients ADD COLUMN IF NOT EXISTS is_active TINYINT(1) NOT NULL DEFAULT 1;

-- Thêm cột role cho employees nếu chưa có
-- ALTER TABLE employees ADD COLUMN IF NOT EXISTS role ENUM('Manager', 'Cashier') DEFAULT 'Cashier';

-- Thêm cột payment_reference cho orders nếu chưa có
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(200);

-- Thêm cột customer_name cho orders nếu chưa có
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(200);

-- Thêm cột permission_code cho employees nếu chưa có
-- ALTER TABLE employees ADD COLUMN IF NOT EXISTS permission_code VARCHAR(10) DEFAULT NULL COMMENT 'Mã phân quyền: PQ0=Admin, PQ2=Nhân viên';

-- ========================================
-- END OF SCHEMA
-- ========================================
