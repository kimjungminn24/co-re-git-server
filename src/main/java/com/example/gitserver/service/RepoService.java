package com.example.gitserver.service;


import com.example.gitserver.dto.BranchDto;
import com.example.gitserver.dto.CommitDto;
import com.example.gitserver.dto.MergeDto;
import com.example.gitserver.dto.RepoInfoDto;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RepoService {

    public void createRepo(RepoInfoDto info){
        try {
            File repoDir = new File("/home/" + info.userName() + "/" + info.repoName() + ".git");
            repoDir.mkdirs();
            ProcessBuilder pb = new ProcessBuilder("git", "init", "--bare");
            pb.directory(repoDir);
            Process process = pb.inheritIO().start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public void mergeBranch(String owner, String repo, MergeDto mergeDto) {
        String baseBranch = mergeDto.base();
        String headBranch = mergeDto.head();
        String commitMessage = mergeDto.commitTitle() + "\n\n" + mergeDto.commitMessage();
        String mergeMethod = mergeDto.mergeMethod(); // "merge", "squash", "rebase"

        String repoPath = "/home/" + owner + "/" + repo + ".git";
        String fileRepoUrl = "file:///" + repoPath.replace("\\", "/");

        File tempDir = null;

        try {
            tempDir = Files.createTempDirectory("git-merge-").toFile();

            runGit(tempDir.getParentFile(), "git", "clone", fileRepoUrl, tempDir.getName());
            File repoDir = new File(tempDir.getParent(), tempDir.getName());

            runGit(repoDir, "git", "fetch", "origin");

            runGit(repoDir, "git", "checkout", "-b", headBranch, "origin/" + headBranch);

            runGit(repoDir, "git", "checkout", baseBranch);

            switch (mergeMethod.toLowerCase()) {
                case "squash" -> {
                    runGit(repoDir, "git", "merge", "--squash", headBranch);
                    runGit(repoDir, "git", "commit", "-m", commitMessage);
                }
                case "rebase" -> {
                    runGit(repoDir, "git", "rebase", headBranch);
                }
                default -> {
                    runGit(repoDir, "git", "merge", "--no-ff", "-m", commitMessage, headBranch);
                }
            }

            runGit(repoDir, "git", "push", "origin", baseBranch);

        } catch (IOException e) {
            throw new RuntimeException("병합 작업 실패 (디렉토리 생성 오류)", e);
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }
    }

    private void runGit(File dir, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Git 명령 실패: " + String.join(" ", command) + "\n" + output);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Git 명령 실행 중 예외 발생", e);
        }
    }

    private void deleteDirectory(File dir) {
        try (Stream<Path> walk = Files.walk(dir.toPath())) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            System.err.println("임시 디렉토리 삭제 실패: " + e.getMessage());
        }
    }


}
