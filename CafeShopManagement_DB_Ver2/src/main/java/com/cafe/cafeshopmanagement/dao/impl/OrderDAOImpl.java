package com.cafe.cafeshopmanagement.dao.impl;

import com.cafe.cafeshopmanagement.dao.OrderDAO;
import com.cafe.cafeshopmanagement.model.Order;
import com.cafe.cafeshopmanagement.model.OrderHistoryDTO;
import com.cafe.cafeshopmanagement.dao.DataAccessException;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class OrderDAOImpl extends JdbcDao implements OrderDAO {
    public OrderDAOImpl() { super(); }
    public OrderDAOImpl(Connection connection) { super(connection); }


    @Override
    public int createOrder(Order order) {
        if (order == null || order.getStaffId() <= 0) {
            throw new IllegalArgumentException("Order requires an explicit positive staffId");
        }
        String sql = "INSERT INTO orders (staff_id, customer_id, total_amount, points_used, payment_method, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW())";

        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, order.getStaffId());

            // Customer ID
            if (order.getCustomerId() != null && order.getCustomerId() > 0) {
                ps.setInt(2, order.getCustomerId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            ps.setInt(3, order.getTotalAmount());
            ps.setInt(4, order.getPointsUsed());
            ps.setString(5, order.getPaymentMethod() != null ? order.getPaymentMethod() : "CASH");
            ps.setString(6, order.getStatus() != null ? order.getStatus() : "COMPLETED");

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1); // Trả về order_id tự tăng vừa tạo
                    }
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("OrderDAOImpl operation failed", e);
        }
        throw new DataAccessException("Order insert returned no generated key");
    }

    @Override
    public List<OrderHistoryDTO> getAllOrderHistory() {
        List<OrderHistoryDTO> list = new ArrayList<>();
        // Dùng LEFT JOIN để lấy tên nhân viên và SĐT khách hàng
        String sql = "SELECT o.order_id, u.full_name AS staff_name, c.phone AS customer_phone, " +
                "o.total_amount, o.payment_method, o.status, o.created_at " +
                "FROM orders o " +
                "LEFT JOIN users u ON o.staff_id = u.user_id " +
                "LEFT JOIN customers c ON o.customer_id = c.customer_id " +
                "ORDER BY o.created_at DESC"; // Sắp xếp hóa đơn mới nhất lên đầu

        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new OrderHistoryDTO(
                        rs.getInt("order_id"),
                        rs.getString("staff_name"),
                        rs.getString("customer_phone"),
                        rs.getInt("total_amount"),
                        rs.getString("payment_method"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (Exception e) {
            throw new DataAccessException("OrderDAOImpl operation failed", e);
        }
        return list;
    }
}