package com.example.gitserver.dto;

public record DiffEntry(
        String filename,
        int additions,
        int deletions,
        String patch
) {}
