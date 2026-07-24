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
package dev.springpr.ai.demo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.ollama.OllamaContainer;

public class OllamaModelTest {

    @Disabled("Disabled until bug #42 is fixed") // test takes few seconds to complete. It ran
    // successfully though.
    @Test
    void testOllamaResponse() throws Exception {
        // 1. Start the Ollama container
        try (OllamaContainer ollama = new OllamaContainer("ollama/ollama:latest")) {
            ollama.start();

            // 2. Pull the required model inside the container
            // This is required unless you use a custom image with the model pre-baked
            ollama.execInContainer("ollama", "pull", "all-minilm");

            // 3. Get the connection details
            String address = ollama.getHost();
            Integer port = ollama.getFirstMappedPort();
            String endpoint = String.format("http://%s:%d", address, port);

            System.out.println("DockerImageName:" + ollama.getDockerImageName());
            System.out.println("ContainerId:" + ollama.getContainerId());

            System.out.println("ContainerInfo:" + ollama.getContainerInfo());
        }
    }
}
