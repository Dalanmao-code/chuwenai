package org.yang.service;

import org.yang.dao.*;
import org.yang.model.*;

import java.util.List;

public class ChatService {
    private final ConversationDao conversationDao = new ConversationDao();
    private final MessageDao messageDao = new MessageDao();
    private final ConsumptionDao consumptionDao = new ConsumptionDao();
    private final UserDao userDao = new UserDao();
    private final DeepSeekService deepSeekService = new DeepSeekService();

    public Conversation createConversation(int userId, String title) throws Exception {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle(title != null ? title : "新的对话");
        return conversationDao.create(conv);
    }

    public List<Conversation> getConversations(int userId) throws Exception {
        return conversationDao.findByUserId(userId);
    }

    public void deleteConversation(int id) throws Exception {
        conversationDao.delete(id);
    }

    public List<Message> getMessages(int conversationId) throws Exception {
        return messageDao.findByConversationId(conversationId);
    }

    public Message sendMessage(int userId, int conversationId, String content) throws Exception {
        // Save user message
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setTokens(0);
        messageDao.create(userMsg);

        // Get user's system prompt
        User user = userDao.findById(userId);
        String systemPrompt = user != null ? user.getSystemPrompt() : "你是一个有帮助的AI助手。";

        // Get chat history (last 20 messages for context)
        List<Message> history = messageDao.findByConversationId(conversationId);
        // Only use messages before the current one for context
        List<Message> contextHistory = history.subList(0, Math.max(0, history.size() - 1));
        // Limit context window
        if (contextHistory.size() > 20) {
            contextHistory = contextHistory.subList(contextHistory.size() - 20, contextHistory.size());
        }

        // Call DeepSeek
        DeepSeekService.ChatResult result = deepSeekService.chat(systemPrompt, contextHistory, content);

        // Save AI response
        Message aiMsg = new Message();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(result.content);
        aiMsg.setTokens(result.totalTokens);
        messageDao.create(aiMsg);

        // Record consumption
        ApiConsumption consumption = new ApiConsumption();
        consumption.setUserId(userId);
        consumption.setConversationId(conversationId);
        consumption.setPromptTokens(result.promptTokens);
        consumption.setCompletionTokens(result.completionTokens);
        consumption.setTotalTokens(result.totalTokens);
        consumption.setModel("deepseek-chat");
        consumptionDao.create(consumption);

        // Update conversation title from first user message
        List<Message> allMessages = messageDao.findByConversationId(conversationId);
        long userMsgCount = allMessages.stream().filter(m -> "user".equals(m.getRole())).count();
        if (userMsgCount == 1) {
            String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            conversationDao.updateTitle(conversationId, title);
        }
        conversationDao.touch(conversationId);

        return aiMsg;
    }
}
