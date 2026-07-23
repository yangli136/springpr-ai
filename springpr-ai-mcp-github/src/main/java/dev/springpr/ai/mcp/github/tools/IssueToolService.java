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
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueBuilder;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueSearchBuilder;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import dev.springpr.ai.mcp.github.tools.web.IssueState;
import lombok.RequiredArgsConstructor;

/** Service for GitHub issue-related operations */
@Service
@RequiredArgsConstructor
public class IssueToolService extends BaseToolService {
    private static final String USER = "user";
    private static final String UPDATED_AT = "updated_at";
    private static final String STATE = "state";
    private static final String NUMBER = "number";
    private static final String ID = "id";
    private static final String TITLE = "title";
    private static final String BODY = "body";
    private static final String CREATED_AT = "created_at";
    private static final String HTML_URL = "html_url";

    private final GitHub github;

    @Tool(
            description =
                    """
                        List issues in a repository.
                        Returns issues with filtering options for state, labels, and more.
                    """)
    public String listIssues(
            @ToolParam(description = "full repository name in format 'owner/repo'")
                    String repository,
            @ToolParam(
                            description = "State of issues to return (open, closed, all)",
                            required = false)
                    IssueState state,
            @ToolParam(description = "Maximum number of issues to return", required = false)
                    Integer limit) {
        final Map<String, Object> issuesMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);
            final List<GHIssue> issues = getIssues(githubRepository, state);

            final List<Map<String, Object>> issueList = new ArrayList<>();
            int count = 0;

            for (GHIssue issue : issues) {
                if (limit != null && count >= limit) {
                    break;
                }

                Map<String, Object> issueData = getIssueDataMap(issue);
                issueList.add(issueData);
                count++;
            }

            issuesMap.put("issues", issueList);
            issuesMap.put("total_count", issues.size());

