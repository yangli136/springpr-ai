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
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.agentclientprotocol.sdk.client.AcpAsyncClient;
import com.agentclientprotocol.sdk.client.AcpClient;
import com.agentclientprotocol.sdk.client.transport.WebSocketAcpClientTransport;
import com.agentclientprotocol.sdk.json.AcpJsonMapper;
import com.agentclientprotocol.sdk.spec.AcpSchema;

import reactor.core.publisher.Mono;

public final class RequestWorker {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final String DEFAULT_SERVER_URI = "ws://localhost:8086/acp";

    private RequestWorker() {}

    public static void main(String[] args) {
        String prompt = args.length > 0 ? args[0] : "Stream a short hello and then finish.";
        String serverUri = env("ACP_WS_URI", DEFAULT_SERVER_URI);
        String workspace =
                env("ACP_WORKSPACE", Path.of(".").toAbsolutePath().normalize().toString());

        var sessionLabels = new ConcurrentHashMap<String, String>();
        var sessionCounters = new ConcurrentHashMap<String, AtomicInteger>();

        WebSocketAcpClientTransport transport =
                new WebSocketAcpClientTransport(
                                URI.create(serverUri), AcpJsonMapper.createDefault())
                        .connectTimeout(CONNECT_TIMEOUT);

        AcpAsyncClient client =
                AcpClient.async(transport)
                        .requestTimeout(REQUEST_TIMEOUT)
                        .sessionUpdateConsumer(
                                notification -> {
                                    String sessionId = notification.sessionId();
                                    int n =
                                            sessionCounters
                                                    .computeIfAbsent(
                                                            sessionId, key -> new AtomicInteger())
                                                    .incrementAndGet();

                                    System.out.printf(
                                            "[session=%s] update #%d: %s%n",
                                            sessionId, n, notification.update());
                                    return Mono.empty();
                                })
                        .build();

        try {
            client.initialize(
                            new AcpSchema.InitializeRequest(1, new AcpSchema.ClientCapabilities()))
                    .block(REQUEST_TIMEOUT);

            var sessionResponse =
                    client.newSession(new AcpSchema.NewSessionRequest(workspace, List.of()))
                            .block(REQUEST_TIMEOUT);

            if (sessionResponse == null) {
                throw new IllegalStateException("newSession returned null");
            }

            String sessionId = sessionResponse.sessionId();
            sessionLabels.put(sessionId, "request");

            System.out.printf("[request] session %s ready%n", sessionId);

            client.prompt(
                            new AcpSchema.PromptRequest(
                                    sessionId, List.of(new AcpSchema.TextContent(prompt))))
                    .doOnNext(
                            response ->
                                    System.out.printf("[request] final response: %s%n", response))
                    .block(REQUEST_TIMEOUT);

        } finally {
            client.closeGracefully().block(REQUEST_TIMEOUT);
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
