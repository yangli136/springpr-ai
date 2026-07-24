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

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.tool.ToolCallback;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingSchedulingControllerTest {

    @Mock private ChatClient chatClient;
    @Mock private McpSyncClient mcpSyncClient;

    @InjectMocks private MeetingSchedulingController controller;

    @Test
    void inquire_withValidUserAndQuestion_returnsAiResponse() {
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.tools(ArgumentMatchers.<ToolCallback>any())).thenReturn(requestSpec);
        when(requestSpec.advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any()))
                .thenReturn(requestSpec);

        CallResponseSpec responseSpec = mock(CallResponseSpec.class);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("We have several dogs available!");

        String result = controller.inquire("john", "What dogs are available?");

        assertEquals("We have several dogs available!", result);
        verify(chatClient).prompt();
        verify(requestSpec).user("What dogs are available?");
    }

    @Test
    void inquire_withDifferentUser_passesUserToAdvisors() {
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user("Hello")).thenReturn(requestSpec);
        when(requestSpec.tools(ArgumentMatchers.<ToolCallback>any())).thenReturn(requestSpec);
        when(requestSpec.advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any()))
                .thenReturn(requestSpec);

        CallResponseSpec responseSpec = mock(CallResponseSpec.class);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Hi there!");

        String result = controller.inquire("jane", "Hello");

        assertEquals("Hi there!", result);
        verify(requestSpec).advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any());
    }

    @Test
    void inquire_whenAiReturnsNull_returnsNull() {
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user("question")).thenReturn(requestSpec);
        when(requestSpec.tools(ArgumentMatchers.<ToolCallback>any())).thenReturn(requestSpec);
        when(requestSpec.advisors(ArgumentMatchers.<Consumer<AdvisorSpec>>any()))
                .thenReturn(requestSpec);

        CallResponseSpec responseSpec = mock(CallResponseSpec.class);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(null);

        String result = controller.inquire("user1", "question");

        assertEquals(null, result);
    }

    @Test
    void schedule_withNoPathVariables_usesDefaults() {
        CallToolResult mockResult = mock(CallToolResult.class);
        when(mockResult.toString()).thenReturn("Meeting scheduled successfully");
        when(mcpSyncClient.callTool(any(CallToolRequest.class))).thenReturn(mockResult);

        String result = controller.schedule(Optional.empty(), Optional.empty());

        assertEquals("Meeting scheduled successfully", result);

        ArgumentCaptor<CallToolRequest> captor = ArgumentCaptor.forClass(CallToolRequest.class);
        verify(mcpSyncClient).callTool(captor.capture());

        CallToolRequest request = captor.getValue();
        assertEquals("schedule", request.name());
        Map<String, Object> args = request.arguments();
        assertEquals(3, args.get("organizerId"));
        assertEquals("Bob", args.get("organizerName"));
    }

    @Test
    void schedule_withOrganizerId_usesProvidedId() {
        CallToolResult mockResult = mock(CallToolResult.class);
        when(mockResult.toString()).thenReturn("Scheduled");
        when(mcpSyncClient.callTool(any(CallToolRequest.class))).thenReturn(mockResult);

        String result = controller.schedule(Optional.of(7), Optional.empty());

        assertEquals("Scheduled", result);

        ArgumentCaptor<CallToolRequest> captor = ArgumentCaptor.forClass(CallToolRequest.class);
        verify(mcpSyncClient).callTool(captor.capture());

        CallToolRequest request = captor.getValue();
        Map<String, Object> args = request.arguments();
        assertEquals(7, args.get("organizerId"));
        assertEquals("Bob", args.get("organizerName"));
    }

    @Test
    void schedule_withOrganizerName_usesProvidedName() {
        CallToolResult mockResult = mock(CallToolResult.class);
        when(mockResult.toString()).thenReturn("Scheduled");
        when(mcpSyncClient.callTool(any(CallToolRequest.class))).thenReturn(mockResult);

        String result = controller.schedule(Optional.empty(), Optional.of("Alice"));

        assertEquals("Scheduled", result);

        ArgumentCaptor<CallToolRequest> captor = ArgumentCaptor.forClass(CallToolRequest.class);
        verify(mcpSyncClient).callTool(captor.capture());

        CallToolRequest request = captor.getValue();
        Map<String, Object> args = request.arguments();
        assertEquals(3, args.get("organizerId"));
        assertEquals("Alice", args.get("organizerName"));
    }

    @Test
    void schedule_withBothPathVariables_usesBothValues() {
        CallToolResult mockResult = mock(CallToolResult.class);
        when(mockResult.toString()).thenReturn("Done");
        when(mcpSyncClient.callTool(any(CallToolRequest.class))).thenReturn(mockResult);

        String result = controller.schedule(Optional.of(10), Optional.of("Charlie"));

        assertEquals("Done", result);

        ArgumentCaptor<CallToolRequest> captor = ArgumentCaptor.forClass(CallToolRequest.class);
        verify(mcpSyncClient).callTool(captor.capture());

        CallToolRequest request = captor.getValue();
        Map<String, Object> args = request.arguments();
        assertEquals(10, args.get("organizerId"));
        assertEquals("Charlie", args.get("organizerName"));
    }
}
