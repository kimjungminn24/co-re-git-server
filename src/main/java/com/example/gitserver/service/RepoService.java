package com.example.gitserver.service;


import com.example.gitserver.dto.RepoInfoDto;
import org.springframework.stereotype.Service;

import java.io.File;

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
}
