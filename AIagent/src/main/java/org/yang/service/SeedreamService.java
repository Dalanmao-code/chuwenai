package org.yang.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public class SeedreamService {
    private static final String API_URL = "https://ark.cn-beijing.volces.com/api/v3/images/generations";
    private static final String API_KEY = "ark-552cacd8-4e1b-400f-b2ab-a1fa27df7024-985d1";
    private static final String MODEL = "doubao-seedream-5-0-lite-260128";

    private final HttpClient httpClient;
    private final Gson gson;

    public SeedreamService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    public GenerateResult generate(String prompt, String referenceImageBase64, String size, String sampleImageBase64) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("prompt", prompt);
        body.addProperty("size", size != null ? size : "2K");
        body.addProperty("response_format", "b64_json");
        body.addProperty("output_format", "png");
        body.addProperty("watermark", false);

        JsonArray imageArray = new JsonArray();
        if (referenceImageBase64 != null && !referenceImageBase64.isEmpty()) {
            imageArray.add("data:image/jpeg;base64," + referenceImageBase64);
        }
        if (sampleImageBase64 != null && !sampleImageBase64.isEmpty()) {
            imageArray.add("data:image/jpeg;base64," + sampleImageBase64);
        }
        if (imageArray.size() > 0) {
            body.add("image", imageArray);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Seedream API error (HTTP " + response.statusCode() + "): " + response.body());
        }

        JsonObject respJson = gson.fromJson(response.body(), JsonObject.class);
        JsonArray data = respJson.getAsJsonArray("data");
        JsonObject firstImage = data.get(0).getAsJsonObject();
        String base64 = firstImage.get("b64_json").getAsString();

        JsonObject usage = respJson.getAsJsonObject("usage");
        int generatedImages = usage.get("generated_images").getAsInt();
        int totalTokens = usage.get("total_tokens").getAsInt();

        return new GenerateResult(base64, generatedImages, totalTokens);
    }

    public static class GenerateResult {
        public String imageBase64;
        public int generatedImages;
        public int totalTokens;

        public GenerateResult(String imageBase64, int generatedImages, int totalTokens) {
            this.imageBase64 = imageBase64;
            this.generatedImages = generatedImages;
            this.totalTokens = totalTokens;
        }
    }
}
