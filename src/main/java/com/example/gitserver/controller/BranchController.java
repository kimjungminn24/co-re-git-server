package com.example.gitserver.controller;

import com.example.gitserver.dto.BranchDto;
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

    @GetMapping("/branch/{owner}/{repo}")
    public ResponseEntity<List<BranchDto>> getBranches(@PathVariable String owner, @PathVariable String repo) {
        List<BranchDto> branches = branchService.getBranches(owner, repo);
        return ResponseEntity.ok(branches);
    }

}
