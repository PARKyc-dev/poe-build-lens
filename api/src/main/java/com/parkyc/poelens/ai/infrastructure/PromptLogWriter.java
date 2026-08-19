package com.parkyc.poelens.ai.infrastructure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Component
public class PromptLogWriter {
    private static final Logger log = LogManager.getLogger(PromptLogWriter.class);
    private final Path directory;

    @Autowired
    public PromptLogWriter(@Value("${poe-lens.openai.prompt-log-directory:../logs/prompt}") String directory) {
        this(Path.of(directory));
    }

    PromptLogWriter(Path directory) {
        this.directory = directory;
    }

    public void write(String prompt, String response) {
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve("prompt-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".txt");
            Files.writeString(file, "Prompt:\n" + prompt + "\n\nResponse:\n" + response + "\n", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.warn("OpenAI 프롬프트 로그 저장에 실패했습니다: 경로={}", directory, exception);
        }
    }
}
