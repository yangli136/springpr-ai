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
package dev.springpr.ai.agent.server;

import java.time.Duration;
import java.util.List;

import com.agentclientprotocol.sdk.agent.AcpAgent;
import com.agentclientprotocol.sdk.agent.AcpAsyncAgent;
import com.agentclientprotocol.sdk.agent.transport.WebSocketAcpAgentTransport;
import com.agentclientprotocol.sdk.json.AcpJsonMapper;
import com.agentclientprotocol.sdk.spec.AcpSchema;

import dev.springpr.ai.agent.server.service.DefaultStreamingAgentService;
import dev.springpr.ai.agent.server.service.SessionStore;
import reactor.core.publisher.Mono;

public final class WebSocketStreamingAgentServer {

    private WebSocketStreamingAgentServer() {}

    public static void main(String[] args) {
        int port = readIntEnv("ACP_PORT", 8086);
        String path =
                System.getenv()
                        .getOrDefault("ACP_PATH", WebSocketAcpAgentTransport.DEFAULT_ACP_PATH);
        Duration idleTimeout = Duration.ofSeconds(readLongEnv("ACP_IDLE_TIMEOUT_SECONDS", 1800L));

        var transport =
                new WebSocketAcpAgentTransport(port, path, AcpJsonMapper.createDefault())
                        .idleTimeout(idleTimeout);

        var sessions = new SessionStore();
        var service = new DefaultStreamingAgentService(sessions);

        AcpAsyncAgent agent =
                AcpAgent.async(transport)
                        .requestTimeout(Duration.ofSeconds(60))
                        .initializeHandler(
                                request ->
                                        Mono.just(
                                                new AcpSchema.InitializeResponse(
                                                        1,
                                                        new AcpSchema.AgentCapabilities(),
                                                        List.of())))
                        .newSessionHandler(
                                request ->
                                        Mono.fromSupplier(
                                                () -> {
                                                    String sessionId = sessions.createSession();
                                                    return new AcpSchema.NewSessionResponse(
                                                            sessionId, null, null);
                                                }))
                        .promptHandler(service::handlePrompt)
                        .build();

        agent.start().block();
        System.out.printf("ACP streaming agent listening on ws://localhost:%d%s%n", port, path);
        agent.awaitTermination().block();
    }

    private static int readIntEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static long readLongEnv(String name, long defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
