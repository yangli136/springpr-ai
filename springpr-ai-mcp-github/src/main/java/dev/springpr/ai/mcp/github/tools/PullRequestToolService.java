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

import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestFileDetail;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import dev.springpr.ai.mcp.github.tools.web.IssueState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Service for GitHub pull request-related operations */
@Service
@RequiredArgsConstructor
@Slf4j
public class PullRequestToolService extends BaseToolService {
    private static final String BODY = "body";
    private static final String ID = "id";
    private static final String USER = "user";
    private static final String HEAD_BRANCH = "head_branch";
    private static final String BASE_BRANCH = "base_branch";
    private static final String IS_MERGED = "is_merged";
    private static final String MERGED_AT = "merged_at";
    private static final String CLOSED_AT = "closed_at";
    private static final String UPDATED_AT = "updated_at";
    private static final String CREATED_AT = "created_at";
    private static final String HTML_URL = "html_url";
    private static final String STATE = "state";
    private static final String TITLE = "title";
    private static final String NUMBER = "number";

    private final GitHub github;

    @Tool(
            description =
                    """
                    List pull requests for a repository.
                    Returns pull requests with filtering options for state.
                    """)
    public String listPullRequests(
            @ToolParam(description = "Repository name in format 'owner/repo'") String repository,
            @ToolParam(description = "State of pull requests (open, closed, all)", required = false)
                    IssueState state,
            @ToolParam(description = "Maximum number of results to return", required = false)
                    Integer limit) {
        final Map<String, Object> pullRequestMessagesMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);

            GHIssueState prState = GHIssueState.OPEN;
            if (state != null) {
                prState =
                        switch (state.name().toLowerCase()) {
                            case "closed" -> GHIssueState.CLOSED;
                            case "all" -> GHIssueState.ALL;
                            default -> prState;
                        };
            }

            final List<GHPullRequest> pullRequests = githubRepository.getPullRequests(prState);
            final List<Map<String, Object>> prList = new ArrayList<>();
            int count = 0;

            for (GHPullRequest pr : pullRequests) {
                if (limit != null && count >= limit) {
                    break;
                }

                Map<String, Object> prDataMap = getBasePrDataMap(pr);
                prList.add(prDataMap);
                count++;
            }

            pullRequestMessagesMap.put("pull_requests", prList);
            pullRequestMessagesMap.put("total_count", pullRequests.size());

