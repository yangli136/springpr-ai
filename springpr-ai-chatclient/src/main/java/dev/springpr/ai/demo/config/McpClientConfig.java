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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;

@Configuration
public class McpClientConfig {
    @Autowired private List<McpSyncClient> mcpSyncClients;

    @Value("${spring.ai.mcp.client.streamable-http.connections.server1.url}")
    private String defaultMcpServerUrl;

    @Bean
    McpSyncClient mcpSyncClient() {
        McpSyncClient mcp;
        if (this.mcpSyncClients != null && this.mcpSyncClients.size() > 0) {
            mcp = this.mcpSyncClients.get(0);
        } else {
            mcp =
                    McpClient.sync(
                                    HttpClientStreamableHttpTransport.builder(defaultMcpServerUrl)
                                            .build())
                            .build();
        }
        mcp.initialize();
        return mcp;
    }
}
