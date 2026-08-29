package org.yang.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.yang.model.Message;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class DeepSeekService {
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = "sk-119d4bb0f6864d899fc9de95ab24666e";
    private static final String MODEL = "deepseek-chat";
    private final HttpClient httpClient;
    private final Gson gson;

    public DeepSeekService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    public ChatResult chat(String systemPrompt, List<Message> history, String userMessage) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);

        JsonArray messages = new JsonArray();

        // Add system message
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt != null ? systemPrompt : "你是一位博学多才的中国传统纹样专家，精通各类非遗纹样艺术。请以温文尔雅的语气，用中文回答用户关于纹样的各种问题。");
        messages.add(sysMsg);

        // Add history
        for (Message msg : history) {
            JsonObject hMsg = new JsonObject();
            hMsg.addProperty("role", msg.getRole());
            hMsg.addProperty("content", msg.getContent());
            messages.add(hMsg);
        }

        // Add current user message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);
        messages.add(userMsg);

        body.add("messages", messages);
        body.addProperty("max_tokens", 4096);
        body.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API error: " + response.body());
        }

        JsonObject respJson = gson.fromJson(response.body(), JsonObject.class);
        JsonArray choices = respJson.getAsJsonArray("choices");
        JsonObject choice = choices.get(0).getAsJsonObject();
        JsonObject message = choice.getAsJsonObject("message");
        String content = message.get("content").getAsString();

        JsonObject usage = respJson.getAsJsonObject("usage");
        int promptTokens = usage.get("prompt_tokens").getAsInt();
        int completionTokens = usage.get("completion_tokens").getAsInt();
        int totalTokens = usage.get("total_tokens").getAsInt();

        return new ChatResult(content, promptTokens, completionTokens, totalTokens);
    }

    public static class ChatResult {
        public String content;
        public int promptTokens;
        public int completionTokens;
        public int totalTokens;

        public ChatResult(String content, int promptTokens, int completionTokens, int totalTokens) {
            this.content = content;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }
    }
}
