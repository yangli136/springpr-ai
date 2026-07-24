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

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientBuilderConfigurer;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.micrometer.observation.ObservationRegistry;
import io.modelcontextprotocol.client.McpSyncClient;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient defaultChatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    ChatClient customChatClient(ChatClient.Builder builder) {
        return builder.defaultSystem("You are a helpful assistant.").build();
    }

    @Bean("chatClient")
    @Primary
    ChatClient ollamaAiChatClient(
            OllamaChatModel chatModel,
            ChatClientBuilderConfigurer configurer,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
            ObjectProvider<AdvisorObservationConvention> advisorObservationConvention,
            ObjectProvider<ToolCallingAdvisor.Builder<?>> toolCallingAdvisorBuilder) {
        return buildChatClient(
                chatModel,
                configurer,
                observationRegistry,
                chatClientObservationConvention,
                advisorObservationConvention,
                toolCallingAdvisorBuilder);
    }

    private ChatClient buildChatClient(
            ChatModel chatModel,
            ChatClientBuilderConfigurer configurer,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<ChatClientObservationConvention> chatClientObservationConvention,
            ObjectProvider<AdvisorObservationConvention> advisorObservationConvention,
            ObjectProvider<ToolCallingAdvisor.Builder<?>> toolCallingAdvisorBuilder) {
        ChatClient.Builder builder =
                ChatClient.builder(
                        chatModel,
                        observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP),
                        chatClientObservationConvention.getIfUnique(),
                        advisorObservationConvention.getIfUnique(),
                        toolCallingAdvisorBuilder.getIfAvailable());
        return configurer.configure(builder).build();
    }

    //    @Bean("chatClient")
    ChatClient chatClient(
            McpSyncClient mcpSyncClient,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor,
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore) {
        var system =
                """
                You are an AI powered assistant to help people find information about a person".
                """;

        //                """
        //                You are an AI powered assistant to help people adopt a dog from the
        // adoption agency named Pooch Palace with locations in Rio de Janeiro, Mexico City, Seoul,
        // Tokyo, Singapore, Paris, Mumbai, New Delhi, Barcelona, London, and San Francisco.
        // Information about the dogs available will be presented below. If there is no information,
        // then return a polite response suggesting we don't have any dogs available.
        //                """;

        VectorStoreDocumentRetriever documentRetriever =
                VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        .similarityThreshold(0.70)
                        .topK(3)
                        .build();

        return chatClientBuilder
                .defaultTools(
                        SyncMcpToolCallbackProvider.builder().addMcpClient(mcpSyncClient).build())
                .defaultAdvisors(
                        messageChatMemoryAdvisor,
                        RetrievalAugmentationAdvisor.builder()
                                .documentRetriever(documentRetriever)
                                .build())
                .defaultSystem(system)
                .build();
    }
}
