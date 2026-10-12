package com.wandile.skillswap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiProvider implements LlmProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public GeminiProvider(RestClient llmRestClient,
                          @Value("${ai.gemini.api-key:}") String apiKey,
                          @Value("${ai.gemini.model:gemini-3.1-flash-lite}") String model,
                          @Value("${ai.max-output-tokens:1024}") int maxTokens) {
        this.restClient = llmRestClient;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generate(String systemPrompt, List<ChatTurn> turns) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (ChatTurn turn : turns) {
            contents.add(Map.of(
                    "role", turn.fromAssistant() ? "model" : "user",
                    "parts", List.of(Map.of("text", turn.text()))));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        body.put("contents", contents);
        body.put("generationConfig", Map.of("maxOutputTokens", maxTokens));

        Map<String, Object> response = restClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        List<?> candidates = (List<?>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini returned no candidates");
        }
        Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
        List<?> parts = content == null ? null : (List<?>) content.get("parts");
        if (parts == null) {
            throw new IllegalStateException("Gemini returned an empty reply");
        }
        StringBuilder text = new StringBuilder();
        for (Object part : parts) {
            Object piece = ((Map<?, ?>) part).get("text");
            if (piece != null) text.append(piece);
        }
        if (text.toString().isBlank()) {
            throw new IllegalStateException("Gemini returned an empty reply");
        }
        return text.toString();
    }
}