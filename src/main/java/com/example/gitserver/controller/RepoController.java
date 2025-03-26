package com.example.gitserver.controller;

import com.example.gitserver.dto.RepoInfoDto;
import com.example.gitserver.service.RepoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/repo")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;

    @PostMapping
    public ResponseEntity<Void> createRepo(@Valid @RequestBody RepoInfoDto repoInfoDto){
        repoService.createRepo(repoInfoDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
