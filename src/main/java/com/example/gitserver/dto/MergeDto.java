package com.example.gitserver.dto;

public record MergeDto(
        String base,
        String head,
        String commitTitle,
        String commitMessage,
        String mergeMethod
) {
}
