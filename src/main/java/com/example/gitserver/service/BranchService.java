package com.example.gitserver.service;

import com.example.gitserver.dto.BranchDto;
import com.example.gitserver.dto.CommitDto;
import com.example.gitserver.dto.CompareBranchResponseDto;
import com.example.gitserver.util.GitCommandUtil;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class BranchService {

    public List<BranchDto> getBranches(String owner, String repo) {
        File gitDir = getGitDirectory(owner, repo);
        List<BranchDto> branchDtos = new ArrayList<>();

        for (String branch : getBranchNames(gitDir)) {
            CommitDto lastCommit = getLastCommitForBranch(gitDir, branch);
            if (lastCommit != null) {
                branchDtos.add(new BranchDto(branch, lastCommit));
            }
        }

        return branchDtos;
    }

    public List<CompareBranchResponseDto> compareBranchHead(String owner, String repo, String base, String head) {
        File gitDir = getGitDirectory(owner, repo);
        List<CompareBranchResponseDto> result = new ArrayList<>();

        List<String> command = List.of(
                "git", "rev-list", "--left-right", "--pretty=format:%H|%s|%an|%cd",
                "--date=iso", base + "..." + head
        );

        try (BufferedReader reader = GitCommandUtil.execute(gitDir, command)) {
            String direction = "unknown";
            String line;
            String commitPrefix = "commit ";

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith(commitPrefix)) {
                    direction = extractDirection(line, commitPrefix);
                } else {
                    result.add(parseCommitInfo(line, direction));
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("브랜치 비교 중 오류 발생", e);
        }

        return result;
    }

    private List<String> getBranchNames(File gitDir) {
        List<String> command = List.of("git", "for-each-ref", "--format=%(refname:short)", "refs/heads/");
        return GitCommandUtil.executeLines(gitDir, command).stream()
                .map(String::trim)
                .toList();
    }


    private CommitDto getLastCommitForBranch(File gitDir, String branch) {
        List<String> command = List.of(
                "git", "log", branch, "-1",
                "--pretty=format:%H|%s|%an|%cd|%P",
                "--date=iso"
        );

        try (BufferedReader reader = GitCommandUtil.execute(gitDir, command)) {
            String line = reader.readLine();
            if (line == null || line.isBlank()) return null;

            String[] parts = line.split("\\|", -1);
            String sha = safePart(parts, 0);
            String message = safePart(parts, 1);
            String author = safePart(parts, 2);
            String date = safePart(parts, 3);
            String[] parents = safePart(parts, 4).split(" ");

            return new CommitDto(sha, message, author, date,
                    parents.length > 0 ? parents[0] : null,
                    parents.length > 1 ? parents[1] : null);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("커밋 정보 조회 실패 (브랜치: " + branch + ")", e);
        }
    }

    private CompareBranchResponseDto parseCommitInfo(String line, String direction) {
        String[] parts = line.split("\\|", -1);
        return new CompareBranchResponseDto(
                safePart(parts, 1), // message
                safePart(parts, 2), // author
                safePart(parts, 3), // date
                direction
        );
    }

    private String extractDirection(String line, String prefix) {
        if (line.startsWith(prefix + ">")) return "head";
        if (line.startsWith(prefix + "<")) return "base";
        return "unknown";
    }

    private String safePart(String[] parts, int index) {
        return (index < parts.length) ? parts[index] : "";
    }

    private File getGitDirectory(String owner, String repo) {
        return new File("/home/" + owner + "/" + repo + ".git");
    }
}