            return successMessage(pullRequestMessagesMap);

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
                    Get a specific pull request.
                    Returns detailed information about the pull request.
                    """)
    public String getPullRequest(
            @ToolParam(description = "Repository name in format 'owner/repo'") String repository,
            @ToolParam(description = "Pull request number") Integer prNumber) {
        final Map<String, Object> prResultMessageMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);
            final GHPullRequest pr = githubRepository.getPullRequest(prNumber);

            final Map<String, Object> prDataMap = getBasePrDataMap(pr);
            prDataMap.put(BODY, pr.getBody());

            final List<Map<String, Object>> commentsList = getCommentList(pr);
            prDataMap.put("comments", commentsList);

            final List<Map<String, Object>> fileList = this.getFileList(pr);
            prDataMap.put("files", fileList);

            prResultMessageMap.put("pull_request", prDataMap);

            return successMessage(prResultMessageMap);

        } catch (GHFileNotFoundException e) {
            log.info("exception:{}", e);
            return failureMessage("Pull request or repository not found: " + e.getMessage());
        } catch (IOException e) {
            log.info("exception:{}", e);
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            log.info("exception:{}", e);
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                        Create a comment on a pull request.
                        Posts a new comment on the specified pull request.
                    """)
    public String createPullRequestComment(
            @ToolParam(description = "Repository name in format 'owner/repo'") String repository,
            @ToolParam(description = "Pull request number") Integer prNumber,
            @ToolParam(description = "Comment body text") String body) {
        final Map<String, Object> successMessagesMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);
            final GHPullRequest pr = githubRepository.getPullRequest(prNumber);

            final GHIssueComment comment = pr.comment(body);

            final Map<String, Object> commentData = getCommentDataMap(comment);

            successMessagesMap.put("comment", commentData);

            return successMessage(successMessagesMap);

        } catch (GHFileNotFoundException e) {
            return failureMessage("Pull request or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                        Merge a pull request.
                        Merges the pull request with the specified merge method.
                    """)
    public String mergePullRequest(
            @ToolParam(description = "Repository name in format 'owner/repo'") String repository,
            @ToolParam(description = "Pull request number") Integer prNumber,
            @ToolParam(description = "Commit message for the merge") String commitMessage,
            @ToolParam(description = "Merge method (merge, squash, rebase)", required = false)
                    String mergeMethod) {
        final Map<String, Object> successMessagesMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);
            final GHPullRequest pr = githubRepository.getPullRequest(prNumber);

            // Check if PR is already merged
            if (pr.isMerged()) {
                return failureMessage("Pull request is already merged");
            }

            // Set default merge method if not provided
            final String method =
                    (mergeMethod != null && !mergeMethod.isEmpty())
                            ? mergeMethod.toLowerCase()
                            : "merge";

            boolean success =
                    switch (method) {
                        case "squash" -> {
                            pr.merge(commitMessage, null, GHPullRequest.MergeMethod.SQUASH);
                            yield true;
                        }
                        case "rebase" -> {
                            pr.merge(commitMessage, null, GHPullRequest.MergeMethod.REBASE);
                            yield true;
                        }
                        case "merge" -> {
                            pr.merge(commitMessage, null, GHPullRequest.MergeMethod.MERGE);
                            yield true;
                        }
                        default -> false;
                    };

            if (success) {
                successMessagesMap.put("merged", true);
                successMessagesMap.put("method", method);
                successMessagesMap.put("pull_request_number", prNumber);
                successMessagesMap.put("repository", repository);

                return successMessage(successMessagesMap);
            } else {
                return failureMessage("Failed to merge pull request");
            }

        } catch (GHFileNotFoundException e) {
            return failureMessage("Pull request or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    private Map<String, Object> getBasePrDataMap(GHPullRequest pr) throws IOException {
        log.info("HTML_URL:{}", pr.getHtmlUrl());
        log.info("NUMBER:{}", pr.getNumber());
        log.info("CREATED_AT:{}", pr.getCreatedAt());
        Map<String, Object> prDataMap = new HashMap<>();
        prDataMap.put(NUMBER, pr.getNumber());
        prDataMap.put(TITLE, pr.getTitle());
        prDataMap.put(STATE, pr.getState().name().toLowerCase());
        prDataMap.put(HTML_URL, pr.getHtmlUrl().toString());
        prDataMap.put(CREATED_AT, pr.getCreatedAt().toString());
        prDataMap.put(UPDATED_AT, pr.getUpdatedAt().toString());
        prDataMap.put(CLOSED_AT, pr.getClosedAt() != null ? pr.getClosedAt().toString() : null);
        prDataMap.put(MERGED_AT, pr.getMergedAt() != null ? pr.getMergedAt().toString() : null);
        prDataMap.put(IS_MERGED, pr.isMerged());

        prDataMap.put(USER, pr.getUser().getLogin());

        prDataMap.put(BASE_BRANCH, pr.getBase().getRef());
        prDataMap.put(HEAD_BRANCH, pr.getHead().getRef());

        return prDataMap;
    }

    private Map<String, Object> getCommentDataMap(GHIssueComment comment) throws IOException {
        Map<String, Object> commentData = new HashMap<>();
        commentData.put(ID, comment.getId());
        commentData.put(USER, comment.getUser().getLogin());
        commentData.put(BODY, comment.getBody());
        commentData.put(CREATED_AT, comment.getCreatedAt().toString());
        commentData.put(UPDATED_AT, comment.getUpdatedAt().toString());

        return commentData;
    }

    private List<Map<String, Object>> getCommentList(GHPullRequest pr) throws IOException {
        // Get comments
        final List<Map<String, Object>> commentsList = new ArrayList<>();
        for (GHIssueComment comment : pr.getComments()) {
            Map<String, Object> commentData = getCommentDataMap(comment);

            commentsList.add(commentData);
        }

        return commentsList;
    }

    private List<Map<String, Object>> getFileList(GHPullRequest pr) {
        // Get files
        final List<Map<String, Object>> fileList = new ArrayList<>();
        for (GHPullRequestFileDetail file : pr.listFiles()) {
            Map<String, Object> fileData = new HashMap<>();
            fileData.put("filename", file.getFilename());
            fileData.put("status", file.getStatus());
            fileData.put("additions", file.getAdditions());
            fileData.put("deletions", file.getDeletions());
            fileData.put("changes", file.getChanges());

            fileList.add(fileData);
        }

        return fileList;
    }
}
