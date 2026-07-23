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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class FreshJvmLauncher {

    private FreshJvmLauncher() {}

    public static void main(String[] args) throws Exception {
        List<String> prompts = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                if (arg != null && !arg.isBlank()) {
                    for (String p : arg.split("\\s*,\\s*")) {
                        if (!p.isBlank()) {
                            prompts.add(p.trim());
                        }
                    }
                }
            }
        }

        if (prompts.isEmpty()) {
            prompts.add("Stream a short hello and then finish.");
            prompts.add("Stream three quick status updates, then end with a brief conclusion.");
        }

        String javaBin = System.getProperty("java.home") + "/bin/java";
        String classpath = System.getProperty("java.class.path");

        for (int i = 0; i < prompts.size(); i++) {
            String prompt = prompts.get(i);

            List<String> command =
                    List.of(
                            javaBin,
                            "-cp",
                            classpath,
                            "dev.springpr.ai.agent.RequestWorker",
                            prompt);

            System.out.printf("%n=== starting request-%d in fresh JVM ===%n", i + 1);

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();

            try (BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("Worker exited with code " + exit);
            }
        }
    }
}
