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
package dev.springpr.ai.demo.config;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptAdvisorConfigTest {

    @Mock private DataSource dataSource;

    @Test
    void promptChatMemoryAdvisor_createsAdvisorWithDataSource() {
        JdbcChatMemoryRepository.Builder jdbcBuilder = mock(JdbcChatMemoryRepository.Builder.class);
        JdbcChatMemoryRepository jdbcRepo = mock(JdbcChatMemoryRepository.class);
        when(jdbcBuilder.dataSource(any(DataSource.class))).thenReturn(jdbcBuilder);
        when(jdbcBuilder.build()).thenReturn(jdbcRepo);

        MessageWindowChatMemory.Builder memoryBuilder = mock(MessageWindowChatMemory.Builder.class);
        MessageWindowChatMemory chatMemory = mock(MessageWindowChatMemory.class);
        when(memoryBuilder.chatMemoryRepository(any())).thenReturn(memoryBuilder);
        when(memoryBuilder.build()).thenReturn(chatMemory);

        MessageChatMemoryAdvisor.Builder advisorBuilder =
                mock(MessageChatMemoryAdvisor.Builder.class);
        MessageChatMemoryAdvisor advisor = mock(MessageChatMemoryAdvisor.class);
        when(advisorBuilder.build()).thenReturn(advisor);

        try (MockedStatic<JdbcChatMemoryRepository> jdbcStatic =
                        mockStatic(JdbcChatMemoryRepository.class);
                MockedStatic<MessageWindowChatMemory> memoryStatic =
                        mockStatic(MessageWindowChatMemory.class);
                MockedStatic<MessageChatMemoryAdvisor> advisorStatic =
                        mockStatic(MessageChatMemoryAdvisor.class)) {

            jdbcStatic.when(JdbcChatMemoryRepository::builder).thenReturn(jdbcBuilder);
            memoryStatic.when(MessageWindowChatMemory::builder).thenReturn(memoryBuilder);
            advisorStatic
                    .when(() -> MessageChatMemoryAdvisor.builder(any()))
                    .thenReturn(advisorBuilder);

            PromptAdvisorConfig config = new PromptAdvisorConfig();
            MessageChatMemoryAdvisor result = config.messageChatMemoryAdvisor(dataSource);

            assertNotNull(result);
        }
    }
}