            return successMessage(issuesMap);

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
                        Get a specific issue in a repository.
                        Returns detailed information about the issue.
                    """)
    public String getIssue(
            @ToolParam(description = "Full repository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "Issue number") Integer issueNumber) {
        final Map<String, Object> issuesMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);
            final GHIssue issue = githubRepository.getIssue(issueNumber);
            final Map<String, Object> issueData = getIssueDataMap(issue);

            // Get comments
            final List<Map<String, Object>> commentsList = new ArrayList<>();
            for (GHIssueComment comment : issue.getComments()) {
                Map<String, Object> commentData = getCommentDataMap(comment);
                commentsList.add(commentData);
            }

            issueData.put("comments", commentsList);
            issueData.put("comments_count", commentsList.size());

            issuesMap.put("issue", issueData);

            return successMessage(issuesMap);

        } catch (GHFileNotFoundException e) {
            return failureMessage("Issue or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                    Create a new issue in a repository.
                    Creates an issue with the specified title, body, and optional labels.
                    """)
    public String createIssue(
            @ToolParam(description = "Full repository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "Issue title") String title,
            @ToolParam(description = "Issue body/description", required = false) String body,
            @ToolParam(description = "Comma-separated list of labels", required = false)
                    String labels) {
        final Map<String, Object> issuesMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);
            final GHIssueBuilder issueBuilder =
                    createIssueBuilder(githubRepository, title, body, labels);
            final GHIssue issue = issueBuilder.create();
            final Map<String, Object> issueData = getBasicIssueDataMap(issue);

            issuesMap.put("issue", issueData);

            return successMessage(issuesMap);

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
                    Add a comment to an issue.
                    Add a new comment on the specified issue.
                    """)
    public String addIssueComment(
            @ToolParam(description = "Full repository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "Issue number") Integer issueNumber,
            @ToolParam(description = "Comment text") String body) {
        final Map<String, Object> issuesMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);
            final GHIssue issue = githubRepository.getIssue(issueNumber);
            final GHIssueComment comment = issue.comment(body);
            final Map<String, Object> commentData = getCommentDataMap(comment);

            issuesMap.put("comment", commentData);

            return successMessage(issuesMap);

        } catch (GHFileNotFoundException e) {
            return failureMessage("Issue or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                    Search issues.
                    Searches for issues matching the query across GitHub or in a specific repository.
                    """)
    public String searchIssues(
            @ToolParam(description = "Search query") String query,
            @ToolParam(
                            description = "Repository name in format 'owner/repo' to limit search",
                            required = false)
                    String repository,
            @ToolParam(
                            description = "State of issues to search for (open, closed)",
                            required = false)
                    IssueState state,
            @ToolParam(description = "Maximum number of results to return", required = false)
                    Integer limit) {
        final Map<String, Object> issuesMap = new HashMap<>();

        try {
            int actualLimit = (limit != null && limit > 0) ? limit : 10;
            final List<Map<String, Object>> issueList = new ArrayList<>();
            final StringBuilder finalQuery = this.buildFinalQuery(query, repository, state);
            final GHIssueSearchBuilder searchBuilder =
                    github.searchIssues()
                            .q(finalQuery.toString())
                            .order(GHDirection.DESC)
                            .sort(GHIssueSearchBuilder.Sort.CREATED);

            searchBuilder
                    .list()
                    .withPageSize(actualLimit)
                    .iterator()
                    .forEachRemaining(
                            issue -> {
                                if (issueList.size() < actualLimit) {
                                    final Map<String, Object> issueData =
                                            this.getBasicIssueDataMap(issue);
                                    issueData.put(
                                            "repository", issue.getRepository().getFullName());
                                    try {
                                        issueData.put(CREATED_AT, issue.getCreatedAt().toString());
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }

                                    issueList.add(issueData);
                                }
                            });

            issuesMap.put("issues", issueList);
            issuesMap.put("query", finalQuery.toString());

            return successMessage(issuesMap);

        } catch (RuntimeException e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    private StringBuilder buildFinalQuery(String query, String repository, IssueState state) {
        // Build search query
        final StringBuilder queryBuilder = new StringBuilder(query);

        // Add repository filter if provided
        if (repository != null && !repository.isEmpty()) {
            queryBuilder.append(" repo:").append(repository);
        }

        // Add state filter if provided
        if (state != null) {
            queryBuilder.append(" is:").append(state.name().toLowerCase());
        }

        return queryBuilder;
    }

    private GHIssueBuilder createIssueBuilder(
            GHRepository githubRepository, String title, String body, String labels) {
        final GHIssueBuilder issueBuilder = githubRepository.createIssue(title);

        if (body != null && !body.isEmpty()) {
            issueBuilder.body(body);
        }

        if (labels != null && !labels.isEmpty()) {
            String[] labelArray = labels.split(",");
            for (String label : labelArray) {
                issueBuilder.label(label.trim());
            }
        }

        return issueBuilder;
    }

    private GHIssueState getIssueState(IssueState state) {
        GHIssueState issueState = GHIssueState.OPEN;
        if (state != null) {
            issueState =
                    switch (state.name().toLowerCase()) {
                        case "closed" -> GHIssueState.CLOSED;
                        case "all" -> GHIssueState.ALL;
                        default -> issueState;
                    };
        }

        return issueState;
    }

    private List<GHIssue> getIssues(GHRepository githubRepository, IssueState state)
            throws IOException {
        final GHIssueState issueState = getIssueState(state);
        List<GHIssue> issues;

        //            if (labels != null && !labels.isEmpty()) {
        //                String[] labelArray = labels.split(",");
        //                issues = repo.getIssues(issueState, labelArray);
        //            } else {
        issues = githubRepository.getIssues(issueState);
        //            }    }

        return issues;
    }

    public Map<String, Object> getBasicIssueDataMap(GHIssue issue) {
        final Map<String, Object> issueData = new HashMap<>();
        issueData.put(NUMBER, issue.getNumber());
        issueData.put(TITLE, issue.getTitle());
        issueData.put(BODY, issue.getBody());
        issueData.put(STATE, issue.getState().name().toLowerCase());
        issueData.put(HTML_URL, issue.getHtmlUrl().toString());

        return issueData;
    }

    public Map<String, Object> getIssueDataMap(GHIssue issue) throws IOException {
        final Map<String, Object> issueData = this.getBasicIssueDataMap(issue);

        final List<String> labels = new ArrayList<>();
        for (GHLabel label : issue.getLabels()) {
            labels.add(label.getName());
        }
        issueData.put("labels", labels);

        issueData.put(
                CREATED_AT, issue.getCreatedAt() != null ? issue.getCreatedAt().toString() : null);
        issueData.put(
                UPDATED_AT, issue.getUpdatedAt() != null ? issue.getUpdatedAt().toString() : null);
        issueData.put(
                "closed_at", issue.getClosedAt() != null ? issue.getClosedAt().toString() : null);

        if (issue.getAssignee() != null) {
            issueData.put("assignee", issue.getAssignee().getLogin());
        }

        return issueData;
    }

    private Map<String, Object> getCommentDataMap(GHIssueComment comment) throws IOException {
        Map<String, Object> commentData = new HashMap<>();
        commentData.put(ID, comment.getId());
        commentData.put(USER, comment.getUser().getLogin());

        commentData.put(BODY, comment.getBody());
        if (comment.getCreatedAt() != null) {
            commentData.put(CREATED_AT, comment.getCreatedAt().toString());
        }
        if (comment.getUpdatedAt() != null) {
            commentData.put(UPDATED_AT, comment.getUpdatedAt().toString());
        }

        return commentData;
    }
}
