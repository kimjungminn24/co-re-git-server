package com.example.gitserver.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GitCommandUtil {

    private GitCommandUtil(){

    }

    public static BufferedReader execute(File directory, List<String> command)
            throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(directory);

        Process process = pb.start();

        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroy();
            throw new RuntimeException("Git 명령어 실행 시간 초과: " + String.join(" ", command));
        }

        if (process.exitValue() != 0) {
            String error = readErrorOutput(process);
            throw new RuntimeException("Git 명령어 실행 실패: " + String.join(" ", command) + "\n" + error);
        }

        return new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    }

    private static String readErrorOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public static List<String> executeLines(File directory, List<String> command) {
        try (BufferedReader reader = execute(directory, command)) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Git 명령어 실행 중 오류 발생: " + String.join(" ", command), e);
        }
    }

    public static String executeAndGetOutput(File directory, List<String> command) {
        return String.join("\n", executeLines(directory, command));
    }

}
