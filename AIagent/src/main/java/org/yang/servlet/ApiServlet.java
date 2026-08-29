package org.yang.servlet;

import com.google.gson.JsonObject;
import jakarta.servlet.http.*;
import org.yang.model.*;
import org.yang.service.*;
import org.yang.util.JsonUtil;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ApiServlet extends HttpServlet {

    private final UserService userService = new UserService();
    private final ChatService chatService = new ChatService();
    private final AdminService adminService = new AdminService();
    private final SeedreamService seedreamService = new SeedreamService();
    private final SeedanceService seedanceService = new SeedanceService();

    // ── Route dispatch ─────────────────────────────────────────────
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        String method = req.getMethod();
        String uri = req.getRequestURI();
        // strip context path
        String path = uri.substring(req.getContextPath().length());

        try {
            // ── Auth (no login required) ──
            if ("POST".equals(method) && "/api/register".equals(path)) { handleRegister(req, resp); return; }
            if ("POST".equals(method) && "/api/login".equals(path))    { handleLogin(req, resp); return; }

            // ── Auth check ──
            User user = (User) req.getSession().getAttribute("user");
            if (user == null) {
                resp.setStatus(401);
                JsonUtil.writeJson(resp, Map.of("error", "未登录"));
                return;
            }

            // ── Auth routes ──
            if ("POST".equals(method) && "/api/logout".equals(path)) { handleLogout(req, resp); return; }
            if ("GET".equals(method)  && "/api/user/info".equals(path)) { handleUserInfo(req, resp); return; }
            if ("PUT".equals(method)  && "/api/user/prompt".equals(path)) { handleUpdatePrompt(req, resp); return; }

            // ── Conversations ──
            if ("GET".equals(method)    && "/api/conversations".equals(path)) { handleListConversations(req, resp, user); return; }
            if ("POST".equals(method)   && "/api/conversations".equals(path)) { handleCreateConversation(req, resp, user); return; }
            if ("DELETE".equals(method) && path.matches("/api/conversations/\\d+")) { handleDeleteConversation(req, resp, extractId(path)); return; }
            if ("GET".equals(method)    && path.matches("/api/conversations/\\d+/messages")) { handleGetMessages(req, resp, extractId(path)); return; }

            // ── Chat ──
            if ("POST".equals(method) && "/api/chat".equals(path)) { handleChat(req, resp, user); return; }

            // ── Image Generation ──
            if ("POST".equals(method) && "/api/generate-image".equals(path)) { handleGenerateImage(req, resp); return; }
            if ("GET".equals(method)  && "/api/patterns/categories".equals(path)) { handleListCategories(req, resp); return; }
            if ("GET".equals(method)  && "/api/patterns".equals(path)) { handleListPatterns(req, resp); return; }
            if ("GET".equals(method)  && "/api/samples/categories".equals(path)) { handleListSampleCategories(req, resp); return; }
            if ("GET".equals(method)  && "/api/samples".equals(path)) { handleListSamples(req, resp); return; }

            // ── Video Generation ──
            if ("POST".equals(method) && "/api/generate-video".equals(path)) { handleGenerateVideo(req, resp); return; }
            if ("GET".equals(method)  && "/api/generate-video/status".equals(path)) { handleVideoTaskStatus(req, resp); return; }

            // ── Admin ──
            if (!"admin".equals(user.getRole())) { resp.setStatus(403); JsonUtil.writeJson(resp, Map.of("error", "无权限")); return; }
            if ("GET".equals(method) && "/api/admin/stats".equals(path)) { handleAdminStats(req, resp); return; }
            if ("GET".equals(method) && "/api/admin/users".equals(path)) { handleAdminUsers(req, resp); return; }

            resp.sendError(404);
        } catch (RuntimeException e) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            resp.setStatus(500);
            e.printStackTrace();
            JsonUtil.writeJson(resp, Map.of("error", "服务器内部错误: " + e.getMessage()));
        }
    }

    // ── Handlers ───────────────────────────────────────────────────

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // 暂时关闭注册功能，重新开放时恢复下方代码
        throw new RuntimeException("数据库未搭建");
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = JsonUtil.readBody(req);
        User user = userService.login(body.get("username").getAsString(), body.get("password").getAsString());
        req.getSession().setAttribute("user", user);
        JsonUtil.writeJson(resp, Map.of("success", true, "user", sanitizeUser(user)));
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        req.getSession().invalidate();
        JsonUtil.writeJson(resp, Map.of("success", true));
    }

    private void handleUserInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = (User) req.getSession().getAttribute("user");
        User fresh = userService.getUser(user.getId());
        JsonUtil.writeJson(resp, sanitizeUser(fresh));
    }

    private void handleUpdatePrompt(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = (User) req.getSession().getAttribute("user");
        JsonObject body = JsonUtil.readBody(req);
        userService.updatePrompt(user.getId(), body.get("systemPrompt").getAsString());
        JsonUtil.writeJson(resp, Map.of("success", true));
    }

    private void handleListConversations(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        List<Conversation> convs = chatService.getConversations(user.getId());
        JsonUtil.writeJson(resp, convs);
    }

    private void handleCreateConversation(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readBody(req);
        String title = body.has("title") ? body.get("title").getAsString() : "新的对话";
        Conversation conv = chatService.createConversation(user.getId(), title);
        JsonUtil.writeJson(resp, conv);
    }

    private void handleDeleteConversation(HttpServletRequest req, HttpServletResponse resp, int convId) throws Exception {
        chatService.deleteConversation(convId);
        JsonUtil.writeJson(resp, Map.of("success", true));
    }

    private void handleGetMessages(HttpServletRequest req, HttpServletResponse resp, int convId) throws Exception {
        List<Message> messages = chatService.getMessages(convId);
        JsonUtil.writeJson(resp, messages);
    }

    private void handleChat(HttpServletRequest req, HttpServletResponse resp, User user) throws Exception {
        JsonObject body = JsonUtil.readBody(req);
        int conversationId = body.get("conversationId").getAsInt();
        String content = body.get("content").getAsString();
        Message aiMsg = chatService.sendMessage(user.getId(), conversationId, content);
        JsonUtil.writeJson(resp, aiMsg);
    }

    private void handleGenerateImage(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = JsonUtil.readBody(req);
        String prompt = body.get("prompt").getAsString();
        String referenceImagePath = body.has("referenceImagePath") && !body.get("referenceImagePath").isJsonNull()
                ? body.get("referenceImagePath").getAsString() : null;
        String referenceImageBase64 = body.has("referenceImageBase64") && !body.get("referenceImageBase64").isJsonNull()
                ? body.get("referenceImageBase64").getAsString() : null;
        String size = body.has("size") && !body.get("size").isJsonNull()
                ? body.get("size").getAsString() : "2K";
        String sampleImageBase64 = body.has("sampleImageBase64") && !body.get("sampleImageBase64").isJsonNull()
                ? body.get("sampleImageBase64").getAsString() : null;

        String referenceBase64 = null;
        if (referenceImageBase64 != null && !referenceImageBase64.isEmpty()) {
            referenceBase64 = referenceImageBase64;
        } else if (referenceImagePath != null && !referenceImagePath.isEmpty()) {
            String realPath = getServletContext().getRealPath(referenceImagePath);
            if (realPath != null && new File(realPath).exists()) {
                byte[] imageBytes = Files.readAllBytes(Path.of(realPath));
                referenceBase64 = Base64.getEncoder().encodeToString(imageBytes);
            }
        }

        SeedreamService.GenerateResult result = seedreamService.generate(prompt, referenceBase64, size, sampleImageBase64);
        JsonUtil.writeJson(resp, Map.of(
            "success", true,
            "imageBase64", result.imageBase64,
            "generatedImages", result.generatedImages,
            "totalTokens", result.totalTokens
        ));
    }

    private void handleGenerateVideo(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = JsonUtil.readBody(req);
        String prompt = body.get("prompt").getAsString();
        String referenceImagePath = body.has("referenceImagePath") && !body.get("referenceImagePath").isJsonNull()
                ? body.get("referenceImagePath").getAsString() : null;
        String referenceImageBase64 = body.has("referenceImageBase64") && !body.get("referenceImageBase64").isJsonNull()
                ? body.get("referenceImageBase64").getAsString() : null;
        int duration = body.has("duration") && !body.get("duration").isJsonNull()
                ? body.get("duration").getAsInt() : 5;
        String ratio = body.has("ratio") && !body.get("ratio").isJsonNull()
                ? body.get("ratio").getAsString() : "9:16";
        String resolution = body.has("resolution") && !body.get("resolution").isJsonNull()
                ? body.get("resolution").getAsString() : "720p";
        boolean generateAudio = body.has("generateAudio") && !body.get("generateAudio").isJsonNull()
                && body.get("generateAudio").getAsBoolean();

        String referenceBase64 = null;
        if (referenceImageBase64 != null && !referenceImageBase64.isEmpty()) {
            referenceBase64 = referenceImageBase64;
        } else if (referenceImagePath != null && !referenceImagePath.isEmpty()) {
            String realPath = getServletContext().getRealPath(referenceImagePath);
            if (realPath != null && new File(realPath).exists()) {
                byte[] imageBytes = Files.readAllBytes(Path.of(realPath));
                referenceBase64 = Base64.getEncoder().encodeToString(imageBytes);
            }
        }

        String taskId = seedanceService.submitTask(prompt, referenceBase64, duration, ratio, resolution, generateAudio);
        JsonUtil.writeJson(resp, Map.of("success", true, "taskId", taskId));
    }

    private void handleVideoTaskStatus(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String taskId = req.getParameter("taskId");
        if (taskId == null || taskId.isEmpty()) {
            resp.setStatus(400);
            JsonUtil.writeJson(resp, Map.of("error", "缺少 taskId 参数"));
            return;
        }
        SeedanceService.TaskStatus status = seedanceService.pollTask(taskId);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("status", status.status);
        result.put("isSuccess", status.isSuccess);
        result.put("isFailed", status.isFailed);
        if (status.videoUrl != null) {
            result.put("videoUrl", status.videoUrl);
        }
        result.put("totalTokens", status.totalTokens);
        JsonUtil.writeJson(resp, result);
    }

    private void handleListSampleCategories(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String samplesPath = getServletContext().getRealPath("/images/samples");
        File dir = new File(samplesPath);
        List<Map<String, Object>> categories = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File sub : subDirs) {
                    File[] images = sub.listFiles((d, n) ->
                        n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp"));
                    int count = images != null ? images.length : 0;
                    if (count > 0) {
                        categories.add(Map.of("name", sub.getName(), "count", count));
                    }
                }
                categories.sort(Comparator.comparing(m -> SAMPLE_CATEGORY_ORDER.getOrDefault((String) m.get("name"), 99)));
            }
        }
        JsonUtil.writeJson(resp, categories);
    }

    private void handleListSamples(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String categoryFilter = req.getParameter("category");
        String samplesPath = getServletContext().getRealPath("/images/samples");
        File dir = new File(samplesPath);
        List<Map<String, String>> samples = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File sub : subDirs) {
                    String categoryName = sub.getName();
                    if (categoryFilter != null && !categoryFilter.isEmpty() && !categoryName.equals(categoryFilter)) {
                        continue;
                    }
                    File[] files = sub.listFiles((d, n) ->
                        n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp"));
                    if (files != null) {
                        for (File f : files) {
                            String filename = f.getName();
                            String baseName = filename.substring(0, filename.lastIndexOf('.'));
                            samples.add(Map.of(
                                "filename", filename,
                                "path", "/images/samples/" + categoryName + "/" + filename,
                                "name", baseName,
                                "category", categoryName
                            ));
                        }
                    }
                }
            }
        }
        samples.sort(Comparator.comparing((Map<String, String> m) -> SAMPLE_CATEGORY_ORDER.getOrDefault(m.get("category"), 99))
            .thenComparing(m -> m.get("filename")));
        JsonUtil.writeJson(resp, samples);
    }

    private static final java.util.regex.Pattern DESIGNER_PAREN = java.util.regex.Pattern.compile("[（(]设计者[：:]\\s*([^）)]+)[）)]");
    private static final java.util.regex.Pattern DESIGNER_PLAIN = java.util.regex.Pattern.compile("设计者[：:]\\s*([^.]+)");
    private static final java.util.regex.Pattern BOOK_TITLE    = java.util.regex.Pattern.compile("《([^》]+)》");

    private static final Map<String, Integer> CATEGORY_ORDER = Map.ofEntries(
        Map.entry("动物纹", 1),
        Map.entry("植物纹", 2),
        Map.entry("人物纹", 3),
        Map.entry("自然天象纹", 4),
        Map.entry("吉祥纹", 5),
        Map.entry("几何纹", 6),
        Map.entry("青铜器纹", 7),
        Map.entry("漆器纹", 8),
        Map.entry("楚式乐器纹", 9),
        Map.entry("建筑纹", 10),
        Map.entry("金银器纹", 11),
        Map.entry("民间纹样", 12),
        Map.entry("荆楚纹样", 13),
        Map.entry("现代纹样", 14),
        Map.entry("连续纹样", 15),
        Map.entry("用户上传", 16)
    );

    private static final Map<String, Integer> SAMPLE_CATEGORY_ORDER = Map.ofEntries(
        Map.entry("文创", 1),
        Map.entry("家居", 2),
        Map.entry("生活类", 3),
        Map.entry("户外装栏", 4),
        Map.entry("默认", 99)
    );

    private void handleListCategories(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String patternsPath = getServletContext().getRealPath("/images/patterns");
        File dir = new File(patternsPath);
        List<Map<String, Object>> categories = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File sub : subDirs) {
                    File[] images = sub.listFiles((d, n) ->
                        n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp"));
                    int count = images != null ? images.length : 0;
                    if (count > 0) {
                        categories.add(Map.of("name", sub.getName(), "count", count));
                    }
                }
                categories.sort(Comparator.comparing(m -> CATEGORY_ORDER.getOrDefault((String) m.get("name"), 99)));
            }
        }
        JsonUtil.writeJson(resp, categories);
    }

    private void handleListPatterns(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String categoryFilter = req.getParameter("category");
        String patternsPath = getServletContext().getRealPath("/images/patterns");
        File dir = new File(patternsPath);
        List<Map<String, String>> patterns = new ArrayList<>();

        if (dir.exists() && dir.isDirectory()) {
            File[] subDirs = dir.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File sub : subDirs) {
                    String categoryName = sub.getName();
                    if (categoryFilter != null && !categoryFilter.isEmpty() && !categoryName.equals(categoryFilter)) {
                        continue;
                    }
                    File[] files = sub.listFiles((d, name) ->
                        name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"));
                    if (files != null) {
                        for (File f : files) {
                            String filename = f.getName();
                            String baseName = filename.substring(0, filename.lastIndexOf('.'));
                            Map<String, String> info = parsePatternName(baseName);
                            patterns.add(Map.of(
                                "filename", filename,
                                "path", "/images/patterns/" + categoryName + "/" + filename,
                                "name", info.get("name"),
                                "category", categoryName,
                                "designer", info.get("designer")
                            ));
                        }
                    }
                }
            }
        }
        patterns.sort(Comparator.comparing((Map<String, String> m) -> CATEGORY_ORDER.getOrDefault(m.get("category"), 99))
            .thenComparing(m -> m.get("filename")));
        JsonUtil.writeJson(resp, patterns);
    }

    private Map<String, String> parsePatternName(String baseName) {
        String designer = "";
        String cleanName = baseName;

        java.util.regex.Matcher mParen = DESIGNER_PAREN.matcher(baseName);
        if (mParen.find()) {
            designer = mParen.group(1).trim();
            cleanName = baseName.substring(0, mParen.start()).trim();
        } else {
            java.util.regex.Matcher mPlain = DESIGNER_PLAIN.matcher(baseName);
            if (mPlain.find()) {
                designer = mPlain.group(1).trim();
                cleanName = baseName.substring(0, mPlain.start()).trim();
            }
        }

        java.util.regex.Matcher mBook = BOOK_TITLE.matcher(cleanName);
        if (mBook.find()) {
            cleanName = mBook.group(1).trim();
        }

        if (cleanName.isEmpty()) { cleanName = baseName; }
        return Map.of("name", cleanName, "designer", designer);
    }

    private void handleAdminStats(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonUtil.writeJson(resp, adminService.getDashboardStats());
    }

    private void handleAdminUsers(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonUtil.writeJson(resp, adminService.getUserSummaries());
    }

    // ── Helpers ────────────────────────────────────────────────────

    private int extractId(String path) {
        String[] parts = path.replaceAll("/messages$", "").split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private Map<String, Object> sanitizeUser(User u) {
        return Map.of("id", u.getId(), "username", u.getUsername(),
            "role", u.getRole(), "systemPrompt", u.getSystemPrompt(),
            "avatarColor", u.getAvatarColor(), "createdAt", u.getCreatedAt());
    }
}
