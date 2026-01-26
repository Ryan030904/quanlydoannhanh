-- POS Management System - MySQL schema
-- Charset and engine recommendations: utf8mb4, InnoDB

-- Create database (run as needed)
CREATE DATABASE IF NOT EXISTS pos_db
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
USE pos_db;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('Manager','Cashier') NOT NULL DEFAULT 'Cashier',
  full_name VARCHAR(200),
  phone VARCHAR(30),
  email VARCHAR(150),
  status TINYINT(1) NOT NULL DEFAULT 1, -- 1 = active, 0 = disabled
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for categories
-- ----------------------------
DROP TABLE IF EXISTS categories;
CREATE TABLE categories (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  description TEXT,
  status TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for items (menu)
-- ----------------------------
DROP TABLE IF EXISTS items;
CREATE TABLE items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NULL,
  name VARCHAR(200) NOT NULL,
  category_id INT,
  price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  description TEXT,
  image_path VARCHAR(500),
  is_active TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_items_name ON items(name);
CREATE INDEX idx_items_category ON items(category_id);

-- ----------------------------
-- Table for inventory (tracking stock for items or ingredients)
-- ----------------------------
DROP TABLE IF EXISTS inventory;
CREATE TABLE inventory (
  id INT AUTO_INCREMENT PRIMARY KEY,
  item_id INT NOT NULL,
  quantity INT NOT NULL DEFAULT 0,
  unit VARCHAR(50) DEFAULT 'pcs',
  last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
  id INT AUTO_INCREMENT PRIMARY KEY,
  order_number VARCHAR(50) NOT NULL UNIQUE,
  user_id INT, -- cashier who created order
  customer_name VARCHAR(200),
  status ENUM('Pending','Paid','Cancelled','Completed') NOT NULL DEFAULT 'Pending',
  subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  tax DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  payment_method VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_orders_date ON orders(created_at);

-- ----------------------------
-- Table structure for order_items
-- ----------------------------
DROP TABLE IF EXISTS order_items;
CREATE TABLE order_items (
  id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL,
  item_id INT,
  item_name VARCHAR(200),
  price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  quantity INT NOT NULL DEFAULT 1,
  line_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
  FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for payments (optional: recording receipts)
-- ----------------------------
DROP TABLE IF EXISTS payments;
CREATE TABLE payments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  order_id INT NOT NULL,
  paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  method ENUM('Cash','BankTransfer','Card','Other') DEFAULT 'Cash',
  reference VARCHAR(200),
  paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table for audit logs (optional)
-- ----------------------------
DROP TABLE IF EXISTS audit_logs;
CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  action VARCHAR(200),
  details TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Sample seed data (basic)
-- ----------------------------
INSERT INTO users (username, password_hash, role, full_name, email)
VALUES
  ('manager','manager','Manager','Quản lý cửa hàng','manager@example.com'),
  ('cashier','cashier','Cashier','Nhân viên bán hàng','cashier@example.com'),
  ('admin','admin','Manager','Administrator','admin@example.com');
  

INSERT INTO categories (name, description)
VALUES ('Burger','Các loại burger'), ('Pizza','Các loại pizza'), ('Kem','Kem và tráng miệng'), ('Nước ép','Đồ uống');

INSERT INTO items (code, name, category_id, price, description, image_path)
VALUES
 ('B001','Classic Chicken Burger', 1, 5.47, 'Burger gà truyền thống', NULL),
 ('P001','Chicken Pizza', 2, 7.00, 'Pizza gà với phô mai', NULL),
 ('K001','Triple Scoop Ice-Cream',3,2.47,'Kem 3 viên',NULL),
 ('J001','Orange Juice',4,1.20,'Nước cam tươi',NULL);

-- Initialize inventory for items
INSERT INTO inventory (item_id, quantity, unit)
SELECT id, 50, 'pcs' FROM items;

-- Example order + items
INSERT INTO orders (order_number, user_id, customer_name, status, subtotal, tax, total, payment_method)
VALUES ('ORD-20260118-0001', 2, 'Khách lẻ', 'Paid', 12.47, 1.25, 13.72, 'Cash');

INSERT INTO order_items (order_id, item_id, item_name, price, quantity, line_total)
VALUES
  (LAST_INSERT_ID(), 1, 'Classic Chicken Burger', 5.47, 1, 5.47);

INSERT INTO payments (order_id, paid_amount, method, reference)
VALUES (1, 13.72, 'Cash', 'Tiền mặt');

-- End of schema file


