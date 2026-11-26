package com.cafe.cafeshopmanagement.dao.impl;

import com.cafe.cafeshopmanagement.dao.ToppingDAO;
import com.cafe.cafeshopmanagement.model.Topping;
import com.cafe.cafeshopmanagement.dao.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ToppingDAOImpl extends JdbcDao implements ToppingDAO {
    public ToppingDAOImpl() { super(); }
    public ToppingDAOImpl(Connection connection) { super(connection); }


    @Override
    public List<Topping> getAllToppings() {
        return getActiveToppings();
    }

    @Override
    public List<Topping> getActiveToppings() {
        List<Topping> list = new ArrayList<>();
        String sql = "SELECT * FROM toppings WHERE is_active = TRUE";

        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Topping t = new Topping();
                t.setToppingId(rs.getInt("topping_id"));
                // Đã sửa từ "topping_name" thành "name" cho khớp với DB
                t.setName(rs.getString("name"));
                t.setPrice(rs.getInt("price"));
                t.setActive(rs.getBoolean("is_active"));
                list.add(t);
            }
        } catch (Exception e) {
            throw new DataAccessException("ToppingDAOImpl operation failed", e);
        }
        return list;
    }

    @Override
    public boolean addTopping(Topping t) {
        // Đã sửa cột topping_name -> name
        String sql = "INSERT INTO toppings (name, price, is_active) VALUES (?, ?, ?)";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setString(1, t.getName());
            ps.setInt(2, t.getPrice());
            ps.setBoolean(3, t.isActive());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("ToppingDAOImpl operation failed", e);
        }
    }

    @Override
    public boolean updateTopping(Topping t) {
        // Đã sửa cột topping_name -> name
        String sql = "UPDATE toppings SET name=?, price=?, is_active=? WHERE topping_id=?";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setString(1, t.getName());
            ps.setInt(2, t.getPrice());
            ps.setBoolean(3, t.isActive());
            ps.setInt(4, t.getToppingId());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("ToppingDAOImpl operation failed", e);
        }
    }

    @Override
    public boolean deleteTopping(int toppingId) {
        String sql = "UPDATE toppings SET is_active=FALSE WHERE topping_id=?";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setInt(1, toppingId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("ToppingDAOImpl operation failed", e);
        }
    }
}