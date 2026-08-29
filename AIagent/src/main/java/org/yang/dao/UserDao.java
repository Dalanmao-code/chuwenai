package org.yang.dao;

import org.yang.model.User;
import org.yang.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User create(User user) throws Exception {
        String sql = "INSERT INTO users (username, password, role, system_prompt, avatar_color) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole() != null ? user.getRole() : "user");
            ps.setString(4, user.getSystemPrompt() != null ? user.getSystemPrompt() : "你是一个有帮助的AI助手。");
            ps.setString(5, user.getAvatarColor() != null ? user.getAvatarColor() : "#667eea");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
            return user;
        }
    }

    public User findByUsername(String username) throws Exception {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        }
        return null;
    }

    public User findById(int id) throws Exception {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        }
        return null;
    }

    public void updatePrompt(int userId, String prompt) throws Exception {
        String sql = "UPDATE users SET system_prompt = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prompt);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public List<User> findAll() throws Exception {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }

    public int count() throws Exception {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setSystemPrompt(rs.getString("system_prompt"));
        u.setAvatarColor(rs.getString("avatar_color"));
        u.setCreatedAt(rs.getString("created_at"));
        return u;
    }
}
