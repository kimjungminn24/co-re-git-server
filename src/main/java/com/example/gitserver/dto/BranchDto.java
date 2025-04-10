package com.example.gitserver.dto;

public record BranchDto(
        String name,
        CommitDto lastCommit
) {
}
