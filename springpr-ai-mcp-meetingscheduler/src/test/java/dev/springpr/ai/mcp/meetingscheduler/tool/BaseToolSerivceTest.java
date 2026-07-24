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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseToolSerivceTest {

    private BaseToolSerivce service;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        service = new BaseToolSerivce();
        mapper = new ObjectMapper();
    }

    @Test
    void errorMessage_returnsJsonWithSuccessFalseAndErrorField() throws Exception {
        String result = service.errorMessage("Something went wrong");
        assertNotNull(result);

        Map<String, Object> parsed = parseJson(result);
        assertEquals(false, parsed.get("success"));
        assertEquals("Something went wrong", parsed.get("error"));
    }

    @Test
    void errorMessage_withEmptyString_returnsJsonWithEmptyError() throws Exception {
        String result = service.errorMessage("");
        assertNotNull(result);

        Map<String, Object> parsed = parseJson(result);
        assertEquals(false, parsed.get("success"));
        assertEquals("", parsed.get("error"));
    }

    @Test
    void errorMessage_withSpecialCharacters_returnsValidJson() throws Exception {
        String result = service.errorMessage("Error: \"quotes\" and \\backslash");
        assertNotNull(result);

        Map<String, Object> parsed = parseJson(result);
        assertEquals(false, parsed.get("success"));
        assertEquals("Error: \"quotes\" and \\backslash", parsed.get("error"));
    }

    @Test
    void errorMessage_withNullMessage_returnsJsonWithNullError() throws Exception {
        String result = service.errorMessage(null);
        assertNotNull(result);

        Map<String, Object> parsed = parseJson(result);
        assertEquals(false, parsed.get("success"));
    }

    @Test
    void successMessage_returnsJsonWithSuccessTrue() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("data", "test-value");

        String result = service.successMessage(input);
        assertNotNull(result);

        Map<String, Object> parsed = parseJson(result);
        assertEquals(true, parsed.get("success"));
        assertEquals("test-value", parsed.get("data"));
    }

    @Test
    void successMessage_withEmptyMap_returnsJsonWithOnlySuccessTrue() throws Exception {
        Map<String, Object> input = new HashMap<>();

        String result = service.successMessage(input);
        assertNotNull(result);

        Map<String, Object> parsed = parseJson(result);
        assertEquals(true, parsed.get("success"));
        assertEquals(1, parsed.size());
    }

    @Test
    void successMessage_withMultipleEntries_returnsAllFields() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("key1", "value1");
        input.put("key2", 42);
        input.put("key3", true);

        String result = service.successMessage(input);
        assertNotNull(result);

        Map<String, Object> parsed = parseJson(result);
        assertEquals(true, parsed.get("success"));
        assertEquals("value1", parsed.get("key1"));
        assertEquals(42, parsed.get("key2"));
        assertEquals(true, parsed.get("key3"));
    }

    @Test
    void successMessage_mutatesInputMapByAddingSuccessKey() {
        Map<String, Object> input = new HashMap<>();
        input.put("data", "value");

        service.successMessage(input);

        assertTrue(input.containsKey("success"));
        assertEquals(true, input.get("success"));
    }

    @Test
    void errorMessage_resultContainsSuccessFalse() {
        String result = service.errorMessage("test error");
        assertTrue(result.contains("\"success\""));
        assertFalse(result.contains("\"success\":true") || result.contains("\"success\" : true"));
    }

    @Test
    void successMessage_resultContainsSuccessTrue() {
        Map<String, Object> input = new HashMap<>();
        String result = service.successMessage(input);
        assertTrue(result.contains("\"success\""));
        assertTrue(result.contains("true"));
    }

    @Test
    void constants_haveExpectedValues() {
        assertEquals("success", BaseToolSerivce.SUCCESS);
        assertEquals("error", BaseToolSerivce.ERROR);
    }

    @Test
    void mapper_isNotNull() {
        assertNotNull(service.mapper);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) throws Exception {
        return mapper.readValue(json, Map.class);
    }
}
