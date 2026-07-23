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
package dev.springpr.ai.agent.server.service;

import java.time.Duration;

import com.agentclientprotocol.sdk.agent.PromptContext;
import com.agentclientprotocol.sdk.spec.AcpSchema;

import reactor.core.publisher.Mono;

public final class DefaultStreamingAgentService {

    private final SessionStore sessionStore;

    public DefaultStreamingAgentService(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public Mono<AcpSchema.PromptResponse> handlePrompt(
            AcpSchema.PromptRequest request, PromptContext context) {
        String sessionId = context.getSessionId();
        int requestNumber = sessionStore.incrementPromptCount(sessionId);
        String messageId = sessionId + "-request-" + requestNumber;

        return context.sendThought(
                        "Starting request #" + requestNumber + " for " + sessionId, messageId)
                .then(Mono.delay(Duration.ofMillis(100)))
                .then(
                        context.sendMessage(
                                "Request #" + requestNumber + ": step 1 complete.", messageId))
                .then(Mono.delay(Duration.ofMillis(100)))
                .then(
                        context.sendMessage(
                                "Request #" + requestNumber + ": step 2 complete.", messageId))
                .then(Mono.delay(Duration.ofMillis(100)))
                .then(context.sendMessage("Request #" + requestNumber + ": finished.", messageId))
                .thenReturn(new AcpSchema.PromptResponse(AcpSchema.StopReason.END_TURN));
    }
}
