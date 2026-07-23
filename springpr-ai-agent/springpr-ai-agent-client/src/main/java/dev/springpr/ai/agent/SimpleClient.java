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
import java.util.List;

import com.agentclientprotocol.sdk.client.AcpClient;
import com.agentclientprotocol.sdk.client.AcpSyncClient;
import com.agentclientprotocol.sdk.client.transport.WebSocketAcpClientTransport;
import com.agentclientprotocol.sdk.json.AcpJsonMapper;
import com.agentclientprotocol.sdk.spec.AcpSchema.NewSessionRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.PromptRequest;
import com.agentclientprotocol.sdk.spec.AcpSchema.TextContent;

public class SimpleClient {
    public static void main(String[] args) {
        var transport =
                new WebSocketAcpClientTransport(
                        URI.create("ws://localhost:8086/acp"), AcpJsonMapper.createDefault());

        AcpSyncClient client = AcpClient.sync(transport).build();

        client.initialize();

        var session = client.newSession(new NewSessionRequest("/workspace", List.of()));

        var response =
                client.prompt(
                        new PromptRequest(
                                session.sessionId(),
                                List.of(new TextContent("Say hello in one sentence."))));

        System.out.println(response);
        client.close();
    }
}
