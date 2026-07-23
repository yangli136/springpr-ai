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
package dev.springpr.ai.mcp.github.tools;

import java.io.IOException;

import org.kohsuke.github.GitHub;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class GitHubClientConfiguration {

    /**
     * Creates a GitHub client based on the provided environment configuration
     *
     * @param env The GitHub environment configuration
     * @return A configured GitHub client
     * @throws IOException if there's an error connecting to GitHub
     */
    @Bean
    public GitHub createClient(GithubEnv env) throws IOException {
        log.info("createClient...");
        if (env.isEnterpriseApi()) {
            log.info("createClient...connectToEnterpriseWithOAuth...");
            return GitHub.connectToEnterpriseWithOAuth(env.host(), null, env.token());
        } else {
            log.info("createClient...connectUsingOAuth...");
            return GitHub.connectUsingOAuth(env.token());
        }
    }
}
