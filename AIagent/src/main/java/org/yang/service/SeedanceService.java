package org.yang.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SeedanceService {
    private static final String BASE_URL = "https://yunwu.ai/volc/v1/contents/generations/tasks";
    private static final String API_KEY = "sk-tLfwXG9LdbhzeZ77jfOqskUecQb2HVuE7xjkfmmvHXWYFIZD";
    private static final String MODEL = "doubao-seedance-1-5-pro-251215";

    private final HttpClient httpClient;
    private final Gson gson;

    public SeedanceService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    public String submitTask(String prompt, String referenceImageBase64,
                             int duration, String ratio, String resolution,
                             boolean generateAudio) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);

        JsonArray content = new JsonArray();

        // text prompt
        JsonObject textItem = new JsonObject();
        textItem.addProperty("type", "text");
        textItem.addProperty("text", prompt);
        content.add(textItem);

        // reference image (first frame)
        if (referenceImageBase64 != null && !referenceImageBase64.isEmpty()) {
            JsonObject imageItem = new JsonObject();
            imageItem.addProperty("type", "image_url");
            JsonObject imageUrl = new JsonObject();
            imageUrl.addProperty("url", "data:image/png;base64," + referenceImageBase64);
            imageItem.add("image_url", imageUrl);
            content.add(imageItem);
        }

        body.add("content", content);
        body.addProperty("duration", duration);
        body.addProperty("ratio", ratio);
        body.addProperty("resolution", resolution);
        body.addProperty("generate_audio", generateAudio);
        body.addProperty("watermark", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Seedance submit error (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject respJson = gson.fromJson(response.body(), JsonObject.class);
        // compatible with both "task_id" and "id" field names
        if (respJson.has("task_id")) {
            return respJson.get("task_id").getAsString();
        }
        if (respJson.has("id")) {
            return respJson.get("id").getAsString();
        }
        throw new RuntimeException("Seedance submit: missing task_id in response: " + response.body());
    }

    public TaskStatus pollTask(String taskId) throws Exception {
        String url = BASE_URL + "/" + taskId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + API_KEY)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Seedance poll error (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);

        // Parse status – compatible with multiple field names
        String status = null;
        if (json.has("status")) {
            status = json.get("status").getAsString();
        } else if (json.has("state")) {
            status = json.get("state").getAsString();
        } else if (json.has("output")) {
            JsonObject output = json.getAsJsonObject("output");
            if (output.has("task_status")) {
                status = output.get("task_status").getAsString();
            }
        }

        if (status == null) {
            status = "unknown";
        }

        boolean isSuccess = status.equalsIgnoreCase("succeeded")
                || status.equalsIgnoreCase("success");
        boolean isFailed = status.equalsIgnoreCase("failed")
                || status.equalsIgnoreCase("cancelled");

        String videoUrl = null;
        int totalTokens = 0;

        if (isSuccess) {
            // result.videos[0].url
            if (json.has("result")) {
                JsonObject result = json.getAsJsonObject("result");
                if (result.has("videos")) {
                    JsonArray videos = result.getAsJsonArray("videos");
                    if (videos.size() > 0) {
                        videoUrl = videos.get(0).getAsJsonObject().get("url").getAsString();
                    }
                }
            }
            // fallback: content.video_url (volcengine official format)
            if (videoUrl == null && json.has("content")) {
                JsonObject content = json.getAsJsonObject("content");
                if (content.has("video_url")) {
                    videoUrl = content.get("video_url").getAsString();
                }
            }
        }

        if (json.has("usage")) {
            JsonObject usage = json.getAsJsonObject("usage");
            if (usage.has("total_tokens")) {
                totalTokens = usage.get("total_tokens").getAsInt();
            } else if (usage.has("completion_tokens")) {
                totalTokens = usage.get("completion_tokens").getAsInt();
            }
        }

        return new TaskStatus(status, videoUrl, totalTokens, isSuccess, isFailed);
    }

    public static class TaskStatus {
        public String status;
        public String videoUrl;
        public int totalTokens;
        public boolean isSuccess;
        public boolean isFailed;

        public TaskStatus(String status, String videoUrl, int totalTokens, boolean isSuccess, boolean isFailed) {
            this.status = status;
            this.videoUrl = videoUrl;
            this.totalTokens = totalTokens;
            this.isSuccess = isSuccess;
            this.isFailed = isFailed;
        }
    }
}
