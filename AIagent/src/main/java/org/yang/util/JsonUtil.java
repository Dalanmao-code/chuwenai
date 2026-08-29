package org.yang.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

public class JsonUtil {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    public static void writeJson(HttpServletResponse resp, Object obj) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.print(GSON.toJson(obj));
        out.flush();
    }

    public static JsonObject readBody(HttpServletRequest req) throws IOException {
        String body = req.getReader().lines().collect(Collectors.joining("\n"));
        if (body == null || body.isEmpty()) return new JsonObject();
        return GSON.fromJson(body, JsonObject.class);
    }

    public static <T> T parseJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
}
