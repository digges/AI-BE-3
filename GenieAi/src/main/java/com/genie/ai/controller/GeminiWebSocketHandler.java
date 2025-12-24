package com.genie.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiWebSocketHandler extends TextWebSocketHandler {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {

        String userInput = message.getPayload().trim();
        System.out.println("🔹 User Input: " + userInput);

        String response = callGeminiForAnswer(userInput);
        session.sendMessage(new TextMessage(response));
    }

    // ---------------- GEMINI CALL ----------------
    private String callGeminiForAnswer(String userInput) {
        try {
            // Create request payload
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", userInput);

            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", List.of(textPart));

            Map<String, Object> payload = new HashMap<>();
            payload.put("contents", List.of(parts));

            String jsonPayload = objectMapper.writeValueAsString(payload);
            System.out.println("📤 Sending to Gemini: " + jsonPayload);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            // Call Gemini API
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey,
                    entity,
                    String.class
            );

            System.out.println("✅ Gemini Response Status: " + response.getStatusCode());
            System.out.println("📥 Gemini Response Body: " + response.getBody());

            // ✅ Return the full Gemini JSON response
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                return response.getBody();
            } else {
                return createErrorJson("Empty response from Gemini", "No data received");
            }

        } catch (Exception e) {
            // ✅ Handle all errors
            System.err.println("❌ Error calling Gemini API: " + e.getClass().getName());
            System.err.println("❌ Error message: " + e.getMessage());
            e.printStackTrace();

            return createErrorJson("Gemini API error", e.getMessage());
        }
    }

    // ✅ Helper method to create error JSON
    private String createErrorJson(String error, String details) {
        try {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("error", error);
            errorMap.put("details", details != null ? details : "Unknown error");

            return objectMapper.writeValueAsString(errorMap);
        } catch (Exception e) {
            return "{\"error\":\"Failed to create error response\",\"details\":\"" + e.getMessage() + "\"}";
        }
    }

}
