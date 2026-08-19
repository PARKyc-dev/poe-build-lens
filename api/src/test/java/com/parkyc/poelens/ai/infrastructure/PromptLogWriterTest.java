package com.parkyc.poelens.ai.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PromptLogWriterTest {

    @TempDir
    Path logDirectory;

    @Test
    void storesPromptAndResponseInATextFile() throws Exception {
        new PromptLogWriter(logDirectory).write("입력 프롬프트", "모델 응답");

        Path logFile = Files.list(logDirectory).findFirst().orElseThrow();
        assertThat(logFile.getFileName().toString()).endsWith(".txt");
        assertThat(Files.readString(logFile)).contains("Prompt:\n입력 프롬프트", "Response:\n모델 응답");
    }
}
