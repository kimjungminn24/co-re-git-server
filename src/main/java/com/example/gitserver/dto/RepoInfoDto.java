package com.example.gitserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RepoInfoDto(
        @NotBlank
        @Size(min = 1, max = 39)
        @Pattern(
                regexp = "^(?!-)(?!.*--)[a-zA-Z0-9-]+(?<!-)$",
                message = "userName은 영문자, 숫자, 하이픈(-)만 사용 가능하며, 하이픈으로 시작/끝/연속 불가"
        )
        String userName,

        @NotBlank
        @Size(min = 1, max = 100)
        @Pattern(
                regexp = "^(?!\\.)([a-zA-Z0-9._-]+)$",
                message = "repoName은 영문자, 숫자, -, _, .만 허용되며 점(.)으로 시작할 수 없습니다"
        )
        String repoName
) {
}
