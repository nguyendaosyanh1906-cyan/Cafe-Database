# CafeShopManagement Database

Bản cập nhật checklist ngày 21/11/2026. Đây là Maven library Java 17, không chạy JavaFX và không chứa CafeController.

## Cấu hình runtime

Không còn username/password hardcode trong DBConnection. Cấu hình qua environment:

| Biến | Ý nghĩa |
|---|---|
| CAFE_DB_URL | JDBC URL; mặc định jdbc:mysql://localhost:3306/cafe_management_db |
| CAFE_DB_USER | Bắt buộc: MySQL account của môi trường triển khai |
| CAFE_DB_PASSWORD | Bắt buộc cung cấp; không có password mặc định |

System properties tương ứng `cafe.db.url`, `cafe.db.user`, `cafe.db.password` có ưu tiên cao hơn environment. Không ghi credential thật vào source/git hoặc command line dùng chung. JDBC URL không tự tắt TLS; cấu hình TLS/timezone theo server triển khai.

## Khởi tạo

Yêu cầu MySQL 8.0.16 trở lên để CHECK được thực thi; kiểm thử bản này dùng MySQL 8.0.46/InnoDB.
Trên schema mới, chạy `schema.sql` rồi `seed.sql` từ MySQL client/Workbench. Không chạy lại hai script này để nâng cấp một database đã có dữ liệu. Chúng không DROP database và không là migration cho dữ liệu hiện hữu.

Tất cả table identifiers dùng lowercase: `users`, `customers`, `categories`, `products`, `toppings`, `orders`, `order_details`, `order_detail_toppings`. Nếu database cũ được tạo bằng tên mixed-case trên server phân biệt hoa/thường, phải sao lưu và chuyển tên bảng/áp dụng ALTER constraints theo kế hoạch migration trước khi dùng DAO mới; không sửa cấu hình lower_case_table_names trên server đang có dữ liệu để né vấn đề.

Schema dùng InnoDB, 14 CHECK constraints và NOT NULL cho các số liệu/cờ tương ứng: giá/lương/điểm/tiền không âm, quantity>0, sugar/ice 0..100, is_active thuộc 0/1. Không áp đặt tỷ lệ loyalty hoặc tính tổng đơn bằng SQL.

Seed gồm 2 users, 2 customers, 3 categories, 15 products, 3 toppings, 3 orders, 3 details và 1 detail-topping. Ba order minh họa COMPLETED/CASH, PENDING/QR và CANCELLED; QR chỉ là nhãn dữ liệu, không phải xác nhận thanh toán. Giá purchase đã bao gồm topping mỗi ly. Seed không tự cộng/trừ điểm như một business workflow.

## Tài khoản demo trong seed

Chỉ dùng với database mẫu; không dùng làm tài khoản production. Hash staff1 đã sửa để khớp tài liệu. Integration test đọc các fixture này và xác thực qua UserDAO; không in password/hash trong log.

| Loại | Fixture đăng nhập | Ghi chú |
|---|---|---|
| **Tài khoản Quản trị (Admin)** | User: `admin` / Pass: `123` | Toàn quyền quản lý món, topping, nhân viên, khách hàng và xem thống kê doanh thu. |
| **Tài khoản Nhân viên (Staff)** | User: `staff1` / Pass: `123` | Truy cập giao diện bán hàng (POS), tạo hóa đơn và tích/đổi điểm cho khách. |

## Contract DAO và lỗi

31 method interface cũ được giữ. Mỗi implementation có constructor không tham số và constructor `(Connection connection)` mới. Constructor nhận connection yêu cầu open và `autoCommit=false`; DAO mượn connection không được commit/close connection đó. Không chia sẻ Connection/DAO bound giữa các thread.

`DataAccessException` (unchecked) báo lỗi kết nối/SQL/mapping; caller không còn nhận empty/null/false giả thành công khi SQL lỗi. `null` của login là không tìm thấy/không active/sai password; lookup khách null là không có row. `false` của update/delete là không có row phù hợp; `updateCustomerPoints` còn false khi số dư không đủ. Không dùng catch rồi bỏ qua lỗi trong transaction. Lỗi commit/network có thể có outcome không rõ; không tự retry write một cách mù quáng.

`createOrder` bắt buộc order.staffId>0; không đọc UserSession và không fallback ID 1. FK kiểm tra user tồn tại; authorization và kiểm active/role là Business Layer. Order ID trả về vẫn do generated keys. `saveOrderDetailsAndToppings` từ chối cart rỗng, kiểm tra row count/keys và rollback nếu topping lỗi.

