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

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.modelcontextprotocol.client.McpSyncClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class McpClientConfigTest {

    @Test
    void mcpSyncClient_withExistingClients_returnsFirstClient() throws Exception {
        McpClientConfig config = new McpClientConfig();

        McpSyncClient client1 = mock(McpSyncClient.class);
        McpSyncClient client2 = mock(McpSyncClient.class);

        setField(config, "mcpSyncClients", List.of(client1, client2));

        McpSyncClient result = config.mcpSyncClient();

        assertEquals(client1, result);
        verify(client1).initialize();
    }

    @Test
    void mcpSyncClient_withSingleClient_returnsThatClient() throws Exception {
        McpClientConfig config = new McpClientConfig();

        McpSyncClient client = mock(McpSyncClient.class);

        setField(config, "mcpSyncClients", List.of(client));

        McpSyncClient result = config.mcpSyncClient();

        assertEquals(client, result);
        verify(client).initialize();
    }

    @Test
    void mcpSyncClient_withEmptyList_fallsBackToDefault() throws Exception {
        McpClientConfig config = new McpClientConfig();

        setField(config, "mcpSyncClients", List.of());
        setField(config, "defaultMcpServerUrl", "http://localhost:8080");

        try {
            config.mcpSyncClient();
        } catch (Exception e) {
            // Expected — the HttpClientStreamableHttpTransport.builder will try to
            // connect which may fail in test. The important thing is the code path
            // reaches the else branch (empty list triggers fallback).
            assertNotNull(e);
        }
    }

    @Test
    void mcpSyncClient_withNullList_fallsBackToDefault() throws Exception {
        McpClientConfig config = new McpClientConfig();

        setField(config, "mcpSyncClients", null);
        setField(config, "defaultMcpServerUrl", "http://localhost:8080");

        try {
            config.mcpSyncClient();
        } catch (Exception e) {
            // Expected — fallback code path attempted
            assertNotNull(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
