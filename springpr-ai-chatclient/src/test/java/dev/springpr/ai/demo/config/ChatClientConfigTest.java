/*
 * Copyright 2025 American Express Travel Related Services Company, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package dev.springpr.ai.demo.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class ChatClientConfigTest {

    @Mock private McpSyncClient mcpSyncClient;
    @Mock private MessageChatMemoryAdvisor promptChatMemoryAdvisor;
    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private VectorStore vectorStore;
    @Mock private ChatClient chatClient;
    @Mock private VectorStoreDocumentRetriever documentRetriever;

    @Test
    void chatClient_createsClientWithDefaultSystem() {
        when(chatClientBuilder.defaultTools(ArgumentMatchers.any(Object[].class)))
                .thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(any(Advisor.class), any(Advisor.class)))
                .thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultSystem(any(String.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        ChatClientConfig config = new ChatClientConfig();
        ChatClient result =
                config.chatClient(
                        mcpSyncClient, promptChatMemoryAdvisor, chatClientBuilder, vectorStore);

        assertNotNull(result);
        verify(chatClientBuilder).defaultTools(ArgumentMatchers.any(Object[].class));
        verify(chatClientBuilder).defaultAdvisors(any(Advisor.class), any(Advisor.class));
        verify(chatClientBuilder).defaultSystem(any(String.class));
        verify(chatClientBuilder).build();
    }

    @Test
    void chatClient_setsSystemPromptContainingPoochPalace() {
        when(chatClientBuilder.defaultTools(ArgumentMatchers.any(Object[].class)))
                .thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultAdvisors(any(Advisor.class), any(Advisor.class)))
                .thenReturn(chatClientBuilder);
        when(chatClientBuilder.defaultSystem(any(String.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        ChatClientConfig config = new ChatClientConfig();
        config.chatClient(mcpSyncClient, promptChatMemoryAdvisor, chatClientBuilder, vectorStore);

        org.mockito.ArgumentCaptor<String> systemCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);
        verify(chatClientBuilder).defaultSystem(systemCaptor.capture());
        String systemPrompt = systemCaptor.getValue();
        assertNotNull(systemPrompt);
        log.info("systemPrompt:{}", systemPrompt);
        org.junit.jupiter.api.Assertions.assertTrue(
                systemPrompt.contains("You are an AI powered assistant"));
    }
}
