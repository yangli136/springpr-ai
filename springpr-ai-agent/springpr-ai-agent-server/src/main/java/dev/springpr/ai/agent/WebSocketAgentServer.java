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

import com.agentclientprotocol.sdk.agent.AcpAgent;
import com.agentclientprotocol.sdk.agent.AcpSyncAgent;
import com.agentclientprotocol.sdk.agent.transport.WebSocketAcpAgentTransport;
import com.agentclientprotocol.sdk.json.AcpJsonMapper;
import com.agentclientprotocol.sdk.spec.AcpSchema;

public class WebSocketAgentServer {
    public static void main(String[] args) {
        var jsonMapper = AcpJsonMapper.createDefault();

        // Listens on ws://localhost:8080/acp by default
        var transport =
                new WebSocketAcpAgentTransport(8086, jsonMapper)
                        .idleTimeout(Duration.ofMinutes(30));

        AcpSyncAgent agent =
                AcpAgent.sync(transport)
                        .requestTimeout(Duration.ofSeconds(60))
                        .initializeHandler(
                                request ->
                                        new AcpSchema.InitializeResponse(
                                                1, new AcpSchema.AgentCapabilities(), List.of()))
                        .newSessionHandler(
                                request ->
                                        new AcpSchema.NewSessionResponse(
                                                UUID.randomUUID().toString(), null, null))
                        .promptHandler(
                                (request, context) -> {
                                    System.out.println("Prompt: " + request);
                                    return new AcpSchema.PromptResponse(
                                            AcpSchema.StopReason.END_TURN);
                                })
                        .build();

        agent.run(); // starts and blocks until the transport closes
    }
}
