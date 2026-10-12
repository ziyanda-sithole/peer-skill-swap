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
public class GroqProvider implements LlmProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public GroqProvider(RestClient llmRestClient,
                        @Value("${ai.groq.api-key:}") String apiKey,
                        @Value("${ai.groq.model:openai/gpt-oss-20b}") String model,
                        @Value("${ai.max-output-tokens:1024}") int maxTokens) {
        this.restClient = llmRestClient;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String name() {
        return "groq";
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String generate(String systemPrompt, List<ChatTurn> turns) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatTurn turn : turns) {
            messages.add(Map.of("role", turn.fromAssistant() ? "assistant" : "user", "content", turn.text()));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);

        Map<String, Object> response = restClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        List<?> choices = (List<?>) response.get("choices");
        Map<?, ?> first = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) first.get("message");
        Object content = message.get("content");
        if (content == null || content.toString().isBlank()) {
            throw new IllegalStateException("Groq returned an empty reply");
        }
        return content.toString();
    }
}