package com.example.gitserver.dto;

public record CompareBranchResponseDto(
        String message,
        String writerName,
        String date,
        String direction
) {

}