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
package dev.springpr.ai.mcp.meetingscheduler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamableHttpClient {

    public static void main(String[] args) {
        HttpClientStreamableHttpTransport transport =
                HttpClientStreamableHttpTransport.builder("http://localhost:8083").build();

        var client = McpClient.sync(transport).build();

        client.initialize();

        client.ping();

        //		 List and demonstrate tools
        ListToolsResult toolsList = client.listTools();
        log.info("Available Tools: {}", toolsList);

        CallToolResult listDirectoryContents =
                client.callTool(
                        new CallToolRequest(
                                "schedule", Map.of("organizerId", 10, "organizerName", "Lucy")));
        List<Content> contents = listDirectoryContents.content();
        log.info("### ### ### ### ### ###");
        log.info("### ### ### list of meetings scheduled: {}", listDirectoryContents);
        log.info("### ### ### ### ### ###");
        log.info("contents.size():{}", contents.size());
        Content content = contents.get(0);
        String[] contentSplit = content.toString().split(",");
        Arrays.stream(contentSplit).forEach(entry -> log.info(entry));

        client.closeGracefully();
    }
}
