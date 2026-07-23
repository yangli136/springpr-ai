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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositorySearchBuilder;
import org.kohsuke.github.GitHub;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepositoryToolService extends BaseToolService {
    private static final String IS_PRIVATE = "isPrivate";
    private static final String LANGUAGE = "language";
    private static final String NUMBER_OF_FORKS = "numberOfForks";
    private static final String STARS = "stars";
    private static final String HTML_URL = "htmlUrl";
    private static final String DESCRIPTION = "description";
    private static final String FULL_NAME = "fullName";
    private static final String NAME = "name";

    private final GitHub github;

    @Tool(
            description =
                    """
                        Get information about a specific repository.
                        Returns details about the repository such as description, stars, forks, etc.
                    """)
    public String getRepositoryInfo(
            @ToolParam(description = "Repository name in format 'owner/repo'", required = false)
                    String repository) {
        Map<String, Object> repositoryInfo = new HashMap<>();
        try {
            GHRepository githubRepository = github.getRepository(repository);
            final Map<String, Object> repositoryDetails =
                    buildCommonRepositoryDetails(githubRepository);

            repositoryDetails.put("openIssues", githubRepository.getOpenIssueCount());
            repositoryDetails.put("watchers", githubRepository.getWatchersCount());
            repositoryDetails.put(
                    "license",
                    githubRepository.getLicense() != null
                            ? githubRepository.getLicense().getName()
                            : null);
            repositoryDetails.put("defaultBranch", githubRepository.getDefaultBranch());
            repositoryDetails.put("createdAt", githubRepository.getCreatedAt().toString());
            repositoryDetails.put("updatedAt", githubRepository.getUpdatedAt().toString());

            repositoryInfo.put("repository", repositoryDetails);

            return successMessage(repositoryInfo);
        } catch (GHFileNotFoundException e) {
            return failureMessage("Repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                        List repositories for the current user.
                        Returns a list of repositories the current user has access to.
                    """)
    public String listRepositories(
            @ToolParam(
                            description = "Maximum number of repositories will be returned",
                            required = false)
                    Integer limit) {
        Map<String, Object> repositoriesFound = new HashMap<>();

        try {
            final GHMyself ghMyself = github.getMyself();
            final Map<String, GHRepository> repositories = ghMyself.getAllRepositories();

            final List<Map<String, Object>> repositoryList = new ArrayList<>();
            int count = 0;

            for (GHRepository githubRepository : repositories.values()) {
                if (limit != null && count >= limit) {
                    break;
                }

                final Map<String, Object> repositoryDetails =
                        buildCommonRepositoryDetails(githubRepository);

                repositoryList.add(repositoryDetails);
                count++;
            }

            repositoriesFound.put("repositories", repositoryList);
            repositoriesFound.put("totalCountofRepositories", repositories.size());

            return successMessage(repositoriesFound);

        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                        Search repositories with query term.
                        Returns resources matching the query term.
                    """)
    public String searchRepositoriesByTerm(
            @ToolParam(
                            description =
                                    "Search repository by query terms (e.g. springpr in:name"
                                            + " created:>2022-10-01")
                    String queryTerm,
            @ToolParam(description = "Maximum number of results to return", required = false)
                    Integer limit) {
        final Map<String, Object> result = new HashMap<>();

        try {
            int actualLimit = (limit != null && limit > 0) ? limit : 10;

            final GHRepositorySearchBuilder searchBuilder =
                    github.searchRepositories()
                            .q(queryTerm)
                            .order(GHDirection.DESC)
                            .sort(GHRepositorySearchBuilder.Sort.STARS);

            final List<Map<String, Object>> repoList = new ArrayList<>();

            searchBuilder
                    .list()
                    .withPageSize(actualLimit)
                    .iterator()
                    .forEachRemaining(
                            githubRepository -> {
                                if (repoList.size() < actualLimit) {
                                    final Map<String, Object> repositoryDetails;

                                    try {
                                        repositoryDetails =
                                                buildCommonRepositoryDetails(githubRepository);

                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }

                                    repoList.add(repositoryDetails);
                                }
                            });

            result.put("repositories", repoList);
            result.put("query", queryTerm);

            return successMessage(result);
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    private Map<String, Object> buildCommonRepositoryDetails(GHRepository githubRepository)
            throws IOException {
        Map<String, Object> repositoryDetails = new HashMap<>();
        repositoryDetails.put(NAME, githubRepository.getName());
        repositoryDetails.put(FULL_NAME, githubRepository.getFullName());
        repositoryDetails.put(DESCRIPTION, githubRepository.getDescription());
        repositoryDetails.put(HTML_URL, githubRepository.getHtmlUrl().toString());
        repositoryDetails.put(STARS, githubRepository.getStargazersCount());
        repositoryDetails.put(NUMBER_OF_FORKS, githubRepository.listForks().toList().size());
        repositoryDetails.put(LANGUAGE, githubRepository.getLanguage());
        repositoryDetails.put(IS_PRIVATE, githubRepository.isPrivate());
        return repositoryDetails;
    }
}
