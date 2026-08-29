package org.yang.dao;

import org.yang.model.ApiConsumption;
import org.yang.util.DBUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsumptionDao {

    public void create(ApiConsumption c) throws Exception {
        String sql = "INSERT INTO api_consumption (user_id, conversation_id, prompt_tokens, completion_tokens, total_tokens, model) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, c.getUserId());
            ps.setInt(2, c.getConversationId());
            ps.setInt(3, c.getPromptTokens());
            ps.setInt(4, c.getCompletionTokens());
            ps.setInt(5, c.getTotalTokens());
            ps.setString(6, c.getModel() != null ? c.getModel() : "deepseek-4pro");
            ps.executeUpdate();
        }
    }

    public List<ApiConsumption> findByUserId(int userId) throws Exception {
        List<ApiConsumption> list = new ArrayList<>();
        String sql = "SELECT * FROM api_consumption WHERE user_id = ? ORDER BY created_at DESC";
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

    public int totalTokensByUser(int userId) throws Exception {
        String sql = "SELECT COALESCE(SUM(total_tokens), 0) FROM api_consumption WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    public int totalTokensTodayByUser(int userId) throws Exception {
        String sql = "SELECT COALESCE(SUM(total_tokens), 0) FROM api_consumption WHERE user_id = ? AND DATE(created_at) = CURRENT_DATE";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    // Admin: all users consumption summary
    public List<UserConsumptionSummary> allUserSummaries() throws Exception {
        List<UserConsumptionSummary> list = new ArrayList<>();
        String sql = "SELECT u.id, u.username, " +
            "COALESCE(SUM(ac.total_tokens), 0) as total_tokens, " +
            "COUNT(DISTINCT ac.conversation_id) as conv_count, " +
            "COUNT(ac.id) as api_calls " +
            "FROM users u LEFT JOIN api_consumption ac ON u.id = ac.user_id " +
            "GROUP BY u.id, u.username ORDER BY total_tokens DESC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UserConsumptionSummary s = new UserConsumptionSummary();
                s.userId = rs.getInt("id");
                s.username = rs.getString("username");
                s.totalTokens = rs.getInt("total_tokens");
                s.conversationCount = rs.getInt("conv_count");
                s.apiCalls = rs.getInt("api_calls");
                list.add(s);
            }
        }
        return list;
    }

    public int totalTokensAll() throws Exception {
        String sql = "SELECT COALESCE(SUM(total_tokens), 0) FROM api_consumption";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int totalTokensToday() throws Exception {
        String sql = "SELECT COALESCE(SUM(total_tokens), 0) FROM api_consumption WHERE DATE(created_at) = CURRENT_DATE";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private ApiConsumption map(ResultSet rs) throws SQLException {
        ApiConsumption c = new ApiConsumption();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setConversationId(rs.getInt("conversation_id"));
        c.setPromptTokens(rs.getInt("prompt_tokens"));
        c.setCompletionTokens(rs.getInt("completion_tokens"));
        c.setTotalTokens(rs.getInt("total_tokens"));
        c.setModel(rs.getString("model"));
        c.setCreatedAt(rs.getString("created_at"));
        return c;
    }

    public static class UserConsumptionSummary {
        public int userId;
        public String username;
        public int totalTokens;
        public int conversationCount;
        public int apiCalls;
    }
}
