# CafeShopManagement Database

Đã cập nhật 10 mục checklist Database ngày 21/11/2026.

- Hướng dẫn setup, DAO transaction và integration tests: [database/README.md](database/README.md).
- Báo cáo sửa đổi và kết quả kiểm thử: [CHECKLIST_COMPLETION.html](CHECKLIST_COMPLETION.html).
- Bản phân tích ban đầu: [Database-to-Controller-Integration-Reference.html](Database-to-Controller-Integration-Reference.html), chỉ là snapshot trước sửa, có banner cảnh báo.
- SOURCE_MANIFEST.json ghi hash bản nguồn đã cập nhật. Source gốc trước sửa vẫn được lưu riêng trong outputs của phiên phân tích.

Java 17; Maven; MySQL 8.0.16+; JDBC Connector/J 8.2.0; jbcrypt 0.4.
Set CAFE_DB_USER/CAFE_DB_PASSWORD trước khi dùng DAO. Build: `mvn verify`. MySQL tests: `mvn -Pmysql-it verify` với CAFE_TEST_DB_* được mô tả trong README Database.

Các method DAO cũ vẫn có; SQL failures nay ném DataAccessException. deleteUser chuyển sang soft delete. getTopSellingProducts trả key name [#productId]. Đọc các thay đổi contract này trước khi nối GUI/Controller.
