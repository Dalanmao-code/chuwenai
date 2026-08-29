package org.yang.service;

import org.yang.dao.UserDao;
import org.yang.model.User;
import org.yang.util.DBUtil;

public class UserService {
    private final UserDao userDao = new UserDao();

    public User register(String username, String password) throws Exception {
        User existing = userDao.findByUsername(username);
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(DBUtil.BCrypt.hashpw(password, DBUtil.BCrypt.gensalt()));
        user.setRole("user");
        user.setSystemPrompt("你是一个有帮助的AI助手。");
        user.setAvatarColor(randomColor());
        return userDao.create(user);
    }

    public User login(String username, String password) throws Exception {
        User user = userDao.findByUsername(username);
        if (user == null || !DBUtil.BCrypt.checkpw(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        return user;
    }

    public User getUser(int id) throws Exception {
        return userDao.findById(id);
    }

    public void updatePrompt(int userId, String prompt) throws Exception {
        userDao.updatePrompt(userId, prompt);
    }

    private String randomColor() {
        String[] colors = {"#667eea", "#764ba2", "#f093fb", "#f5576c", "#4facfe", "#00f2fe", "#43e97b", "#fa709a"};
        return colors[(int) (Math.random() * colors.length)];
    }
}
