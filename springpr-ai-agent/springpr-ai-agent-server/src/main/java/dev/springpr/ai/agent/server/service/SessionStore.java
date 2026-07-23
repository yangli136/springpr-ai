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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class SessionStore {

    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger sessionCounter = new AtomicInteger();

    public String createSession() {
        String sessionId = "session-" + sessionCounter.incrementAndGet();
        sessions.put(sessionId, new SessionState(sessionId));
        return sessionId;
    }

    public int incrementPromptCount(String sessionId) {
        return sessions.computeIfAbsent(sessionId, SessionState::new)
                .requestCount
                .incrementAndGet();
    }

    public void closeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public boolean hasSession(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    private static final class SessionState {
        private final String sessionId;
        private final AtomicInteger requestCount = new AtomicInteger();

        private SessionState(String sessionId) {
            this.sessionId = sessionId;
        }
    }
}
