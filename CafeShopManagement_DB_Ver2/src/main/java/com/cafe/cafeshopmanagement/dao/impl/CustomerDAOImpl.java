package com.cafe.cafeshopmanagement.dao.impl;

import com.cafe.cafeshopmanagement.dao.CustomerDAO;
import com.cafe.cafeshopmanagement.model.Customer;
import com.cafe.cafeshopmanagement.dao.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAOImpl extends JdbcDao implements CustomerDAO {
    public CustomerDAOImpl() { super(); }
    public CustomerDAOImpl(Connection connection) { super(connection); }


    // --------------------------------------------------------
    // CÁC HÀM QUẢN LÝ DÀNH CHO GIAO DIỆN ADMIN (CRUD CƠ BẢN)
    // --------------------------------------------------------

    @Override
    public List<Customer> getAllCustomers() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("phone"),
                        rs.getString("full_name"),
                        rs.getInt("points")
                ));
            }
        } catch (Exception e) {
            throw new DataAccessException("CustomerDAOImpl operation failed", e);
        }
        return list;
    }

    @Override
    public boolean addCustomer(Customer c) {
        String sql = "INSERT INTO customers (phone, full_name, points) VALUES (?, ?, ?)";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {
            ps.setString(1, c.getPhone());
            ps.setString(2, c.getFullName());
            ps.setInt(3, c.getPoints());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("CustomerDAOImpl operation failed", e);
        }
    }

    @Override
    public boolean updateCustomer(Customer c) {
        String sql = "UPDATE customers SET phone=?, full_name=?, points=? WHERE customer_id=?";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {
            ps.setString(1, c.getPhone());
            ps.setString(2, c.getFullName());
            ps.setInt(3, c.getPoints());
            ps.setInt(4, c.getCustomerId());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("CustomerDAOImpl operation failed", e);
        }
    }

    @Override
    public boolean deleteCustomer(int customerId) {
        String sql = "DELETE FROM customers WHERE customer_id=?";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {
            ps.setInt(1, customerId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("CustomerDAOImpl operation failed", e);
        }
    }

    // --------------------------------------------------------
    // CÁC HÀM NGHIỆP VỤ DÀNH CHO GIAO DIỆN BÁN HÀNG (POS)
    // --------------------------------------------------------

    @Override
    public Customer getCustomerByPhone(String phone) {
        String sql = "SELECT * FROM customers WHERE phone = ?";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Customer(
                            rs.getInt("customer_id"),
                            rs.getString("phone"),
                            rs.getString("full_name"),
                            rs.getInt("points")
                    );
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("CustomerDAOImpl operation failed", e);
        }
        return null;
    }

    @Override
    public boolean updateCustomerPoints(int customerId, int pointsToAdd, int pointsUsed) {
        if (pointsToAdd < 0 || pointsUsed < 0) {
            throw new IllegalArgumentException("Point changes must be nonnegative");
        }
        String sql = "UPDATE customers SET points = points + ? - ? WHERE customer_id = ? AND points >= ?";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setInt(1, pointsToAdd);
            ps.setInt(2, pointsUsed);
            ps.setInt(3, customerId);
            ps.setInt(4, pointsUsed);

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("CustomerDAOImpl operation failed", e);
        }
    }

    @Override
    public Customer registerCustomer(String phone, String fullName) {
        String sql = "INSERT INTO customers (phone, full_name, points) VALUES (?, ?, 0)";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, phone);
            ps.setString(2, fullName);
            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int newId = rs.getInt(1);
                        return new Customer(newId, phone, fullName, 0);
                    }
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("CustomerDAOImpl operation failed", e);
        }
        throw new DataAccessException("Customer insert returned no generated key");
    }
}
