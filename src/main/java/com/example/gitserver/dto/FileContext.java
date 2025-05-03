package com.example.gitserver.dto;

import java.io.File;
import java.util.Map;

public record FileContext(
        File gitDir,
        String repoName,
        Map<String, String> statusMap
) {}