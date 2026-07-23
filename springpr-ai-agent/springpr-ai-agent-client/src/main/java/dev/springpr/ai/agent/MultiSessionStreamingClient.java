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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.agentclientprotocol.sdk.client.AcpAsyncClient;
import com.agentclientprotocol.sdk.client.AcpClient;
import com.agentclientprotocol.sdk.client.transport.WebSocketAcpClientTransport;
import com.agentclientprotocol.sdk.json.AcpJsonMapper;
import com.agentclientprotocol.sdk.spec.AcpSchema;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class MultiSessionStreamingClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final String DEFAULT_SERVER_URI = "ws://localhost:8086/acp";

    private MultiSessionStreamingClient() {}

    public static void main(String[] args) {
        String serverUri = env("ACP_WS_URI", DEFAULT_SERVER_URI);
        String workspace =
                env("ACP_WORKSPACE", Path.of(".").toAbsolutePath().normalize().toString());
        List<String> prompts = promptsFromArgs(args);

        var sessionLabels = new ConcurrentHashMap<String, String>();
        var sessionCounters = new ConcurrentHashMap<String, AtomicInteger>();

        try {
            Flux.fromIterable(indexed(prompts))
                    .flatMap(
                            item ->
                                    runConversation(
                                            serverUri,
                                            workspace,
                                            item.index(),
                                            item.prompt(),
                                            sessionLabels,
                                            sessionCounters),
                            prompts.size())
                    .blockLast(REQUEST_TIMEOUT);
        } finally {
            System.out.println("Client finished.");
        }
    }

    private static Mono<Void> runConversation(
            String serverUri,
            String workspace,
            int index,
            String prompt,
            Map<String, String> sessionLabels,
            Map<String, AtomicInteger> sessionCounters) {

        String label = "request-" + index;
        System.out.printf("[%s] opening client and session%n", label);

        return Mono.usingWhen(
                Mono.fromSupplier(() -> createClient(serverUri, sessionLabels, sessionCounters)),
                client ->
                        client.initialize(
                                        new AcpSchema.InitializeRequest(
                                                1, new AcpSchema.ClientCapabilities()))
                                .then(
                                        client.newSession(
                                                new AcpSchema.NewSessionRequest(
                                                        workspace, List.of())))
                                .flatMap(
                                        sessionResponse -> {
                                            String sessionId = sessionResponse.sessionId();
                                            sessionLabels.put(sessionId, label);

                                            System.out.printf(
                                                    "[%s] session %s ready%n", label, sessionId);

                                            return client.prompt(
                                                            new AcpSchema.PromptRequest(
                                                                    sessionId,
                                                                    List.of(
                                                                            new AcpSchema
                                                                                    .TextContent(
                                                                                    prompt))))
                                                    .doOnNext(
                                                            response ->
                                                                    System.out.printf(
                                                                            "[%s] final response:"
                                                                                    + " %s%n",
                                                                            label, response))
                                                    .then();
                                        }),
                client -> client.closeGracefully().then());
    }

    private static AcpAsyncClient createClient(
            String serverUri,
            Map<String, String> sessionLabels,
            Map<String, AtomicInteger> sessionCounters) {

        WebSocketAcpClientTransport transport =
                new WebSocketAcpClientTransport(
                                URI.create(serverUri), AcpJsonMapper.createDefault())
                        .connectTimeout(CONNECT_TIMEOUT);

        return AcpClient.async(transport)
                .requestTimeout(REQUEST_TIMEOUT)
                .sessionUpdateConsumer(
                        notification -> {
                            String sessionId = notification.sessionId();
                            String label = sessionLabels.getOrDefault(sessionId, sessionId);
                            int n =
                                    sessionCounters
                                            .computeIfAbsent(sessionId, key -> new AtomicInteger())
                                            .incrementAndGet();

                            System.out.printf(
                                    "[%s] update #%d: %s%n", label, n, notification.update());

                            return Mono.empty();
                        })
                .build();
    }

    private static List<String> promptsFromArgs(String[] args) {
        List<String> prompts = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                if (arg != null && !arg.isBlank()) {
                    prompts.addAll(splitPrompts(arg));
                }
            }
        }

        if (prompts.isEmpty()) {
            prompts.add("Stream a short hello and then summarize the result in one sentence.");
            prompts.add("Stream three quick status updates, then end with a brief conclusion.");
        }

        return prompts;
    }

    private static List<String> splitPrompts(String arg) {
        String[] parts = arg.split("\\s*,\\s*");
        List<String> prompts = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                prompts.add(part.trim());
            }
        }
        return prompts;
    }

    private static List<PromptItem> indexed(List<String> prompts) {
        List<PromptItem> items = new ArrayList<>(prompts.size());
        for (int i = 0; i < prompts.size(); i++) {
            items.add(new PromptItem(i + 1, prompts.get(i)));
        }
        return items;
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private record PromptItem(int index, String prompt) {}
}
