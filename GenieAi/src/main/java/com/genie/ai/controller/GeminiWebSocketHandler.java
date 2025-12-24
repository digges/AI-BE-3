package com.genie.ai.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genie.ai.entity.Product;
//import com.genie.ai.repo.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GeminiWebSocketHandler extends TextWebSocketHandler {

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {

        String userInput = message.getPayload().trim().toLowerCase();
        System.out.println("🔹 User Input: " + userInput);

        String response = callGeminiForAnswer(userInput);
        session.sendMessage(new TextMessage(response));
    }

    // ---------------- ECOMMERCE CHECK ----------------
    private boolean isEcommerceQuery(String userInput) {
        String[] categories = {"ac", "refrigerator", "fridge", "washing machine", "tv", "television"};

        for (String category : categories) {
            if (userInput.contains(category) &&
                    (userInput.contains("under") || userInput.contains("below"))) {
                return true;
            }
        }
        return false;
    }

    // ---------------- GEMINI CALL ----------------
    private String callGeminiForAnswer(String userInput) {
        try {
            Map<String, Object> payload = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", userInput)
                                    )
                            )
                    )
            );

            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    GEMINI_URL + apiKey,
                    entity,
                    String.class
            );

            // ✅ Return the full Gemini JSON response (not just extracted text)
            return response.getBody();

        } catch (HttpClientErrorException e) {
            // ✅ Return error as JSON
            System.err.println("❌ Gemini API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return createErrorJson("Gemini API error: " + e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            // ✅ Return error as JSON
            e.printStackTrace();
            return createErrorJson("Server error", e.getMessage());
        }
    }

    // ✅ Helper method to create error JSON
    private String createErrorJson(String error, String details) {
        try {
            Map<String, String> errorMap = Map.of(
                    "error", error,
                    "details", details != null ? details : "Unknown error"
            );
            return objectMapper.writeValueAsString(errorMap);
        } catch (Exception e) {
            return "{\"error\":\"Failed to create error response\"}";
        }
    }

    // ---------------- RESPONSE PARSER (NOT NEEDED ANYMORE) ----------------
    // We're now returning the full JSON response from Gemini
    // Your frontend already handles this format with data.candidates[0].content.parts[0].text
}
