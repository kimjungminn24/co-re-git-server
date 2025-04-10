package com.example.gitserver.service;

import com.example.gitserver.dto.BranchDto;
import com.example.gitserver.dto.CommitDto;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BranchService {
    public List<BranchDto> getBranches(String owner, String repo) {
        String repoPath = "/home/" + owner + "/" + repo + ".git";
        File gitDir = new File(repoPath);

        List<String> branchNames = getBranchNames(gitDir);
        List<BranchDto> branchDtos = new ArrayList<>();

        for (String branch : branchNames) {
            CommitDto lastCommit = getLastCommitForBranch(gitDir, branch);
            if (lastCommit != null) {
                branchDtos.add(new BranchDto(branch, lastCommit));
            }
        }

        return branchDtos;
    }

    private List<String> getBranchNames(File gitDir) {
        List<String> branches = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder("git", "for-each-ref", "--format=%(refname:short)", "refs/heads/");
        pb.directory(gitDir);

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    branches.add(line.trim());
                }
            }

            if (process.waitFor() != 0) {
                throw new RuntimeException("브랜치 목록 조회 실패: " + readErrorOutput(process));
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("브랜치 정보를 가져오는 중 오류 발생", e);
        }

        return branches;
    }

    private CommitDto getLastCommitForBranch(File gitDir, String branch) {
        ProcessBuilder pb = new ProcessBuilder(
                "git", "log", branch, "-1",
                "--pretty=format:%H|%s|%an|%cd|%P",
                "--date=iso"
        );
        pb.directory(gitDir);

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line == null || line.isBlank()) {
                    return null;
                }

                String[] parts = line.split("\\|", -1);
                String sha = safePart(parts, 0);
                String message = safePart(parts, 1);
                String author = safePart(parts, 2);
                String date = safePart(parts, 3);
                String[] parents = safePart(parts, 4).split(" ");

                String parent = parents.length > 0 ? parents[0] : null;
                String secondParent = parents.length > 1 ? parents[1] : null;

                return new CommitDto(sha, message, author, date, parent, secondParent);
            }

        } catch (IOException e) {
            throw new RuntimeException("커밋 정보 조회 실패 (브랜치: " + branch + ")", e);
        }
    }

    private String readErrorOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private String safePart(String[] parts, int index) {
        return (index < parts.length) ? parts[index] : "";
    }
}
