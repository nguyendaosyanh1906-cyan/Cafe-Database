package com.cafe.cafeshopmanagement.dao.impl;

import com.cafe.cafeshopmanagement.dao.ReportDAO;
import com.cafe.cafeshopmanagement.dao.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportDAOImpl extends JdbcDao implements ReportDAO {
    public ReportDAOImpl() { super(); }
    public ReportDAOImpl(Connection connection) { super(connection); }


    // Lấy doanh thu 7 ngày gần nhất (Dùng cho BarChart)
    @Override
    public Map<String, Integer> getRevenueLast7Days() {
        Map<String, Integer> data = new LinkedHashMap<>();
        // Giả sử bảng orders có cột created_at (DATETIME) và total_amount (INT)
        String sql = "SELECT DATE(created_at) as order_date, SUM(total_amount) as revenue " +
                "FROM orders " +
                "WHERE status = 'COMPLETED' " +
                "AND created_at >= CURDATE() - INTERVAL 6 DAY " +
                "AND created_at < CURDATE() + INTERVAL 1 DAY " +
                "GROUP BY DATE(created_at) " +
                "ORDER BY DATE(created_at) ASC LIMIT 7";

        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("order_date"), rs.getInt("revenue"));
            }
        } catch (Exception e) {
            throw new DataAccessException("ReportDAOImpl operation failed", e);
        }
        return data;
    }

    // Lấy Top 5 món bán chạy nhất (Dùng cho PieChart)
    @Override
    public Map<String, Integer> getTopSellingProducts() {
        Map<String, Integer> data = new LinkedHashMap<>();
        // Kết hợp order_details và products để đếm số lượng
        String sql = "SELECT p.product_id, p.name, SUM(od.quantity) as total_sold " +
                "FROM order_details od " +
                "JOIN products p ON od.product_id = p.product_id " +
                "JOIN orders o ON od.order_id = o.order_id " +
                "WHERE o.status = 'COMPLETED' " +
                "GROUP BY p.product_id, p.name " +
                "ORDER BY total_sold DESC, p.product_id ASC LIMIT 5";

        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                data.put(rs.getString("name") + " [#" + rs.getInt("product_id") + "]", rs.getInt("total_sold"));
            }
        } catch (Exception e) {
            throw new DataAccessException("ReportDAOImpl operation failed", e);
        }
        return data;
    }

    // Tổng doanh thu hôm nay (Dùng cho Label hiển thị nhanh)
    @Override
    public int getTodayRevenue() {
        String sql = "SELECT SUM(total_amount) as today_revenue FROM orders " +
                "WHERE DATE(created_at) = CURDATE() AND status = 'COMPLETED'";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("today_revenue");
            }
        } catch (Exception e) {
            throw new DataAccessException("ReportDAOImpl operation failed", e);
        }
        return 0;
    }
}