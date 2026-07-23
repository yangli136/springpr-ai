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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHContentBuilder;
import org.kohsuke.github.GHContentSearchBuilder;
import org.kohsuke.github.GHContentUpdateResponse;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitCommit;
import org.kohsuke.github.GitHub;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** Service for GitHub repository content management operations */
@Service
@RequiredArgsConstructor
public class ContentToolService extends BaseToolService {
    private static final String PATH = "path";
    private static final String HTML_URL = "html_url";
    private static final String NAME = "name";
    private static final String SHA = "sha";

    private final GitHub github;

    @Tool(
            description =
                    """
                        Get the contents of a file in a repository.
                        Returns the file content and metadata such as size and sha.
                    """)
    public String getFileContents(
            @ToolParam(description = "Full epository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "Path to the file in the repository") String path,
            @ToolParam(
                            description = "Branch or commit SHA (defaults to the default branch)",
                            required = false)
                    String ref) {
        final Map<String, Object> contentsMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);

            // Get contents, using ref if provided
            final GHContent content;
            if (ref != null && !ref.isEmpty()) {
                content = githubRepository.getFileContent(path, ref);
            } else {
                content = githubRepository.getFileContent(path);
            }

            // Check if it's a file
            if (content.isDirectory()) {
                return failureMessage("Path points to a directory, not a file");
            }

            final Map<String, Object> contentData = getContentDetails(content);
            addDecodedContent(content, contentData);
            contentsMap.put("file", contentData);

            return successMessage(contentsMap);

        } catch (GHFileNotFoundException e) {
            return failureMessage("File or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                    Create or update a file in a repository.
                    If the file doesn't exist, it will be created. If it exists, it will be updated.
                    """)
    public String createOrUpdateFile(
            @ToolParam(description = "Full epository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "Path to the file in the repository") String path,
            @ToolParam(description = "File content") String content,
            @ToolParam(description = "Commit message") String commitMessage,
            @ToolParam(
                            description = "Branch name (defaults to the default branch)",
                            required = false)
                    String branch,
            @ToolParam(
                            description =
                                    "Current file SHA (required for updates, not for new files)",
                            required = false)
                    String sha) {
        final Map<String, Object> contentsMap = new HashMap<>();

        try {

            final GHContentBuilder contentBuilder =
                    createContentBuilder(repository, branch, content, commitMessage, path, sha);

            // Commit the changes
            final GHContentUpdateResponse response = contentBuilder.commit();

            // Prepare response data
            final Map<String, Object> contentData = new HashMap<>();
            contentData.put(PATH, path);

            // Get the commit info
            final GitCommit commit = response.getCommit();
            final Map<String, Object> commitData = new HashMap<>();
            commitData.put(SHA, commit.getSHA1());
            commitData.put(HTML_URL, commit.getHtmlUrl());
            commitData.put("message", commit.getMessage());
            contentData.put("commit", commitData);

            // Get the content info
            GHContent fileContent = response.getContent();
            contentData.put(SHA, fileContent.getSha());
            contentData.put(NAME, fileContent.getName());
            contentData.put(HTML_URL, fileContent.getHtmlUrl());

            contentsMap.put("operation", sha != null ? "update" : "create");
            contentsMap.put("file", contentData);

            return successMessage(contentsMap);

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
                    List contents of a directory in a repository.
                    Returns a list of files and directories at the specified path.
                    """)
    public String listDirectoryContents(
            @ToolParam(description = "Full epository name in format 'owner/repo'")
                    String repository,
            @ToolParam(
                            description =
                                    "Path to the directory in the repository (use '/' for root)",
                            required = false)
                    String path,
            @ToolParam(
                            description = "Branch or commit SHA (defaults to the default branch)",
                            required = false)
                    String ref) {
        final Map<String, Object> contentsMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);

            // Set default path if not provided
            final String dirPath = (path != null && !path.isEmpty()) ? path : "";

            // Get contents, using ref if provided
            final List<GHContent> contents;
            if (ref != null && !ref.isEmpty()) {
                contents = githubRepository.getDirectoryContent(dirPath, ref);
            } else {
                contents = githubRepository.getDirectoryContent(dirPath);
            }

            final List<Map<String, Object>> contentsList = new ArrayList<>();

            for (GHContent content : contents) {
                Map<String, Object> contentData = getContentDetails(content);
                contentData.put("type", content.isDirectory() ? "directory" : "file");
                contentData.put(HTML_URL, content.getHtmlUrl());
                if (!content.isDirectory()) {
                    contentData.put("download_url", content.getDownloadUrl());
                }

                contentsList.add(contentData);
            }

            contentsMap.put("contents", contentsList);
            contentsMap.put(PATH, dirPath);

            return successMessage(contentsMap);

        } catch (GHFileNotFoundException e) {
            return failureMessage("Directory or repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    @Tool(
            description =
                    """
                    Search for code within repositories.
                    Searches GitHub for code matching the query.
                    """)
    public String searchCode(
            @ToolParam(description = "Search query") String query,
            @ToolParam(description = "Full epository name in format 'owner/repo'", required = false)
                    String repository,
            @ToolParam(
                            description = "Filter by file extension (e.g., 'java', 'py')",
                            required = false)
                    String extension,
            @ToolParam(description = "Maximum number of results to return", required = false)
                    Integer limit) {
        final Map<String, Object> contentsMap = new HashMap<>();

        try {
            final StringBuilder finalQuery = this.buildFinalQuery(query, repository, extension);

            final GHContentSearchBuilder searchBuilder =
                    github.searchContent().q(finalQuery.toString());

            final List<Map<String, Object>> resultsList = new ArrayList<>();
            int count = 0;

            int actualLimit = (limit != null && limit > 0) ? limit : 10; // Default to 20 results

            for (GHContent content : searchBuilder.list().withPageSize(actualLimit)) {
                if (count >= actualLimit) {
                    break;
                }

                final Map<String, Object> contentData = getContentDetails(content);

                // Try to get a snippet of content for context
                try {
                    // The content is base64 encoded
                    final String base64Content =
                            new String(content.read().readAllBytes(), StandardCharsets.UTF_8);
                    if (base64Content != null) {
                        final String decodedContent =
                                new String(
                                        Base64.getMimeDecoder().decode(base64Content),
                                        StandardCharsets.UTF_8);

                        // Get a snippet (first 200 chars or less)
                        int snippetLength = Math.min(decodedContent.length(), 200);
                        String snippet = decodedContent.substring(0, snippetLength);
                        if (snippetLength < decodedContent.length()) {
                            snippet += "...";
                        }

                        contentData.put("text_matches", snippet);
                    }
                } catch (RuntimeException e) {
                    // Ignore content retrieval errors for search results
                    contentData.put("text_matches", "[Content unavailable]");
                }

                resultsList.add(contentData);
                count++;
            }

            contentsMap.put("items", resultsList);
            contentsMap.put("count", resultsList.size());
            contentsMap.put("query", finalQuery.toString());

            return successMessage(contentsMap);

        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    private Map<String, Object> getContentDetails(GHContent content) {
        Map<String, Object> contentData = new HashMap<>();
        contentData.put(NAME, content.getName());
        contentData.put(PATH, content.getPath());
        contentData.put("repository", content.getOwner().getFullName());
        contentData.put(HTML_URL, content.getHtmlUrl());
        contentData.put(SHA, content.getSha());
        contentData.put("size", content.getSize());
        return contentData;
    }

    private void addDecodedContent(GHContent content, Map<String, Object> contentData)
            throws IOException {
        // Get and decode content
        String base64Content = new String(content.read().readAllBytes(), StandardCharsets.UTF_8);
        String decodedContent =
                new String(Base64.getMimeDecoder().decode(base64Content), StandardCharsets.UTF_8);
        contentData.put("content", decodedContent);
    }

    private StringBuilder buildFinalQuery(String query, String repository, String extension) {
        final StringBuilder queryBuilder = new StringBuilder(query);

        // Add repository filter if provided
        if (repository != null && !repository.isEmpty()) {
            queryBuilder.append(" repo:").append(repository);
        }

        // Add extension filter if provided
        if (extension != null && !extension.isEmpty()) {
            queryBuilder.append(" extension:").append(extension);
        }

        return queryBuilder;
    }

    private GHContentBuilder createContentBuilder(
            String repository,
            String branch,
            String content,
            String commitMessage,
            String path,
            String sha)
            throws IOException {
        final GHRepository githubRepository = github.getRepository(repository);

        // Determine branch to use
        final String branchToUse =
                (branch != null && !branch.isEmpty())
                        ? branch
                        : githubRepository.getDefaultBranch();

        // Create GHContentBuilder
        final GHContentBuilder contentBuilder =
                githubRepository
                        .createContent()
                        .content(content)
                        .message(commitMessage)
                        .path(path)
                        .branch(branchToUse);

        // Add SHA if updating an existing file
        if (sha != null && !sha.isEmpty()) {
            contentBuilder.sha(sha);
        }

        return contentBuilder;
    }
}
