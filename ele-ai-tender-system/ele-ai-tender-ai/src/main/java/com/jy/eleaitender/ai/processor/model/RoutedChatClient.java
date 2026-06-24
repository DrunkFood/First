package com.jy.eleaitender.ai.processor.model;

import org.springframework.ai.chat.client.ChatClient;

public record RoutedChatClient(ChatClient chatClient, String modelName) {
}
