package com.example.gitserver.dto;

public record CommitDto(
        String sha,
        String message,
        String writerId,
        String date,
        String parent,
        String secondParent
) {
}
