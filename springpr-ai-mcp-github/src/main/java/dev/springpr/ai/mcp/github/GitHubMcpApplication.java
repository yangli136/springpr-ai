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
package dev.springpr.ai.mcp.github;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import dev.springpr.ai.mcp.github.tools.ContentToolService;
import dev.springpr.ai.mcp.github.tools.PullRequestToolService;
import dev.springpr.ai.mcp.github.tools.RepositoryToolService;

@SpringBootApplication
@ConfigurationPropertiesScan({"dev.springpr.ai.mcp.github"})
@PropertySource("classpath:github.properties")
public class GitHubMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitHubMcpApplication.class, args);
    }

    //    @Bean
    //    public ToolCallbackProvider mcpServices(IssueService issueService,
    //                                            PullRequestService pullRequestService,
    //                                            RepositoryService repositoryService,
    //                                            BranchService branchService,
    //                                            CommitService commitService,
    //                                            ContentService contentService) {
    //        return MethodToolCallbackProvider.builder()
    //                .toolObjects(issueService, pullRequestService, repositoryService,
    // branchService, commitService, contentService)
    //                .build();
    //    }
    //
    @Bean
    ToolCallbackProvider mcpServices(
            RepositoryToolService repositoryToolService,
            ContentToolService contentService,
            PullRequestToolService pullRequestService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(repositoryToolService, contentService, pullRequestService)
                .build();
    }
}
