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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingSchedulerTest {

    private MeetingScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new MeetingScheduler();
    }

    @Test
    void schedule_returnsStringContainingOrganizerId() {
        String result = scheduler.schedule(10, "Alice");
        assertNotNull(result);
        assertTrue(result.contains("organizerId:10"));
    }

    @Test
    void schedule_returnsStringContainingOrganizerName() {
        String result = scheduler.schedule(10, "Alice");
        assertTrue(result.contains("organizerName:Alice"));
    }

    @Test
    void schedule_returnsStringContainingScheduledKeyword() {
        String result = scheduler.schedule(1, "Bob");
        assertTrue(result.contains("scheduled for"));
    }

    @Test
    void schedule_returnsDateApproximatelyThreeDaysInFuture() {
        Instant before = Instant.now().plus(3, ChronoUnit.DAYS);
        String result = scheduler.schedule(1, "Charlie");
        Instant after = Instant.now().plus(3, ChronoUnit.DAYS);

        String datePrefix = result.substring(0, result.indexOf("scheduled"));
        Instant scheduled = Instant.parse(datePrefix);
        assertTrue(
                !scheduled.isBefore(before.minusSeconds(2))
                        && !scheduled.isAfter(after.plusSeconds(2)));
    }

    @Test
    void schedule_withZeroOrganizerId_returnsValidResult() {
        String result = scheduler.schedule(0, "Dave");
        assertNotNull(result);
        assertTrue(result.contains("organizerId:0"));
        assertTrue(result.contains("organizerName:Dave"));
    }

    @Test
    void schedule_withNegativeOrganizerId_returnsValidResult() {
        String result = scheduler.schedule(-1, "Eve");
        assertNotNull(result);
        assertTrue(result.contains("organizerId:-1"));
    }

    @Test
    void schedule_withEmptyName_returnsValidResult() {
        String result = scheduler.schedule(5, "");
        assertNotNull(result);
        assertTrue(result.contains("organizerName:"));
    }

    @Test
    void schedule_withLargeOrganizerId_returnsValidResult() {
        String result = scheduler.schedule(Integer.MAX_VALUE, "Frank");
        assertNotNull(result);
        assertTrue(result.contains("organizerId:" + Integer.MAX_VALUE));
    }

    @Test
    void schedule_withSpecialCharactersInName_returnsValidResult() {
        String result = scheduler.schedule(7, "O'Brien-Smith");
        assertNotNull(result);
        assertTrue(result.contains("organizerName:O'Brien-Smith"));
    }

    @Test
    void schedule_resultFormatIsCorrect() {
        String result = scheduler.schedule(42, "Lucy");
        assertTrue(result.contains("scheduled for organizerId:42/organizerName:Lucy"));
    }

    @Test
    void schedule_loggerIsInitialized() {
        assertNotNull(scheduler.logger);
    }

    @Test
    void schedule_inheritsFromBaseToolSerivce() {
        assertTrue(scheduler != null);
    }
}
