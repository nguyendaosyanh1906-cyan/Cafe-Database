package com.cafe.cafeshopmanagement.dao.impl;

import com.cafe.cafeshopmanagement.dao.OrderDetailDAO;
import com.cafe.cafeshopmanagement.dao.DataAccessException;
import com.cafe.cafeshopmanagement.model.CartItem;
import com.cafe.cafeshopmanagement.model.Topping;
import java.sql.*;
import java.util.List;

public class OrderDetailDAOImpl extends JdbcDao implements OrderDetailDAO {
    public OrderDetailDAOImpl() { super(); }
    public OrderDetailDAOImpl(Connection connection) { super(connection); }

    @Override
    public boolean saveOrderDetailsAndToppings(int orderId, List<CartItem> cartItems) {
        if (orderId <= 0 || cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("An existing order and nonempty cart are required");
        }
        try (ConnectionScope scope = openConnection()) {
            Connection conn = scope.connection();
            if (scope.owned()) conn.setAutoCommit(false);
            Savepoint savepoint = scope.owned() ? null : conn.setSavepoint();
            try {
                insertItems(conn, orderId, cartItems);
                if (scope.owned()) conn.commit();
                else conn.releaseSavepoint(savepoint);
                return true;
            } catch (Exception | Error failure) {
                try {
                    if (scope.owned()) conn.rollback();
                    else conn.rollback(savepoint);
                } catch (SQLException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
                if (failure instanceof Error) throw (Error) failure;
                throw new DataAccessException("Cannot save order details and toppings", failure);
            }
        } catch (DataAccessException e) { throw e; }
        catch (SQLException e) { throw new DataAccessException("Order detail connection failed", e); }
    }

    private void insertItems(Connection conn, int orderId, List<CartItem> items) throws SQLException {
        String detailSql = "INSERT INTO order_details (order_id, product_id, size, sugar_level, ice_level, quantity, price_at_purchase) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String toppingSql = "INSERT INTO order_detail_toppings (order_detail_id, topping_id, topping_price) VALUES (?, ?, ?)";
        for (CartItem item : items) {
            try (PreparedStatement detail = conn.prepareStatement(detailSql, Statement.RETURN_GENERATED_KEYS)) {
                detail.setInt(1, orderId);
                detail.setInt(2, item.getProduct().getProductId());
                detail.setString(3, item.getSize() == null ? "M" : item.getSize());
                detail.setInt(4, item.getSugarPercent());
                detail.setInt(5, item.getIcePercent());
                detail.setInt(6, item.getQuantity());
                detail.setInt(7, (int) item.getUnitPrice());
                if (detail.executeUpdate() != 1) throw new SQLException("Detail insert did not affect one row");
                try (ResultSet keys = detail.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Detail insert returned no generated key");
                    int detailId = keys.getInt(1);
                    if (item.getToppings() != null && !item.getToppings().isEmpty()) {
                        try (PreparedStatement topping = conn.prepareStatement(toppingSql)) {
                            for (Topping t : item.getToppings()) {
                                topping.setInt(1, detailId);
                                topping.setInt(2, t.getToppingId());
                                topping.setInt(3, t.getPrice());
                                topping.addBatch();
                            }
                            for (int count : topping.executeBatch()) {
                                if (count != 1 && count != Statement.SUCCESS_NO_INFO) {
                                    throw new SQLException("Topping insert did not succeed");
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
