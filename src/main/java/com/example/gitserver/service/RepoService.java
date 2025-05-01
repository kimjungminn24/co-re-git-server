package com.example.gitserver.service;

import com.example.gitserver.dto.MergeDto;
import com.example.gitserver.dto.RepoInfoDto;
import com.example.gitserver.util.GitCommandUtil;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class RepoService {

    public void createRepo(RepoInfoDto info){
        try {
            File repoDir = new File("/home/" + info.userName() + "/" + info.repoName() + ".git");
            repoDir.mkdirs();
            GitCommandUtil.executeLines(repoDir, List.of("git", "init", "--bare"));
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

            File tempParent = tempDir.getParentFile();
            String tempName = tempDir.getName();
            File repoDir = new File(tempParent, tempName);

            GitCommandUtil.executeLines(tempParent, List.of("git", "clone", fileRepoUrl, tempName));
            GitCommandUtil.executeLines(repoDir, List.of("git", "fetch", "origin"));
            GitCommandUtil.executeLines(repoDir, List.of("git", "checkout", "-b", headBranch, "origin/" + headBranch));
            GitCommandUtil.executeLines(repoDir, List.of("git", "checkout", baseBranch));

            switch (mergeMethod.toLowerCase()) {
                case "squash" -> {
                    GitCommandUtil.executeLines(repoDir, List.of("git", "merge", "--squash", headBranch));
                    GitCommandUtil.executeLines(repoDir, List.of("git", "commit", "-m", commitMessage));
                }
                case "rebase" -> {
                    GitCommandUtil.executeLines(repoDir, List.of("git", "rebase", headBranch));
                }
                default -> {
                    GitCommandUtil.executeLines(repoDir, List.of("git", "merge", "--no-ff", "-m", commitMessage, headBranch));
                }
            }
            GitCommandUtil.executeLines(repoDir, List.of("git", "push", "origin", baseBranch));

        } catch (IOException e) {
            throw new RuntimeException("병합 작업 실패 (디렉토리 생성 오류)", e);
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
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
