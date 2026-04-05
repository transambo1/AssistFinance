package com.financeai.finance_management.service.impl;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiChatMemoryService {

    private static final int MAX_HISTORY = 10;

    private final Map<String, List<String>> conversationStore = new ConcurrentHashMap<>();

    public List<String> getHistory(String userId) {
        return conversationStore.getOrDefault(userId, new ArrayList<>());
    }

    public void addUserMessage(String userId, String message) {
        addLine(userId, "User: " + message);
    }

    public void addBotMessage(String userId, String message) {
        addLine(userId, "Bot: " + message);
    }

    public void clearHistory(String userId) {
        conversationStore.remove(userId);
    }

    private void addLine(String userId, String line) {
        conversationStore.compute(userId, (key, history) -> {
            if (history == null) {
                history = new ArrayList<>();
            }

            history.add(line);

            if (history.size() > MAX_HISTORY) {
                history = new ArrayList<>(history.subList(history.size() - MAX_HISTORY, history.size()));
            }

            return history;
        });
    }
}