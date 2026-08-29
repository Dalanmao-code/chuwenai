package org.yang.service;

import org.yang.dao.ConsumptionDao;
import org.yang.dao.ConsumptionDao.UserConsumptionSummary;
import org.yang.dao.ConversationDao;
import org.yang.dao.UserDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    private final ConsumptionDao consumptionDao = new ConsumptionDao();
    private final UserDao userDao = new UserDao();
    private final ConversationDao conversationDao = new ConversationDao();

    public Map<String, Object> getDashboardStats() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userDao.count());
        stats.put("totalConversations", conversationDao.count());
        stats.put("totalTokens", consumptionDao.totalTokensAll());
        stats.put("todayTokens", consumptionDao.totalTokensToday());
        stats.put("userSummaries", consumptionDao.allUserSummaries());
        return stats;
    }

    public List<UserConsumptionSummary> getUserSummaries() throws Exception {
        return consumptionDao.allUserSummaries();
    }
}
