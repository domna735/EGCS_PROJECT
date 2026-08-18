package com.ruoyi.migration.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.migration.model.MigrationAuditEntry;
import com.ruoyi.migration.model.MigrationJobContext;

@Service
public class MigrationAuditService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final Path AUDIT_FILE = Paths.get(System.getProperty("java.io.tmpdir"), "ruoyi-migration-audit.jsonl");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public synchronized MigrationAuditEntry record(MigrationJobContext context, String action, String message, String status, String detail) {
        MigrationAuditEntry entry = new MigrationAuditEntry();
        entry.setTimestamp(LocalDateTime.now().format(TIME_FORMAT));
        entry.setJobId(context == null ? null : context.getJobId());
        entry.setAction(action);
        entry.setMessage(message);
        entry.setSourceEnv(context == null ? null : context.getSourceEnv());
        entry.setTargetEnv(context == null ? null : context.getTargetEnv());
        entry.setStatus(status);
        entry.setDetail(detail);

        if (context != null) {
            context.getAuditEntries().add(entry);
        }

        appendToFile(entry);
        return entry;
    }

    public synchronized List<MigrationAuditEntry> list(String jobId) {
        List<MigrationAuditEntry> entries = readAll();
        if (jobId == null || jobId.trim().isEmpty()) {
            return entries;
        }

        List<MigrationAuditEntry> result = new ArrayList<>();
        for (MigrationAuditEntry entry : entries) {
            if (jobId.equals(entry.getJobId())) {
                result.add(entry);
            }
        }
        return result;
    }

    private void appendToFile(MigrationAuditEntry entry) {
        try {
            Files.createDirectories(AUDIT_FILE.getParent());
            String json = objectMapper.writeValueAsString(entry) + System.lineSeparator();
            Files.write(AUDIT_FILE, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new IllegalStateException("寫入 audit log 失敗", ex);
        }
    }

    private List<MigrationAuditEntry> readAll() {
        if (!Files.exists(AUDIT_FILE)) {
            return Collections.emptyList();
        }

        try {
            List<String> lines = Files.readAllLines(AUDIT_FILE, StandardCharsets.UTF_8);
            List<MigrationAuditEntry> entries = new ArrayList<>();
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                entries.add(objectMapper.readValue(line, MigrationAuditEntry.class));
            }
            return entries;
        } catch (IOException ex) {
            throw new IllegalStateException("讀取 audit log 失敗", ex);
        }
    }
}