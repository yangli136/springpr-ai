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
package dev.springpr.ai.agent;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.agentclientprotocol.sdk.client.AcpClient;
import com.agentclientprotocol.sdk.client.AcpSyncClient;
import com.agentclientprotocol.sdk.client.transport.WebSocketAcpClientTransport;
import com.agentclientprotocol.sdk.json.AcpJsonMapper;
import com.agentclientprotocol.sdk.spec.AcpSchema;

public final class StreamingUpdatesClient {

    private StreamingUpdatesClient() {}

    public static void main(String[] args) {
        String wsUri = System.getenv().getOrDefault("ACP_WS_URI", "ws://localhost:8086/acp");
        String workspace =
                System.getenv().getOrDefault("ACP_WORKSPACE", System.getProperty("user.dir"));

        var transport =
                new WebSocketAcpClientTransport(URI.create(wsUri), AcpJsonMapper.createDefault())
                        .connectTimeout(Duration.ofSeconds(30));

        AtomicInteger updateCount = new AtomicInteger(0);

        AcpSyncClient client =
                AcpClient.sync(transport)
                        .requestTimeout(Duration.ofSeconds(90))
                        .sessionUpdateConsumer(
                                notification -> {
                                    int n = updateCount.incrementAndGet();
                                    Object update = notification.update();
                                    String updateType =
                                            update == null
                                                    ? "null"
                                                    : update.getClass().getSimpleName();

                                    System.out.printf(
                                            "[update %02d] session=%s type=%s payload=%s%n",
                                            n, notification.sessionId(), updateType, update);
                                })
                        .build();

        try {
            System.out.println("Connecting to " + wsUri);
            client.initialize();

            var session = client.newSession(new AcpSchema.NewSessionRequest(workspace, List.of()));
            System.out.println("Opened session: " + session.sessionId());

            sendPrompt(
                    client, session.sessionId(), "Give me a quick progress update in 3 bullets.");
            sendPrompt(
                    client, session.sessionId(), "Now summarize the last answer in one sentence.");

            System.out.println("Done.");
        } finally {
            client.closeGracefully();
        }
    }

    private static void sendPrompt(AcpSyncClient client, String sessionId, String text) {
        System.out.println();
        System.out.println(">>> Prompt: " + text);

        var response =
                client.prompt(
                        new AcpSchema.PromptRequest(
                                sessionId, List.of(new AcpSchema.TextContent(text))));

        System.out.println("<<< Final response: " + response);
    }
}
