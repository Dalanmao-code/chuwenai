package org.yang.model;

public class Message {
    private int id;
    private int conversationId;
    private String role;
    private String content;
    private int tokens;
    private String createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
