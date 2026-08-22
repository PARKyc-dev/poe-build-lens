package com.parkyc.poelens.ai.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class PromptLogWriterTest {

    @TempDir
    Path logDirectory;

    @Test
    void storesPromptAndResponseWithTheFirstDailySequenceNumber() throws Exception {
        new PromptLogWriter(logDirectory).write("입력 프롬프트", "모델 응답");

        Path logFile = Files.list(logDirectory).findFirst().orElseThrow();
        assertThat(logFile.getFileName().toString()).matches("\\d{8}-01\\.txt");
        assertThat(Files.readString(logFile)).contains("Prompt:\n입력 프롬프트", "Response:\n모델 응답");
    }

    @Test
    void continuesTheExistingDailySequenceNumber() throws Exception {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Files.writeString(logDirectory.resolve(date + "-07.txt"), "기존 로그");

        new PromptLogWriter(logDirectory).write("입력 프롬프트", "모델 응답");

        assertThat(logDirectory.resolve(date + "-08.txt")).exists();
    }
}
