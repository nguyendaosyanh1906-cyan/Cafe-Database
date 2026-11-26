package com.cafe.cafeshopmanagement.dao;

import com.cafe.cafeshopmanagement.DBConnection;
import com.cafe.cafeshopmanagement.dao.impl.*;
import com.cafe.cafeshopmanagement.model.*;
import com.cafe.cafeshopmanagement.model.Order;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real MySQL tests. Creates and drops ONLY its own randomly named cafe_it_* schema. */
class DatabaseIT {
    private static String serverUrl, user, password, schema, url;

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null) throw new IllegalStateException("Missing test environment variable: " + name);
        return value;
    }
    private static Connection admin() throws SQLException { return DriverManager.getConnection(serverUrl, user, password); }
    private static Connection database() throws SQLException { return DriverManager.getConnection(url, user, password); }

    @BeforeAll static void initialize() throws Exception {
        serverUrl = required("CAFE_TEST_DB_URL");
        user = required("CAFE_TEST_DB_USER");
        password = required("CAFE_TEST_DB_PASSWORD");
        if (!serverUrl.matches("jdbc:mysql://[^/]+/(\\?.*)?")) {
            throw new IllegalArgumentException("CAFE_TEST_DB_URL must be a server URL with no database path");
        }
        try (Connection c = admin()) {
            DatabaseMetaData m = c.getMetaData();
            assertEquals("MySQL", m.getDatabaseProductName());
            assertEquals(8, m.getDatabaseMajorVersion(), "Integration tests require MySQL 8");
            String version = m.getDatabaseProductVersion();
            if (m.getDatabaseMinorVersion() == 0) {
                Matcher patch = Pattern.compile("^8\\.0\\.(\\d+)").matcher(version);
                assertTrue(patch.find() && Integer.parseInt(patch.group(1)) >= 16, "CHECK enforcement requires 8.0.16+");
            }
            System.out.println("Integration test server: MySQL " + version);
        }
        schema = "cafe_it_" + UUID.randomUUID().toString().replace("-", "");
        int question = serverUrl.indexOf('?');
        url = question < 0 ? serverUrl + schema : serverUrl.substring(0, question) + schema + serverUrl.substring(question);
        System.setProperty("cafe.db.url", url);
        System.setProperty("cafe.db.user", user);
        System.setProperty("cafe.db.password", password);
    }

    private static void dropOwnedSchema() throws SQLException {
        if (schema == null) return;
        if (!schema.matches("cafe_it_[a-f0-9]{32}")) throw new IllegalStateException("Unsafe test schema name");
        try (Connection c = admin(); Statement s = c.createStatement()) { s.execute("DROP DATABASE IF EXISTS `" + schema + "`"); }
    }
    @BeforeEach void reset() throws Exception {
        dropOwnedSchema();
        try (Connection c = admin(); Statement s = c.createStatement()) {
            for (String file : List.of("schema.sql", "seed.sql")) {
                String sql = Files.readString(Path.of("database", file)).replace("cafe_management_db", schema);
                sql = sql.replaceAll("(?m)^\\s*--.*$", "");
                for (String statement : sql.split(";")) if (!statement.isBlank()) s.execute(statement);
            }
        }
    }
    @AfterAll static void cleanup() throws Exception {
        try { dropOwnedSchema(); }
        finally {
            System.clearProperty("cafe.db.url"); System.clearProperty("cafe.db.user"); System.clearProperty("cafe.db.password");
        }
    }
    private static int scalar(String sql) throws SQLException {
        try (Connection c = database(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }
    private static void sql(String sql) throws SQLException {
        try (Connection c = database(); Statement s = c.createStatement()) { s.execute(sql); }
    }
    private static Order order() {
        Order o = new Order(); o.setStaffId(2); o.setCustomerId(1); o.setTotalAmount(17500);
        o.setPaymentMethod("CASH"); o.setStatus("COMPLETED"); o.setPointsUsed(200); return o;
    }
    private static CartItem item() {
        Product p = new ProductDAOImpl().getActiveProducts().stream().filter(x -> x.getProductId() == 1).findFirst().orElseThrow();
        Topping t = new ToppingDAOImpl().getActiveToppings().stream().filter(x -> x.getToppingId() == 1).findFirst().orElseThrow();
        return new CartItem(p, "M", 70, 50, new ArrayList<>(List.of(t)), 1);
    }
    private static String fixturePassword(String name) throws Exception {
        String readme = Files.readString(Path.of("database/README.md"));
        Matcher m = Pattern.compile("(?m)^.*User:\\s*`" + Pattern.quote(name) + "`\\s*/\\s*Pass:\\s*`([^`]+)`.*$").matcher(readme);
        if (!m.find()) throw new IllegalStateException("Missing documented fixture credentials");
        return m.group(1);
    }

    @Test void seededAccountsActuallyAuthenticate() throws Exception {
        UserDAO dao = new UserDAOImpl();
        for (String name : List.of("admin", "staff1")) {
            User u = dao.login(name, fixturePassword(name));
            assertNotNull(u, "Documented fixture login must succeed"); assertEquals(name, u.getUsername());
        }
        assertNull(dao.login("staff1", UUID.randomUUID().toString()));
    }
    @Test void schemaAndSeedCoverAllTables() throws Exception {
        assertEquals(8, scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='" + schema + "'"));
        for (String t : List.of("users", "customers", "categories", "products", "toppings", "orders", "order_details", "order_detail_toppings")) {
            assertTrue(scalar("SELECT COUNT(*) FROM " + t) > 0, t);
        }
        assertEquals(0, scalar("SELECT COUNT(*) FROM orders o JOIN (SELECT order_id,SUM(quantity*price_at_purchase) amount FROM order_details GROUP BY order_id) d ON o.order_id=d.order_id WHERE o.total_amount<>d.amount"));
    }
    @Test void connectionUsesExternalSettingsAndFailsExplicitly() {
        String prior = System.getProperty("cafe.db.url");
        try {
            System.setProperty("cafe.db.url", "jdbc:no-such-driver:invalid");
            assertThrows(DataAccessException.class, DBConnection::getConnection);
        } finally { System.setProperty("cafe.db.url", prior); }
    }
    @Test void allDaosCanBorrowOneConnectionWithoutClosingOrCommitting() throws Exception {
        try (Connection c = database()) {
            assertThrows(IllegalArgumentException.class, () -> new UserDAOImpl(c));
            c.setAutoCommit(false);
            assertFalse(new UserDAOImpl(c).getAllUsers().isEmpty());
            assertFalse(new CustomerDAOImpl(c).getAllCustomers().isEmpty());
            assertFalse(new CategoryDAOImpl(c).getAllCategories().isEmpty());
            assertFalse(new ProductDAOImpl(c).getActiveProducts().isEmpty());
            assertFalse(new ToppingDAOImpl(c).getActiveToppings().isEmpty());
            assertFalse(new OrderDAOImpl(c).getAllOrderHistory().isEmpty());
            new ReportDAOImpl(c).getTodayRevenue();
            int id = new OrderDAOImpl(c).createOrder(order());
            new OrderDetailDAOImpl(c).saveOrderDetailsAndToppings(id, List.of(item()));
            assertFalse(c.isClosed()); assertFalse(c.getAutoCommit());
            assertEquals(0, scalar("SELECT COUNT(*) FROM orders WHERE order_id=" + id));
            c.rollback();
            assertEquals(0, scalar("SELECT COUNT(*) FROM orders WHERE order_id=" + id));
        }
    }
    @Test void transactionCommitsHeaderDetailsToppingsAndPointsTogether() throws Exception {
        int id = TransactionManager.inTransaction(c -> {
            int created = new OrderDAOImpl(c).createOrder(order());
            assertTrue(new OrderDetailDAOImpl(c).saveOrderDetailsAndToppings(created, List.of(item())));
            assertTrue(new CustomerDAOImpl(c).updateCustomerPoints(1, 175, 200));
            return created;
        });
        assertEquals(1, scalar("SELECT COUNT(*) FROM orders WHERE order_id=" + id));
        assertEquals(1, scalar("SELECT COUNT(*) FROM order_detail_toppings t JOIN order_details d USING(order_detail_id) WHERE d.order_id=" + id));
        assertEquals(975, scalar("SELECT points FROM customers WHERE customer_id=1"));
    }
    @Test void lateFailureRollsBackAllThreeDaoWrites() throws Exception {
        int before = scalar("SELECT COUNT(*) FROM orders");
        assertThrows(DataAccessException.class, () -> TransactionManager.inTransaction(c -> {
            int id = new OrderDAOImpl(c).createOrder(order());
            new OrderDetailDAOImpl(c).saveOrderDetailsAndToppings(id, List.of(item()));
            new CustomerDAOImpl(c).updateCustomerPoints(1, 175, 200);
            throw new DataAccessException("Injected failure after all writes");
        }));
        assertEquals(before, scalar("SELECT COUNT(*) FROM orders"));
        assertEquals(3, scalar("SELECT COUNT(*) FROM order_details"));
        assertEquals(1, scalar("SELECT COUNT(*) FROM order_detail_toppings"));
        assertEquals(1000, scalar("SELECT points FROM customers WHERE customer_id=1"));
    }
    @Test void realForeignKeyFailureRollsBackHeaderAndLoyalty() throws Exception {
        assertThrows(DataAccessException.class, () -> TransactionManager.inTransaction(c -> {
            int id = new OrderDAOImpl(c).createOrder(order());
            new CustomerDAOImpl(c).updateCustomerPoints(1, 10, 200);
            CartItem broken = new CartItem(new Product(999999, 1, "Missing", null, null, 100, true), "M", 100, 100, List.of(), 1);
            return new OrderDetailDAOImpl(c).saveOrderDetailsAndToppings(id, List.of(broken));
        }));
        assertEquals(3, scalar("SELECT COUNT(*) FROM orders"));
        assertEquals(1000, scalar("SELECT points FROM customers WHERE customer_id=1"));
    }
    @Test void standaloneDetailFailureRollsBackBatchAndLeavesHeader() throws Exception {
        int id = new OrderDAOImpl().createOrder(order());
        CartItem bad = item(); bad.getToppings().add(bad.getToppings().get(0));
        assertThrows(DataAccessException.class, () -> new OrderDetailDAOImpl().saveOrderDetailsAndToppings(id, List.of(item(), bad)));
        assertEquals(1, scalar("SELECT COUNT(*) FROM orders WHERE order_id=" + id));
        assertEquals(0, scalar("SELECT COUNT(*) FROM order_details WHERE order_id=" + id));
        assertThrows(IllegalArgumentException.class, () -> new OrderDetailDAOImpl().saveOrderDetailsAndToppings(id, List.of()));
    }
    @Test void failedBorrowedDetailUsesSavepointAndKeepsConnectionOpen() throws Exception {
        try (Connection c = database()) {
            c.setAutoCommit(false);
            int id = new OrderDAOImpl(c).createOrder(order());
            CartItem bad = item(); bad.getToppings().add(bad.getToppings().get(0));
            assertThrows(DataAccessException.class, () -> new OrderDetailDAOImpl(c).saveOrderDetailsAndToppings(id, List.of(item(), bad)));
            assertFalse(c.isClosed());
            try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM order_details WHERE order_id=" + id)) { r.next(); assertEquals(0, r.getInt(1)); }
            c.rollback();
        }
    }
    @Test void missingStaffNeverFallsBackAndUnknownStaffViolatesFk() throws Exception {
        Order o = order(); o.setStaffId(0);
        assertThrows(IllegalArgumentException.class, () -> new OrderDAOImpl().createOrder(o));
        o.setStaffId(-1);
        assertThrows(IllegalArgumentException.class, () -> new OrderDAOImpl().createOrder(o));
        o.setStaffId(99999);
        assertThrows(DataAccessException.class, () -> new OrderDAOImpl().createOrder(o));
        assertEquals(3, scalar("SELECT COUNT(*) FROM orders"));
    }
    @Test void userDeleteIsSoftAndForeignKeyStillProtectsHistory() throws Exception {
        assertThrows(SQLException.class, () -> sql("DELETE FROM users WHERE user_id=2"));
        assertTrue(new UserDAOImpl().deleteUser(2));
        assertEquals(1, scalar("SELECT COUNT(*) FROM users WHERE user_id=2 AND is_active=0"));
        assertEquals(3, scalar("SELECT COUNT(*) FROM orders WHERE staff_id=2"));
        assertNull(new UserDAOImpl().login("staff1", fixturePassword("staff1")));
        assertEquals(1, new UserDAOImpl().getActiveUsers().size());
        assertEquals(2, new UserDAOImpl().getAllUsers().size());
        assertFalse(new UserDAOImpl().deleteUser(99999));
    }
    @Test void customerUniqueAndForeignKeySetNullAreEnforced() throws Exception {
        CustomerDAO dao = new CustomerDAOImpl();
        Customer c = dao.registerCustomer("test-phone", "Test member");
        assertNotNull(c); assertTrue(c.getCustomerId() > 0);
        assertThrows(DataAccessException.class, () -> dao.registerCustomer("test-phone", "Duplicate"));
        assertTrue(dao.deleteCustomer(1));
        assertEquals(1, scalar("SELECT COUNT(*) FROM orders WHERE order_id=1001 AND customer_id IS NULL"));
    }
    @Test void userCrudHashesOnceAndCanReactivateSoftDeletedAccount() {
        UserDAO dao = new UserDAOImpl();
        String plain = UUID.randomUUID().toString();
        User created = new User(0,"integration-staff",plain,"STAFF","Test staff",null,0,true);
        assertTrue(dao.addUser(created));
        User loaded = dao.login("integration-staff",plain);
        assertNotNull(loaded);
        assertTrue(dao.deleteUser(loaded.getUserId()));
        assertNull(dao.login("integration-staff",plain));
        loaded.setActive(true); loaded.setFullName("Reactivated staff");
        assertTrue(dao.updateUser(loaded));
        assertEquals("Reactivated staff",dao.login("integration-staff",plain).getFullName());
    }
    @Test void sevenDayRevenueExcludesOldFutureAndNoncompletedOrders() throws Exception {
        sql("DELETE FROM orders");
        sql("INSERT INTO orders (staff_id,total_amount,payment_method,status,created_at) VALUES " +
            "(2,10,'CASH','COMPLETED',CURDATE()-INTERVAL 20 DAY)," +
            "(2,20,'CASH','COMPLETED',CURDATE()-INTERVAL 7 DAY)," +
            "(2,100,'CASH','COMPLETED',CURDATE()-INTERVAL 6 DAY)," +
            "(2,200,'CASH','COMPLETED',CURDATE())," +
            "(2,30,'QR','PENDING',CURDATE())," +
            "(2,40,'CASH','CANCELLED',CURDATE())," +
            "(2,50,'CASH','COMPLETED',CURDATE()+INTERVAL 1 DAY)");
        Map<String,Integer> actual = new ReportDAOImpl().getRevenueLast7Days();
        assertEquals(List.of(100,200), new ArrayList<>(actual.values()));
        LocalDate today;
        try (Connection c = database(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT CURDATE()")) { r.next(); today = r.getDate(1).toLocalDate(); }
        assertEquals(List.of(today.minusDays(6).toString(), today.toString()), new ArrayList<>(actual.keySet()));
        assertEquals(200, new ReportDAOImpl().getTodayRevenue());
    }
    @Test void topProductsExcludePendingCancelledAndKeepDuplicateNamesSeparate() throws Exception {
        sql("UPDATE products SET name='Same name' WHERE product_id IN (1,2)");
        sql("INSERT INTO order_details (order_id,product_id,size,quantity,price_at_purchase) VALUES (1001,2,'M',3,15000),(1002,2,'M',100,15000)");
        Map<String,Integer> top = new ReportDAOImpl().getTopSellingProducts();
        assertEquals(2, top.size());
        assertEquals(3, top.get("Same name [#2]"));
        assertEquals(2, top.get("Same name [#1]"));
        assertEquals("Same name [#2]", top.keySet().iterator().next());
    }
    @Test void pointsCannotOverspendOrAcceptNegativeChanges() throws Exception {
        CustomerDAO dao = new CustomerDAOImpl();
        assertFalse(dao.updateCustomerPoints(1, 5000, 1001));
        assertThrows(IllegalArgumentException.class, () -> dao.updateCustomerPoints(1,-1,0));
        assertThrows(IllegalArgumentException.class, () -> dao.updateCustomerPoints(1,0,-1));
        assertEquals(1000, scalar("SELECT points FROM customers WHERE customer_id=1"));
        assertTrue(dao.updateCustomerPoints(1,10,1000));
        assertEquals(10, scalar("SELECT points FROM customers WHERE customer_id=1"));
    }
    @Test void concurrentRedemptionCannotSpendSamePointsTwice() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<Boolean> redeem = () -> { start.await(); return new CustomerDAOImpl().updateCustomerPoints(1,0,800); };
            Future<Boolean> a = pool.submit(redeem), b = pool.submit(redeem); start.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS), b.get(15,TimeUnit.SECONDS));
            assertEquals(200, scalar("SELECT points FROM customers WHERE customer_id=1"));
        } finally { pool.shutdownNow(); }
    }
    @ParameterizedTest @ValueSource(strings = {
        "UPDATE users SET salary=-1 WHERE user_id=2", "UPDATE users SET is_active=2 WHERE user_id=2",
        "UPDATE customers SET points=-1 WHERE customer_id=1", "UPDATE customers SET points=NULL WHERE customer_id=1",
        "UPDATE toppings SET price=-1 WHERE topping_id=1", "UPDATE products SET base_price=-1 WHERE product_id=1",
        "UPDATE orders SET total_amount=-1 WHERE order_id=1001", "UPDATE orders SET points_used=-1 WHERE order_id=1001",
        "UPDATE order_details SET quantity=0 WHERE order_detail_id=1001", "UPDATE order_details SET quantity=-1 WHERE order_detail_id=1001",
        "UPDATE order_details SET sugar_level=-1 WHERE order_detail_id=1001", "UPDATE order_details SET sugar_level=101 WHERE order_detail_id=1001",
        "UPDATE order_details SET ice_level=-1 WHERE order_detail_id=1001", "UPDATE order_details SET ice_level=101 WHERE order_detail_id=1001",
        "UPDATE order_details SET price_at_purchase=-1 WHERE order_detail_id=1001", "UPDATE order_detail_toppings SET topping_price=-1 WHERE order_detail_id=1001"
    }) void checkConstraintsRejectInvalidData(String statement) { assertThrows(SQLException.class, () -> sql(statement)); }
    @Test void constraintsAcceptBoundaryValues() throws Exception {
        sql("UPDATE order_details SET sugar_level=0,ice_level=100,quantity=1,price_at_purchase=0 WHERE order_detail_id=1001");
        sql("UPDATE customers SET points=0 WHERE customer_id=1");
        sql("UPDATE orders SET total_amount=0,points_used=0 WHERE order_id=1001");
    }
    @Test void menuAndCustomerCrudStillWork() throws Exception {
        assertEquals(3, new CategoryDAOImpl().getAllCategories().size());
        ProductDAO p = new ProductDAOImpl();
        assertTrue(p.addProduct(new Product(0,1,"Test product",null,null,1000,true)));
        Product created = p.getProductsByCategory(1).stream().filter(x -> x.getName().equals("Test product")).findFirst().orElseThrow();
        created.setBasePrice(2000); assertTrue(p.updateProduct(created)); assertTrue(p.deleteProduct(created.getProductId()));
        assertTrue(p.getActiveProducts().stream().noneMatch(x -> x.getProductId()==created.getProductId()));
        ToppingDAO t = new ToppingDAOImpl();
        assertTrue(t.addTopping(new Topping(0,"Test topping",100,true)));
        Topping added = t.getAllToppings().stream().filter(x -> x.getName().equals("Test topping")).findFirst().orElseThrow();
        added.setPrice(200); assertTrue(t.updateTopping(added)); assertTrue(t.deleteTopping(added.getToppingId()));
        CustomerDAO c = new CustomerDAOImpl();
        assertTrue(c.addCustomer(new Customer(0,"crud-phone","Customer",0)));
        Customer member = c.getCustomerByPhone("crud-phone"); member.setFullName("Changed"); assertTrue(c.updateCustomer(member));
        assertEquals("Changed", c.getCustomerByPhone("crud-phone").getFullName());
    }
}
