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
package dev.springpr.ai.mcp.meetingscheduler.tool;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class MeetingScheduler extends BaseToolSerivce {
    Logger logger = LoggerFactory.getLogger(MeetingScheduler.class);

    @Tool(description = "schedule an appointment to meet.")
    String schedule(int organizerId, String organizerName) {
        logger.info("Scheduling meeting for " + organizerId);
        return Instant.now().plus(3, ChronoUnit.DAYS).toString()
                + "scheduled for organizerId:"
                + organizerId
                + "/organizerName:"
                + organizerName;
    }
}
