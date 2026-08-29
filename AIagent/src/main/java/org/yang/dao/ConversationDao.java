package org.yang.dao;

import org.yang.model.Conversation;
import org.yang.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationDao {
    public Conversation create(Conversation conv) throws Exception {
        String sql = "INSERT INTO conversations (user_id, title) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, conv.getUserId());
            ps.setString(2, conv.getTitle() != null ? conv.getTitle() : "新的对话");
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                conv.setId(rs.getInt(1));
            }
            return conv;
        }
    }

    public List<Conversation> findByUserId(int userId) throws Exception {
        List<Conversation> list = new ArrayList<>();
        String sql = "SELECT * FROM conversations WHERE user_id = ? ORDER BY updated_at DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Conversation findById(int id) throws Exception {
        String sql = "SELECT * FROM conversations WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
        }
        return null;
    }

    public void updateTitle(int id, String title) throws Exception {
        String sql = "UPDATE conversations SET title = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void touch(int id) throws Exception {
        String sql = "UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws Exception {
        String sql = "DELETE FROM conversations WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int count() throws Exception {
        String sql = "SELECT COUNT(*) FROM conversations";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Conversation map(ResultSet rs) throws SQLException {
        Conversation c = new Conversation();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setTitle(rs.getString("title"));
        c.setCreatedAt(rs.getString("created_at"));
        c.setUpdatedAt(rs.getString("updated_at"));
        return c;
    }
}