## Transaction dùng chung

Dùng `TransactionManager.inTransaction(connection -> ...)` để quản lý một connection/commit/rollback cho toàn callback; tạo tất cả DAO cần dùng bằng chính connection được truyền vào. Có thể quản lý connection thủ công theo cùng contract. DAO không tham số vẫn chạy độc lập; không tự gia nhập một transaction đang tồn tại.

Ví dụ cách nối các contract (không phải triển khai CafeController):

```java
int orderId = TransactionManager.inTransaction(connection -> {
    OrderDAO orders = new OrderDAOImpl(connection);
    OrderDetailDAO details = new OrderDetailDAOImpl(connection);
    CustomerDAO customers = new CustomerDAOImpl(connection);
    int id = orders.createOrder(validatedOrder);
    details.saveOrderDetailsAndToppings(id, validatedCart);
    if (!customers.updateCustomerPoints(customerId, pointsEarned, pointsUsed)) {
        throw new DataAccessException("Customer missing or insufficient points");
    }
    return id;
});
```

Caller tính/validate dữ liệu trước; nhánh khách vãng lai không gọi update điểm. Exception thoát callback làm rollback toàn bộ. Trả false từ callback **không tự rollback**, vì đó có thể là kết quả hợp lệ; caller phải throw khi điều kiện business thất bại. Detail DAO standalone có transaction riêng; khi mượn connection, dùng savepoint để lỗi batch không để lại một phần detail, và vẫn ném exception cho owner quyết định rollback toàn workflow. Khi lỗi rollback/commit, owner nên bỏ connection và xử lý trạng thái chưa xác định.

## Report và soft delete

- getRevenueLast7Days: hôm nay và 6 ngày lịch trước đó theo timezone MySQL, chỉ COMPLETED, theo ngày tăng dần. Không tự điền ngày doanh thu 0.
- getTopSellingProducts: top 5 theo tổng quantity của COMPLETED trên toàn lịch sử; ties theo product_id. Map key là `name [#productId]`, tránh mất sản phẩm trùng tên. Return type vẫn Map<String,Integer>.
- deleteUser: UPDATE is_active=false, giữ hàng user và FK Orders. Login loại user inactive. getAllUsers vẫn trả inactive; getActiveUsers loại chúng. Dùng updateUser để kích hoạt lại nếu Business cho phép. Database giữ ON DELETE RESTRICT để chặn hard delete user có lịch sử; không cascade xóa hóa đơn.
- updateCustomerPoints: không chấp nhận input âm và UPDATE có điều kiện points>=pointsUsed. Điểm mới tích trong cùng đơn không được dùng để bù số điểm muốn đổi. Hai lần đổi đồng thời không thể cùng chi tiêu một số dư cũ.

## Build và MySQL integration tests

`mvn test` kiểm tra compile/test thông thường; không chạy DatabaseIT. Dùng `mvn -Pmysql-it verify` để chạy tests MySQL thật. Profile thất bại nếu thiếu cấu hình hoặc server không phải MySQL 8.0.16+, không tự skip rồi báo pass.

Thiết lập 3 environment variables dành riêng cho test:

- CAFE_TEST_DB_URL: server URL **không có database path**, ví dụ `jdbc:mysql://127.0.0.1:33367/?allowPublicKeyRetrieval=true&useSSL=false`. Ví dụ chỉ dành MySQL tạm localhost, không dùng nguyên TLS settings này cho server production.
- CAFE_TEST_DB_USER và CAFE_TEST_DB_PASSWORD: tài khoản test có quyền CREATE/DROP schema riêng và CRUD/DDL trong schema đó; cấp bằng môi trường, không lưu trong source.

Tests tạo schema random `cafe_it_<UUID>`, reset chỉ schema đó giữa các test và xóa khi kết thúc. Không chạm cafe_management_db; không chạy tests bằng database production. Có kiểm tra version, seed/login, generated keys, CHECK/FK/UNIQUE, soft delete, 8 DAO chia sẻ connection, commit/rollback, savepoint, concurrent points, report boundaries/duplicate names và CRUD.

Các lớp mới: dao.DataAccessException, dao.TransactionManager, dao.impl.JdbcDao. Không thêm GUI/business rules hoặc tích hợp QR provider. Những hạn chế ngoài checklist như int SUM overflow, double→int rounding, active-only getAllProducts/getAllToppings, credential field của User, order lifecycle APIs vẫn cần xử lý riêng theo yêu cầu dự án.
