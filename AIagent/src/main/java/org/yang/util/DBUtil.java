package org.yang.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBUtil {
    private static final String DB_URL = "jdbc:h2:file:" + System.getProperty("user.home") + "/aiagent_data;AUTO_SERVER=TRUE;MODE=MySQL";
    private static final String USER = "sa";
    private static final String PASS = "";

    static {
        try {
            Class.forName("org.h2.Driver");
            initDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Database init failed", e);
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    private static void initDatabase() throws Exception {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) UNIQUE NOT NULL, " +
                "password VARCHAR(100) NOT NULL, " +
                "role VARCHAR(10) DEFAULT 'user', " +
                "system_prompt TEXT DEFAULT '你是一位博学多才的中国传统纹样专家，精通各类非物质文化遗产中的纹样艺术。你熟悉云锦、苏绣、蜀绣、湘绣、粤绣、宋锦、缂丝、蜡染、扎染、蓝印花布等传统工艺中的经典纹样，对龙纹、凤纹、麒麟纹、缠枝纹、卷草纹、回纹、云纹、如意纹、八宝纹、暗八仙等纹样元素如数家珍。你能够为用户讲解纹样的历史渊源、文化寓意、构图特点、配色技巧以及创新应用。请以温文尔雅的语气，用中文回答用户关于纹样的各种问题。', " +
                "avatar_color VARCHAR(20) DEFAULT '#667eea', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS conversations (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "title VARCHAR(100) DEFAULT '新的对话', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS messages (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "conversation_id INT NOT NULL, " +
                "role VARCHAR(20) NOT NULL, " +
                "content TEXT NOT NULL, " +
                "tokens INT DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS api_consumption (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "conversation_id INT NOT NULL, " +
                "prompt_tokens INT DEFAULT 0, " +
                "completion_tokens INT DEFAULT 0, " +
                "total_tokens INT DEFAULT 0, " +
                "model VARCHAR(50) DEFAULT 'deepseek-4pro', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");

            // Insert default accounts if not exists
            insertUserIfAbsent(conn, "admin", "admin123", "admin", "你是一位博学多才的中国传统纹样专家，精通各类非物质文化遗产中的纹样艺术。你熟悉云锦、苏绣、蜀绣、湘绣、粤绣、宋锦、缂丝、蜡染、扎染、蓝印花布等传统工艺中的经典纹样，对龙纹、凤纹、麒麟纹、缠枝纹、卷草纹、回纹、云纹、如意纹、八宝纹、暗八仙等纹样元素如数家珍。你能够为用户讲解纹样的历史渊源、文化寓意、构图特点、配色技巧以及创新应用。请以温文尔雅的语气，用中文回答用户关于纹样的各种问题。");
            insertUserIfAbsent(conn, "dalanmao", "123456", "user", "你是一个有帮助的AI助手。");
        }
    }

    private static void insertUserIfAbsent(Connection conn, String username, String password, String role, String systemPrompt) throws Exception {
        var check = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
        check.setString(1, username);
        var rs = check.executeQuery();
        rs.next();
        if (rs.getInt(1) == 0) {
            var insert = conn.prepareStatement(
                "INSERT INTO users (username, password, role, system_prompt) VALUES (?, ?, ?, ?)");
            insert.setString(1, username);
            insert.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            insert.setString(3, role);
            insert.setString(4, systemPrompt);
            insert.execute();
        }
    }

    // Password hashing using PBKDF2 (no external dependency)
    public static class BCrypt {
        public static String hashpw(String password, String salt) {
            try {
                var spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt.getBytes(), 10000, 256);
                var factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] hash = factory.generateSecret(spec).getEncoded();
                return salt + ":" + java.util.Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static String gensalt() {
            byte[] salt = new byte[16];
            new java.security.SecureRandom().nextBytes(salt);
            return java.util.Base64.getEncoder().encodeToString(salt);
        }

        public static boolean checkpw(String plaintext, String hashed) {
            String[] parts = hashed.split(":", 2);
            if (parts.length != 2) return false;
            return hashpw(plaintext, parts[0]).equals(hashed);
        }
    }
}
