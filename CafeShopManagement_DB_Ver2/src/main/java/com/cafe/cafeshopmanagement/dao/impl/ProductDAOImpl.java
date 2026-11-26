package com.cafe.cafeshopmanagement.dao.impl;

import com.cafe.cafeshopmanagement.dao.ProductDAO;
import com.cafe.cafeshopmanagement.model.Product;
import com.cafe.cafeshopmanagement.dao.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDAOImpl extends JdbcDao implements ProductDAO {
    public ProductDAOImpl() { super(); }
    public ProductDAOImpl(Connection connection) { super(connection); }


    // Alias hỗ trợ cho các Controller gọi getAllProducts()
    @Override
    public List<Product> getAllProducts() {
        return getActiveProducts();
    }

    // 1. Lấy tất cả sản phẩm đang kinh doanh (is_active = TRUE)
    @Override
    public List<Product> getActiveProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE is_active = TRUE";

        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setCategoryId(rs.getInt("category_id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setImageUrl(rs.getString("image_url"));
                p.setBasePrice(rs.getInt("base_price"));
                p.setActive(rs.getBoolean("is_active"));
                list.add(p);
            }
        } catch (Exception e) {
            throw new DataAccessException("ProductDAOImpl operation failed", e);
        }
        return list;
    }

    // 2. Lấy sản phẩm theo Category (dùng khi bấm vào nút lọc trên UI)
    @Override
    public List<Product> getProductsByCategory(int categoryId) {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE category_id = ? AND is_active = TRUE";

        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Product p = new Product(
                            rs.getInt("product_id"),
                            rs.getInt("category_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("image_url"),
                            rs.getInt("base_price"),
                            rs.getBoolean("is_active")
                    );
                    list.add(p);
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("ProductDAOImpl operation failed", e);
        }
        return list;
    }

    // 3. Thêm mới sản phẩm
    @Override
    public boolean addProduct(Product p) {
        String sql = "INSERT INTO products (category_id, name, description, image_url, base_price, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getDescription() != null ? p.getDescription() : "");
            ps.setString(4, p.getImageUrl() != null ? p.getImageUrl() : "");
            ps.setInt(5, p.getBasePrice());
            ps.setBoolean(6, p.isActive());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("ProductDAOImpl operation failed", e);
        }
    }

    // 4. Cập nhật sản phẩm
    @Override
    public boolean updateProduct(Product p) {
        String sql = "UPDATE products SET category_id=?, name=?, description=?, image_url=?, base_price=?, is_active=? WHERE product_id=?";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setInt(1, p.getCategoryId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getDescription() != null ? p.getDescription() : "");
            ps.setString(4, p.getImageUrl() != null ? p.getImageUrl() : "");
            ps.setInt(5, p.getBasePrice());
            ps.setBoolean(6, p.isActive());
            ps.setInt(7, p.getProductId());

            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("ProductDAOImpl operation failed", e);
        }
    }

    // 5. Xóa mềm sản phẩm (Ngừng bán - ẩn khỏi UI nhưng vẫn giữ data cũ để đối soát hóa đơn)
    @Override
    public boolean deleteProduct(int productId) {
        String sql = "UPDATE products SET is_active=FALSE WHERE product_id=?";
        try (ConnectionScope scope = openConnection();
             PreparedStatement ps = scope.connection().prepareStatement(sql)) {

            ps.setInt(1, productId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            throw new DataAccessException("ProductDAOImpl operation failed", e);
        }
    }
}