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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

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

    public synchronized void write(String prompt, String response) {
        try {
            Files.createDirectories(directory);
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            Path file = directory.resolve(date + "-" + String.format("%02d", nextSequence(date)) + ".txt");
            Files.writeString(file, "Prompt:\n" + prompt + "\n\nResponse:\n" + response + "\n", StandardCharsets.UTF_8);
        } catch (IOException exception) {
            log.warn("OpenAI 프롬프트 로그 저장에 실패했습니다: 경로={}", directory, exception);
        }
    }

    private int nextSequence(String date) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.matches(date + "-\\d+\\.txt"))
                    .mapToInt(name -> Integer.parseInt(name.substring(date.length() + 1, name.length() - 4)))
                    .max()
                    .orElse(0) + 1;
        }
    }
}
