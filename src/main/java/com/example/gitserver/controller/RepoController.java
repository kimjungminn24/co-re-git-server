package com.example.gitserver.controller;

import com.example.gitserver.dto.MergeDto;
import com.example.gitserver.dto.RepoInfoDto;
import com.example.gitserver.service.RepoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/repo")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;

    @PostMapping
    public ResponseEntity<Void> createRepo(@Valid @RequestBody RepoInfoDto repoInfoDto) {
        repoService.createRepo(repoInfoDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/{owner}/{repo}")
    public ResponseEntity<Void> mergeBranch(@PathVariable String owner,@PathVariable String repo, @RequestBody MergeDto mergeDto) {
        repoService.mergeBranch(owner,repo,mergeDto);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
