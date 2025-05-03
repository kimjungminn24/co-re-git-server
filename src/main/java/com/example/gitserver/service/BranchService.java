package com.example.gitserver.service;

import com.example.gitserver.dto.*;
import com.example.gitserver.util.GitCommandUtil;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

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

    public List<FileDto> getChangedFiles(String owner, String repo, String base, String head) {
        File gitDir = getGitDirectory(owner, repo);
        Map<String, String> statusMap = extractFileStatuses(gitDir, base, head);
        List<String> diffOutput = extractDiffOutput(gitDir, base, head);

        FileContext context = new FileContext(gitDir, repo, statusMap);
        return parseDiffOutput(context, diffOutput);
    }


    private Map<String, String> extractFileStatuses(File gitDir, String base, String head) {
        List<String> command = List.of("git", "diff", "--name-status", base + "..." + head);
        return GitCommandUtil.executeLines(gitDir, command).stream()
                .map(line -> line.trim().split("\\s+"))
                .filter(parts -> parts.length >= 2)
                .collect(Collectors.toMap(parts -> parts[1], parts -> parts[0]));
    }

    private List<String> extractDiffOutput(File gitDir, String base, String head) {
        List<String> command = List.of("git", "diff", "--numstat", "--patch", base + "..." + head);
        return GitCommandUtil.executeLines(gitDir, command);
    }

    private List<FileDto> parseDiffOutput(FileContext context, List<String> diffOutput) {
        List<FileDto> result = new ArrayList<>();

        String filename = null;
        int additions = 0;
        int deletions = 0;
        StringBuilder patch = new StringBuilder();

        for (String line : diffOutput) {
            if (line.matches("^\\d+\\s+\\d+\\s+.+$")) {
                if (filename != null) {
                    result.add(buildFileDto(context, new DiffEntry(filename, additions, deletions, patch.toString())));
                    patch.setLength(0);
                }

                String[] parts = line.split("\\s+", 3);
                additions = Integer.parseInt(parts[0]);
                deletions = Integer.parseInt(parts[1]);
                filename = parts[2];
            } else if (filename != null) {
                patch.append(line).append("\n");
            }
        }

        if (filename != null) {
            result.add(buildFileDto(context, new DiffEntry(filename, additions, deletions, patch.toString())));
        }

        return result;
    }


    private FileDto buildFileDto(FileContext context, DiffEntry entry) {
        String status = context.statusMap().getOrDefault(entry.filename(), "M");
        String sha = computeFileSha(context, entry.filename(), status);
        String content = loadFileContent(context, entry.filename(), status);
        int changes = entry.additions() + entry.deletions();

        return new FileDto(
                entry.filename(),
                sha,
                status,
                entry.additions(),
                entry.deletions(),
                changes,
                entry.patch(),
                content
        );
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
    private String computeFileSha(FileContext context, String filename, String status) {
        if (status.equals("D")) return "DELETED";

        File repoRoot = context.gitDir().getParentFile();
        File file = new File(repoRoot, context.repoName() + File.separator + filename);

        if (!file.exists()) return "MISSING";

        return GitCommandUtil.executeLines(repoRoot, List.of("git", "hash-object", context.repoName() + "/" + filename))
                .stream().findFirst().orElse("UNKNOWN");
    }

    private String loadFileContent(FileContext context, String filename, String status) {
        if (status.equals("D")) return null;

        File repoRoot = context.gitDir().getParentFile();
        File file = new File(repoRoot, context.repoName() + File.separator + filename);

        if (!file.exists()) return null;

        try {
            byte[] contentBytes = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(contentBytes);
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + file.getAbsolutePath(), e);
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
