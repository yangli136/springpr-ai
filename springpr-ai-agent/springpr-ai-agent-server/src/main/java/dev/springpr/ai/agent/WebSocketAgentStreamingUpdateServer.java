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

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.agentclientprotocol.sdk.agent.AcpAgent;
import com.agentclientprotocol.sdk.agent.AcpSyncAgent;
import com.agentclientprotocol.sdk.agent.transport.WebSocketAcpAgentTransport;
import com.agentclientprotocol.sdk.json.AcpJsonMapper;
import com.agentclientprotocol.sdk.spec.AcpSchema;

public class WebSocketAgentStreamingUpdateServer {

    private static final ConcurrentHashMap<String, AtomicInteger> REQUEST_COUNTS =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {
        var transport = new WebSocketAcpAgentTransport(8086, AcpJsonMapper.createDefault());

        AcpSyncAgent agent =
                AcpAgent.sync(transport)
                        .requestTimeout(Duration.ofSeconds(60))
                        .initializeHandler(
                                req ->
                                        new AcpSchema.InitializeResponse(
                                                1, new AcpSchema.AgentCapabilities(), List.of()))
                        .newSessionHandler(
                                req -> {
                                    String sessionId = UUID.randomUUID().toString();
                                    REQUEST_COUNTS.put(sessionId, new AtomicInteger(0));
                                    return new AcpSchema.NewSessionResponse(sessionId, null, null);
                                })
                        .promptHandler(
                                (req, context) -> {
                                    String sessionId = req.sessionId();
                                    int n =
                                            REQUEST_COUNTS
                                                    .computeIfAbsent(
                                                            sessionId, k -> new AtomicInteger())
                                                    .incrementAndGet();

                                    context.sendUpdate(
                                            sessionId,
                                            new AcpSchema.AgentMessageChunk(
                                                    "agent_message_chunk",
                                                    new AcpSchema.TextContent(
                                                            "Handled request #"
                                                                    + n
                                                                    + " for session "
                                                                    + sessionId)));

                                    return new AcpSchema.PromptResponse(
                                            AcpSchema.StopReason.END_TURN);
                                })
                        .build();

        agent.run();
    }
}
