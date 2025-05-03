package com.example.gitserver.controller;

import com.example.gitserver.dto.BranchDto;
import com.example.gitserver.dto.CompareBranchResponseDto;
import com.example.gitserver.dto.FileDto;
import com.example.gitserver.dto.RepoInfoDto;
import com.example.gitserver.service.BranchService;
import com.example.gitserver.service.RepoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping("/{owner}/{repo}")
    public ResponseEntity<List<BranchDto>> getBranches(@PathVariable String owner, @PathVariable String repo) {
        List<BranchDto> branches = branchService.getBranches(owner, repo);
        return ResponseEntity.ok(branches);
    }

    @GetMapping("/{owner}/{repo}/compare/{base}/{head}")
    public ResponseEntity<List<CompareBranchResponseDto>> compareBranchHead(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String base,
            @PathVariable String head
    ) {
        List<CompareBranchResponseDto> branches = branchService.compareBranchHead(owner, repo,base,head);
        return ResponseEntity.ok(branches);
    }

    @GetMapping("/{owner}/{repo}/compare/{base}/{head}/file")
    public ResponseEntity<List<FileDto>> getChangeFiles(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String base,
            @PathVariable String head
    ) {
        List<FileDto> branches = branchService.getChangedFiles(owner, repo,base,head);
        return ResponseEntity.ok(branches);
    }



}
