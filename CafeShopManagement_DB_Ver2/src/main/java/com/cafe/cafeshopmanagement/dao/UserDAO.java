package com.cafe.cafeshopmanagement.dao;

import com.cafe.cafeshopmanagement.model.User;
import java.util.List;

public interface UserDAO {
    User login(String username, String password);
    List<User> getActiveUsers();
    List<User> getAllUsers();
    boolean addUser(User u);
    boolean updateUser(User u);
    /** Soft delete: sets is_active=false; preserves rows referenced by orders. */
    boolean deleteUser(int userId);
}