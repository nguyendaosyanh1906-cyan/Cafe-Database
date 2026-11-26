-- Chỉ định rõ Database cần thêm dữ liệu
USE cafe_management_db;

-- ==============================================================================
-- 3. THÊM DỮ LIỆU MẪU (INSERT DATA)
-- ==============================================================================

-- 3.1 Thêm dữ liệu vào bảng users (Mật khẩu đang được mã hóa BCrypt)
INSERT INTO users (user_id, username, password_hash, role, full_name, phone, salary, is_active) VALUES
                                                                                                    (1, 'admin', '$2a$12$Oxv1pgXcaokh5PfCgYSisu6VPTXIrOK5Tr3XD.MzKp2akWZkj5Nk.', 'ADMIN', 'Quản trị viên hệ thống', '104240750', 15000000, 1),
                                                                                                    (2, 'staff1', '$2a$10$CGRqYv2Zpyg7Jfj7K2isiuQm4XC4MjGGKdaGspE8gMmAfSrBTHGKW', 'STAFF', 'Nhân viên quầy số 1', '104240022', 6000000, 1);

-- 3.2 Thêm dữ liệu vào bảng customers
INSERT INTO customers (customer_id, phone, full_name, points, created_at) VALUES
                                                                              (1, '104240047', 'Nguyễn Phúc Vinh', 1000, NULL),
                                                                              (2, '123456789', 'Alexander đệ tam', 0, NULL);

-- 3.3 Thêm dữ liệu vào bảng categories
INSERT INTO categories (category_id, name) VALUES
                                               (1, 'Cà Phê Truyền Thống'),
                                               (2, 'Cà Phê Pha Máy'),
                                               (3, 'Phindi');

-- 3.4 Thêm dữ liệu vào bảng products
INSERT INTO products (product_id, category_id, name, base_price, is_active) VALUES
                                                                                (1, 1, 'Cà Phê Đen', 15000, 1),
                                                                                (2, 1, 'Cà Phê Sữa', 15000, 1),
                                                                                (3, 1, 'Cà Phê Muối', 20000, 1),
                                                                                (4, 1, 'Cà Phê Trứng', 20000, 1),
                                                                                (5, 1, 'Cà Phê Cốt Dừa', 20000, 1),
                                                                                (6, 1, 'Bạc Xỉu', 15000, 1),
                                                                                (7, 2, 'Espresso', 35000, 1),
                                                                                (8, 2, 'Americano', 35000, 1),
                                                                                (9, 2, 'Cappuccino', 45000, 1),
                                                                                (10, 2, 'Latte', 45000, 1),
                                                                                (11, 2, 'Caramel Macchiato', 55000, 1),
                                                                                (12, 2, 'Mocha', 55000, 1),
                                                                                (13, 3, 'Phindi Kem Sữa', 40000, 1),
                                                                                (14, 3, 'Phindi Hạnh Nhân', 40000, 1),
                                                                                (15, 3, 'Phindi Choco', 40000, 1);

-- Demo data for a fresh database only; not a production migration.
INSERT INTO Toppings (topping_id, name, price, is_active) VALUES
(1, 'Kem muối', 5000, 1),
(2, 'Kem béo', 5000, 1),
(3, 'Thạch Cà Phê', 7000, 1),
(4, 'Bánh Flan', 7000, 1),
(5, 'Dừa sấy giòn', 7000, 1),
(6, 'Whipped Cream', 7000, 1),
(7, 'Bột Cacao', 5000, 1);

INSERT INTO orders (order_id, staff_id, customer_id, total_amount, points_used, payment_method, status, created_at) VALUES
    (1001, 2, 1, 35000, 0, 'CASH', 'COMPLETED', NOW() - INTERVAL 1 DAY),
    (1002, 2, NULL, 15000, 0, 'QR', 'PENDING', NOW()),
    (1003, 2, 2, 35000, 0, 'CASH', 'CANCELLED', NOW());

-- price_at_purchase includes topping price per drink; do not add it twice.
INSERT INTO order_details (order_detail_id, order_id, product_id, size, sugar_level, ice_level, quantity, price_at_purchase) VALUES
    (1001, 1001, 1, 'M', 100, 100, 2, 17500),
    (1002, 1002, 2, 'M', 70, 50, 1, 15000),
    (1003, 1003, 7, 'M', 0, 100, 1, 35000);
INSERT INTO order_detail_toppings (order_detail_id, topping_id, topping_price) VALUES (1001, 1, 2500);
