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
package dev.springpr.ai.demo.web;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import dev.springpr.ai.demo.config.DateTimeTools;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@DependsOn("chatClient")
@RequiredArgsConstructor
@ResponseBody
@Slf4j
class MeetingSchedulingController {
    Logger logger = LoggerFactory.getLogger(MeetingSchedulingController.class);

    private final ChatClient chatClient;
    private final McpSyncClient mcpSyncClient;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;

    @GetMapping("/ai/date")
    public String date(@RequestParam(defaultValue = "Tell me a joke") String message) {

        return this.chatClient
                .prompt("What day is tomorrow?")
                .tools(getToolCallback())
                .call()
                .content();
    }

    private ToolCallback getToolCallback() {
        Method method = ReflectionUtils.findMethod(DateTimeTools.class, "getCurrentDateTime");
        ToolCallback toolCallback =
                MethodToolCallback.builder()
                        .toolDefinition(
                                ToolDefinitions.builder(method)
                                        .description(
                                                "Get the current date and time in the user's"
                                                        + " timezone")
                                        .build())
                        .toolMethod(method)
                        .toolObject(new DateTimeTools())
                        .build();
        return toolCallback;
    }

    @GetMapping("/ai/simple")
    public Map<String, String> simple(
            @RequestParam(defaultValue = "Tell me a joke") String message) {
        return Map.of("completion", this.chatClient.prompt().user(message).call().content());
    }

    @Tag(
            name = "assistant",
            description = "example of question: do you have any person from Chicago!")
    @GetMapping("/{userId}/assistant")
    String inquire(@PathVariable String userId, @RequestParam String question) {
        log.info("entering /{user}/assistant, question: [{}]", question);
        @Nullable String chatResponse =
                chatClient
                        .prompt()
                        .user(question)
                        .tools(getToolCallback())
                        .advisors(
                                a ->
                                        a.advisors(
                                                        MessageChatMemoryAdvisor.builder(chatMemory)
                                                                .build(),
                                                        QuestionAnswerAdvisor.builder(vectorStore)
                                                                .build())
                                                .param(ChatMemory.CONVERSATION_ID, userId))
                        .call()
                        .content();

        log.info("chatResponse:{}", chatResponse);

        return chatResponse;
    }

    @Tag(
            name = "assistant",
            description = "example of question: do you have any person from Chicago!")
    @GetMapping("/{userId}/assistant-nomemory")
    String inquireNoMemory(@PathVariable String userId, @RequestParam String question) {
        log.info("entering /{user}/assistant-nomemory, question: [{}]", question);
        org.springframework.ai.chat.model.@Nullable ChatResponse chatResponse =
                chatClient.prompt().user(question).tools(getToolCallback()).call().chatResponse();

        log.info("chatResponse:{}", chatResponse);

        return chatResponse.getResult().getOutput().getText();
    }

    @GetMapping({"/schedule", "/schedule/{organizerId}/{organizerName}"})
    String schedule(
            @PathVariable Optional<Integer> organizerId,
            @PathVariable Optional<String> organizerName) {

        CallToolResult scheduleResult =
                this.mcpSyncClient.callTool(
                        new CallToolRequest(
                                "schedule",
                                Map.of(
                                        "organizerId",
                                        organizerId.orElse(3),
                                        "organizerName",
                                        organizerName.orElse("Bob"))));
        return scheduleResult.toString();
    }
}
