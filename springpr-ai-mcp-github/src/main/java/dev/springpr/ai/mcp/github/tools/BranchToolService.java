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

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRef;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** Service for GitHub branch-related operations */
@Service
@RequiredArgsConstructor
public class BranchToolService extends BaseToolService {
    private static final String SHA = "sha";
    private static final String NAME = "name";

    private final GitHub github;

    @Tool(
            description =
                    """
                    Create a new branch.
                    new branch will be created from a specified SHA or reference (defaults to the default branch if not specified).
                    """)
    public String createNewBranch(
            @ToolParam(description = "full repository name in format 'owner/repo'")
                    String repository,
            @ToolParam(description = "New branch name") String branchName,
            @ToolParam(
                            description =
                                    "SHA or reference to the new branch to be created. (defaults to"
                                            + " default branch)",
                            required = false)
                    String fromRef) {
        final Map<String, Object> newBranchDataMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repository);

            // Check if branch already exists
            try {
                GHBranch existingBranch = githubRepository.getBranch(branchName);
                if (existingBranch != null) {
                    return failureMessage(String.format("Branch %s already exists", branchName));
                }
            } catch (GHFileNotFoundException ex) {
                // Branch doesn't exist that is required.
            }

            // Determine the SHA to create from
            final String sha = getSharFromReference(fromRef, githubRepository);

            // Create the new branch
            final GHRef newBranch = githubRepository.createRef("refs/heads/" + branchName, sha);

            final Map<String, Object> branchData = new HashMap<>();
            branchData.put(NAME, branchName);
            branchData.put(SHA, sha);
            branchData.put("url", newBranch.getUrl().toString());

            newBranchDataMap.put("branch", branchData);

            return successMessage(newBranchDataMap);

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
                    List branches in a repository.
                    Returns a list of branches with details.
                    """)
    public String listBranches(
            @ToolParam(
                            description = "Full repository name in format 'owner/repo'",
                            required = false)
                    String repositoryName,
            @ToolParam(
                            description = "Filter branches by keyword contained in branch name",
                            required = false)
                    String filter,
            @ToolParam(
                            description = "Maximum number of branches are allowed to be returned",
                            required = false)
                    Integer limit) {
        final Map<String, Object> branchListMap = new HashMap<>();

        try {
            final GHRepository githubRepository = github.getRepository(repositoryName);

            final List<Map<String, Object>> branchList =
                    getBranchList(githubRepository, filter, limit);

            branchListMap.put("branches", branchList);
            branchListMap.put("total_count", branchList.size());

            return successMessage(branchListMap);

        } catch (GHFileNotFoundException e) {
            return failureMessage("Repository not found: " + e.getMessage());
        } catch (IOException e) {
            return failureMessage("IO error: " + e.getMessage());
        } catch (Exception e) {
            return failureMessage("Unexpected error: " + e.getMessage());
        }
    }

    private String getSharFromReference(String fromRef, GHRepository githubRepository)
            throws IOException {
        String sha;

        if (fromRef != null && !fromRef.isEmpty()) {
            try {
                // Try to get the SHA from the reference
                GHRef ref = githubRepository.getRef("heads/" + fromRef);
                sha = ref.getObject().getSha();
            } catch (GHFileNotFoundException ex) {
                try {
                    // Try to resolve as a direct SHA
                    GHCommit commit = githubRepository.getCommit(fromRef);
                    sha = commit.getSHA1();
                } catch (Exception e) {
                    return failureMessage("Invalid reference: " + fromRef);
                }
            }
        } else {
            // Use default branch
            String defaultBranch = githubRepository.getDefaultBranch();
            GHRef ref = githubRepository.getRef("heads/" + defaultBranch);
            sha = ref.getObject().getSha();
        }

        return sha;
    }

    private List<Map<String, Object>> getBranchList(
            GHRepository githubRepository, String filter, Integer limit) throws IOException {
        List<Map<String, Object>> branchList = new ArrayList<>();
        int count = 0;

        for (GHBranch branch : githubRepository.getBranches().values()) {
            // Apply filter if provided
            if (filter != null && !filter.isEmpty() && !branch.getName().contains(filter)) {
                continue;
            }

            if (limit != null && count >= limit) {
                break;
            }

            Map<String, Object> branchData = new HashMap<>();
            branchData.put(NAME, branch.getName());
            branchData.put(SHA, branch.getSHA1());

            // Get the latest commit for this branch
            GHCommit commit = githubRepository.getCommit(branch.getSHA1());
            if (commit != null) {
                Map<String, Object> commitData = new HashMap<>();
                commitData.put("message", commit.getCommitShortInfo().getMessage());
                commitData.put("author", commit.getCommitShortInfo().getAuthor().getName());
                commitData.put("date", commit.getCommitShortInfo().getCommitDate().toString());
                branchData.put("latest_commit", commitData);
            }

            // Check if this is the default branch
            branchData.put(
                    "is_default", branch.getName().equals(githubRepository.getDefaultBranch()));

            // Get protection status (requires separate API call)
            try {
                githubRepository.getBranch(branch.getName()).getProtection();
                branchData.put("protected", true);
                // Add protection details if needed
            } catch (GHFileNotFoundException ex) {
                // Branch is not protected
                branchData.put("protected", false);
            } catch (Exception ex) {
                // Ignore other exceptions for protection status
                branchData.put("protected", false);
            }

            branchList.add(branchData);
            count++;
        }
        return branchList;
    }
}
