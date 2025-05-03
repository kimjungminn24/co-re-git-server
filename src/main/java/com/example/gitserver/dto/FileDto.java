package com.example.gitserver.dto;

public record FileDto(
        String filename,
        String fileSha,
        String status,
        int additions,
        int deletions,
        int changes,
        String patch,
        String content
) {
}
