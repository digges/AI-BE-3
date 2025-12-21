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

//    @Autowired
//    private ProductRepository productRepository;

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {

        String userInput = message.getPayload().trim().toLowerCase();
        System.out.println("🔹 User Input: " + userInput);

        String response;



            response = callGeminiForAnswer(userInput);


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

    // ---------------- ECOMMERCE HANDLER ----------------


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

            return extractGeminiText(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return "Gemini API error.";
        }
    }

    // ---------------- RESPONSE PARSER ----------------
    private String extractGeminiText(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        return root.path("candidates")
                .get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText("No response from Gemini");
    }
}
