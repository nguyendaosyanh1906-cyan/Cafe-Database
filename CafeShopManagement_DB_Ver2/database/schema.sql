-- Requires MySQL 8.0.16+ (enforced CHECK constraints). Fresh schema only.
-- ==============================================================================
-- 1. KHỞI TẠO DATABASE
-- ==============================================================================
CREATE SCHEMA `cafe_management_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cafe_management_db;
-- ==============================================================================
-- 2. TẠO CÁC BẢNG (CẤU TRÚC DATABASE)
-- ==============================================================================

-- 2.1 BẢNG NGƯỜI DÙNG (Auth & HR)
CREATE TABLE users (
                       user_id INT AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role ENUM('ADMIN', 'STAFF') NOT NULL,
                       full_name VARCHAR(100) NOT NULL,
                       phone VARCHAR(15),
                       salary INT NOT NULL DEFAULT 0,
                       is_active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_salary CHECK (salary >= 0),
    CONSTRAINT chk_users_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

-- 2.2 BẢNG KHÁCH HÀNG (Customer & Loyalty)
CREATE TABLE customers (
                           customer_id INT AUTO_INCREMENT PRIMARY KEY,
                           phone VARCHAR(15) NOT NULL UNIQUE,
                           full_name VARCHAR(100) NOT NULL,
                           points INT NOT NULL DEFAULT 0,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_customers_points CHECK (points >= 0)
) ENGINE=InnoDB;

-- 2.3 BẢNG DANH MỤC SẢN PHẨM (Menu)
CREATE TABLE categories (
                            category_id INT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

-- 2.4 BẢNG TOPPING (Customize)
CREATE TABLE toppings (
                          topping_id INT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          price INT NOT NULL,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_toppings_price CHECK (price >= 0),
    CONSTRAINT chk_toppings_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

-- 2.5 BẢNG SẢN PHẨM (Menu)
CREATE TABLE products (
                          product_id INT AUTO_INCREMENT PRIMARY KEY,
                          category_id INT NOT NULL,
                          name VARCHAR(150) NOT NULL,
                          description TEXT,
                          image_url VARCHAR(255),
                          base_price INT NOT NULL,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT,
    CONSTRAINT chk_products_price CHECK (base_price >= 0),
    CONSTRAINT chk_products_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

-- 2.6 BẢNG HÓA ĐƠN (Cart, Order & Payment)
CREATE TABLE orders (
                        order_id INT AUTO_INCREMENT PRIMARY KEY,
                        staff_id INT NOT NULL,
                        customer_id INT,
                        total_amount INT NOT NULL DEFAULT 0,
                        points_used INT NOT NULL DEFAULT 0,
                        payment_method ENUM('CASH', 'QR') NOT NULL,
                        status ENUM('PENDING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (staff_id) REFERENCES users(user_id) ON DELETE RESTRICT,
                        FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL,
    CONSTRAINT chk_orders_total CHECK (total_amount >= 0),
    CONSTRAINT chk_orders_points CHECK (points_used >= 0),
    INDEX idx_orders_status_created (status, created_at)
) ENGINE=InnoDB;

-- 2.7 BẢNG CHI TIẾT HÓA ĐƠN (Customize)
CREATE TABLE order_details (
                               order_detail_id INT AUTO_INCREMENT PRIMARY KEY,
                               order_id INT NOT NULL,
                               product_id INT NOT NULL,
                               size ENUM('S', 'M', 'L') NOT NULL DEFAULT 'S',
                               sugar_level INT NOT NULL DEFAULT 100,
                               ice_level INT NOT NULL DEFAULT 100,
                               quantity INT NOT NULL DEFAULT 1,
                               price_at_purchase INT NOT NULL,
                               FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
                               FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT,
    CONSTRAINT chk_details_quantity CHECK (quantity > 0),
    CONSTRAINT chk_details_price CHECK (price_at_purchase >= 0),
    CONSTRAINT chk_details_sugar CHECK (sugar_level BETWEEN 0 AND 100),
    CONSTRAINT chk_details_ice CHECK (ice_level BETWEEN 0 AND 100)
) ENGINE=InnoDB;

-- 2.8 BẢNG TRUNG GIAN CHI TIẾT TOPPING (Customize)
CREATE TABLE order_detail_toppings (
                                       order_detail_id INT NOT NULL,
                                       topping_id INT NOT NULL,
                                       topping_price INT NOT NULL,
                                       PRIMARY KEY (order_detail_id, topping_id),
                                       FOREIGN KEY (order_detail_id) REFERENCES order_details(order_detail_id) ON DELETE CASCADE,
                                       FOREIGN KEY (topping_id) REFERENCES toppings(topping_id) ON DELETE RESTRICT,
    CONSTRAINT chk_detail_toppings_price CHECK (topping_price >= 0)
) ENGINE=InnoDB;