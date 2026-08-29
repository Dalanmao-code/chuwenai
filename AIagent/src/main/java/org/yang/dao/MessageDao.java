package org.yang.dao;

import org.yang.model.Message;
import org.yang.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {

    public Message create(Message msg) throws Exception {
        String sql = "INSERT INTO messages (conversation_id, role, content, tokens) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, msg.getConversationId());
            ps.setString(2, msg.getRole());
            ps.setString(3, msg.getContent());
            ps.setInt(4, msg.getTokens());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                msg.setId(rs.getInt(1));
            }
            return msg;
        }
    }

    public List<Message> findByConversationId(int conversationId) throws Exception {
        List<Message> list = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE conversation_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public void deleteByConversationId(int conversationId) throws Exception {
        String sql = "DELETE FROM messages WHERE conversation_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.executeUpdate();
        }
    }

    private Message map(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setConversationId(rs.getInt("conversation_id"));
        m.setRole(rs.getString("role"));
        m.setContent(rs.getString("content"));
        m.setTokens(rs.getInt("tokens"));
        m.setCreatedAt(rs.getString("created_at"));
        return m;
    }
}
