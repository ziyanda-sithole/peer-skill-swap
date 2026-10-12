package com.wandile.skillswap.service;

import java.util.List;

public interface LlmProvider {

    record ChatTurn(boolean fromAssistant, String text) {}

    String name();

    boolean isConfigured();

    String generate(String systemPrompt, List<ChatTurn> turns);
}