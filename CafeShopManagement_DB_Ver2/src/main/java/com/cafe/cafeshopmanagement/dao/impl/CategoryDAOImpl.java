package com.cafe.cafeshopmanagement.dao.impl;

import com.cafe.cafeshopmanagement.dao.CategoryDAO;
import com.cafe.cafeshopmanagement.model.Category;
import com.cafe.cafeshopmanagement.dao.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAOImpl extends JdbcDao implements CategoryDAO {
    public CategoryDAOImpl() { super(); }
    public CategoryDAOImpl(Connection connection) { super(connection); }


    @Override
    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM categories";

        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Category cat = new Category();
                cat.setCategoryId(rs.getInt("category_id"));
                cat.setName(rs.getString("name"));
                list.add(cat);
            }
        } catch (Exception e) {
            throw new DataAccessException("CategoryDAOImpl operation failed", e);
        }
        return list;
    }
}