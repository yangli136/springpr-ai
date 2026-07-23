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

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCommitQueryBuilder;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** Service for GitHub commit-related operations */
@Service
@RequiredArgsConstructor
public class CommitToolService extends BaseToolService {
    private static final String BRANCH = "branch";
    private static final String COUNT = "count";
    private static final String COMMITS = "commits";
    private static final String STATS = "stats";
    private static final String TOTAL = "total";
    private static final String AUTHOR = "author";
    private static final String DELETIONS = "deletions";
    private static final String ADDITIONS = "additions";
    private static final String DATE = "date";
    private static final String EMAIL = "email";
    private static final String NAME = "name";
    private static final String HTML_URL = "html_url";
    private static final String MESSAGE = "message";
    private static final String SHA = "sha";

    private final GitHub github;

    @Tool(
            description =
                    """
                        Get details about a specific commit.
                        Returns commit data including author, committer, message, and file changes.
                    """)
    public String getCommitDetails(
            @ToolParam(description = "Full repository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "Commit SHA") String sha) {
        Map<String, Object> commitDetailsMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);
            final GHCommit commit = githubRepository.getCommit(sha);
            final Map<String, Object> commitData = new HashMap<>();

            addCommitInfo(commit, commitData);
            addAutherDetailsAndCommitterDetails(commit, commitData);
            addParentDetails(commit, commitData);
            addChangeDetails(commit, commitData);
            addChangeStats(commit, commitData);

            commitDetailsMap.put("commit", commitData);

            return successMessage(commitDetailsMap);

        } catch (GHFileNotFoundException e) {
            return failureMessage("Commit or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                    Search for a commit in the project history based on the provided text or keywords.
                    Useful for finding specific change sets or code modifications by commit messages or diff content.
                    Takes a query parameter and returns the matching commit information.
                    Returns matched commit hashes as a JSON array.
                    """)
    public String findCommitByKeyWordInMessage(
            @ToolParam(description = "Text to search for in commit messages") String text,
            @ToolParam(description = "Full repository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "Branch or tag name to search within", required = false)
                    String branch,
            @ToolParam(description = "Maximum number of results to return", required = false)
                    Integer limit) {
        final Map<String, Object> commitsMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);

            final GHCommitQueryBuilder queryBuilder = githubRepository.queryCommits();

            if (branch != null && !branch.isEmpty()) {
                queryBuilder.from(branch);
            }

            // case-insensitive
            String searchText = text.toLowerCase();
            // Default to 20 results
            int actualLimit = (limit != null && limit > 0) ? limit : 10;

            final List<Map<String, Object>> matchedCommits = new ArrayList<>();
            int count = 0;

            for (GHCommit commit : queryBuilder.list()) {
                if (count >= actualLimit) {
                    break;
                }

                GHCommit.ShortInfo info = commit.getCommitShortInfo();
                String message = info.getMessage();

                // Check if the commit message contains the search text
                if (message != null && message.toLowerCase().contains(searchText)) {
                    Map<String, Object> commitData = new HashMap<>();
                    addCommitInfo(commit, commitData);
                    addAutherDetailsAndCommitterDetails(commit, commitData);
                    addChangeStats(commit, commitData);

                    matchedCommits.add(commitData);
                    count++;
                }
            }

            commitsMap.put(COMMITS, matchedCommits);
            commitsMap.put(COUNT, matchedCommits.size());
            commitsMap.put("search_text", text);

            if (branch != null && !branch.isEmpty()) {
                commitsMap.put(BRANCH, branch);
            }

            return successMessage(commitsMap);

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
                    List commits in a repository.
                    Returns a list of commits and optionally filtered by branch and author.
                    """)
    public String listCommits(
            @ToolParam(description = "Full repository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "Branch or tag name made the commits", required = false)
                    String branch,
            @ToolParam(description = "Author name or email made the commits", required = false)
                    String author,
            @ToolParam(description = "commits made in specific path", required = false) String path,
            @ToolParam(description = "Maximum number of commits to return", required = false)
                    Integer limit) {
        final Map<String, Object> commitsMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);

            int actualLimit = (limit != null && limit > 0) ? limit : 30; // Default to 30 commits

            final List<Map<String, Object>> commitList = new ArrayList<>();
            int count = 0;

            final GHCommitQueryBuilder queryBuilder =
                    queryBuilder(githubRepository, branch, author, path);

            for (GHCommit commit : queryBuilder.list().withPageSize(actualLimit)) {
                if (count >= actualLimit) {
                    break;
                }

                final Map<String, Object> commitData = new HashMap<>();
                addCommitInfo(commit, commitData);
                addCommitterDetails(commit, commitData);
                addChangeStats(commit, commitData);

                commitList.add(commitData);
                count++;
            }

            commitsMap.put(COMMITS, commitList);
            commitsMap.put(COUNT, commitList.size());

            if (branch != null && !branch.isEmpty()) {
                commitsMap.put(BRANCH, branch);
            }

            if (author != null && !author.isEmpty()) {
                commitsMap.put(AUTHOR, author);
            }

            if (path != null && !path.isEmpty()) {
                commitsMap.put("path", path);
            }

            return successMessage(commitsMap);

        } catch (GHFileNotFoundException e) {
            return failureMessage("Repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    private void addCommitInfo(GHCommit commit, Map<String, Object> commitData) throws IOException {
        commitData.put(SHA, commit.getSHA1());
        commitData.put(MESSAGE, commit.getCommitShortInfo().getMessage());
        commitData.put(HTML_URL, commit.getHtmlUrl().toString());
    }

    private void addAutherDetails(GHCommit commit, Map<String, Object> commitData)
            throws IOException {
        // Author details
        final GHCommit.ShortInfo info = commit.getCommitShortInfo();
        final Map<String, Object> authorData = new HashMap<>();
        authorData.put(NAME, info.getAuthor().getName());
        authorData.put(EMAIL, info.getAuthor().getEmail());
        authorData.put(DATE, info.getAuthoredDate().toString());
        commitData.put(AUTHOR, authorData);
    }

    private void addCommitterDetails(GHCommit commit, Map<String, Object> commitData)
            throws IOException {

        final GHCommit.ShortInfo info = commit.getCommitShortInfo();
        final Map<String, Object> committerData = new HashMap<>();
        committerData.put(NAME, info.getCommitter().getName());
        committerData.put(EMAIL, info.getCommitter().getEmail());
        committerData.put(DATE, info.getCommitDate().toString());
        commitData.put("committer", committerData);
    }

    private void addAutherDetailsAndCommitterDetails(
            GHCommit commit, Map<String, Object> commitData) throws IOException {
        addAutherDetails(commit, commitData);
        addCommitterDetails(commit, commitData);
    }

    private void addParentDetails(GHCommit commit, Map<String, Object> commitData)
            throws IOException {
        final List<Map<String, String>> parentsList = new ArrayList<>();
        for (GHCommit parent : commit.getParents()) {
            Map<String, String> parentData = new HashMap<>();
            parentData.put(SHA, parent.getSHA1());
            parentData.put(HTML_URL, parent.getHtmlUrl().toString());
            parentsList.add(parentData);
        }
        commitData.put("parents", parentsList);
    }

    private void addChangeDetails(GHCommit commit, Map<String, Object> commitData)
            throws IOException {
        final List<Map<String, Object>> filesList = new ArrayList<>();
        for (GHCommit.File file : commit.listFiles()) {
            Map<String, Object> fileData = new HashMap<>();
            fileData.put("filename", file.getFileName());
            fileData.put("status", file.getStatus());
            fileData.put(ADDITIONS, file.getLinesAdded());
            fileData.put(DELETIONS, file.getLinesDeleted());
            fileData.put("changes", file.getLinesChanged());
            fileData.put("patch", file.getPatch());
            filesList.add(fileData);
        }
        commitData.put("files", filesList);
    }

    private void addChangeStats(GHCommit commit, Map<String, Object> commitData)
            throws IOException {
        final Map<String, Integer> statsData = new HashMap<>();
        statsData.put(ADDITIONS, commit.getLinesAdded());
        statsData.put(DELETIONS, commit.getLinesDeleted());
        statsData.put(TOTAL, commit.getLinesChanged());
        commitData.put(STATS, statsData);
    }

    private GHCommitQueryBuilder queryBuilder(
            GHRepository githubRepository, String branch, String author, String path) {
        final GHCommitQueryBuilder queryBuilder = githubRepository.queryCommits();

        if (branch != null && !branch.isEmpty()) {
            queryBuilder.from(branch);
        }

        if (author != null && !author.isEmpty()) {
            queryBuilder.author(author);
        }

        if (path != null && !path.isEmpty()) {
            queryBuilder.path(path);
        }

        return queryBuilder;
    }
}
